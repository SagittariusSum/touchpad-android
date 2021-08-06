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

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;

public final class AppPrefs {

    public static final Integer KPORT_MIN = 1024;
    public static final Integer KPORT_MAX = 65535;
    public static final Integer KPORT_DEFAULT = 19999;

    // bounce values are in milliseconds
    public static final Integer KBOUNCE_MIN = 5;
    public static final Integer KBOUNCE_MAX = 150;
    public static final Integer KBOUNCE_DEFAULT = 55;

    public static final Integer KVIBRATE_SHORT = 100;
    public static final Integer KVIBRATE_LONG = 150;

    public static final long KPING_INTERVAL = 1000;

    private static final String KAPP_PREFS_NAME = "app_prefs";
    private static final String KKEY_SCROLL_MULTIPLIER = "SCROLL.MULTIPLIER";
    private static final String KKEY_SCROLL_NATURAL = "SCROLL.NATURAL";
    private static final String KKEY_HOST_SYSTEM = "HOST_SYSTEM";
    private static final String KKEY_HOST_PORT = "HOST_PORT";
    private static final String KKEY_BOUNCE = "BOUNCE";
    private static float mScrollMultiplier = 2.f;
    private static boolean mScrollNatural = false;
    private static String mHostSystem = ""; // stored HOST_NAME{SPACE}IP
    private static Integer mHostPort = KPORT_DEFAULT;
    private static Integer mBounce = KBOUNCE_DEFAULT;

    public static void loadPreferences(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(KAPP_PREFS_NAME, Context.MODE_PRIVATE);
        mScrollMultiplier = prefs.getFloat(KKEY_SCROLL_MULTIPLIER, 2.f);
        mScrollNatural = prefs.getBoolean(KKEY_SCROLL_NATURAL, false);
        mHostSystem = prefs.getString(KKEY_HOST_SYSTEM, "");

        mHostPort = prefs.getInt(KKEY_HOST_PORT, KPORT_DEFAULT);
        if (!isPortValid(mHostPort)) mHostPort = KPORT_DEFAULT;

        mBounce = prefs.getInt(KKEY_BOUNCE, KBOUNCE_DEFAULT);
        if (!isBounceValid(mBounce)) mBounce = KBOUNCE_DEFAULT;
    }

    public static void savePreferences(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(KAPP_PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat(KKEY_SCROLL_MULTIPLIER, mScrollMultiplier);
        editor.putBoolean(KKEY_SCROLL_NATURAL, mScrollNatural);
        editor.putString(KKEY_HOST_SYSTEM, mHostSystem);
        editor.putInt(KKEY_HOST_PORT, mHostPort);
        editor.putInt(KKEY_BOUNCE, mBounce);
        editor.apply();
    }

    public static String getMsgErrInvalidPortRange(Context context) {
        return String.format(Locale.ENGLISH,
                context.getString(R.string.msg_settings_invalid_port_range),
                KPORT_MIN,
                KPORT_MAX);
    }

    public static String getMsgErrInvalidBounceRange(Context context) {
        return String.format(Locale.ENGLISH,
                context.getString(R.string.msg_settings_invalid_bounce_range),
                KBOUNCE_MIN,
                KBOUNCE_MAX);
    }

    private static boolean isIntInRange(Integer value, Integer start, Integer end) {
        return ((value >= start) && (value <= end));
    }

    public static boolean isPortValid(Integer port) {
        return isIntInRange(port, KPORT_MIN, KPORT_MAX);
    }

    public static boolean isBounceValid(Integer bounce) {
        return isIntInRange(bounce, KBOUNCE_MIN, KBOUNCE_MAX);
    }

    public static String formatMultiplier(float multiplier) {
        return String.format(Locale.ENGLISH, "x%.1f", multiplier);
    }

    public static String getScrollMultiplierText() {
        return formatMultiplier(getScrollMultiplier());
    }

    public static float getScrollMultiplier() {
        return mScrollMultiplier;
    }

    public static void setScrollMultiplier(float scrollMultiplier) {
        mScrollMultiplier = scrollMultiplier;
    }

    public static boolean getScrollNatural() {
        return mScrollNatural;
    }

    public static void setScrollNatural(boolean scrollNatural) {
        mScrollNatural = scrollNatural;
    }

    public static void setHostSystem(String name, String hostIP) {
        mHostSystem = name + " " + hostIP;
    }

    public static void setHostPort(Integer port) {
        if (isPortValid(port)) mHostPort = port;
    }

    public static void setBounce(Integer bounce) {
        if (isBounceValid(bounce)) mBounce = bounce;
    }

    private static String[] getHostSystemParts() {
        return mHostSystem.split("\\s+");
    }

    private static String getHostSystemPart(int index, String defaultValue) {
        String[] parts = getHostSystemParts();
        return (index < parts.length) ? parts[index] : defaultValue;
    }

    public static String getHostSystemIP() {
        return getHostSystemPart(1, "");
    }

    public static String getHostSystemName() {
        return getHostSystemPart(0, "");
    }

    public static Integer getHostPort() {
        return mHostPort;
    }

    public static Integer getBounce() {
        return mBounce;
    }
}


