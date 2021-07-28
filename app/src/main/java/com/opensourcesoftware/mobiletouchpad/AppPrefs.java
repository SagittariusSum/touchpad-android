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
    private static final String KAPP_PREFS_NAME = "app_prefs";
    private static final String KKEY_SCROLL_MULTIPLIER = "SCROLL.MULTIPLIER";
    private static final String KKEY_SCROLL_NATURAL = "SCROLL.NATURAL";
    private static final String KKEY_HOST_SYSTEM = "HOST_SYSTEM";
    private static float mScrollMultiplier = 2.f;
    private static boolean mScrollNatural = false;
    private static String mHostSystem = ""; // stored HOST_NAME{SPACE}IP

    public static void loadPreferences(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(AppPrefs.KAPP_PREFS_NAME, Context.MODE_PRIVATE);
        mScrollMultiplier = prefs.getFloat(AppPrefs.KKEY_SCROLL_MULTIPLIER, 2.f);
        mScrollNatural = prefs.getBoolean(AppPrefs.KKEY_SCROLL_NATURAL, false);
        mHostSystem = prefs.getString(AppPrefs.KKEY_HOST_SYSTEM, "");
    }

    public static void savePreferences(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(AppPrefs.KAPP_PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat(AppPrefs.KKEY_SCROLL_MULTIPLIER, mScrollMultiplier);
        editor.putBoolean(AppPrefs.KKEY_SCROLL_NATURAL, mScrollNatural);
        editor.putString(AppPrefs.KKEY_HOST_SYSTEM, mHostSystem);
        editor.commit();
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

    public static String getHostSystem() {
        return mHostSystem;
    }

    public static void setHostSystem(String name, String hostIP) {
        mHostSystem = name + " " + hostIP;
    }

    public static void setHostSystem(String hostSystem) {
        mHostSystem = hostSystem;
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
}


