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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceClickListener;
import android.view.Menu;
import android.view.Window;
import android.widget.EditText;


public class PresetEditor extends PreferenceActivity implements
        OnSharedPreferenceChangeListener, OnPreferenceClickListener {
    private static final int DELETE_DIALOG = Menu.FIRST + 1;
    private static int mCurKey = 0;
    private static PreferenceScreen root;
    private FmSharedPreferences mPrefs;
    private PresetStation mStationNow = FMRadio.getCurrentTunedStation();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /* Create First Default Stations for testing */
        mPrefs = FMRadio.mPrefs;
        setPreferenceScreen(createPreferenceHierarchy());
        mStationNow = new PresetStation("FM", 102.1);
        mStationNow.Copy(FMRadio.getCurrentTunedStation());
    }



    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPrefs.Save();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPrefs.Save();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            String key) {
        int listIndex = mPrefs.getCurrentListIndex();

        /* Edit station */
        if (key.startsWith("editTitle")) {
            EditTextPreference mEdit;
            mEdit = (EditTextPreference) findPreference(key);
            String newName = (String) mEdit.getText();
            mEdit.setTitle(("Name:        " + newName));
            key = key.replaceAll("editTitle", "");
            int newKey = Integer.parseInt(key);
            PresetList curList = mPrefs.getStationList(listIndex);

            curList.setStationName(newKey, newName);
            Preference myPref = (Preference) findPreference("stationName" + key);
            myPref.setTitle(    newName + " - "
                    + curList.getStationFrequency(newKey));
            int listLength = curList.getStationCount();
            UpdateStationPreference(curList, listLength);
            return;
        }
    }

    private PreferenceScreen createPreferenceHierarchy() {
        /* Root */
        int listIndex = mPrefs.getCurrentListIndex();

        PresetList curList = mPrefs.getStationList(listIndex);
        String curListName = curList.getName();

        /* Launch preferences */
        root = getPreferenceManager().createPreferenceScreen(this);

        /* Preset Stations view */
        PreferenceCategory stationsPC = new PreferenceCategory(this);
        stationsPC.setTitle("FM Stations in : \"" + curListName + "\"");
        root.addPreference(stationsPC);
        int listLength = curList.getStationCount();
        if(listLength > 0) {
            /* Category */
            for (int i = 0; i < listLength; i++) {
                AddStationPreference(root, curList, i);
            }
        } else {
            Preference addList = new Preference(this);
            addList.setTitle("No Stations in the List");
            addList.setSummary("Add Stations");
            addList.setEnabled(false);
            root.addPreference(addList);
        }

        mStationNow.Copy(FMRadio.getCurrentTunedStation());
        return root;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case DELETE_DIALOG:
            int listIndex = mPrefs.getCurrentListIndex();
            PresetList curList2 = mPrefs.getStationList(listIndex);
            Preference stationName = root.findPreference("stationName"+mCurKey);
            return new AlertDialog.Builder(this).setIcon(
                    R.drawable.alert_dialog_icon).setTitle("Delete Preset")
                    .setMessage("Delete "+stationName+" from "+curList2.getName()+" ?")
                    .setPositiveButton(R.string.alert_dialog_ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int whichButton) {

                                    int listIndex = mPrefs.getCurrentListIndex();
                                    PreferenceScreen stationName = (PreferenceScreen)root.findPreference(mCurKey+"");

                                    root.removePreference(stationName);
                                    PresetList curList = mPrefs.getStationList(listIndex);
                                    curList.removeStation(mCurKey);
                                    setScreen();
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

    public void setScreen(){
        mPrefs.Save();
            finish();
        Intent launchPreferencesIntent = new Intent().setClass(this,
                PresetEditor.class);
        startActivity(launchPreferencesIntent);
    }

    private void AddStationPreference(PreferenceScreen root,
            PresetList curList, int i) {
        String curStationName = curList.getStationName(i);
        double curStationFreq = curList.getStationFrequency(i);
        String stationSummary = "";
        boolean myChange = true;
        if(mStationNow.getFrequency() == curStationFreq) {
            stationSummary = "Now Playing";
        }

        PreferenceScreen stationPref = getPreferenceManager()
                .createPreferenceScreen(this);
        String key = Integer.toString(i);
        stationPref.setKey(key);
        stationPref.setTitle(curStationName + " - " + curStationFreq);
        stationPref.setSummary(stationSummary);

        root.addPreference(stationPref);


        Preference stationName = new Preference(this);
        stationName.setSelectable(false);
        stationName.setTitle(curStationName + " - " + curStationFreq);
        stationName.setKey("stationName" + key);
        stationName.setSummary(stationSummary);

        /* Edit menu for station */
        EditTextPreference editTextPref = new EditTextPreference(this);
        editTextPref.setDialogTitle("Enter New Name");
        editTextPref.setKey("editTitle" + key);
        editTextPref.setTitle("Name:        " + curStationName);
        editTextPref.setSummary("Enter your preferred name");

        /* Delete Preference */
        Preference deletePref = new Preference(this);
        deletePref.setTitle(getString(R.string.delete_station_title));
        deletePref.setSummary("Delete " + curStationName + " from \"" + curList.getName()+"\"");
        deletePref.setKey("deleteStation" + key);

        /* Alternate Frequency Preference */
        Preference findAf = new Preference(this);
        findAf.setTitle("Search");
        findAf.setSummary("Search for station \"" + curStationName + " - " + curStationFreq+"\"");
        findAf.setKey("searchPref"+key);

        deletePref.setOnPreferenceClickListener(mDeleteStationPreferenceClickListener);
        stationPref.addPreference(stationName);
        stationPref.addPreference(editTextPref);
        stationPref.addPreference(deletePref);
        stationPref.addPreference(findAf);

        editTextPref = (EditTextPreference) root.findPreference("editTitle"
                + key);

        editTextPref.setText(curStationName);
        EditText et = editTextPref.getEditText();
        et.setText(curStationName);
        myChange = false;

    }

    private void UpdateStationPreference(PresetList curList, int listLength) {
        boolean myChange = true;
        for (int i = 0; i < listLength; i++) {
            String curStationName = curList.getStationName(i);
            double curStationFreq = curList.getStationFrequency(i);
            String key = Integer.toString(i);

            PreferenceScreen stationPref = (PreferenceScreen) root
                    .findPreference(key);
            stationPref.setTitle(curStationName + " - " + curStationFreq);
            stationPref.setSummary("");

            /* Context Menu for Station (Edit / Move) */
            Preference stationName = root.findPreference("stationName" + key);
            stationName.setTitle(curStationName + " - " + curStationFreq);
            if(mStationNow.getFrequency() == curStationFreq) {
                stationName.setSummary("Now Playing");
            } else {
                stationName.setSummary("");
            }

            /* Edit menu for station */
            EditTextPreference editTextPref = (EditTextPreference) root
                    .findPreference("editTitle" + key);
            editTextPref.setTitle("Name:        " + curStationName);
            editTextPref.setText(curStationName);
            EditText et = editTextPref.getEditText();
            et.setText(curStationName);

        }
        myChange = false;
    }

    private Preference.OnPreferenceClickListener mDeleteStationPreferenceClickListener = new Preference.OnPreferenceClickListener() {
        public boolean onPreferenceClick(Preference preference) {
            String key =preference.getKey();
            key = key.replaceAll("deleteStation", "");
            mCurKey = Integer.parseInt(key);
            showDialog(DELETE_DIALOG);
            return true;
        }
    };

    public boolean onPreferenceClick(Preference preference) {
        return true;
    }
}
