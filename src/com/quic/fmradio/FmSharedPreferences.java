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

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public class FmSharedPreferences {
    private static final String SHARED_PREFS = "my_prefs";
    private static final String LIST_NUM = "list_number";
    private static final String LIST_NAME = "list_name";
    private static final String STATION_NAME = "station_name";
    private static final String STATION_FREQUENCY = "station_freq";
    private static final String STATION_ID = "station_id";
    private static final String STATION_PTY = "station_pty";
    private static final String STATION_RDS = "station_rds";
    private static final String STATION_NUM = "preset_number";


    private static Map<String, String> mNameMap = new HashMap<String, String>();
    private List<PresetList> mListOfPlists = new ArrayList<PresetList>();

    private static final String DEFAULT_NO_NAME = "FM";
    private static final int DEFAULT_NO_FREQUENCY = 9810;
    private static final String DEFAULT_NO_PTY = "";
    private static final String DEFAULT_NO_STATIONID = "";
    private static final int DEFAULT_NO_RDSSUP = 0;
    private static CharSequence[] mListEntries;
    private static CharSequence[] mListValues;
    private static int mListIndex;
    private Context mContext;

    FmSharedPreferences(Context context){
        mContext = context;
        LoadPreferences();
    }

    public void Save(){
        SavePreferences();
    }

    public void removeStation(int listIndex, int stationIndex){
        mListOfPlists.get(listIndex).removeStation(stationIndex);
    }

    public void setListName(int listIndex, String name){
        mListOfPlists.get(listIndex).setName(name);
    }

    public void setStationName(int listIndex, int stationIndex, String name){
        mListOfPlists.get(listIndex).setStationName(stationIndex, name);
    }

    public String getListName(int listIndex){
        String name = "";
        addListIfEmpty(listIndex);
        if(listIndex < mListOfPlists.size()){
            name= mListOfPlists.get(listIndex).getName();
        }
        return name;
    }

    public String getStationName(int listIndex, int stationIndex){
        return mListOfPlists.get(listIndex).getStationName(stationIndex);
    }

    public double getStationFrequency(int listIndex, int stationIndex){
        return mListOfPlists.get(listIndex).getStationFrequency(stationIndex);
    }

    public PresetList getStationList(int listIndex){
        return mListOfPlists.get(listIndex);
    }

    public PresetStation getselectedStation(){
        int listIndex = getCurrentListIndex();
        PresetStation station = null;
        if (listIndex < getNumList()) {
            station = mListOfPlists.get(listIndex).getSelectedStation();
        }
        return station;
    }

    public PresetStation getStationInList(int index){
        int listIndex = getCurrentListIndex();
        PresetStation station = null;
        if (listIndex < getNumList()) {
            station = mListOfPlists.get(listIndex).getStationFromIndex(index);
        }
        return station;
    }
    public PresetStation getStationFromFrequency(double frequency){
        int listIndex = getCurrentListIndex();
        PresetStation station = null;
        if (listIndex < getNumList()) {
            station = mListOfPlists.get(listIndex).getStationFromFrequency(frequency);
        }
        return station;
    }

    public PresetStation selectNextStation(){
        int listIndex = getCurrentListIndex();
        PresetStation station = null;
        if (listIndex < getNumList()) {
            station = mListOfPlists.get(listIndex).selectNextStation();
        }
        return station;
    }

    public PresetStation selectPrevStation(){
        int listIndex = getCurrentListIndex();
        PresetStation station = null;
        if (listIndex < getNumList()) {
            station = mListOfPlists.get(listIndex).selectPrevStation();
        }
        return station;
    }

    public void selectStation(PresetStation station){
        int listIndex = getCurrentListIndex();
        if (listIndex < getNumList()) {
            mListOfPlists.get(listIndex).selectStation(station);
        }
    }

    public int getNumList(){
        return mListOfPlists.size();
    }

    public int getCurrentListIndex(){
        return mListIndex;
    }

    public void setListIndex(int index){
        mListIndex = index;
    }

    public Map<String, String> getNameMap(){
        return mNameMap;
    }

    private void addListIfEmpty(int listIndex){
        if( (listIndex < 1) && (getNumList() == 0))
        {
            createPresetList("FM");
        }
    }

    public void addStation(String name, double freq, int listIndex){
        /* If no lists exists and a new station is added, add a new Preset List
         * if "listIndex" requested was "0"
         */
        addListIfEmpty(listIndex);
        if (getNumList() > listIndex){
            mListOfPlists.get(listIndex).addStation(name, freq);
        }
    }

    /** Add "station" into the Preset List indexed by "listIndex" */
    public void addStation(int listIndex, PresetStation station){
        /* If no lists exists and a new station is added, add a new Preset List
         * if "listIndex" requested was "0"
         */
        addListIfEmpty(listIndex);
        if (getNumList() > listIndex)
        {
            mListOfPlists.get(listIndex).addStation(station);
        }
    }
    /** Does "station" already exist in the Preset List indexed by "listIndex" */
    public boolean sameStationExists(int listIndex, PresetStation station){
        boolean exists = false;
        if (getNumList() > listIndex){
            exists = mListOfPlists.get(listIndex).sameStationExists(station);
        }
        return exists;
    }

    /** Does "station" already exist in the current Preset List*/
    public boolean sameStationExists( PresetStation station){
        int listIndex = getCurrentListIndex();
        boolean exists = false;
        if (getNumList() > listIndex){
            exists = mListOfPlists.get(listIndex).sameStationExists(station);
        }
        return exists;
    }

    /** Does "station" already exist in the current Preset List*/
    public int getListStationCount( ){
        int listIndex = getCurrentListIndex();
        int numStations = 0;
        if (getNumList() > listIndex){
            numStations = mListOfPlists.get(listIndex).getStationCount();
        }
        return numStations;
    }

    public void renamePresetList(String newName, int listIndex){
        PresetList curList =    mListOfPlists.get(listIndex);
        if (curList != null){
            String oldListName = curList.getName();
            curList.setName(newName);
            String index = mNameMap.get(oldListName);
            mNameMap.remove(oldListName);
            mNameMap.put((String) newName, index);
            repopulateEntryValueLists();
        }
    }

    /* Returns the index of the list just created */
    public int createPresetList(String name) {
        int numLists = mListOfPlists.size();
        mListOfPlists.add(new PresetList(mContext, name));
        String index = String.valueOf(numLists);
        mNameMap.put(name, index);
        repopulateEntryValueLists();
        return numLists;
    }


    public void createFirstPresetList(String name) {
        mListIndex = 0;
        createPresetList(name);
    }

    public CharSequence[] repopulateEntryValueLists() {
        ListIterator<PresetList> presetIter;
        presetIter = mListOfPlists.listIterator();
        int numLists = mListOfPlists.size();

        mListEntries = new CharSequence[numLists];
        mListValues = new CharSequence[numLists];
        for (int i = 0; i < numLists; i++) {
            PresetList temp = presetIter.next();
            mListEntries[i] = temp.getName();
            mListValues[i] = temp.getName();
        }
        return mListEntries;
    }

    public List<PresetList> getPresetLists() {
        return mListOfPlists;
    }


    private void LoadPreferences(){
        SharedPreferences sp = mContext.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
        int num_lists = sp.getInt(LIST_NUM, 1);
        for (int listIter = 0; listIter < num_lists; listIter++) {
            String listName = sp.getString(LIST_NAME + listIter, "FM - " + listIter);
            int numStations = sp.getInt(STATION_NUM + listIter, 1);
            if (listIter == 0) {
                createFirstPresetList(listName);
            } else {
                createPresetList(listName);
            }

            PresetList curList = mListOfPlists.get(listIter);
            for (int stationIter = 0; stationIter < numStations; stationIter++) {
                String stationName = sp.getString(STATION_NAME + listIter + "x" + stationIter,
                        DEFAULT_NO_NAME);
                double stationFreq = sp.getInt(STATION_FREQUENCY + listIter + "x" + stationIter,
                        DEFAULT_NO_FREQUENCY);
                stationFreq = stationFreq / 100.0;
                PresetStation station = curList.addStation(stationName, stationFreq);

                String stationId = sp.getString(STATION_ID + listIter + "x" + stationIter, DEFAULT_NO_STATIONID);
                station.setStationId(stationId);

                String pty = sp.getString(STATION_PTY + listIter + "x" + stationIter, DEFAULT_NO_PTY);
                station.setPty(pty);

                int rdsSupported = sp.getInt(STATION_RDS + listIter + "x" + stationIter,
                        DEFAULT_NO_RDSSUP);
                if(rdsSupported != 0) {
                    station.setRDSSupported(true);
                } else {
                    station.setRDSSupported(false);
                }

            }
        }
        mListIndex = 0;
    }

    private void SavePreferences() {
        int numLists = mListOfPlists.size();
        SharedPreferences sp = mContext.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putInt(LIST_NUM, numLists);

        for (int listIter = 0; listIter < numLists; listIter++) {
            PresetList curList = mListOfPlists.get(listIter);
            ed.putString(LIST_NAME + listIter, curList.getName());
            int numStations = curList.getStationCount();
            ed.putInt(STATION_NUM + listIter, numStations);
            int numStation = 0;
            for (int stationIter = 0; stationIter < numStations; stationIter++) {
                PresetStation station = curList.getStationFromIndex(stationIter);
                if(station != null) {
                    ed.putString(STATION_NAME + listIter + "x" + numStation, station.getName());
                    ed.putInt(STATION_FREQUENCY + listIter + "x" + numStation,
                            (int) ((double) station.getFrequency() * 100.0));
                    ed.putString(STATION_ID + listIter + "x" + numStation, station.getStationId());
                    ed.putString(STATION_PTY + listIter + "x" + numStation, station.getPty());
                    ed.putInt(STATION_RDS + listIter + "x" + numStation,
                            (station.getRDSSupported() == true? 1:0));
                    numStation ++;
                }
            }
        }
        ed.commit();
    }

    public void SetDefaults() {
        mListOfPlists.clear();
        SavePreferences();
    }

    public void removeStationList(int listIndex) {
        mListIndex = listIndex;
        PresetList toRemove = mListOfPlists.get(mListIndex);

        mNameMap.remove(toRemove.getName());
        mListOfPlists.remove(mListIndex);
        int numLists = mListOfPlists.size();

        /* Remove for others */
        for (int i = mListIndex; i < numLists; i++) {
            PresetList curList = mListOfPlists.get(i);
            if (curList!=null){
                String listName = curList.getName();
                /* Removals */
                mNameMap.remove(listName);
                mNameMap.put(listName, String.valueOf(i));
            }
        }
        mListIndex = 0;
        repopulateEntryValueLists();
    }
}
