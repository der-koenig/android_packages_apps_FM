/*
 * Copyright (c) 2009, Code Aurora Forum. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *        * Redistributions of source code must retain the above copyright
 *            notice, this list of conditions and the following disclaimer.
 *        * Redistributions in binary form must reproduce the above copyright
 *            notice, this list of conditions and the following disclaimer in the
 *            documentation and/or other materials provided with the distribution.
 *        * Neither the name of Code Aurora nor
 *            the names of its contributors may be used to endorse or promote
 *            products derived from this software without specific prior written
 *            permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NON-INFRINGEMENT ARE DISCLAIMED.    IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.quicinc.fmradio;

import java.lang.ref.WeakReference;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RemoteViews;

import android.hardware.fmradio.FmReceiver;
import android.hardware.fmradio.FmTransmitter;
import android.hardware.fmradio.FmRxEvCallbacksAdaptor;
import android.hardware.fmradio.FmRxRdsData;
import android.hardware.fmradio.FmConfig;

/**
 * Provides "background" FM Radio (that uses the hardware) capabilities,
 * allowing the user to switch between activities without stopping playback.
 */
public class FMTransmitterService extends Service
{
   private static final int FMTRANSMITTERSERVICE_STATUS = 102;
   private static final int FM_TX_RDS_TX_PROGRAM_TYPE = 0;
   private static final int FM_TX_RDS_TX_PS_REPEAT_COUNT = 0;

   private static final String FMRADIO_DEVICE_FD_STRING = "/dev/radio0";
   private static final String LOGTAG = "FMTxService";//FMRadio.LOGTAG;

   private static FmReceiver mReceiver;
   private static FmTransmitter mTransmitter;
   private int mTunedFrequency = 0;

   private static FmSharedPreferences mPrefs;
   private IFMTransmitterServiceCallbacks mCallbacks;
   private WakeLock mWakeLock;
   private int mServiceStartId = -1;
   private boolean mServiceInUse = false;
   private boolean mMuted = false;
   private boolean mResumeAfterCall = false;

   private boolean mFMOn = false;
   private int mFMSearchStations = 0;

   private FmRxRdsData mFMRxRDSData=null;
   // interval after which we stop the service when idle
   private static final int IDLE_DELAY = 60000;

   public FMTransmitterService() {
   }

   @Override
   public void onCreate() {
      super.onCreate();

      mCallbacks = null;
      mPrefs = new FmSharedPreferences(this);

      //TelephonyManager tmgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
      //tmgr.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
      PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
      mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this.getClass().getName());
      mWakeLock.setReferenceCounted(false);

