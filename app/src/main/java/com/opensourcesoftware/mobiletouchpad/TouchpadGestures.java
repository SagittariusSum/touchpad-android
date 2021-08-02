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

import android.os.Build;
import android.os.Handler;
import android.os.VibrationEffect;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewConfiguration;
import android.util.Log;
import android.os.Vibrator;

import androidx.core.view.GestureDetectorCompat;

/*
    NOTES:
    - if pointer on screen moves in a choppy fashion, go into your phone Settings and change
    pointer speed from default value to something else and see if it improves
*/

import static android.content.Context.VIBRATOR_SERVICE;

public class TouchpadGestures extends GestureDetector.SimpleOnGestureListener
        implements
            View.OnTouchListener,
            ScaleGestureDetector.OnScaleGestureListener {

    public interface TouchGesturesEventsListener {
        void OnSwipeEvent(SwipeDirection direction, int fingers);
        void OnPinchEvent(PinchDirection direction, int fingers);
        void OnMoveDragBeginEvent();
        void OnMoveDragEndEvent();
        void OnMoveEvent(int x, int y);
        void OnScrollDownEvent(String speed);
        void OnScrollUpEvent(String speed);
        void OnScrollLeftEvent(String speed);
        void OnScrollRightEvent(String speed);
        void OnClickDefaultEvent();
        void OnClickOptionsEvent();
        void OnClickDoubleEvent();
        void OnFlashScreen(long duration);
    }

    private final static String TAG = "TouchGestures";
    private final static float KBOUNDS_XY = 100.f;
    private final static long KBOUNCE_MS = 55;

    public enum SwipeDirection {
        swipeUp,
        swipeDown,
        swipeLeft,
        swipeRight
    };

    public enum PinchDirection {
        pinchIn,
        pinchOut
    };

    public enum MovementAction {
        actionNone,
        actionSwipe,
        actionPinch,
        actionTaps,
        actionMove
    };

    public interface UILongPressGestureRecognizerListener {
        void onLongPressEvent();
    }

    private class UILongPressGestureRecognizer {

        private UILongPressGestureRecognizerListener mListener = null;
        private final Handler mHandler = new Handler();
        private Runnable mLongPressed = () -> mListener.onLongPressEvent();
        private long mTouchDownTS;
        private int mLongPressTimeout = 500;

        public UILongPressGestureRecognizer(UILongPressGestureRecognizerListener longPressListener) {
            this.mListener = longPressListener;
            this.mLongPressTimeout = ViewConfiguration.getLongPressTimeout();
        }

        private void resetHandler(boolean addCallback) {
            mHandler.removeCallbacks(mLongPressed);
            if (addCallback) mHandler.postDelayed(mLongPressed, mLongPressTimeout);
        }

        public boolean onTouchEvent(MotionEvent event) {
            int action = (event.getAction() & MotionEvent.ACTION_MASK);

            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    resetHandler(true);
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
//                    Log.d(TAG, "UILongPressGestureRecognizer onTouchEvent: ACTION_POINTER_DOWN");
                    resetHandler(true);
                    mTouchDownTS = System.currentTimeMillis();
                    break;
                case MotionEvent.ACTION_MOVE:
                    long tsSinceTouchDown = System.currentTimeMillis() - mTouchDownTS;
                    if ((tsSinceTouchDown < 60))
                        break;
                case MotionEvent.ACTION_UP:
//                    Log.d(TAG, "UILongPressGestureRecognizer onTouchEvent: " + (action == MotionEvent.ACTION_MOVE ? "ACTION_MOVE" : "ACTION_UP"));
                    resetHandler(false);
                    break;
            }
            return true;
        }
    }

    private int mFingersDown = 0;
    private int mPinchFingersDown = 0;
    private int mScrollFingersDown = 0;
    private SwipeDirection mSwipeDirection = SwipeDirection.swipeDown;

    private long mLastActionTS = 0;
    private MovementAction mMovementAction = MovementAction.actionNone;
    private PinchDirection mMovePinchDirection;
    private float mPinchScaleFactor = 0.f;

    private TouchGesturesEventsListener mListener = null;

//    private boolean rightIsTop = true;
    private boolean rightIsTop = false;
    private boolean moveDrag = false;
    private View targetView = null;

    private final ScaleGestureDetector mScaleDetector;
    private final GestureDetectorCompat mGestureDetectorCompat;
    private final UILongPressGestureRecognizer mLongPressGestureRecognizer;

    private boolean mScrollNatural = false;
    private float mScrollMultiplier = 1.f;

