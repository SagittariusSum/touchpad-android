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
import android.util.Log;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class DiscoveryThread extends Thread {

    private static final String TAG = "DiscoveryThread";
    private static final String ANNOUNCE_PREFIX = "@*TOUCHPAD-MEV";
    private static final String KMULTICAST_ADDR = "239.255.255.250";

    public class MEVSystemItem {
        private String mHostIP = "";
        private String mName = "";

        public MEVSystemItem(String name, String hostIP) {
            this.mName = name;
            this.mHostIP = hostIP;
        }

        public String getHostIP() {
            return mHostIP;
        }

        public String getName() {
            return mName;
        }
    }

    private List<MEVSystemItem> mList = new ArrayList<>();
    private int mPort = 0;
    private DiscoveryThreadListener mListener = null;
    private Context mContext = null;

    public interface DiscoveryThreadListener {
        void onSystemFound(MEVSystemItem item);
    }

    public DiscoveryThread(int port, Context context, DiscoveryThreadListener listener) {
        this.mPort = port;
        this.mListener = listener;
        this.mContext = context;
    }

    private void checkServer(MEVSystemItem item) {
        Log.d(TAG, "checkServer: " + item.toString());
        mListener.onSystemFound(item);
    }

    @Override
    public void run() {
        Log.d(TAG, "run: START");
        String data = "";
        byte[] rawMsg = new byte[1000];
        //MulticastSocket ms = null;
//        DatagramSocket ds = null;
        MulticastSocket ds = null;

        DatagramPacket packet = new DatagramPacket(rawMsg, rawMsg.length);
        try {
            ds = new MulticastSocket(mPort);
            ds.setReuseAddress(true);
            InetAddress group = InetAddress.getByName(KMULTICAST_ADDR);
            ds.joinGroup(group);
            while (!this.isInterrupted()) {
                try {
                    ds.receive(packet);
                } catch (SocketTimeoutException x) {
                    continue;
                }
                data = new String(rawMsg, StandardCharsets.UTF_8);
                data = data.substring(0, data.indexOf("\0"));
                data = data.substring(0, data.indexOf("\n"));
                if (data.startsWith(ANNOUNCE_PREFIX)) {
                    String[] parts = data.split("\\s+");
                    if (parts.length >= 2) {
                        checkServer(new MEVSystemItem(parts[1], packet.getAddress().getHostAddress()));
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "run: ", e);
        } finally {
            if (ds != null) {
                ds.close();
            }
        }
    }
}


