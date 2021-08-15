/*
Copyright(c) Dorin Duminica. All rights reserved.
Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
  1. Redistributions of source code must retain the above copyright notice,
	 this list of conditions and the following disclaimer.
  2. Redistributions in binary form must reproduce the above copyright notice,
	 this list of conditions and the following disclaimer in the documentation
	 and/or other materials provided with the distribution.
  3. Neither the name of the copyright holder nor the names of its
	 contributors may be used to endorse or promote products derived from this
	 software without specific prior written permission.
THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.opensourcesoftware.mobiletouchpad;

import android.os.Handler;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

public class UILongPressGestureRecognizer {

    public interface UILongPressGestureRecognizerListener {
        void onLongPressEvent();
    }

    private final String TAG = "UILongPressGestureRecog";

    public static final long KLONG_PRESS_TIMEOUT_MAX = 500;

    private UILongPressGestureRecognizerListener mListener = null;
    private final Handler mHandler = new Handler();
    private final Runnable mLongPressed = () -> mListener.onLongPressEvent();
    private long mTouchDownTS;
    private long mLongPressTimeout = KLONG_PRESS_TIMEOUT_MAX;
    private boolean mInitLastXY = false;
    private boolean mHasCallbacks = false;
    private float mLastX = 0.f;
    private float mLastY = 0.f;

    public UILongPressGestureRecognizer(UILongPressGestureRecognizerListener longPressListener) {
        Logging.d(TAG, "UILongPressGestureRecognizer()");
        this.mListener = longPressListener;
        this.mLongPressTimeout = ViewConfiguration.getLongPressTimeout();
        if (this.mLongPressTimeout > KLONG_PRESS_TIMEOUT_MAX) {
            this.mLongPressTimeout = KLONG_PRESS_TIMEOUT_MAX;
        }
    }

    private void resetHandler(boolean addCallback) {
        Logging.d(TAG, "resetHandler");
        mHasCallbacks = false;
        mHandler.removeCallbacks(mLongPressed);
        if (addCallback) {
            mHandler.postDelayed(mLongPressed, mLongPressTimeout);
            mHasCallbacks = true;
        }
    }

    public long getLongPressTimeOut() {
        return mLongPressTimeout;
    }

    public boolean onTouchEvent(MotionEvent event) {
        int action = (event.getAction() & MotionEvent.ACTION_MASK);

        Logging.d(TAG, "onTouchEvent action: " + action);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                resetHandler(true);
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                Logging.d(TAG, "onTouchEvent: ACTION_POINTER_DOWN");
                resetHandler(true);
                mTouchDownTS = System.currentTimeMillis();
                break;
            case MotionEvent.ACTION_MOVE:
                float currX = event.getX();
                float currY = event.getY();
                Logging.d(TAG, "onTouchEvent: ACTION_MOVE " + currX + " / " + currY);
                long tsSinceTouchDown = System.currentTimeMillis() - mTouchDownTS;
                boolean ignoreMovement = ((Math.abs(currX - mLastX) < 5) && (Math.abs(currY - mLastY) < 5));
                if ((tsSinceTouchDown < 60) || (!mInitLastXY) || ignoreMovement) {
                    mInitLastXY = true;
                    mLastX = currX;
                    mLastY = currY;
                    if (!mHasCallbacks) resetHandler(true);
                    break;
                }
            case MotionEvent.ACTION_UP:
                Logging.d(TAG, "onTouchEvent: ACTION_UP");
                resetHandler(false);
                break;
        }
        return true;
    }
}


