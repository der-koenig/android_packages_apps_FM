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

package com.quic.utils;

import android.content.Context;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;

/**
 * A button that will repeatedly call a 'listener' method
 * as long as the button is pressed.
 */
public class RepeatingImageButton extends ImageButton {

        private long mStartTime;
        private int mRepeatCount;
        private RepeatListener mListener;
        private long mInterval = 500;

        public RepeatingImageButton(Context context) {
                this(context, null);
        }

        public RepeatingImageButton(Context context, AttributeSet attrs) {
                this(context, attrs, android.R.attr.imageButtonStyle);
        }

        public RepeatingImageButton(Context context, AttributeSet attrs, int defStyle) {
                super(context, attrs, defStyle);
                setFocusable(true);
                setLongClickable(true);
        }

        /**
         * Sets the listener to be called while the button is pressed and
         * the interval in milliseconds with which it will be called.
         * @param l The listener that will be called
         * @param interval The interval in milliseconds for calls
         */
        public void setRepeatListener(RepeatListener l, long interval) {
                mListener = l;
                mInterval = interval;
        }

        @Override
        public boolean performLongClick() {
                mStartTime = SystemClock.elapsedRealtime();
                mRepeatCount = 0;
                post(mRepeater);
                return true;
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                        /* remove the repeater, but call the hook one more time */
                        removeCallbacks(mRepeater);
                        if (mStartTime != 0) {
                                doRepeat(true);
                                mStartTime = 0;
                        }
                }
                return super.onTouchEvent(event);
        }

        @Override
        public boolean onKeyUp(int keyCode, KeyEvent event) {
                switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                        /* remove the repeater, but call the hook one more time */
                        removeCallbacks(mRepeater);
                        if (mStartTime != 0) {
                                doRepeat(true);
                                mStartTime = 0;
                        }
                }
                return super.onKeyUp(keyCode, event);
        }

        private Runnable mRepeater = new Runnable() {
                public void run() {
                        doRepeat(false);
                        if (isPressed()) {
                                postDelayed(this, mInterval);
                        }
                }
        };

        private    void doRepeat(boolean last) {
                long now = SystemClock.elapsedRealtime();
                if (mListener != null) {
                        mListener.onRepeat(this, now - mStartTime, last ? -1 : mRepeatCount++);
                }
        }

        public interface RepeatListener {
                /**
                 * This method will be called repeatedly at roughly the interval
                 * specified in setRepeatListener(), for as long as the button
                 * is pressed.
                 * @param v The button as a View.
                 * @param duration The number of milliseconds the button has been pressed so far.
                 * @param repeatcount The number of previous calls in this sequence.
                 * If this is going to be the last call in this sequence (i.e. the user
                 * just stopped pressing the button), the value will be -1.
                 */
                void onRepeat(View v, long duration, int repeatcount);
        }
}
