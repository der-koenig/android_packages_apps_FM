/*
 * Copyright (c) 2009-2012, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *        * Redistributions of source code must retain the above copyright
 *            notice, this list of conditions and the following disclaimer.
 *        * Redistributions in binary form must reproduce the above copyright
 *            notice, this list of conditions and the following disclaimer in the
 *            documentation and/or other materials provided with the distribution.
 *        * Neither the name of The Linux Foundation nor
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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.ListPreference;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceCategory;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;

import android.util.Log;

public class Settings extends PreferenceActivity implements
                OnSharedPreferenceChangeListener, OnPreferenceClickListener {
        public static final String RX_MODE = "rx_mode";


        public static final String REGIONAL_BAND_KEY = "regional_band";
        public static final String AUDIO_OUTPUT_KEY = "audio_output_mode";
        public static final String RECORD_DURATION_KEY = "record_duration";
        public static final String AUTO_AF = "af_checkbox_preference";
        public static final String RESTORE_FACTORY_DEFAULT = "revert_to_fac";
        public static final int RESTORE_FACTORY_DEFAULT_INT = 1;
        public static final String RESTORE_FACTORY_DEFAULT_ACTION = "com.quicinc.fmradio.settings.revert_to_defaults";

        private static final String LOGTAG = FMRadio.LOGTAG;

        private ListPreference mBandPreference;
        private ListPreference mAudioPreference;
        private ListPreference mRecordDurPreference;
        private CheckBoxPreference mAfPref;
        private Preference mRestoreDefaultPreference;

        private FmSharedPreferences mPrefs = null;
        private boolean mRxMode = false;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                Intent intent = getIntent();
                if (intent != null) {
                        mRxMode = intent.getBooleanExtra(RX_MODE, false);
                }
                mPrefs = new FmSharedPreferences(this);
                if (mPrefs != null) {
                        setPreferenceScreen(createPreferenceHierarchy());
                }
        }

        private PreferenceScreen createPreferenceHierarchy() {
                int index = 0;
                if (mPrefs == null) {
                        return null;
                }
                // Root
                PreferenceScreen root = getPreferenceManager().createPreferenceScreen(
                                this);

                // Band/Country
                String[] summaryBandItems = getResources().getStringArray(
                                R.array.regional_band_summary);
                mBandPreference = new ListPreference(this);
                mBandPreference.setEntries(R.array.regional_band_entries);
                mBandPreference.setEntryValues(R.array.regional_band_values);
                mBandPreference.setDialogTitle(R.string.sel_band_menu);
                mBandPreference.setKey(REGIONAL_BAND_KEY);
                mBandPreference.setTitle(R.string.regional_band);
                index = FmSharedPreferences.getCountry();
                Log.d(LOGTAG, "createPreferenceHierarchy: Country: " + index);
                // Get the preference and list the value.
                if ((index < 0) || (index >= summaryBandItems.length)) {
                        index = 0;
                }
                Log.d(LOGTAG, "createPreferenceHierarchy: CountrySummary: "
                                + summaryBandItems[index]);
                mBandPreference.setSummary(summaryBandItems[index]);
                mBandPreference.setValueIndex(index);
                root.addPreference(mBandPreference);

                if (mRxMode) {
                        // Audio Output (Stereo or Mono)
                        String[] summaryAudioModeItems = getResources().getStringArray(
                                        R.array.ster_mon_entries);
                        mAudioPreference = new ListPreference(this);
                        mAudioPreference.setEntries(R.array.ster_mon_entries);
                        mAudioPreference.setEntryValues(R.array.ster_mon_values);
                        mAudioPreference.setDialogTitle(R.string.sel_audio_output);
                        mAudioPreference.setKey(AUDIO_OUTPUT_KEY);
                        mAudioPreference.setTitle(R.string.aud_output_mode);
                        boolean audiomode = FmSharedPreferences.getAudioOutputMode();
                        if (audiomode) {
                                index = 0;
                        } else {
                                index = 1;
                        }
                        Log.d(LOGTAG, "createPreferenceHierarchy: audiomode: " + audiomode);
                        mAudioPreference.setSummary(summaryAudioModeItems[index]);
                        mAudioPreference.setValueIndex(index);
                        root.addPreference(mAudioPreference);

                        // AF Auto Enable (Checkbox)
                        mAfPref = new CheckBoxPreference(this);
                        mAfPref.setKey(AUTO_AF);
                        mAfPref.setTitle(R.string.auto_select_af);
                        mAfPref.setSummaryOn(R.string.auto_select_af_enabled);
                        mAfPref.setSummaryOff(R.string.auto_select_af_disabled);
                        boolean bAFAutoSwitch = FmSharedPreferences.getAutoAFSwitch();
                        Log.d(LOGTAG, "createPreferenceHierarchy: bAFAutoSwitch: "
                                        + bAFAutoSwitch);
                        mAfPref.setChecked(bAFAutoSwitch);
                        root.addPreference(mAfPref);


         if(FMRadio.RECORDING_ENABLE)
         {
            String[] summaryRecordItems = getResources().getStringArray(
                  R.array.record_durations_entries);
            int nRecordDuration = 0;
            mRecordDurPreference = new ListPreference(this);
            mRecordDurPreference.setEntries(R.array.record_durations_entries);
            mRecordDurPreference.setEntryValues(R.array.record_duration_values);
            mRecordDurPreference.setDialogTitle(R.string.sel_rec_dur);
            mRecordDurPreference.setKey(RECORD_DURATION_KEY);
            mRecordDurPreference.setTitle(R.string.record_dur);
            nRecordDuration = FmSharedPreferences.getRecordDuration();
            Log
                  .d(LOGTAG, "createPreferenceHierarchy: recordDuration: "
                        + nRecordDuration);
            switch( nRecordDuration ) {
             case FmSharedPreferences.RECORD_DUR_INDEX_0_VAL:
                 index =0;
                 break;
             case FmSharedPreferences.RECORD_DUR_INDEX_1_VAL:
                 index =1;
                 break;
             case FmSharedPreferences.RECORD_DUR_INDEX_2_VAL:
                 index =2;
                 break;
             case FmSharedPreferences.RECORD_DUR_INDEX_3_VAL:
                 index =3;
                 break;
             }
            // Get the preference and list the value.
            if ((index < 0) || (index >= summaryRecordItems.length)) {
               index = 0;
            }
            Log.d(LOGTAG, "createPreferenceHierarchy: recordDurationSummary: "
                  + summaryRecordItems[index]);
            mRecordDurPreference.setSummary(summaryRecordItems[index]);
            mRecordDurPreference.setValueIndex(index);
            root.addPreference(mRecordDurPreference);
         }
                }

                // Add a new category
                PreferenceCategory prefCat = new PreferenceCategory(this);
                root.addPreference(prefCat);

                mRestoreDefaultPreference = new Preference(this);
                mRestoreDefaultPreference
                                .setTitle(R.string.settings_revert_defaults_title);
                mRestoreDefaultPreference.setKey(RESTORE_FACTORY_DEFAULT);
                mRestoreDefaultPreference
                                .setSummary(R.string.settings_revert_defaults_summary);
                mRestoreDefaultPreference.setOnPreferenceClickListener(this);
                root.addPreference(mRestoreDefaultPreference);
                return root;
        }

        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                        String key) {
                int mTunedFreq = 0;
                boolean bStatus = false;
                if (key.equals(REGIONAL_BAND_KEY)) {
		       int curListIndex = FmSharedPreferences.getCurrentListIndex();
		           PresetList curList = FmSharedPreferences.getStationList(curListIndex);
                           String[] summaryBandItems = getResources().getStringArray(
                                        R.array.regional_band_summary);
                        String valueStr = sharedPreferences.getString(key, "");
                        int index = 0;
                        if (valueStr != null) {
                                index = mBandPreference.findIndexOfValue(valueStr);
                        }
                        if ((index < 0) || (index >= summaryBandItems.length)) {
                                index = 0;
                                mBandPreference.setValueIndex(0);
                        }
                        Log.d(LOGTAG, "onSharedPreferenceChanged: Country Change: "
                                                        + index);
                        mBandPreference.setSummary(summaryBandItems[index]);
                        FmSharedPreferences.setCountry(index);
                        bStatus = FMRadio.fmConfigure();
                        FMTransmitterActivity.fmConfigure();
                        if (curList != null) {
                           curList.clear();
                        }
                } else {
                        if (mRxMode) {
                                if (key.equals(AUTO_AF)) {
                                        boolean bAFAutoSwitch = mAfPref.isChecked();
                                        Log.d(LOGTAG, "onSharedPreferenceChanged: Auto AF Enable: "
                                                        + bAFAutoSwitch);
                                        FmSharedPreferences.setAutoAFSwitch(bAFAutoSwitch);
                                        FMRadio.fmAutoAFSwitch();
                                        mPrefs.Save();
                                } else if (key.equals(RECORD_DURATION_KEY)) {
               if(FMRadio.RECORDING_ENABLE)
               {
                                           String[] recordItems = getResources().getStringArray(
                                                           R.array.record_durations_entries);
                                           String valueStr = mRecordDurPreference.getValue();
                                           int index = 0;
                                           if (valueStr != null) {
                                                   index = mRecordDurPreference.findIndexOfValue(valueStr);
                                           }
                                           if ((index < 0) || (index >= recordItems.length)) {
                                                   index = 0;
                                                   mRecordDurPreference.setValueIndex(index);
                                           }
                                           Log.d(LOGTAG, "onSharedPreferenceChanged: recorddur: "
                                                           + recordItems[index]);
                                           mRecordDurPreference.setSummary(recordItems[index]);
                                           FmSharedPreferences.setRecordDuration(index);
               }
                                } else if (key.equals(AUDIO_OUTPUT_KEY)) {
                                        String[] bandItems = getResources().getStringArray(
                                                        R.array.ster_mon_entries);
                                        String valueStr = mAudioPreference.getValue();
                                        int index = 0;
                                        if (valueStr != null) {
                                                index = mAudioPreference.findIndexOfValue(valueStr);
                                        }
                                        if (index != 1) {
                                                if (index != 0) {
                                                        index = 0;
                                                        /* It shud be 0(Stereo) or 1(Mono) */
                                                        mAudioPreference.setValueIndex(index);
                                                }
                                        }
                                        Log.d(LOGTAG, "onSharedPreferenceChanged: audiomode: "
                                                        + bandItems[index]);
                                        mAudioPreference.setSummary(bandItems[index]);
                                        if (index == 0) {
                                                // Stereo
                                                FmSharedPreferences.setAudioOutputMode(true);
                                        } else {
                                                // Mono
                                                FmSharedPreferences.setAudioOutputMode(false);
                                        }
                                        FMRadio.fmAudioOutputMode();
                                }
                        }
                }
                if (mPrefs != null)
                {
                        if(bStatus)
                                mPrefs.Save();
                        else {
                                mTunedFreq = FmSharedPreferences.getTunedFrequency();
                                if (mTunedFreq > FmSharedPreferences.getUpperLimit() || mTunedFreq < FmSharedPreferences.getLowerLimit()) {
                                        FmSharedPreferences.setTunedFrequency(FmSharedPreferences.getLowerLimit());
                                }
                                mPrefs.Save();
                        }
                }
        }

        public boolean onPreferenceClick(Preference preference) {
                boolean handled = false;
                if (preference == mRestoreDefaultPreference) {
                        showDialog(RESTORE_FACTORY_DEFAULT_INT);
                }
                return handled;
        }

        @Override
        protected Dialog onCreateDialog(int id) {
                switch (id) {
                case RESTORE_FACTORY_DEFAULT_INT:
                        return new AlertDialog.Builder(this).setIcon(
                                        R.drawable.alert_dialog_icon).setTitle(
                                        R.string.settings_revert_confirm_title).setMessage(
                                        R.string.settings_revert_confirm_msg).setPositiveButton(
                                        R.string.alert_dialog_ok,
                                        new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog,
                                                                int whichButton) {
                                                        Intent data = new Intent(
                                                                        RESTORE_FACTORY_DEFAULT_ACTION);
                                                        setResult(RESULT_OK, data);
                                                        restoreSettingsDefault();
                                                        finish();
                                                }

                                        }).setNegativeButton(R.string.alert_dialog_cancel,
                                        new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog,
                                                                int whichButton) {
                                                }
                                        }).create();
                default:
                        break;
                }
                return null;
        }

        private void restoreSettingsDefault() {
                if (mPrefs != null) {
                        mBandPreference.setValueIndex(0);
                        if (mRxMode) {
                                mAudioPreference.setValueIndex(0);
            if(FMRadio.RECORDING_ENABLE)
            {
               mRecordDurPreference.setValueIndex(0);
            }
                                mAfPref.setChecked(false);
                                FmSharedPreferences.SetDefaults();
                        }
                        else
                        {
                                FmSharedPreferences.setCountry(FmSharedPreferences.REGIONAL_BAND_NORTH_AMERICA);
                        }
                        mPrefs.Save();
             }
        }

        @Override
        protected void onResume() {
                super.onResume();
                PreferenceScreen preferenceScreen = getPreferenceScreen();
                SharedPreferences sharedPreferences = null;
                if (preferenceScreen != null) {
                   sharedPreferences = preferenceScreen.getSharedPreferences();
                }
                if (sharedPreferences != null) {
                   sharedPreferences.registerOnSharedPreferenceChangeListener(this);
                }
        }

        @Override
        protected void onPause() {
                super.onPause();
                PreferenceScreen preferenceScreen = getPreferenceScreen();
                SharedPreferences sharedPreferences = null;
                if (preferenceScreen != null) {
                   sharedPreferences = preferenceScreen.getSharedPreferences();
                }
                if (sharedPreferences != null) {
                   sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
                }
        }

}