//    private final Handler mainHandler = new Handler();

    public TouchpadGestures(View v, TouchGesturesEventsListener listener) {
        targetView = v;
        mListener = listener;

        mScaleDetector = new ScaleGestureDetector(v.getContext(), this);
        mScaleDetector.setQuickScaleEnabled(false);
        mGestureDetectorCompat = new GestureDetectorCompat(v.getContext(), this);

        mLongPressGestureRecognizer = new UILongPressGestureRecognizer(new UILongPressGestureRecognizerListener() {
            @Override
            public void onLongPressEvent() {
//                Log.d(TAG, "onLongPressEvent: FINGERS: " + mFingersDown);
                moveDrag = (mFingersDown == 1);
                if (moveDrag) {
//                    Log.d(TAG, "onLongPressEvent: MOVE_DRAG_BEGIN");
                    vibrate(100);
                    mListener.OnMoveDragBeginEvent();
                }
                if (mFingersDown == 2) {
                    vibrate(150);
                    mListener.OnClickOptionsEvent();
                }
            }
        });
    }

    public void setScrollMultiplier(float multiplier) {
        mScrollMultiplier = multiplier;
    }

    public void setScrollNatural(boolean natural) {
        mScrollNatural = natural;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
//        Log.d(TAG, "onScaleBegin: FINGERS " + mFingersDown);
        mPinchFingersDown = mFingersDown;
        mMovementAction = MovementAction.actionPinch;
        return true;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
//        Log.d(TAG, "onScale: ");
        mPinchScaleFactor = detector.getScaleFactor();
        if ((mPinchScaleFactor < 0.7f || mPinchScaleFactor > 1.3f) && (mMovementAction == MovementAction.actionPinch)) {
            mMovePinchDirection = (mPinchScaleFactor < 1.f) ?
                    PinchDirection.pinchOut :
                    PinchDirection.pinchIn;
        }
        return false;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        if (mMovementAction != MovementAction.actionPinch) return;
        mMovementAction = MovementAction.actionNone;
//        Log.d(TAG, "onScaleEnd: " + mPinchScaleFactor);
        long currTS = System.currentTimeMillis();
        if (!isBounce(mLastActionTS, currTS)) {
            mLastActionTS = currTS;
            mListener.OnPinchEvent(mMovePinchDirection, mPinchFingersDown);
        }
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
//        Log.d(TAG, "onFling: FINGERS " + mScrollFingersDown);
//        if (mScrollFingersDown >= 3) {
//            if (isBounce(lastMovementActionTS, System.currentTimeMillis())) {
//                return true;
//            }
//            lastMovementActionTS = System.currentTimeMillis();
//            mListener.OnSwipeEvent(mSwipeDirection, mScrollFingersDown);
//        }
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if (mMovementAction == MovementAction.actionPinch && mFingersDown != 1) return true;
        SwipeDirection swipeDirection = swipeDirectionFromXYDist(distanceX, distanceY);
        long currTS = System.currentTimeMillis();
        Log.d(TAG, "onScroll: " + swipeDirection.toString() + " X: " + distanceX + " Y: " + distanceY + " FINGERS: " + mFingersDown);
        if ((mFingersDown > 2) && (mMovementAction == MovementAction.actionNone)) {
            // swipe with 3 or more fingers
            if (mFingersDown >= mScrollFingersDown) {
                mScrollFingersDown = mFingersDown;
                mSwipeDirection = swipeDirection;
                mMovementAction = MovementAction.actionSwipe;
            }
            mLastActionTS = currTS;
            return true;
        } else if ((mFingersDown == 2) && !isBounce(mLastActionTS, currTS) && (mMovementAction == MovementAction.actionNone)) {
            // scroll with 2 fingers
            String speed = "FAST";
            float absX = Math.abs(distanceX);
            float absY = Math.abs(distanceY);
            float dist = Math.max(absX, absY);
            if (dist <= 15.f) {
                speed = "SLOW";
            } else if (dist <= 30.f) {
                speed = "NORMAL";
            }
            speed += " " + dist;
            float scrollDelta = 5;
            if (dist >= scrollDelta) {
                switch (swipeDirection) {
                    case swipeUp:
                        if (mScrollNatural) {
                            mListener.OnScrollDownEvent(speed);
                        } else mListener.OnScrollUpEvent(speed); break;
                    case swipeDown:
                        if (mScrollNatural) {
                            mListener.OnScrollUpEvent(speed);
                        } else mListener.OnScrollDownEvent(speed); break;
                    case swipeLeft: mListener.OnScrollLeftEvent(speed); break;
                    case swipeRight: mListener.OnScrollRightEvent(speed); break;
                }
                mLastActionTS = System.currentTimeMillis();
            }
        } else if (!isBounce(mLastActionTS, currTS) && (mMovementAction == MovementAction.actionNone)) {
            // move cursor with one finger
            float x = -distanceX;
            float y = -distanceY;
            if (rightIsTop) {
                // rotate 90 degrees counter-clockwise
                y = distanceX * 2;
                x = -distanceY * 2;
            }

            if ((x != 0) && (y != 0)) {
                sendOnMoveEvent(x, y);
            }
        }
        return true;
    }

    private float limitToBounds(float value, float lower, float upper) {
        if (value > upper) {
            return upper;
        } else if (value < lower) {
            return lower;
        }
        return value;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
//        Log.d(TAG, "onSingleTapConfirmed: ");
        mListener.OnClickDefaultEvent();
        return super.onSingleTapConfirmed(e);
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
//        Log.d(TAG, "onDoubleTap: ");
        mListener.OnClickDoubleEvent();
        return super.onDoubleTap(e);
    }

    private SwipeDirection swipeDirectionFromXYDist(float xDist, float yDist) {
        SwipeDirection direction;
        float dist = 0;
        if (Math.abs(xDist) > Math.abs(yDist)) {
            // x axis
            direction = xDist > 0 ? SwipeDirection.swipeLeft : SwipeDirection.swipeRight;
            dist = Math.abs(xDist);
        } else {
            // y axis
            direction = yDist > 0 ? SwipeDirection.swipeUp : SwipeDirection.swipeDown;

            // natural scroll support ^_^
//            if (mScrollNatural && (mFingersDown == 2)) {
//                direction = (direction == SwipeDirection.swipeDown) ?
//                        SwipeDirection.swipeUp :
//                        SwipeDirection.swipeDown;
//            }

            dist = Math.abs(yDist);
        }
        if (rightIsTop) {
            // translate 90 degrees counter-clockwise
            switch (direction) {
                case swipeUp: direction = SwipeDirection.swipeLeft; break;
                case swipeDown: direction = SwipeDirection.swipeRight; break;
                case swipeLeft: direction = SwipeDirection.swipeUp; break;
                case swipeRight: direction = SwipeDirection.swipeDown; break;
            }
        }
//        Log.d(TAG, "swipeDirectionFromXYDist: " + direction.toString() + " dist: " + dist + " x: " + xDist + " y: " + yDist);
        return direction;
    }

    private boolean isBounce(long prevTMS, long currTMS) {
        return ((currTMS - prevTMS) <= KBOUNCE_MS);
    }

    private void vibrate(long duration) {
        mListener.OnFlashScreen((duration < 200) ? duration * 2 : duration);
        Vibrator v = (Vibrator)targetView.getContext().getSystemService(VIBRATOR_SERVICE);
        if (v == null) return;
        if (Build.VERSION.SDK_INT >= 26) {
            v.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            v.vibrate(duration);
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int action = (event.getAction() & MotionEvent.ACTION_MASK);
        long currentTimeMS = System.currentTimeMillis();

        if (action == MotionEvent.ACTION_DOWN) {
            // at least one finger down
            mFingersDown = 1;
            mScrollFingersDown = 0;
            mMovementAction = MovementAction.actionNone;
        } else if (action == MotionEvent.ACTION_POINTER_DOWN) {
            // two or more fingers down
            mFingersDown++;
        } else if (action == MotionEvent.ACTION_POINTER_UP) {
            // one finger was raised from the screen surface
            mFingersDown--;
        } else if (action == MotionEvent.ACTION_UP) {
            // all fingers are raised from the screen surface
            if (moveDrag) {
                moveDrag = false;
                mListener.OnMoveDragEndEvent();
            } else if (mMovementAction == MovementAction.actionSwipe) {
                mListener.OnSwipeEvent(mSwipeDirection, mScrollFingersDown);
            }
        } else if (action == MotionEvent.ACTION_MOVE) {
            if (moveDrag) {
                int historySize = event.getHistorySize();
                if (historySize > 1) {
                    float x0 = event.getHistoricalX(historySize -2);
                    float x1 = event.getHistoricalX(historySize -1);
                    float y0 = event.getHistoricalY(historySize -2);
                    float y1 = event.getHistoricalY(historySize -1);
                    float x = x1 - x0;
                    float y = y1 - y0;

                    if (rightIsTop) {
                        // rotate 90 degrees counter-clockwise
                        float t = y;
                        y = -x;
                        x = t;
                    }

                    if ((x != 0) && (y != 0)) {
                        sendOnMoveEvent(x, y);
                    }
                }
                return true;
            }
        }


        mScaleDetector.onTouchEvent(event);
        mGestureDetectorCompat.onTouchEvent(event);
        mLongPressGestureRecognizer.onTouchEvent(event);

        return true;
    }

    private void sendOnMoveEvent(float x, float y) {
        x = limitToBounds(x, -KBOUNDS_XY, KBOUNDS_XY) * mScrollMultiplier;
        y = limitToBounds(y, -KBOUNDS_XY, KBOUNDS_XY) * mScrollMultiplier;
        mListener.OnMoveEvent(Math.round(x), Math.round(y));
    }

}


