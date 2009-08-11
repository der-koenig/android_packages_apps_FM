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
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.GestureDetector.OnGestureListener;
import android.widget.LinearLayout;

public class StationLayout extends LinearLayout implements OnGestureListener{
    private static final int MOVE_DIRECTION_NONE = 0;
    private static final int MOVE_DIRECTION_NEXT = 1;
    private static final int MOVE_DIRECTION_PREV = 2;
    private static final int FLING_SPEED_SLOW = 0;
    private static final int FLING_SPEED_MEDIUM = 1;
    private static final int FLING_SPEED_FAST = 2;
    private static final String TAG = "SL";

    /* Callback to provide previous and Next station */
    public static interface OnStationLayoutListener {
        /**
         * Next Station.
         */
        public void onFlingPrevious(StationLayout layout);

        /**
         * Previous Station
         */
        public void onFlingNext(StationLayout layout);

        /**
         * Show all presets Station
         */
        public void onSingleTap(StationLayout layout);

        /**
         * Show all presets Station
         */
        public void onLongPress(StationLayout layout);

    }

    private OnStationLayoutListener stationLayoutListener;

    private GestureDetector mGestureDetector;


    public boolean onDown(MotionEvent e) {
        return true;
    }

    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
            float velocityY) {
         int direction = MOVE_DIRECTION_NONE;
         float e1x = e1.getX();
         float e2x = e2.getX();
         float e1y = e1.getY();
         float e2y = e2.getY();
         int xdelta = (int)(e1x - e2x);
         int ydelta = (int)(e1y - e2y);
            if( (xdelta > 0) &&    (ydelta > 0)){
                direction = MOVE_DIRECTION_NEXT;
            } else if ( (xdelta < 0) &&    (ydelta < 0)) {
                /* If both > 0, then it is a right/up (prev station) fling */
                direction = MOVE_DIRECTION_PREV;
            } else {
                /* It is more horizontal movement than vertical movement */
                if (Math.abs(xdelta) > Math.abs(ydelta)) {
                    if(xdelta > 0){
                        direction = MOVE_DIRECTION_NEXT;
                    } else {
                        direction = MOVE_DIRECTION_PREV;
                    }
                } else {
                    if(ydelta > 0){
                        direction = MOVE_DIRECTION_NEXT;
                    } else {
                        direction = MOVE_DIRECTION_PREV;
                    }
                }
            }
            if (stationLayoutListener != null) {
                if(direction == MOVE_DIRECTION_PREV) {
                    stationLayoutListener.onFlingPrevious(this);
                    return true;
                } else if(direction == MOVE_DIRECTION_NEXT) {
                    stationLayoutListener.onFlingNext(this);
                    return true;
                }
            }
        return false;
    }

    public void onLongPress(MotionEvent e) {
        /* not used */
        if (stationLayoutListener != null) {
        }
    }

    public boolean onScroll(MotionEvent e1, MotionEvent e2,
            float distanceX, float distanceY) {
    return false;
    }

    public void onShowPress(MotionEvent e) {
        if (stationLayoutListener != null) {
        }
    }

    public boolean onSingleTapUp(MotionEvent e) {
        if (stationLayoutListener != null) {
            stationLayoutListener.onSingleTap(this);
        }
        return false;
    }

    @Override
        public boolean onTouchEvent(MotionEvent e) {
                return mGestureDetector.onTouchEvent(e);
        }

    public StationLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mGestureDetector = new GestureDetector(this);
        mGestureDetector.setIsLongpressEnabled(true);

    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        View    handle = findViewById(R.id.stationinfo_layout);
        if (handle == null) {
            throw new RuntimeException("No 'R.id.stationinfo_layout'");
        }
    }


    public void setOnPanelListener(OnStationLayoutListener onStationListener) {
        stationLayoutListener = onStationListener;
    }
}
