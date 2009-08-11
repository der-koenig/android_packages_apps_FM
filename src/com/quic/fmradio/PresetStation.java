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

import android.text.TextUtils;

public class PresetStation{
    private double mFrequency = 102.1;
    private String mName = "";
    private String mPty = "";
    private String mStationId = "";
    private boolean mRdsSupport = false;

    public PresetStation(String name, double frequency) {
        mName = name;
        setFrequency(frequency);
        mStationId = name;
    }

    public PresetStation(PresetStation station) {
        mName = station.getName();
        setFrequency(station.getFrequency());
        mStationId = station.getStationId();
        mPty = station.getPty();
        mRdsSupport = station.getRDSSupported();
    }

    public void Copy(PresetStation station) {
        mName = station.getName();
        mFrequency = station.getFrequency();
        mStationId = station.getStationId();
        mPty = station.getPty();
        mRdsSupport = station.getRDSSupported();
    }

    public boolean equals(PresetStation station) {
        boolean equal = false;
        if(mFrequency == station.getFrequency()){
            if(mRdsSupport == (station.getRDSSupported())) {
                if(mStationId.equalsIgnoreCase(station.getStationId())) {
                    if(mPty.equalsIgnoreCase(station.getPty())) {
                        equal = true;
                    }
                }
            }
        }
        return equal;
    }

    public void setName(String name){
        if (!TextUtils.isEmpty(name)) {
            mName = name;
        } else {
            mName = ""+mFrequency;
        }
    }

    public void setFrequency(double freq){
        mFrequency = freq;
        /* If no name set it to the frequency */
        if (TextUtils.isEmpty(mName)) {
            mName = ""+mFrequency;
        }
        return;
    }

    public void setPty(String pty){
        mPty = pty;
    }

    public void setStationId(String stationId){
        mStationId = stationId;
    }

    public void setRDSSupported(boolean rds){
        mRdsSupport = rds;
    }

    public String getName(){
        return mName;
    }

    public double getFrequency(){
        return mFrequency;
    }

    public String getPty(){
        return mPty;
    }

    public String getStationId(){
        return mStationId;
    }

    public boolean getRDSSupported(){
        return mRdsSupport;
    }
}
