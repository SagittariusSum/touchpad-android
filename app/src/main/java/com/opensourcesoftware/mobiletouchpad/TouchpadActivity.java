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

import android.annotation.SuppressLint;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.opensourcesoftware.mobiletouchpad.databinding.ActivityTouchpadBinding;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class TouchpadActivity extends AppCompatActivity
        implements TouchpadGestures.TouchGesturesEventsListener,
                    DeviceShakeDetector.DeviceShakeDetectorListener {

    public static final ConcurrentLinkedQueue<String> mUDPCmdQueue = new ConcurrentLinkedQueue<String>();
    private Thread mUDPClientThread = null;

    private static final String TAG = "TouchpadActivity";

    private enum StatusType {
        OK,
        Error,
    }

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mMainHandler = new Handler();
    private View mContentView;
    private TextView mStatusTextView;
    private PowerManager.WakeLock mWakeLock = null;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
//            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = () -> hide();

    private ActivityTouchpadBinding binding;
    private DeviceShakeDetector mDeviceShakeDetector = new DeviceShakeDetector(this);
    private TouchpadGestures mTouchpadGestures = null;
    private boolean mWifiIsOnline = false;
    private boolean mWifiFirstGo = true;
    private int mContentBackgroundColor;
    private int mContentBackgroundColorFlash;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityTouchpadBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setTitle(getResources().getString(R.string.app_name));

        mVisible = true;
        mContentView = binding.fullscreenContent;
        mStatusTextView = binding.statusTextView;

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

        AppPrefs.loadPreferences(this);

        Resources resources = getResources();
        Resources.Theme theme = getTheme();
        mContentBackgroundColor = resources.getColor(R.color.black, theme);
        mContentBackgroundColorFlash = resources.getColor(R.color.flash, theme);

        mTouchpadGestures = new TouchpadGestures(mContentView, this);
        mContentView.setOnTouchListener(mTouchpadGestures);

        PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Touchpad:wakelock");

        mDeviceShakeDetector.initAccelerometer(this);
        mDeviceShakeDetector.registerListener();

        applySettings();

        findViewById(R.id.ivSettings).setOnClickListener(v -> showSettings());

        this.registerReceiver(this.WifiStateChangedReceiver,
                new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));

        if (AppPrefs.getHostSystemIP().isEmpty()) {
            mMainHandler.postDelayed(() -> showSettings(), 3000);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mDeviceShakeDetector.unregisterListener();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        delayedHide(100);
        mDeviceShakeDetector.registerListener();
        applySettings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mWakeLock != null) {
            mWakeLock.acquire(10*60*1000L /*10 minutes*/);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mWakeLock != null) {
            mWakeLock.release();
        }
    }

    private void applySettings() {
        AppPrefs.loadPreferences(this);
        mTouchpadGestures.setBounce(AppPrefs.getBounce());
        mTouchpadGestures.setScrollMultiplier(AppPrefs.getScrollMultiplier());
        mTouchpadGestures.setScrollNatural(AppPrefs.getScrollNatural());
        startUDPClientThread(AppPrefs.getHostSystemIP(), AppPrefs.getHostPort());
    }

    private void setStatusColors(StatusType statusType) {
        int colorBg = 0;
        int colorFg = 0;
        Resources.Theme theme = getTheme();
        Resources resources = getResources();
        switch (statusType) {
            case OK:
                colorBg = resources.getColor(R.color.conn_bg_ok, theme);
                colorFg = resources.getColor(R.color.conn_fg_ok, theme);
                break;
            case Error:
                colorBg = resources.getColor(R.color.conn_bg_err, theme);
                colorFg = resources.getColor(R.color.conn_fg_err, theme);
                break;
        }
        mStatusTextView.setBackgroundColor(colorBg);
        mStatusTextView.setTextColor(colorFg);
    }

    private void setStatusText(StatusType statusType, String text) {
        setStatusColors(statusType);
        mStatusTextView.setText(text);
    }

    private final BroadcastReceiver WifiStateChangedReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            int extraWifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                    WifiManager.WIFI_STATE_UNKNOWN);

            switch (extraWifiState) {
                case WifiManager.WIFI_STATE_ENABLED:
                    mWifiIsOnline = true;
                    if (mWifiFirstGo) {
                        mWifiFirstGo = false;
                        startUDPClientThread(AppPrefs.getHostSystemIP(), AppPrefs.getHostPort());
                    } else {
                        // wifi was off and then on
                        // give it about 5 seconds
                        mMainHandler.postDelayed(() -> startUDPClientThread(AppPrefs.getHostSystemIP(), AppPrefs.getHostPort()), 5000);
                    }
                    break;
                case WifiManager.WIFI_STATE_DISABLED:
                case WifiManager.WIFI_STATE_DISABLING:
                    mWifiIsOnline = false;
                    setStatusText(StatusType.Error, "WIFI IS OFFLINE!");
                    break;
            }

        }
    };

    @Override
    public void onDeviceShakeDetected() {
        Log.d(TAG, "onDeviceShakeDetected: SHAKE");

//        showSettings();
    }

    private void showSettings() {
        Intent i = new Intent(this, SettingsActivity.class);
        startActivity(i);
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
//        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mMainHandler.removeCallbacks(mShowPart2Runnable);
        mMainHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mMainHandler.removeCallbacks(mHideRunnable);
        mMainHandler.postDelayed(mHideRunnable, delayMillis);
    }

    @Override
    public void OnSwipeEvent(TouchpadGestures.SwipeDirection direction, int fingers) {
//        Log.d(TAG, "OnSwipeEvent: " + direction.toString() + " fingers: " + String.valueOf(fingers));
        int up = 0;
        int down = 0;
        int left = 0;
        int right = 0;
        switch (direction) {
            case swipeUp:
                up++;
                break;
            case swipeDown:
                down++;
                break;
            case swipeLeft:
                left++;
                break;
            case swipeRight:
                right++;
                break;
        }
        sendGestureEvent(String.format(Locale.ENGLISH, CmdConsts.KGESTURE_SWIPE + " %d U%d,D%d,L%d,R%d ok",
                fingers,
                up,
                down,
                left,
                right));
    }

    @Override
    public void OnPinchEvent(TouchpadGestures.PinchDirection direction, int fingers) {
//        Log.d(TAG, "OnPinchEvent: " + direction.toString() + " fingers: " + String.valueOf(fingers));
        sendGestureEvent(CmdConsts.KGESTURE_PINCH + " " + (direction == TouchpadGestures.PinchDirection.pinchIn ? "IN" : "OUT") + " " + fingers + " ok");
    }

    @Override
    public void OnMoveDragBeginEvent() {
//        Log.d(TAG, "OnMoveDragBeginEvent: ");
        sendGestureEvent(CmdConsts.KACTION_MOVE_DRAG_BEGIN);
    }

    @Override
    public void OnMoveDragEndEvent() {
//        Log.d(TAG, "OnMoveDragEndEvent: ");
        sendGestureEvent(CmdConsts.KACTION_MOVE_DRAG_END);
    }

    @Override
    public void OnMoveEvent(int x, int y) {
//        Log.d(TAG, "OnMoveEvent: ");
        sendGestureEvent(CmdConsts.KACTION_MOVE + " " + x + " " + y);
    }

    @Override
    public void OnScrollDownEvent(String speed) {
//        Log.d(TAG, "OnScrollDownEvent: " + speed);
        sendGestureEvent(CmdConsts.KSCROLL_DOWN + " " + speed);
    }

    @Override
    public void OnScrollUpEvent(String speed) {
//        Log.d(TAG, "OnScrollUpEvent: " + speed);
        sendGestureEvent(CmdConsts.KSCROLL_UP + " " + speed);
    }

    @Override
    public void OnScrollLeftEvent(String speed) {
//        Log.d(TAG, "OnScrollLeftEvent: " + speed);
        sendGestureEvent(CmdConsts.KSCROLL_LEFT + " " + speed);
    }

    @Override
    public void OnScrollRightEvent(String speed) {
//        Log.d(TAG, "OnScrollRightEvent: " + speed);
        sendGestureEvent(CmdConsts.KSCROLL_RIGHT + " " + speed);
    }

    @Override
    public void OnClickDefaultEvent() {
//        Log.d(TAG, "OnClickDefaultEvent: ");
        sendGestureEvent(CmdConsts.KACTION_CLICK_DEFAULT);
    }

    @Override
    public void OnClickOptionsEvent() {
//        Log.d(TAG, "OnClickOptionsEvent: ");
        sendGestureEvent(CmdConsts.KACTION_CLICK_OPTIONS);
    }

    @Override
    public void OnClickDoubleEvent() {
//        Log.d(TAG, "OnClickDoubleEvent: ");
        sendGestureEvent(CmdConsts.KACTION_CLICK_DOUBLE);
    }

    @Override
    public void OnTapEvent(int fingers) {
        sendGestureEvent(CmdConsts.KACTION_TAP + " " + fingers);
    }

    @Override
    public void OnFlashScreen(long duration) {
        mContentView.setBackgroundColor(mContentBackgroundColorFlash);
        mMainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mContentView.setBackgroundColor(mContentBackgroundColor);
            }
        }, duration);
    }

    private void sendGestureEvent(String s) {
//        Log.d(TAG, "sendGestureEvent: " + s);
        mUDPCmdQueue.add(s);
    }

    private void startUDPClientThread(String hostIP, Integer hostPort) {
        //TODO PING-PONG for connection status
        if (mUDPClientThread != null) {
            mUDPClientThread.interrupt();
        }
        if (hostIP.isEmpty()) {
            setStatusText(StatusType.Error, getText(R.string.settings_notif_notset).toString());
            return;
        }
        if (!mWifiIsOnline) {
            return;
        }
        mUDPCmdQueue.clear();
        setStatusText(StatusType.OK, String.format("%s / %s", AppPrefs.getHostSystemName(), AppPrefs.getHostSystemIP()));
        mUDPClientThread = new UDPClientThread(hostIP, hostPort, mUDPCmdQueue);
        mUDPClientThread.start();
    }

}


