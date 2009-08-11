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

package com.quic.fmradio;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.FMRxAPI.FmReceiver;
import android.hardware.FMRxAPI.FmRxEvCallbacksAdaptor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.Formatter;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;

public class FMRadio extends Activity {
    private static final String TAG = "FMRadio";

    private static final String PREF_LAST_TUNED_FREQUENCY = "last_frequency";

    private static final int MENU_SCAN_START = Menu.FIRST + 2;
    private static final int MENU_SCAN_STOP = Menu.FIRST + 3;

    private static final int MENU_RECORD_START = Menu.FIRST + 4;
    private static final int MENU_RECORD_STOP = Menu.FIRST + 5;

    private static final int MENU_SLEEP = Menu.FIRST + 6;
    private static final int MENU_SLEEP_CANCEL = Menu.FIRST + 7;

    private static final int MENU_SETTINGS = Menu.FIRST + 8;

    private static final int MENU_WIRED_HEADSET = Menu.FIRST + 9;

    private static final int DIALOG_SEARCH = 1;
    private static final int DIALOG_SLEEP = 2;
    private static final int DIALOG_SELECT_PRESET_LIST = 3;
    private static final int DIALOG_PRESETS_LIST = 4;
    private static final int DIALOG_PRESET_LIST_RENAME = 5;
    private static final int DIALOG_PRESET_LIST_DELETE = 6;
    private static final int DIALOG_PRESET_LIST_AUTO_SET = 7;

    private static final int ACTIVITY_RESULT_GET_PRESET = 1;
    private static final int ACTIVITY_RESULT_GET_SEARCH_CATEGORY = 2;
    private static final int ACTIVITY_RESULT_SETTINGS = 3;

    private static String DEFAULT_LAST_TUNED_FREQUENCY = "102.1";
    private static final int MAX_PRESETS_PER_PAGE = 5;
    private static final int SLEEP_TOGGLE_SECONDS = 60;

    public static FmSharedPreferences mPrefs;

    private int mPresetPageNumber = 0;
    /* Button Resources */
    private ImageButton mOnOffButton;
    private ImageButton mMuteButton;
    /* Button to navigate Preset pages */
    private ImageButton mPresetPageButton;
    /* 6 Preset Buttons */
    private Button[] mPresetButtons = { null, null, null, null, null, null };
    private Button mPresetListButton;
    // private ImageButton mSearchButton;
    private RepeatingImageView mForwardButton;
    private RepeatingImageView mBackButton;

    /* station info layout */
    private StationLayout mStationInfoLayout;

    /* Top row in the station info layout */
    private ImageView mRSSI;
    private TextView mProgramServiceTV;
    private TextView mStereoTV;

    /* Middle row in the station info layout */
    private TextView mTuneStationFrequencyTV;
    private TextView mStationCallSignTV;
    private TextView mProgramTypeTV;

    /* Bottom row in the station info layout */
    private TextView mRadioTextTV;

    /* Sleep and Recording Messages */
    private TextView mSleepMsgTV;
    private TextView mRecordingMsgTV;

    private double mFrequencyBand_Min = 88.1;
    private double mFrequencyBand_Max = 107.9;
    private double mFrequencyBand_Stepsize = 0.2;
    private int mFrequencyBand_TotalSteps = 99;

    /* Current Status Indicators */
    private static boolean mRecording = false;
    private static boolean mMuteMode = false;
    private static boolean mTurnedOnMode = true;
    private static boolean mScanningMode = false;
    private Animation mPresetButtonAnimation = null;

    private static PresetStation mTunedStation = new PresetStation("", 102.1);