      // If the service was idle, but got killed before it stopped itself, the
      // system will relaunch it. Make sure it gets stopped again in that case.
      Message msg = mDelayedStopHandler.obtainMessage();
      mDelayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
   }

   @Override
   public void onDestroy() {
      Log.d(LOGTAG, "onDestroy");
      if (isFmOn())
      {
         Log.e(LOGTAG, "Service being destroyed while still playing.");
      }

      // make sure there aren't any other messages coming
      mDelayedStopHandler.removeCallbacksAndMessages(null);

      /* Since the service is closing, disable the receiver */
      fmOff();

      //TelephonyManager tmgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
      //tmgr.listen(mPhoneStateListener, 0);

      mWakeLock.release();
      super.onDestroy();
   }

   @Override
   public IBinder onBind(Intent intent) {
      mDelayedStopHandler.removeCallbacksAndMessages(null);
      mServiceInUse = true;
      Log.d(LOGTAG, "onBind");
      return mBinder;
   }

   @Override
   public void onRebind(Intent intent) {
      mDelayedStopHandler.removeCallbacksAndMessages(null);
      mServiceInUse = true;
      Log.d(LOGTAG, "onRebind");
   }

   @Override
   public void onStart(Intent intent, int startId) {
      Log.d(LOGTAG, "onStart");
      mServiceStartId = startId;
      mDelayedStopHandler.removeCallbacksAndMessages(null);

      // make sure the service will shut down on its own if it was
      // just started but not bound to and nothing is playing
      mDelayedStopHandler.removeCallbacksAndMessages(null);
      Message msg = mDelayedStopHandler.obtainMessage();
      mDelayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
   }

   @Override
   public boolean onUnbind(Intent intent) {
      mServiceInUse = false;
      Log.d(LOGTAG, "onUnbind");

      if (isFmOn())
      {
         // something is currently playing, or will be playing once
         // an in-progress call ends, so don't stop the service now.
         return true;
      }

      stopSelf(mServiceStartId);
      return true;
   }

   /* Handle Phone Call + FM Concurrency */
   private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
      @Override
      public void onCallStateChanged(int state, String incomingNumber) {
         if (state == TelephonyManager.CALL_STATE_RINGING)
         {
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            int ringvolume = audioManager.getStreamVolume(AudioManager.STREAM_RING);
            if (ringvolume > 0)
            {
               mResumeAfterCall = true;
            }
         } //ringing
         else if (state == TelephonyManager.CALL_STATE_OFFHOOK)
         {
            // pause the music while a conversation is in progress
            mResumeAfterCall = true;
         } //offhook
         else if (state == TelephonyManager.CALL_STATE_IDLE)
         {
            // start playing again
            if (mResumeAfterCall)
            {
               // resume playback only if FM Radio was playing
               // when the call was answered
               //unMute-FM
               mResumeAfterCall = false;
            }
         }//idle
      }
   };

   private Handler mDelayedStopHandler = new Handler() {
      @Override
      public void handleMessage(Message msg) {
         // Check again to make sure nothing is playing right now
         if (isFmOn() || mServiceInUse)
         {
            return;
         }
         Log.d(LOGTAG, "mDelayedStopHandler: stopSelf");
         stopSelf(mServiceStartId);
      }
   };

   /* Show the FM Notification */
   public void startNotification() {
      RemoteViews views = new RemoteViews(getPackageName(), R.layout.statusbar);
      views.setImageViewResource(R.id.icon, R.drawable.ic_status_fm_tx);
      if (isFmOn())
      {
         views.setTextViewText(R.id.frequency, getTunedFrequencyString());
      } else
      {
         views.setTextViewText(R.id.frequency, "");
      }

      Notification status = new Notification();
      status.contentView = views;
      status.flags |= Notification.FLAG_ONGOING_EVENT;
      status.icon = R.drawable.ic_status_fm_tx;
      status.contentIntent = PendingIntent.getActivity(this, 0,
                                                       new Intent("com.quicinc.fmradio.FMTRANSMITTER_ACTIVITY"), 0);
      startForeground(FMTRANSMITTERSERVICE_STATUS, status);
      //NotificationManager nm = (NotificationManager)
      //                         getSystemService(Context.NOTIFICATION_SERVICE);
      //nm.notify(FMTRANSMITTERSERVICE_STATUS, status);
      //setForeground(true);
      mFMOn = true;
   }

   private void stop() {
      gotoIdleState();
      mFMOn = false;
   }

   private void gotoIdleState() {
      mDelayedStopHandler.removeCallbacksAndMessages(null);
      Message msg = mDelayedStopHandler.obtainMessage();
      mDelayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
      //NotificationManager nm =
      //(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
      //nm.cancel(FMTRANSMITTERSERVICE_STATUS);
      //setForeground(false);
      stopForeground(true);
   }

   /*
    * By making this a static class with a WeakReference to the Service, we
    * ensure that the Service can be GCd even when the system process still
    * has a remote reference to the stub.
    */
   static class ServiceStub extends IFMTransmitterService.Stub
   {
      WeakReference<FMTransmitterService> mService;

      ServiceStub(FMTransmitterService service)
      {
         mService = new WeakReference<FMTransmitterService>(service);
      }

      public boolean fmOn() throws RemoteException
      {
         return(mService.get().fmOn());
      }

      public boolean fmOff() throws RemoteException
      {
         return(mService.get().fmOff());
      }
      public boolean fmRestart() throws RemoteException
      {
         return(mService.get().fmRestart());
      }

      public boolean isFmOn()
      {
         return(mService.get().isFmOn());
      }
      public boolean fmReconfigure()
      {
         return(mService.get().fmReconfigure());
      }

      public void registerCallbacks(IFMTransmitterServiceCallbacks cb)
      throws RemoteException {
         mService.get().registerCallbacks(cb);
      }

      public boolean searchWeakStationList(int numStations)
      throws RemoteException {
         return(mService.get().searchWeakStationList(numStations));
      }

      public void unregisterCallbacks() throws RemoteException
      {
         mService.get().unregisterCallbacks();
      }

      public boolean tune(int frequency)
      {
         return(mService.get().tune(frequency));
      }

      public boolean cancelSearch()
      {
         return(mService.get().cancelSearch());
      }

      public String getRadioText()
      {
         return(mService.get().getRadioText());
      }

      public int[] getSearchList()
      {
         return(mService.get().getSearchList());
      }

      public boolean isInternalAntennaAvailable()
      {
         return(mService.get().isInternalAntennaAvailable());
      }
   }

   private final IBinder mBinder = new ServiceStub(this);
   /*
    * Turn ON FM: Powers up FM hardware, and initializes the FM module
    *                                                                                 .
    * @return true if fm Enable api was invoked successfully, false if the api failed.
    */
   private boolean fmOn() {
      boolean bStatus=false;
      Log.d(LOGTAG, "fmOn");
      mTransmitter = new FmTransmitter(FMRADIO_DEVICE_FD_STRING, fmCallbacks);
      //mTransmitter = new FmTransmitter(FMRADIO_DEVICE_FD_STRING, transmitCallbacks);

      if (mTransmitter == null)
      {
         throw new RuntimeException("FmTransmitter service not available!");
      }
      if (mTransmitter != null)
      {
         // This sets up the FM radio device
         FmConfig config = FmSharedPreferences.getFMConfiguration();
         Log.d(LOGTAG, "fmOn: RadioBand   :"+ config.getRadioBand());
         Log.d(LOGTAG, "fmOn: Emphasis    :"+ config.getEmphasis());
         Log.d(LOGTAG, "fmOn: ChSpacing   :"+ config.getChSpacing());
         Log.d(LOGTAG, "fmOn: RdsStd      :"+ config.getRdsStd());
         Log.d(LOGTAG, "fmOn: LowerLimit  :"+ config.getLowerLimit());
         Log.d(LOGTAG, "fmOn: UpperLimit  :"+ config.getUpperLimit());
         bStatus = mTransmitter.enable(config);
         //AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
         //if (audioManager != null)
         {
            //Log.d(LOGTAG, "mAudioManager.setFmRadioOn = true \n" );
            //TEST ONLY:
            //audioManager.setParameters("FMRadioOn=true");
            //Log.d(LOGTAG, "mAudioManager.setFmRadioOn done \n" );
         }
         startNotification();
         bStatus = true;
      }
      return(bStatus);
   }

   /*
    * Turn OFF FM: Disable the FM Host and hardware                                  .
    *                                                                                 .
    * @return true if fm Disable api was invoked successfully, false if the api failed.
    */
   private boolean fmOff() {
      boolean bStatus=false;
      Log.d(LOGTAG, "fmOff" );
      //AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
      //if (audioManager != null)
      {
         //Log.d(LOGTAG, "audioManager.setFmRadioOn = false \n" );
         //TEST ONLY:
         //audioManager.setParameters("FMRadioOn=false");
         //Log.d(LOGTAG, "audioManager.setFmRadioOn false done \n" );
      }
      // This will disable the FM radio device
      if (mTransmitter != null)
      {
         bStatus = mTransmitter.disable();
         mTransmitter = null;
      }
      /* Disable Receiver */
      if (mReceiver != null)
      {
         bStatus = mReceiver.disable();
         mReceiver = null;
      }
      stop();
      return(bStatus);
   }


   /*
    * Restart FM Transmitter: Disables FM receiver mode or transmitter is already active
    * and Powers up FM hardware, and initializes the FM module
    *
    * @return true if fm Enable api was invoked successfully, false if the api failed.
    */

   private boolean fmRestart() {
      boolean bStatus=false;
      Log.d(LOGTAG, "fmRestart");

      /* First Disable Transmitter, if enabled */
      if (mTransmitter != null)
      {
         bStatus = mTransmitter.disable();
         mTransmitter = null;
      }

      /* Disable Receiver */
      if (mReceiver != null)
      {
         bStatus = mReceiver.disable();
         mReceiver = null;
      }
      bStatus = fmOn();
      return(bStatus);
   }

   /* Returns whether FM hardware is ON.
    *
    * @return true if FM was tuned, searching. (at the end of
    * the search FM goes back to tuned).
    *
    */
   public boolean isFmOn() {
      return mFMOn;
   }

   /*
    *  ReConfigure the FM Setup parameters
    *  - Band
    *  - Channel Spacing (50/100/200 KHz)
    *  - Emphasis (50/75)
    *  - Frequency limits
    *  - RDS/RBDS standard
    *
    * @return true if configure api was invoked successfully, false if the api failed.
    */
   public boolean fmReconfigure() {
      boolean bStatus=false;
      Log.d(LOGTAG, "fmReconfigure");
      if (mTransmitter != null)
      {
         // This sets up the FM radio device
         FmConfig config = FmSharedPreferences.getFMConfiguration();
         Log.d(LOGTAG, "RadioBand   :"+ config.getRadioBand());
         Log.d(LOGTAG, "Emphasis    :"+ config.getEmphasis());
         Log.d(LOGTAG, "ChSpacing   :"+ config.getChSpacing());
         Log.d(LOGTAG, "RdsStd      :"+ config.getRdsStd());
         Log.d(LOGTAG, "LowerLimit  :"+ config.getLowerLimit());
         Log.d(LOGTAG, "UpperLimit  :"+ config.getUpperLimit());
         bStatus = mTransmitter.configure(config);
      }
      return(bStatus);
   }

   /*
    * Register UI/Activity Callbacks
    */
   public void registerCallbacks(IFMTransmitterServiceCallbacks cb)
   {
      mCallbacks = cb;
   }

   /*
    *  unRegister UI/Activity Callbacks
    */
   public void unregisterCallbacks()
   {
      mCallbacks=null;
   }

   /* Tunes to the specified frequency
    *
    * @return true if Tune command was invoked successfully, false if not muted.
    *  Note: Callback FmRxEvRadioTuneStatus will be called when the tune
    *        is complete
    */
   public boolean tune(int frequency) {
      boolean bCommandSent=false;
      double doubleFrequency = frequency/1000.00;

      Log.d(LOGTAG, "tune:  " + doubleFrequency);
      if (mTransmitter != null)
      {
         mTransmitter.setStation(frequency);
         mTunedFrequency = frequency;
         bCommandSent = true;

         /**
          * Test only till APIs are implemented
          */
         if (mCallbacks != null)
         {
            try
            {
               mCallbacks.onTuneStatusChanged(frequency);
            } catch (RemoteException e)
            {
               e.printStackTrace();
            }
         }
         /* Update the frequency in the StatusBar's Notification */
         startNotification();

      }
      return bCommandSent;
   }

   /* Search for the 'numStations' number of weak FM Stations.
    *
    * It searches in the forward direction relative to the current tuned station.
    * int numStations: maximum number of stations to search.
    *
    * @return true if Search command was invoked successfully, false if not muted.
    *  Note: 1. Callback FmRxEvSearchListComplete will be called when the Search
    *        is complete
    *        2. Callback FmRxEvRadioTuneStatus will also be called when tuned to
    *        the previously tuned station.
    */
   public boolean searchWeakStationList(int numStations)
   {
      boolean bStatus=false;
      /* First Disable Transmitter */
      if (mTransmitter != null)
      {
         bStatus = mTransmitter.disable();
         mTransmitter = null;
      }

      mReceiver = new FmReceiver(FMRADIO_DEVICE_FD_STRING, fmCallbacks);
      if (mReceiver == null)
      {
         throw new RuntimeException("FmReceiver service not available!");
      }
      if (mReceiver != null)
      {
         // This sets up the FM radio device
         FmConfig config = FmSharedPreferences.getFMConfiguration();
         Log.d(LOGTAG, "RadioBand   :"+ config.getRadioBand());
         Log.d(LOGTAG, "Emphasis    :"+ config.getEmphasis());
         Log.d(LOGTAG, "ChSpacing   :"+ config.getChSpacing());
         Log.d(LOGTAG, "RdsStd      :"+ config.getRdsStd());
         Log.d(LOGTAG, "LowerLimit  :"+ config.getLowerLimit());
         Log.d(LOGTAG, "UpperLimit  :"+ config.getUpperLimit());
         bStatus = mReceiver.enable(config);
         Log.d(LOGTAG, "mReceiver.enable:  bStatus: " + bStatus);
         bStatus = mReceiver.setMuteMode(FmReceiver.FM_RX_MUTE);
         Log.d(LOGTAG, "mReceiver.setMuteMode:  bStatus: " + bStatus);
         bStatus = mReceiver.setStation(config.getLowerLimit());
         Log.d(LOGTAG, "mReceiver.setStation:  bStatus: " + bStatus);
         bStatus = mReceiver.searchStationList(FmReceiver.FM_RX_SRCHLIST_MODE_WEAK,
                                               FmReceiver.FM_RX_SEARCHDIR_UP,
                                               numStations,//mFMSearchStations,
                                               0);

         mFMSearchStations = 0;//numStations;
      }
      return bStatus;
   }

   /* Cancel any ongoing Search (Seek/Scan/SearchStationList).
    *
    * @return true if Search command was invoked successfully, false if not muted.
    *  Note: 1. Callback FmRxEvSearchComplete will be called when the Search
    *        is complete/cancelled.
    *        2. Callback FmRxEvRadioTuneStatus will also be called when tuned to a station
    *        at the end of the Search or if the seach was cancelled.
    */
   public boolean cancelSearch()
   {
      boolean bStatus=false;
      if (mReceiver != null)
      {
         bStatus = mReceiver.cancelSearch();
         Log.d(LOGTAG, "mReceiver.cancelSearch: bStatus: " + bStatus);
         try
         {
            if (mCallbacks != null)
            {
               mCallbacks.onSearchListComplete(false);
            }
         } catch (RemoteException e)
         {
            e.printStackTrace();
         }

      }
      return bStatus;
   }

   /* Retrieves the RDS Radio Text (RT) String being transmitted
    *
    * @return String - RDS RT String.
    *  Note: 1. This is a synchronous call that should typically called when
    *           Callback FmRxEvRdsRtInfo is invoked.
    *        2. Since RT contains multiple fields, this Service reads all the fields and "caches"
    *        the values and provides this helper routine for the Activity to get only the information it needs.
    *        3. The "cached" data fields are always "cleared" when the tune status changes.
    */
   public String getRadioText() {
      String str = "Radio Text: Transmitting ...";
      Log.d(LOGTAG, "Radio Text: [" + str + "]");
      return str;
   }

   /* Retrieves the station list from the SearchStationlist.
    *
    * @return Array of integers that represents the station frequencies.
    * Note: 1. This is a synchronous call that should typically called when
    *           Callback onSearchListComplete.
    */
   public int[] getSearchList()
   {
      int[] frequencyList = null;
      if (mReceiver != null)
      {
         Log.d(LOGTAG, "getSearchList: ");
         frequencyList = mReceiver.getStationList();
      }
      return frequencyList;
   }
   /** Determines if an internal Antenna is available.
    *
    * @return true if internal antenna is available, false if
    *         internal antenna is not available.
    */
   public boolean isInternalAntennaAvailable()
   {
      boolean bAvailable  = false;
      /* Update this when the API is available */
      bAvailable = true;
      //bAvailable = FmReceiver.internalAntennaAvailable();
      Log.d(LOGTAG, "internalAntennaAvailable: " + bAvailable);
      return bAvailable;
   }

   /*
   private FmTransmitter.TransmitterCallbacks transmitCallbacks = new  FmTransmitter.TransmitterCallbacks() {
      public void onRDSGroupsAvailable() {
         // TODO Auto-generated method stub

      }

      public void onRDSGroupsComplete() {
         // TODO Auto-generated method stub

      }

      public void onTuneFrequencyChange(int freq) {
         // TODO Auto-generated method stub

      }
   };*/

   /* Receiver callbacks back from the FM Stack */
   FmRxEvCallbacksAdaptor fmCallbacks = new FmRxEvCallbacksAdaptor()
   {
      public void FmRxEvEnableReceiver()
      {
         Log.d(LOGTAG, "FmRxEvEnableReceiver");
      }

      public void FmRxEvDisableReceiver()
      {
         Log.d(LOGTAG, "FmRxEvDisableReceiver");
      }

      public void FmRxEvRadioTuneStatus(int frequency)
      {
         Log.d(LOGTAG, "FmRxEvRadioTuneStatus: Tuned Frequency: " +frequency);
         if(mFMSearchStations>0)
         {
            Log.d(LOGTAG, "searchWeakStationList:  numStations: " + mFMSearchStations);
            boolean bStatus = mReceiver.searchStationList(FmReceiver.FM_RX_SRCHLIST_MODE_WEAK,
                                                  FmReceiver.FM_RX_SEARCHDIR_UP,
                                                  mFMSearchStations,
                                                  0);
            mFMSearchStations = 0;
            Log.d(LOGTAG, "searchWeakStationList:  bStatus: " + bStatus);
            /**
             * If search could not be initiated, let the UI know
             */
            if(bStatus == false)
            {
               try
               {
                  if (mCallbacks != null)
                  {
                     mCallbacks.onSearchListComplete(false);
                  }
               } catch (RemoteException e)
               {
                  e.printStackTrace();
               }
            }
         }
      }

      public void FmRxEvSearchListComplete()
      {
         Log.d(LOGTAG, "FmRxEvSearchListComplete");
         try
         {
            if (mCallbacks != null)
            {
               mCallbacks.onSearchListComplete(true);
            }
         } catch (RemoteException e)
         {
            e.printStackTrace();
         }
      }
   };


   /*
    *  Read the Tuned Frequency from the FM module.
    */
   private String getTunedFrequencyString() {
      double frequency = mTunedFrequency / 1000.0;
      String frequencyString = getString(R.string.stat_notif_tx_frequency, (""+frequency));
      return frequencyString;
   }
}
