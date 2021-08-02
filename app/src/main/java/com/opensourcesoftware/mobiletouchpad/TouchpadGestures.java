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
        void OnTapEvent(int fingers);
        void OnFlashScreen(long duration);
    }

    private final static String TAG = "TouchGestures";
    private final static float KBOUNDS_XY = 100.f;

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
        actionScroll,
        actionLongPress,
        actionTaps,
        actionMove
    };

    private int mFingersDown = 0;
    private int mTapFingers = 0;
    private int mPinchFingersDown = 0;
    private int mScrollFingersDown = 0;
    private SwipeDirection mSwipeDirection = SwipeDirection.swipeDown;

    private long mLastActionTS = 0;
    private MovementAction mMovementAction = MovementAction.actionNone;
    private PinchDirection mMovePinchDirection;
    private float mPinchScaleFactor = 0.f;

    private TouchGesturesEventsListener mListener = null;
    private boolean moveDrag = false;
    private View targetView = null;

    private final ScaleGestureDetector mScaleDetector;
    private final GestureDetectorCompat mGestureDetectorCompat;
    private final UILongPressGestureRecognizer mLongPressGestureRecognizer;

    private boolean mScrollNatural = false;
    private float mScrollMultiplier = 1.f;

    private long mBounceTimeOutMS = 55;

//    private final Handler mainHandler = new Handler();

    public TouchpadGestures(View v, TouchGesturesEventsListener listener) {
        targetView = v;
        mListener = listener;

        mScaleDetector = new ScaleGestureDetector(v.getContext(), this);
        mScaleDetector.setQuickScaleEnabled(false);
        mGestureDetectorCompat = new GestureDetectorCompat(v.getContext(), this);

        mLongPressGestureRecognizer = new UILongPressGestureRecognizer(() -> {
//                Log.d(TAG, "onLongPressEvent: FINGERS: " + mFingersDown);
            mMovementAction = MovementAction.actionLongPress;
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
        });
    }

    public void setBounce(long bounce) {
        mBounceTimeOutMS = bounce;
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
        boolean isScrollOrNone = (mMovementAction == MovementAction.actionNone) || (mMovementAction == MovementAction.actionScroll);
//        Log.d(TAG, "onScroll: " + swipeDirection.toString() + " X: " + distanceX + " Y: " + distanceY + " FINGERS: " + mFingersDown);
        if ((mFingersDown > 2) && (mMovementAction == MovementAction.actionNone)) {
            // swipe with 3 or more fingers
            if (mFingersDown >= mScrollFingersDown) {
                mScrollFingersDown = mFingersDown;
                mSwipeDirection = swipeDirection;
                mMovementAction = MovementAction.actionSwipe;
            }
            mLastActionTS = currTS;
            return true;
        } else if ((mFingersDown == 2) && !isBounce(mLastActionTS, currTS) && isScrollOrNone) {
            mMovementAction = MovementAction.actionScroll;
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

            sendOnMoveEvent(x, y);
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

//        Log.d(TAG, "swipeDirectionFromXYDist: " + direction.toString() + " dist: " + dist + " x: " + xDist + " y: " + yDist);
        return direction;
    }

    private boolean isBounce(long prevTMS, long currTMS) {
        return ((currTMS - prevTMS) <= mBounceTimeOutMS);
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
            mTapFingers = 1;
            mScrollFingersDown = 0;
            mMovementAction = MovementAction.actionNone;
        } else if (action == MotionEvent.ACTION_POINTER_DOWN) {
            // two or more fingers down
            mFingersDown++;
            mTapFingers++;
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
            } else if ((mMovementAction == MovementAction.actionNone) && (mTapFingers > 1)) {
//                Log.d(TAG, "onTouch: UP " + mTapFingers);
                mListener.OnTapEvent(mTapFingers);
            }
            mMovementAction = MovementAction.actionNone;
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

                    sendOnMoveEvent(x, y);
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


