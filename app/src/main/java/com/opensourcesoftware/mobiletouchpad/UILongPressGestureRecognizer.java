package com.opensourcesoftware.mobiletouchpad;

import android.os.Handler;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

public class UILongPressGestureRecognizer {

    public interface UILongPressGestureRecognizerListener {
        void onLongPressEvent();
    }

    private final String TAG = "UILongPressGestureRecog";

    private UILongPressGestureRecognizerListener mListener = null;
    private final Handler mHandler = new Handler();
    private final Runnable mLongPressed = () -> mListener.onLongPressEvent();
    private long mTouchDownTS;
    private int mLongPressTimeout = 500;
    private boolean mInitLastXY = false;
    private boolean mHasCallbacks = false;
    private float mLastX = 0.f;
    private float mLastY = 0.f;

    public UILongPressGestureRecognizer(UILongPressGestureRecognizerListener longPressListener) {
        this.mListener = longPressListener;
        this.mLongPressTimeout = ViewConfiguration.getLongPressTimeout();
    }

    private void resetHandler(boolean addCallback) {
        mHasCallbacks = false;
        mHandler.removeCallbacks(mLongPressed);
        if (addCallback) {
            mHandler.postDelayed(mLongPressed, mLongPressTimeout);
            mHasCallbacks = true;
        }
    }

    public boolean onTouchEvent(MotionEvent event) {
        int action = (event.getAction() & MotionEvent.ACTION_MASK);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                resetHandler(true);
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
//                    Log.d(TAG, "onTouchEvent: ACTION_POINTER_DOWN");
                resetHandler(true);
                mTouchDownTS = System.currentTimeMillis();
                break;
            case MotionEvent.ACTION_MOVE:
                float currX = event.getX();
                float currY = event.getY();
//                    Log.d(TAG, "onTouchEvent: ACTION_MOVE " + currX + " / " + currY);
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
//                    Log.d(TAG, "onTouchEvent: ACTION_UP");
                resetHandler(false);
                break;
        }
        return true;
    }
}
