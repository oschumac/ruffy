package org.monkey.d.ruffy.ruffy.driver;

import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;

/**
 * Created by fishermen21 on 15.05.17.
 */

class ListenThread implements Runnable {
    private final BluetoothServerSocket srvSock;
    private PairData pData = new PairData();

    public ListenThread(BluetoothServerSocket srvSock) {
        this.srvSock = srvSock;
    }
    public void run() {
        Log.d("ListenThread","ListenThread started");
        BluetoothSocket socket = null;
        while (pData.BTVisible == true) {
            try {
                if (socket == null) {
                    Log.d("ListenThread", "ListenThread accecpt");
                    socket = srvSock.accept();

                }
                if (socket != null) {
                    socket.close();
                    socket = null;
                }
            } catch (Exception e) {

            }
        }
        Log.d("ListenThread","ListenThread stopped");

    }
    public void halt()
    {
        try {
            srvSock.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
