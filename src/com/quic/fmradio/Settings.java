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
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.ListPreference;
import android.preference.Preference.OnPreferenceClickListener;
import android.widget.Toast;

public class Settings extends PreferenceActivity implements OnSharedPreferenceChangeListener,
 OnPreferenceClickListener{
    public static final String AUTO_AF = "af_checkbox_preference";
    public static final String REGIONAL_BAND_KEY = "regional_band";
    public static final String AUDIO_OUTPUT_KEY = "audio_output_mode";
    public static final String RECORD_DURATION_KEY = "record_duration";
    public static final String RESTORE_FACTORY_DEFAULT = "revert_to_fac";
    public static final int RESTORE_FACTORY_DEFAULT_INT = 1;

    private ListPreference mBandPreference;
    private ListPreference mAudioPreference;
    private ListPreference mRecordDurPreference;
    private CheckBoxPreference mAfPref;
    private Preference mRestoreDefaultPreference;
    private Preference mDonePref;
    private FmSharedPreferences mPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPrefs = FMRadio.mPrefs;
        addPreferencesFromResource(R.xml.settings);
        mAfPref = (CheckBoxPreference)getPreferenceScreen().findPreference(AUTO_AF);
        mBandPreference = (ListPreference)getPreferenceScreen().findPreference(
                REGIONAL_BAND_KEY);
        mAudioPreference = (ListPreference)getPreferenceScreen().findPreference(
                AUDIO_OUTPUT_KEY);
        mRecordDurPreference = (ListPreference)getPreferenceScreen().findPreference(
                RECORD_DURATION_KEY);
        mRestoreDefaultPreference =(Preference)getPreferenceScreen().findPreference(
                RESTORE_FACTORY_DEFAULT);
        mRestoreDefaultPreference.setOnPreferenceClickListener(this);
    }

    @Override
    protected void onResume(){
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        mBandPreference.setSummary(mBandPreference.getValue());
        mAudioPreference.setSummary(mAudioPreference.getValue());
        mRecordDurPreference.setSummary(mRecordDurPreference.getValue());
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            String key) {
        System.out.println("*** Shared pref change happened");
        if(key.equals(REGIONAL_BAND_KEY))
        {
            String value = sharedPreferences.getString(key, "");
            mBandPreference.setSummary(value);
            return;
        }
        else if(key.equals(AUDIO_OUTPUT_KEY)){
            String value = sharedPreferences.getString(key, "");
            mAudioPreference.setSummary(value);
            }
        else if(key.equals(RECORD_DURATION_KEY)){
            String value = sharedPreferences.getString(key, "");
            mRecordDurPreference.setSummary(value);
        }
        else if(key.equals(RESTORE_FACTORY_DEFAULT)){
            System.out.println("*** Restore default settings");
            Toast.makeText(this, "Restore Default", Toast.LENGTH_SHORT).show();
        }

    }

    public boolean onPreferenceClick(Preference preference) {
        boolean handled = false;
        if(preference == mDonePref) {
            setResult( RESULT_CANCELED, null);
            handled = true;
            finish();
        } else if(preference == mRestoreDefaultPreference){
            showDialog(RESTORE_FACTORY_DEFAULT_INT);
        }
        return handled;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case RESTORE_FACTORY_DEFAULT_INT:
            return new AlertDialog.Builder(this).setIcon(
                R.drawable.alert_dialog_icon).setTitle("Confirm Reset")
                .setMessage("This will delete all settings including Presets")
                .setPositiveButton(R.string.alert_dialog_ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                    int whichButton) {
                                    restoreSettingsDefault();
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

    private void restoreSettingsDefault(){
        mBandPreference.setValue(getString(R.string.default_band));
        mAudioPreference.setValue(getString(R.string.default_audio));
        mRecordDurPreference.setValue(getString(R.string.default_record_duration));
        mAfPref.setChecked(false);
        mPrefs.SetDefaults();
    }
}
