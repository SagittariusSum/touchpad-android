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

import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.concurrent.ConcurrentLinkedQueue;

public class UDPClientThread extends Thread {

    private final static String TAG = "UDPClientThread";

    private String mHostIP = "";
    private Integer mHostPort = 0;
    private ConcurrentLinkedQueue<String> mCmdQueue;

    UDPClientThread(String hostIP, Integer hostPort, ConcurrentLinkedQueue<String> cmdQueue) {
        mHostIP = hostIP;
        mHostPort = hostPort;
        mCmdQueue = cmdQueue;
    }

    @Override
    public void run() {
        DatagramSocket ds = null;
        try {
            ds = new DatagramSocket();
            // IP Address below is the IP address of that Device where server socket is opened.
            InetAddress serverAddr = InetAddress.getByName(mHostIP);
            DatagramPacket dp;
            ds.setReuseAddress(true);
            ds.setSoTimeout(1000);
            while (!isInterrupted()) {
                if (!mCmdQueue.isEmpty()) {
                    String data = mCmdQueue.poll();
                    if (data == null) continue;
                    data += "\n";
                    dp = new DatagramPacket(data.getBytes(), data.length(), serverAddr, mHostPort);
                    ds.send(dp);
                } else Thread.sleep(0, 1);
            }
        } catch (SocketTimeoutException ste) {
            Log.e(TAG, "run: ", ste);
            interrupt();
        } catch (Exception e) {
            Log.e(TAG, "startUDPClientThread Thread run: ", e);
        } finally {
            if (ds != null) {
                ds.close();
            }
        }
    }
}