    /* Radio Vars */
    private FmReceiver mReceiver;
    final Handler mHandler = new Handler();
    private StringBuffer mRadioText;


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);
        mPresetButtonAnimation = AnimationUtils.loadAnimation(this,
                R.anim.preset_select);

        mPrefs = new FmSharedPreferences(this);

        mMuteButton = (ImageButton) findViewById(R.id.btn_silent);
        mMuteButton.setOnClickListener(mMuteModeClickListener);

        mOnOffButton = (ImageButton) findViewById(R.id.btn_onoff);
        mOnOffButton.setOnClickListener(mTurnOnOffClickListener);

        mForwardButton = (RepeatingImageView) findViewById(R.id.btn_forward);
        mForwardButton.setOnClickListener(mForwardClickListener);
        mForwardButton.setRepeatListener(mForwardListener, 300);

        mBackButton = (RepeatingImageView) findViewById(R.id.btn_back);
        mBackButton.setOnClickListener(mBackClickListener);
        mBackButton.setRepeatListener(mBackListener, 300);

        mPresetPageButton = (ImageButton) findViewById(R.id.btn_preset_page);
        mPresetPageButton.setOnClickListener(mPresetsPageClickListener);

        mPresetListButton = (Button) findViewById(R.id.btn_presets_list);
        mPresetListButton.setOnClickListener(mPresetListClickListener);
        mPresetListButton
                .setOnLongClickListener(mPresetListButtonOnLongClickListener);

        /* 6 Preset Buttons */
        mPresetButtons[0] = (Button) findViewById(R.id.presets_button_1);
        mPresetButtons[1] = (Button) findViewById(R.id.presets_button_2);
        mPresetButtons[2] = (Button) findViewById(R.id.presets_button_3);
        mPresetButtons[3] = (Button) findViewById(R.id.presets_button_4);
        mPresetButtons[4] = (Button) findViewById(R.id.presets_button_5);
        mPresetButtons[5] = (Button) findViewById(R.id.presets_button_6);
        for (int nButton = 0; nButton <= MAX_PRESETS_PER_PAGE; nButton++) {
            mPresetButtons[nButton]
                    .setOnClickListener(mPresetButtonClickListener);
            mPresetButtons[nButton]
                    .setOnLongClickListener(mPresetButtonOnLongClickListener);
        }

        mTuneStationFrequencyTV = (TextView) findViewById(R.id.prog_frequency_tv);
        mTuneStationFrequencyTV.setText("102.9");
        mProgramServiceTV = (TextView) findViewById(R.id.prog_service_tv);
        mStereoTV = (TextView) findViewById(R.id.stereo_text_tv);

        mStationCallSignTV = (TextView) findViewById(R.id.call_sign_tv);
        mProgramTypeTV = (TextView) findViewById(R.id.pty_tv);

        mRadioTextTV = (TextView) findViewById(R.id.radio_text_tv);
        mSleepMsgTV = (TextView) findViewById(R.id.sleep_msg_tv);
        mRecordingMsgTV = (TextView) findViewById(R.id.record_msg_tv);
        mRSSI = (ImageView) findViewById(R.id.signal_level);

        mStationInfoLayout = (StationLayout) findViewById(R.id.stationinfo_layout);
        if (mStationInfoLayout != null) {
            mStationInfoLayout.setOnPanelListener(mStationLayoutListener);
        }
        double frequency = Double.parseDouble((String) mTuneStationFrequencyTV
                .getText());
        mTuneStationFrequencyTV.setText(getFrequencyString(frequency));
        setupPresetLayout();
        enableRadioOnOffUI(mTurnedOnMode);
        /* TODO: comment for surf */
        setUpRadio();
        radioOnOff(true);
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPrefs.Save();
        SharedPreferences.Editor editor = getPreferences(0).edit();
        editor.putString(PREF_LAST_TUNED_FREQUENCY, mTuneStationFrequencyTV
                .getText().toString());

        editor.commit();
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences prefs = getPreferences(0);
        String restoredText = prefs.getString(PREF_LAST_TUNED_FREQUENCY,
                DEFAULT_LAST_TUNED_FREQUENCY);
        if (restoredText != null) {
            mTuneStationFrequencyTV.setText(restoredText);
        } else {
            mTuneStationFrequencyTV.setText(DEFAULT_LAST_TUNED_FREQUENCY);
        }
        double frequency = Double.parseDouble((String) mTuneStationFrequencyTV
                .getText());
        PresetStation station = mPrefs.getStationFromFrequency(frequency);
        if (station != null) {
            mTunedStation.Copy(station);
        }
        updateStationfInfoUI();
    }

    @Override
    public void onDestroy() {
        endSleepTimer();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuItem item;
        boolean radioOn = isRadioOn();
        boolean recording = isRecording();
        boolean sleepActive = isSleepTimerActive();
        boolean scanActive = isScanningActive();

        item = menu.add(0, MENU_SCAN_START, 0, "Scan").setIcon(
                R.drawable.ic_btn_search);
        if (item != null) {
            item.setVisible(!scanActive && radioOn);
        }
        item = menu.add(0, MENU_SCAN_STOP, 0, "Stop Scaning").setIcon(
                R.drawable.ic_btn_search);
        if (item != null) {
            item.setVisible(scanActive && radioOn);
        }

        item = menu.add(0, MENU_RECORD_START, 0, R.string.menu_record_start)
                .setIcon(R.drawable.ic_menu_record).setShortcut('0', 'r');
        if (item != null) {
            item.setVisible(!recording && radioOn);
        }
        item = menu.add(0, MENU_RECORD_STOP, 0, R.string.menu_record_stop)
                .setIcon(R.drawable.ic_menu_record).setShortcut('0', 'r');
        if (item != null) {
            item.setVisible(recording && radioOn);
        }
        /* Settings can be active */
        item = menu.add(0, MENU_SETTINGS, 0, R.string.menu_settings).setIcon(
                android.R.drawable.ic_menu_preferences).setShortcut('0', 's');

        item = menu.add(0, MENU_SLEEP, 0, R.string.menu_sleep).setTitle("Sleep");
        if (item != null) {
            item.setVisible(!sleepActive && radioOn);
        }
        item = menu.add(0, MENU_SLEEP_CANCEL, 0, R.string.menu_sleep_cancel)
                .setTitle("Cancel Sleep");
        if (item != null) {
            item.setVisible(sleepActive && radioOn);
        }

        if (isWiredHeadsetConnected()) {
            item = menu.add(0, MENU_WIRED_HEADSET, 0,
                    R.string.menu_wired_headset).setIcon(R.drawable.ic_stereo);
            if (item != null) {
                item.setCheckable(true);
                item.setChecked(false);
                item.setVisible(radioOn);
            }
        }

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        MenuItem item;
        boolean radioOn = isRadioOn();
        boolean recording = isRecording();
        boolean scanActive = isScanningActive();

        item = menu.findItem(MENU_SCAN_START);
        if (item != null) {
            item.setVisible(!scanActive && radioOn);
        }
        item = menu.findItem(MENU_SCAN_STOP);
        if (item != null) {
            item.setVisible(scanActive && radioOn);
        }

        item = menu.findItem(MENU_RECORD_START);
        if (item != null) {
            item.setVisible(!recording && radioOn);
        }
        item = menu.findItem(MENU_RECORD_STOP);
        if (item != null) {
            item.setVisible(recording && radioOn);
        }

        boolean sleepActive = isSleepTimerActive();
        item = menu.findItem(MENU_SLEEP);
        if (item != null) {
            item.setVisible(!sleepActive && radioOn);
        }
        item = menu.findItem(MENU_SLEEP_CANCEL);
        if (item != null) {
            item.setVisible(sleepActive && radioOn);
        }

        if (isWiredHeadsetConnected() && radioOn) {
            if (menu.findItem(MENU_WIRED_HEADSET) == null) {
                item = menu.add(0, MENU_WIRED_HEADSET, 0,
                        R.string.menu_wired_headset).setIcon(
                        R.drawable.ic_stereo);
                if (item != null) {
                    item.setCheckable(true);
                    item.setChecked(false);
                }
            }
        } else {
            menu.removeItem(MENU_WIRED_HEADSET);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_SETTINGS:
            Intent launchPreferencesIntent = new Intent().setClass(this,
                    Settings.class);
            startActivityForResult(launchPreferencesIntent,
                    ACTIVITY_RESULT_SETTINGS);
            return true;

        case MENU_SCAN_START:
            showDialog(DIALOG_SEARCH);
            return true;

        case MENU_SCAN_STOP:
            cancelSearch();
            return true;

        case MENU_RECORD_START:
            startRecord();
            return true;

        case MENU_RECORD_STOP:
            stopRecord();
            return true;

        case MENU_SLEEP:
            showDialog(DIALOG_SLEEP);
            return true;

        case MENU_SLEEP_CANCEL:
            DebugToasts("Sleep Cancelled", Toast.LENGTH_SHORT);
            endSleepTimer();
            return true;

        case MENU_WIRED_HEADSET:
            DebugToasts("Route Audio over headset", Toast.LENGTH_SHORT);
            return true;
        default:
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view,
            ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);

        /* SHow the context menu for the station */
        if (findViewById(R.id.stationinfo_layout) != null) {
            if (view.getId() == R.id.stationinfo_layout) {

                menu.setHeaderTitle(mTuneStationFrequencyTV.getText() + " - "
                        + mStationCallSignTV.getText());

                if (isRecording()) {
                    menu.add(0, MENU_RECORD_STOP, 0, R.string.menu_record_stop)
                            .setIcon(R.drawable.ic_menu_record);
                } else {
                    menu.add(0, MENU_RECORD_START, 0,
                            R.string.menu_record_start).setIcon(
                            R.drawable.ic_menu_record);
                }
            }
        }
    }

    private void launchPresetListEditDlg(){
        Intent launchPresetIntent = new Intent().setClass(this,
                PresetEditor.class);
        startActivity(launchPresetIntent);
    }

    private Dialog createSearchDlg(int id) {
        AlertDialog.Builder dlgBuilder = new AlertDialog.Builder(this);
        LayoutInflater factory = LayoutInflater.from(this);
        final View listView = factory.inflate(R.layout.alert_dialog_list, null);
        ListView lv = (ListView) listView.findViewById(R.id.list);
        if (lv != null) {
            String[] items = getResources().getStringArray(
                    R.array.search_category_types);
            ArrayAdapter<String> typeAdapter = new ArrayAdapter<String>(this,
                    android.R.layout.simple_list_item_single_choice, items);
            lv.setAdapter(typeAdapter);
            lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            lv.clearChoices();
            lv.setItemChecked(0, true);
            lv.setSelection(0);
        } else {
            return null;
        }
        dlgBuilder.setView(listView);
        dlgBuilder.setIcon(R.drawable.ic_btn_search);
        dlgBuilder.setTitle("Scan Stations");

        dlgBuilder.setPositiveButton(R.string.alert_dialog_ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        ListView lv = (ListView) listView
                                .findViewById(R.id.list);
                        int selectedPos = -1;
                        if (lv != null) {
                            selectedPos = lv.getCheckedItemPosition();
                        }

                        String[] items = getResources().getStringArray(
                                R.array.search_category_types);
                        /*
                         * User clicked on a radio button do some stuff
                         */
                        dialog.dismiss();
                        if ((items != null) && (selectedPos >= 0)) {
                            if (selectedPos <= items.length)
                                DebugToasts("Search Stations for : "
                                        + items[selectedPos],
                                        Toast.LENGTH_SHORT);
                            initiateSearch(selectedPos);
                        }
                    }
                }).setNegativeButton(R.string.alert_dialog_cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        /* User clicked No so do some stuff */
                    }
                });
        return dlgBuilder.create();
    }

    private Dialog createSleepDlg(int id) {
        AlertDialog.Builder dlgBuilder = new AlertDialog.Builder(this);
        LayoutInflater factory = LayoutInflater.from(this);
        final View listView = factory.inflate(R.layout.alert_dialog_list, null);
        ListView lv = (ListView) listView.findViewById(R.id.list);
        if (lv != null) {
            String[] items = getResources().getStringArray(
                    R.array.sleep_duration_values);
            ArrayAdapter<String> typeAdapter = new ArrayAdapter<String>(this,
                    android.R.layout.simple_list_item_single_choice, items);
            lv.setAdapter(typeAdapter);
            lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            lv.clearChoices();
            lv.setItemChecked(1, true);
            lv.setSelection(1);
        } else {
            return null;
        }
        dlgBuilder.setView(listView);
        dlgBuilder.setTitle("Select Sleep Timer");

        dlgBuilder.setPositiveButton(R.string.alert_dialog_ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        ListView lv = (ListView) listView
                                .findViewById(R.id.list);
                        int selectedPos = -1;
                        if (lv != null) {
                            selectedPos = lv.getCheckedItemPosition();
                        }

                        String[] items = getResources().getStringArray(
                                R.array.sleep_duration_values);
                        /*
                         * User clicked on a radio button do some stuff
                         */
                        dialog.dismiss();
                        if ((items != null) && (selectedPos >= 0)) {
                            long seconds = 0;
                            if (selectedPos <= items.length) {
                                seconds = (long) (900 * (selectedPos + 1));
                                // seconds = (long) (70 * (selectedPos + 1));
                                initiateSleepTimer(seconds);
                            }
                        }
                    }
                });
        dlgBuilder.setNegativeButton(R.string.alert_dialog_cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                });
        return dlgBuilder.create();
    }

    private void updateSelectPresetListDlg(ListView lv) {
        if (lv != null) {
            List<PresetList> presetLists = mPrefs.getPresetLists();
            ListIterator<PresetList> presetIter;
            presetIter = presetLists.listIterator();
            int numLists = presetLists.size();
            int curIndex = mPrefs.getCurrentListIndex();
            ArrayAdapter<String> typeAdapter = new ArrayAdapter<String>(this,
                    android.R.layout.simple_list_item_single_choice);
            for (int stationIter = 0; stationIter < numLists; stationIter++) {
                PresetList temp = presetIter.next();
                if (temp != null) {
                    typeAdapter.add("Select \"" + temp.getName() + "\"");
                }
            }
            typeAdapter.add("Add new List");
            lv.setAdapter(typeAdapter);
            lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            lv.clearChoices();
            if (curIndex >= numLists) {
                curIndex = 0;
            }
            if (lv.getCount() >= curIndex) {
                lv.setItemChecked(curIndex, true);
                lv.setSelection(curIndex);
            } else {
                lv.setItemChecked(0, true);
                lv.setSelection(0);
            }
        }
    }

    private Dialog createSelectPresetListDlg(int id) {
        AlertDialog.Builder dlgBuilder = new AlertDialog.Builder(this);
        LayoutInflater factory = LayoutInflater.from(this);
        final View listView = factory.inflate(R.layout.alert_dialog_list, null);
        ListView lv = (ListView) listView.findViewById(R.id.list);

        if (lv != null) {
            updateSelectPresetListDlg(lv);
        } else {
            return null;
        }
        dlgBuilder.setView(listView);
        dlgBuilder.setIcon(R.drawable.alert_dialog_icon);
        dlgBuilder.setTitle("Preset Lists");
        dlgBuilder.setPositiveButton(R.string.alert_dialog_ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        ListView lv = (ListView) listView
                                .findViewById(R.id.list);
                        int selectedPos = -1;
                        if (lv != null) {
                            selectedPos = lv.getCheckedItemPosition();
                        }
                        if (selectedPos >= 0) {
                            if (selectedPos < mPrefs.getNumList()) {
                                mPrefs.setListIndex(selectedPos);
                            } else {
                                String presetListName = "FM - "
                                        + (mPrefs.getNumList() + 1);
                                int newIndex = mPrefs
                                        .createPresetList(presetListName);
                                mPrefs.setListIndex(newIndex);
                                showDialog(DIALOG_PRESET_LIST_RENAME);
                            }
                        }
                        setupPresetLayout();
                    }
                });
        dlgBuilder.setNegativeButton(R.string.alert_dialog_cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                });
        return dlgBuilder.create();
    }

    private Dialog createPresetListEditDlg(int id) {
        AlertDialog.Builder dlgBuilder = new AlertDialog.Builder(this);
        LayoutInflater factory = LayoutInflater.from(this);
        final View listView = factory.inflate(R.layout.alert_dialog_list, null);
        ListView lv = (ListView) listView.findViewById(R.id.list);
        if (lv != null) {
            String[] items = getResources().getStringArray(
                    R.array.presetlist_edit_category);
            ArrayAdapter<String> typeAdapter = new ArrayAdapter<String>(this,
                    android.R.layout.simple_list_item_single_choice, items);
            lv.setAdapter(typeAdapter);
            lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            lv.clearChoices();
            lv.setItemChecked(0, true);
            lv.setSelection(0);
        } else {
            return null;
        }
        dlgBuilder.setView(listView);

        int currentList = mPrefs.getCurrentListIndex();
        PresetList curList = mPrefs.getStationList(currentList);

        dlgBuilder.setIcon(R.drawable.alert_dialog_icon);
        dlgBuilder.setTitle("\"" + curList.getName() + "\"");
        dlgBuilder.setPositiveButton(R.string.alert_dialog_ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        ListView lv = (ListView) listView
                                .findViewById(R.id.list);
                        int selectedPos = -1;
                        if (lv != null) {
                            selectedPos = lv.getCheckedItemPosition();
                        }
                        if (selectedPos == 0) {
                            // Rename
                            showDialog(DIALOG_PRESET_LIST_RENAME);
                        } else if (selectedPos == 1) {
                            // Edit Stations
                            launchPresetListEditDlg();

                        } else if (selectedPos == 2) {
                            // Auto-Select - Build Preset List
                            showDialog(DIALOG_PRESET_LIST_AUTO_SET);
                        } else if (selectedPos == 3) {
                            // Delete
                            showDialog(DIALOG_PRESET_LIST_DELETE);
                        }
                    }
                });
        dlgBuilder.setNegativeButton(R.string.alert_dialog_cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                });
        return dlgBuilder.create();
    }

    private Dialog createPresetListRenameDlg(int id) {
        LayoutInflater factory = LayoutInflater.from(this);
        final View textEntryView = factory.inflate(
                R.layout.alert_dialog_text_entry, null);
        AlertDialog.Builder dlgBuilder = new AlertDialog.Builder(this);
        dlgBuilder.setTitle("Enter a Name");
        dlgBuilder.setView(textEntryView);
        dlgBuilder.setPositiveButton(R.string.alert_dialog_ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        int curList = mPrefs.getCurrentListIndex();

                        EditText mTV = (EditText) textEntryView
                                .findViewById(R.id.list_edit);
                        CharSequence newName = mTV.getEditableText();
                        String nName = String.valueOf(newName);
                        mPrefs.renamePresetList(nName, curList);
                        setupPresetLayout();
                    }
                });
        dlgBuilder.setNegativeButton(R.string.alert_dialog_cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                });
        return (dlgBuilder.create());
    }

    private Dialog createPresetListDeleteDlg(int id) {
        int currentList = mPrefs.getCurrentListIndex();
        PresetList curList = mPrefs.getStationList(currentList);
        AlertDialog.Builder dlgBuilder = new AlertDialog.Builder(this);
        dlgBuilder.setIcon(R.drawable.alert_dialog_icon).setTitle(
                curList.getName());
        dlgBuilder.setMessage("Delete \"" + curList.getName()
                + "\" and its Stations?");
        dlgBuilder.setPositiveButton(R.string.alert_dialog_ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        int currentList = mPrefs.getCurrentListIndex();
                        mPrefs.removeStationList(currentList);
                        setupPresetLayout();
                    }
                });
        dlgBuilder.setNegativeButton(R.string.alert_dialog_cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                });
        return (dlgBuilder.create());
    }

    private Dialog createPresetListAutoSelectWarnDlg(int id) {
        int currentList = mPrefs.getCurrentListIndex();
        PresetList curList = mPrefs.getStationList(currentList);
        AlertDialog.Builder dlgBuilder = new AlertDialog.Builder(this);
        dlgBuilder.setIcon(R.drawable.alert_dialog_icon).setTitle(
                "Confirm Auto-Select Stations");
        dlgBuilder
                .setMessage("Auto-Select will delete all the stations in the list \""
                        + curList.getName() + "\"");

        dlgBuilder.setPositiveButton(R.string.alert_dialog_ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        int currentList = mPrefs.getCurrentListIndex();
                        PresetList curList = mPrefs.getStationList(currentList);
                        curList.addDummyStations();
                        setupPresetLayout();
                    }
                });

        dlgBuilder.setNegativeButton(R.string.alert_dialog_cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                });
        return (dlgBuilder.create());
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case DIALOG_SELECT_PRESET_LIST: {
            return createSelectPresetListDlg(id);
        }
        case DIALOG_PRESETS_LIST: {
            return createPresetListEditDlg(id);
        }
        case DIALOG_PRESET_LIST_RENAME: {
            return createPresetListRenameDlg(id);
        }
        case DIALOG_PRESET_LIST_DELETE: {
            return createPresetListDeleteDlg(id);
        }
        case DIALOG_PRESET_LIST_AUTO_SET: {
            return createPresetListAutoSelectWarnDlg(id);
        }
        case DIALOG_SEARCH: {
            return createSearchDlg(id);
        }

        case DIALOG_SLEEP: {
            return createSleepDlg(id);
        }
        default:
            break;
        }
        return null;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        super.onPrepareDialog(id, dialog);
        int curListIndex = mPrefs.getCurrentListIndex();
        PresetList curList = mPrefs.getStationList(curListIndex);
        switch (id) {
        case DIALOG_PRESET_LIST_RENAME: {
            EditText et = (EditText) dialog.findViewById(R.id.list_edit);
            if (et != null) {
                et.setText(curList.getName());
            }
            break;
        }
        case DIALOG_PRESET_LIST_DELETE: {
            AlertDialog alertDlg = ((AlertDialog) dialog);
            alertDlg.setTitle("Delete \"" + curList.getName() + "\"");
            alertDlg.setMessage("Delete \"" + curList.getName()
                    + "\" and its Stations?");
            break;
        }

        case DIALOG_PRESET_LIST_AUTO_SET: {
            AlertDialog alertDlg = ((AlertDialog) dialog);
            alertDlg.setTitle("\"" + curList.getName() + "\"");
            ((AlertDialog) dialog)
                    .setMessage("Auto-Select will delete all the stations in the list \""
                            + curList.getName() + "\"");
            break;
        }
        case DIALOG_SELECT_PRESET_LIST: {
            AlertDialog alertDlg = ((AlertDialog) dialog);
            ListView lv = (ListView) alertDlg.findViewById(R.id.list);
            if (lv != null) {
                updateSelectPresetListDlg(lv);
            }
            break;
        }
        case DIALOG_PRESETS_LIST: {
            AlertDialog alertDlg = ((AlertDialog) dialog);
            alertDlg.setTitle("\"" + curList.getName() + "\"");
            ListView lv = (ListView) alertDlg.findViewById(R.id.list);
            if (lv != null) {
                lv.clearChoices();
                lv.setItemChecked(0, true);
                lv.setSelection(0);
            }
            break;
        }
        default:
            break;
        }
    }

    public void DebugToasts(String str, int duration) {
        Toast.makeText(this, str, duration).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
            case ACTIVITY_RESULT_GET_PRESET:
                PresetStation selectedStation = mPrefs.getselectedStation();
                if (selectedStation != null) {
                    Log.d(TAG, "onActivityResultSelected station - "
                            + selectedStation.getName() + " ("
                            + selectedStation.getFrequency() + ")");
                    mTunedStation.Copy(selectedStation);
                    updateStationfInfoUI();
                }
                break;

            case ACTIVITY_RESULT_GET_SEARCH_CATEGORY: {

                break;
            }
            case ACTIVITY_RESULT_SETTINGS: {
                /* */
                if (data != null) {
                    boolean restoreDefaults = (boolean) data.getBooleanExtra(
                            Settings.RESTORE_FACTORY_DEFAULT, false);
                    if (restoreDefaults == true) {
                        RestoreDefaults();
                    }
                }
                break;
            }
            default:
                break;
            }
        }
    }

    private void RestoreDefaults() {
        PreferenceManager.setDefaultValues(this, R.xml.settings, true);
        PreferenceManager.setDefaultValues(this, R.xml.presets, true);
        mPrefs.SetDefaults();

    }

    @SuppressWarnings("unused")
    private void computeTotalSteps(double min, double max, double step) {
        mFrequencyBand_Min = min;
        mFrequencyBand_Max = max;
        mFrequencyBand_Stepsize = step;
        mFrequencyBand_TotalSteps = (int) ((mFrequencyBand_Max - mFrequencyBand_Min) / (mFrequencyBand_Stepsize));
        Log.d(TAG, "computeTotalSteps - Min: " + mFrequencyBand_Min);
        Log.d(TAG, "computeTotalSteps - Max: " + mFrequencyBand_Max);
        Log.d(TAG, "computeTotalSteps - Step: " + mFrequencyBand_Stepsize);
        Log.d(TAG, "computeTotalSteps - TotalSize: "
                + mFrequencyBand_TotalSteps);
    }

    /** Routines that need to call non-UI FM Routines */

    public boolean isWiredHeadsetConnected() {
        return false;
    }

    public boolean startRecord() {
        mRecording = true;
        DebugToasts("Started Recording", Toast.LENGTH_SHORT);
        return mRecording;
    }

    public boolean isRecording() {
        return mRecording;
    }

    public boolean stopRecord() {
        mRecording = false;
        DebugToasts("Stopped Recording", Toast.LENGTH_SHORT);
        return mRecording;
    }

    public void addToPresets() {
        int currentList = mPrefs.getCurrentListIndex();
        PresetStation selectedStation = getCurrentTunedStation();
        mPrefs.addStation(selectedStation.getName(), selectedStation
                .getFrequency(), currentList);
        setupPresetLayout();
        showPresetAddedUI(selectedStation.getFrequency());
    }

    public void LaunchEditPresets() {
        DebugToasts("Launch Edit Presets", Toast.LENGTH_SHORT);
    }

    @SuppressWarnings("unused")
    private double getFrequencyFromSeekbarPosition(int position) {
        double frequency = (mFrequencyBand_Min + (position * mFrequencyBand_Stepsize));
        DecimalFormat twoDForm = new DecimalFormat("#.##");
        frequency = Double.valueOf(twoDForm.format(frequency));
        return frequency;
    }

    @SuppressWarnings("unused")
    private int getSeekbarPositionFromFrequency(double frequency) {
        int position = (int) ((frequency - mFrequencyBand_Min) / mFrequencyBand_Stepsize);
        return position;
    }

    private double getTunedFrequency() {
        double frequency = Double.parseDouble((String) mTuneStationFrequencyTV
                .getText());
        return (frequency);
    }

    private double getNextSeekFrequency() {
        double nextFrequency = getTunedFrequency()
                + (mFrequencyBand_Stepsize * 5);
        if (nextFrequency > mFrequencyBand_Max) {
            nextFrequency = mFrequencyBand_Min;
        }
        DecimalFormat twoDForm = new DecimalFormat("#.##");
        nextFrequency = Double.valueOf(twoDForm.format(nextFrequency));
        return nextFrequency;
    }

    private double getPrevSeekFrequency() {
        double frequency = Double.parseDouble((String) mTuneStationFrequencyTV
                .getText());
        double prevFrequency = frequency - (mFrequencyBand_Stepsize * 5);
        if (prevFrequency < mFrequencyBand_Min) {
            prevFrequency = mFrequencyBand_Max;
        }
        DecimalFormat twoDForm = new DecimalFormat("#.##");
        prevFrequency = Double.valueOf(twoDForm.format(prevFrequency));
        return prevFrequency;
    }

    private double getNextTuneFrequency() {
        double nextFrequency = getTunedFrequency() + mFrequencyBand_Stepsize;
        if (nextFrequency > mFrequencyBand_Max) {
            nextFrequency = mFrequencyBand_Min;
        }
        DecimalFormat twoDForm = new DecimalFormat("#.##");
        nextFrequency = Double.valueOf(twoDForm.format(nextFrequency));
        return nextFrequency;
    }

    private double getPrevTuneFrequency() {
        double frequency = Double.parseDouble((String) mTuneStationFrequencyTV
                .getText());
        double prevFrequency = frequency - mFrequencyBand_Stepsize;
        if (prevFrequency < mFrequencyBand_Min) {
            prevFrequency = mFrequencyBand_Max;
        }
        DecimalFormat twoDForm = new DecimalFormat("#.##");
        prevFrequency = Double.valueOf(twoDForm.format(prevFrequency));
        return prevFrequency;
    }

    /*
     * Routine to return the frequency string to be used when setting the
     * textview
     */
    private String getFrequencyString(double frequency) {
        String frequencyString;
        if (frequency < 100.0) {
            frequencyString = (" " + frequency);
        } else {
            frequencyString = ("" + frequency);
        }
        return frequencyString;
    }

    /****** All Listeners *******************/
    private View.OnClickListener mPresetListClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            showDialog(DIALOG_SELECT_PRESET_LIST);
        }
    };
    private View.OnLongClickListener mPresetListButtonOnLongClickListener = new View.OnLongClickListener() {
        public boolean onLongClick(View view) {
            showDialog(DIALOG_PRESETS_LIST);
            return true;
        }
    };

    private View.OnClickListener mPresetsPageClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            mPresetPageNumber++;
            Log.d(TAG, "mPresetsPageClickListener:-> mPresetPageNumber"
                    + mPresetPageNumber);
            setupPresetLayout();
        }
    };

    private View.OnClickListener mPresetButtonClickListener = new View.OnClickListener() {
        public void onClick(View view) {
            PresetStation station = (PresetStation) view.getTag();
            if (station != null) {
                Log.d(TAG, "station - " + station.getName() + " ("
                        + station.getFrequency() + ")");
                mTunedStation.Copy(station);
                updateStationfInfoUI();
                updateRadio();
            }
        }
    };

    private View.OnLongClickListener mPresetButtonOnLongClickListener = new View.OnLongClickListener() {
        public boolean onLongClick(View view) {
            PresetStation station = (PresetStation) view.getTag();
            if (station != null) {
                Log.d(TAG, "station - " + station.getName() + " ("
                        + station.getFrequency() + ")");
                station.Copy(mTunedStation);
            } else {
                addToPresets();
            }
            updateStationfInfoUI();
            showPresetAddedUI(mTunedStation.getFrequency());
            view.startAnimation(mPresetButtonAnimation);
            return true;
        }
    };

    private StationLayout.OnStationLayoutListener mStationLayoutListener = new StationLayout.OnStationLayoutListener() {

        public void onFlingNext(StationLayout layout) {
            SeekNextStation();
            mTuneStationFrequencyTV.startAnimation(mPresetButtonAnimation);
        }

        public void onFlingPrevious(StationLayout layout) {
            SeekPreviousStation();
            mTuneStationFrequencyTV.startAnimation(mPresetButtonAnimation);
        }

        public void onSingleTap(StationLayout layout) {
            // launchPresetPicker();
        }
        public void onLongPress(StationLayout layout) {
            // launchPresetPicker();
        }
    };

    private View.OnClickListener mStopSearchClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (isScanningActive()) {
                cancelSearch();
            }
        }
    };

    private View.OnClickListener mForwardClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            SeekNextStation();
        }
    };

    private RepeatingImageView.RepeatListener mForwardListener = new RepeatingImageView.RepeatListener() {
        public void onRepeat(View v, long howlong, int repcnt) {
            TuneNextStation();
        }
    };

    private View.OnClickListener mBackClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            SeekPreviousStation();
        }
    };

    private RepeatingImageView.RepeatListener mBackListener = new RepeatingImageView.RepeatListener() {
        public void onRepeat(View v, long howlong, int repcnt) {
            TunePreviousStation();
        }
    };

    private View.OnClickListener mMuteModeClickListener = new View.OnClickListener() {
        public void onClick(View v) {

            if (mMuteMode == true) {
                mMuteMode = false;
            } else {
                mMuteMode = true;
            }
            setMuteModeButtonImage(true);
            v.startAnimation(mPresetButtonAnimation);
        }
    };

    private void setMuteModeButtonImage(boolean notify) {
        String fmMutedString;
        if (mMuteMode == true) {
            mMuteButton.setImageResource(R.drawable.ic_silent_mode);
            fmMutedString = "FM Radio Muted";
        } else {
            /* Find a icon for Stations */
            mMuteButton.setImageResource(R.drawable.ic_silent_mode_off);
            fmMutedString = "FM Radio Un-Muted";
        }
        if (notify) {
            Toast.makeText(this, fmMutedString, Toast.LENGTH_SHORT).show();
        }
    }

    public void TurnOnRadio() {
        mTurnedOnMode = true;
        /* Not muted */
        mMuteMode = false;
        mScanningMode = false;
        enableRadioOnOffUI(true);
    }

    public void TurnOffRadio() {
        cancelSearch();
        mTurnedOnMode = false;
        /* Muted */
        mMuteMode = !mTurnedOnMode;
        endSleepTimer();
        enableRadioOnOffUI(false);
    }

    private View.OnClickListener mTurnOnOffClickListener = new View.OnClickListener() {
        public void onClick(View v) {

            if (isRadioOn()) {
                TurnOffRadio();
            } else {
                TurnOnRadio();
            }
            setTurnOnOffButtonImage();
        }
    };

    private boolean isRadioOn() {
        return (mTurnedOnMode);
    }

    private boolean isScanningActive() {
        return (mScanningMode);
    }

    private void setTurnOnOffButtonImage() {
        if (isRadioOn() == true) {
            mOnOffButton.setImageResource(R.drawable.ic_btn_onoff);
        } else {
            /* Find a icon to indicate off */
            mOnOffButton.setImageResource(R.drawable.ic_btn_onoff);
        }
    }

    public static PresetStation getCurrentTunedStation() {
        return mTunedStation;
    }

    private void updateStationfInfoUI() {
        mTuneStationFrequencyTV.setText("" + mTunedStation.getFrequency());
        mStationCallSignTV.setText(mTunedStation.getStationId());
        mProgramTypeTV.setText(mTunedStation.getPty());
        setupPresetLayout();
    }

    /** Scan related */
    private void initiateSearch(int option) {
        mScanningMode = true;
        enableScanningOnOffUI();
    }

    private void cancelSearch() {
        mScanningMode = false;
        enableScanningOnOffUI();
    }

    /** Sleep Handling: After the timer expires, the app needs to shut down */
    private static final int SLEEPTIMER_EXPIRED = 0x1001;
    private static final int SLEEPTIMER_UPDATE = 0x1002;
    private Thread mSleepUpdateHandlerThread = null;
    /*
     * Phone time when the App has to be shut down, calculated based on what the
     * user configured
     */
    private long mSleepAtPhoneTime = 0;
    private boolean mSleepCancelled = false;

    public void initiateSleepTimer(long seconds) {
        mSleepAtPhoneTime = (SystemClock.elapsedRealtime()) + (seconds * 1000);
        Log.d(TAG, "Sleep in seconds : " + seconds);

        mSleepCancelled = false;
        if (mSleepUpdateHandlerThread == null) {
            mSleepUpdateHandlerThread = new Thread(null, doSleepProcessing,
                    "SleepUpdateThread");
        }
        /* Launch he dummy thread to simulate the transfer progress */
        Log.d(TAG, "Thread State: " + mSleepUpdateHandlerThread.getState());
        if (mSleepUpdateHandlerThread.getState() == Thread.State.TERMINATED) {
            mSleepUpdateHandlerThread = new Thread(null, doSleepProcessing,
                    "SleepUpdateThread");
        }
        /* If the thread state is "new" then the thread has not yet started */
        if (mSleepUpdateHandlerThread.getState() == Thread.State.NEW) {
            mSleepUpdateHandlerThread.start();
        }
    }

    public void endSleepTimer() {
        mSleepAtPhoneTime = 0;
        mSleepCancelled = true;
        // Log.d(TAG, "endSleepTimer");
    }

    public boolean hasSleepTimerExpired() {
        boolean expired = true;
        if (isSleepTimerActive()) {
            long timeNow = ((SystemClock.elapsedRealtime()));
            Log.d(TAG, "hasSleepTimerExpired - " + mSleepAtPhoneTime + " now: "
                    + timeNow);
            if (timeNow < mSleepAtPhoneTime) {
                expired = false;
            }
        }
        // Log.d(TAG, "hasSleepTimerExpired - " + expired);
        return expired;
    }

    public boolean isSleepTimerActive() {
        boolean active = false;
        if (mSleepAtPhoneTime > 0) {
            active = true;
        }
        // Log.d(TAG, "isSleepTimerActive - " + active);
        return active;
    }

    private void updateExpiredSleepTime() {
        int vis = View.INVISIBLE;
        if (isSleepTimerActive()) {
            long timeNow = ((SystemClock.elapsedRealtime()));
            if (mSleepAtPhoneTime >= timeNow) {
                long seconds = (mSleepAtPhoneTime - timeNow) / 1000;
                String sleepMsg = "Sleep : " + makeTimeString(seconds);
                Log.d(TAG, "updateExpiredSleepTime: " + sleepMsg);
                mSleepMsgTV.setText(sleepMsg);
                if (seconds < SLEEP_TOGGLE_SECONDS) {
                    int nowVis = mSleepMsgTV.getVisibility();
                    vis = (nowVis == View.INVISIBLE) ? View.VISIBLE
                            : View.INVISIBLE;
                } else {
                    vis = View.VISIBLE;
                }
            } else {
                /* Clean up timer */
                mSleepAtPhoneTime = 0;
            }
        }
        mSleepMsgTV.setVisibility(vis);
    }

    private Handler mUIUpdateHandlerHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case SLEEPTIMER_EXPIRED: {
                mSleepAtPhoneTime = 0;
                if (mSleepCancelled != true) {
                    Log.d(TAG, "mUIUpdateHandlerHandler - SLEEPTIMER_EXPIRED");
                    DebugToasts("Turning Off FM Radio", Toast.LENGTH_SHORT);
                    TurnOffRadio();
                }
                return;
            }
            case SLEEPTIMER_UPDATE: {
                Log.d(TAG, "mUIUpdateHandlerHandler - SLEEPTIMER_UPDATE");
                updateExpiredSleepTime();
                break;
            }
            default:
                break;
            }
            super.handleMessage(msg);
        }
    };

    /* Thread processing */
    private Runnable doSleepProcessing = new Runnable() {
        public void run() {
            boolean sleepTimerExpired = hasSleepTimerExpired();
            while (sleepTimerExpired == false) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                Message statusUpdate = new Message();
                statusUpdate.what = SLEEPTIMER_UPDATE;
                mUIUpdateHandlerHandler.sendMessage(statusUpdate);
                // Update the progress bar
                // mUIUpdateHandlerHandler.post(doUpdateProgressUI);
                sleepTimerExpired = hasSleepTimerExpired();
            }
            Message finished = new Message();
            finished.what = SLEEPTIMER_EXPIRED;
            mUIUpdateHandlerHandler.sendMessage(finished);
        }
    };

    /*** All Tune/Seek Nex/Prev Stations and Next/Prev Preset handlers */
    private void TunePreviousStation() {
        mTunedStation.setFrequency(getPrevTuneFrequency());
        mTunedStation.setName("");
        mTunedStation.setStationId("");
        mTunedStation.setPty("");
        updateStationfInfoUI();
        updateRadio();
    }

    private void TuneNextStation() {
        mTunedStation.setFrequency(getNextTuneFrequency());
        mTunedStation.setName("");
        mTunedStation.setStationId("");
        mTunedStation.setPty("");
        updateStationfInfoUI();
        updateRadio();
    }

    private void SeekPreviousStation() {
        mTunedStation.setFrequency(getPrevSeekFrequency());
        mTunedStation.setName("");
        mTunedStation.setStationId("");
        mTunedStation.setPty("");
        updateStationfInfoUI();
        updateRadio();
    }

    private void SeekNextStation() {
        mTunedStation.setFrequency(getNextSeekFrequency());
        mTunedStation.setName("");
        mTunedStation.setStationId("");
        mTunedStation.setPty("");
        updateStationfInfoUI();
        updateRadio();

    }

    public void enableRadioOnOffUI(boolean bEnable) {
        mMuteButton.setEnabled(bEnable);
        setMuteModeButtonImage(false);
        if (bEnable) {
            mStationInfoLayout.setOnPanelListener(mStationLayoutListener);
        } else {
            mStationInfoLayout.setOnPanelListener(null);
        }

        mForwardButton.setVisibility(((bEnable == true) ? View.VISIBLE
                : View.INVISIBLE));
        mBackButton.setVisibility(((bEnable == true) ? View.VISIBLE
                : View.INVISIBLE));
        mTuneStationFrequencyTV.setVisibility(((bEnable == true) ? View.VISIBLE
                : View.INVISIBLE));
        mStationCallSignTV.setVisibility(((bEnable == true) ? View.VISIBLE
                : View.INVISIBLE));
        mProgramTypeTV.setVisibility(((bEnable == true) ? View.VISIBLE
                : View.INVISIBLE));
        mSleepMsgTV.setVisibility(((bEnable == true) ? View.VISIBLE
                : View.INVISIBLE));
        mRecordingMsgTV.setVisibility(((bEnable == true) ? View.VISIBLE
                : View.INVISIBLE));
        mRadioTextTV.setVisibility(((bEnable == true) ? View.VISIBLE
                : View.INVISIBLE));

        mRSSI
                .setVisibility(((bEnable == true) ? View.VISIBLE
                        : View.INVISIBLE));
        mStereoTV.setVisibility(((bEnable == true) ? View.VISIBLE
                : View.INVISIBLE));

        if (bEnable) {
            mProgramServiceTV.setText("NOW PLAY");
        } else {
            mProgramServiceTV.setText("Radio Off");
        }
        mPresetListButton.setEnabled(bEnable);
        mPresetPageButton.setEnabled(bEnable
                && (mPrefs.getListStationCount() >= MAX_PRESETS_PER_PAGE));
        for (int nButton = 0; nButton <= MAX_PRESETS_PER_PAGE; nButton++) {
            mPresetButtons[nButton].setEnabled(bEnable);
        }
    }

    public void enableScanningOnOffUI() {
        boolean bEnable = !isScanningActive();
        LinearLayout presetLayout = (LinearLayout) findViewById(R.id.presets_layout);
        LinearLayout scanningStopLayout = (LinearLayout) findViewById(R.id.scanning_stoplayout);

        if (bEnable) {
            mStationInfoLayout.setOnPanelListener(mStationLayoutListener);
            mProgramServiceTV.setText("NOW PLAY");

        } else {
            mStationInfoLayout.setOnPanelListener(null);
            mProgramServiceTV.setText("Scanning");
        }

        if (presetLayout != null) {
            presetLayout.setVisibility(((bEnable == true) ? View.VISIBLE
                    : View.GONE));
        }

        Button stopScanButton = (Button) findViewById(R.id.btn_scanning_stop);
        if ((scanningStopLayout != null) && (stopScanButton != null)) {
            scanningStopLayout.setVisibility(((bEnable != true) ? View.VISIBLE
                    : View.GONE));
            if (bEnable != true) {
                stopScanButton.setOnClickListener(mStopSearchClickListener);
            } else {
                stopScanButton.setOnClickListener(null);
            }
        }

        mForwardButton.setVisibility(((bEnable == true) ? View.VISIBLE
                : View.INVISIBLE));
        mBackButton.setVisibility(((bEnable == true) ? View.VISIBLE
                : View.INVISIBLE));
        mPresetListButton.setEnabled(bEnable);

        mSleepMsgTV.setVisibility(((bEnable == true) ? View.VISIBLE
                : View.INVISIBLE));
        mRecordingMsgTV.setVisibility(((bEnable == true) ? View.VISIBLE
                : View.INVISIBLE));
        mRadioTextTV.setVisibility(((bEnable == true) ? View.VISIBLE
                : View.INVISIBLE));

        mRSSI
                .setVisibility(((bEnable == true) ? View.VISIBLE
                        : View.INVISIBLE));
        mStereoTV.setVisibility(((bEnable == true) ? View.VISIBLE
                : View.INVISIBLE));
    }

    public void showPresetAddedUI(double frequency) {

    }

    private void setupPresetLayout() {

        int numStations = mPrefs.getListStationCount();
        int addedStations = 0;
        Log.d(TAG, "setupPresetLayout: numStations" + numStations
                + " -> mPresetPageNumber" + mPresetPageNumber + " -> calc"
                + ((numStations) / MAX_PRESETS_PER_PAGE));

        if (mPresetPageNumber > ((numStations) / MAX_PRESETS_PER_PAGE)) {
            mPresetPageNumber = 0;
        }

        for (int buttonIndex = 0; (buttonIndex < MAX_PRESETS_PER_PAGE); buttonIndex++) {
            if (mPresetButtons[buttonIndex] != null) {
                int stationIdex = (mPresetPageNumber * MAX_PRESETS_PER_PAGE)
                        + buttonIndex;
                PresetStation station = mPrefs.getStationInList(stationIdex);
                String display = "";
                if (station != null) {
                    display = station.getName();
                    mPresetButtons[buttonIndex].setText(display);
                    mPresetButtons[buttonIndex].setTag(station);
                    addedStations++;
                } else {
                    mPresetButtons[buttonIndex].setText(display);
                    mPresetButtons[buttonIndex].setTag(station);
                }
            }
        }
        /*
         * Only if more than 4 presets are added, else there is nothing more
         * anyway
         */
        mPresetPageButton.setEnabled((numStations >= MAX_PRESETS_PER_PAGE));
        mPresetListButton.setText(mPrefs.getListName(mPrefs
                .getCurrentListIndex()));
    }

    private static StringBuilder sFormatBuilder = new StringBuilder();
    private static Formatter sFormatter = new Formatter(sFormatBuilder, Locale
            .getDefault());
    private static final Object[] sTimeArgs = new Object[5];

    public String makeTimeString(long secs) {
        String durationformat = getString(R.string.durationformat);

        /*
         * Provide multiple arguments so the format can be changed easily by
         * modifying the xml.
         */
        sFormatBuilder.setLength(0);

        final Object[] timeArgs = sTimeArgs;
        timeArgs[0] = secs / 3600;
        timeArgs[1] = secs / 60;
        timeArgs[2] = (secs / 60) % 60;
        timeArgs[3] = secs;
        timeArgs[4] = secs % 60;

        return sFormatter.format(durationformat, timeArgs).toString();
    }

    /* Radio Functions */
    private void setUpRadio() {
        FmRxEvCallbacksAdaptor ad = new FmRxEvCallbacksAdaptor(){
            public void FmRxEvEnableReceiver() {

            };
            public void FmRxEvRdsRtInfo(StringBuffer RTData) {
                Log.d("Event thing", "Blah " + RTData);
                StringBuffer sb = new StringBuffer();
                sb.append("");
                mReceiver.FmRxApi_RxGetRTInfo(sb);
                Log.d("Get RT Info", "" + sb);
                updateRt(sb);
                mHandler.post(mUpdateRt);


            };
        };
        mReceiver = new FmReceiver();
        mReceiver.FmApi_Acquire("/dev/radio0");

    }
    /* TODO: Comment for emu */
    private void radioOnOff(boolean on) {
        if(on)
            mReceiver.FmApi_Enable(null);
        else
            mReceiver.FmApi_Disable();

    }
    private void updateRadio(){
        radioTune(mTunedStation.getFrequency());
    }
    private void radioTune(double freq){
        mReceiver.FmApi_SetStation(freq);
    }
    /* Create runnable for posting */
    final Runnable mUpdateRt = new Runnable() {
        public void run() {
            mRadioTextTV.setText(mRadioText);
        }
    };
    private void updateRt(StringBuffer sb){
        mRadioText = sb;
    }

}
