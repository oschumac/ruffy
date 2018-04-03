package org.monkey.d.ruffy.ruffy.driver;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by fishermen21 on 15.05.17.
 */

public class ConnectReceiver extends BroadcastReceiver {
    private final BTHandler handler;


    public ConnectReceiver(BTHandler handler)
    {
        this.handler = handler;
    }

    public void onReceive(Context context, Intent intent) {

        // TODO check this again
        String action = intent.getAction();
        handler.log("Action ("+action+") received");

        for(String k: intent.getExtras().keySet())
        {
            if(k.equals(BluetoothDevice.EXTRA_DEVICE))
            {
                if(intent.getStringExtra("address")== null)
                {

                    BluetoothDevice bd = ((BluetoothDevice)intent.getExtras().get(k));
                    String address = bd.getAddress();
                    intent.getExtras().putString("address",address);
                    if (address.substring(0, 8).equals("00:0E:2F")) {
                        BluetoothAdapter.getDefaultAdapter().cancelDiscovery();

                        handler.log("Pump found: " +  bd.getName() + "(" + bd.getAddress()+")" );

                        handler.deviceFound(bd);
                    }
                }
            }
        }
    }
}
