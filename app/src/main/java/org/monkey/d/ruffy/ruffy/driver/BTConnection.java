package org.monkey.d.ruffy.ruffy.driver;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.lang.Thread.sleep;

/**
 * Created by SandraK82 on 15.05.17.
 */

public class BTConnection {
    private final BTHandler handler;
    private BluetoothAdapter bluetoothAdapter;
    private ListenThread listen;
    public boolean shouldbevisible;
    private BluetoothSocket currentConnection;

    public int seqNo;
    private InputStream currentInput;
    private OutputStream currentOutput;
    private PairingRequest pairingReciever;
    private ConnectReceiver connectReceiver;

    private boolean connect=false;
    private PumpData pumpData;
    private PairData pData = new PairData();

    public BTConnection(final BTHandler handler)
    {
        this.handler = handler;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            handler.requestBlueTooth();
        }
    }

    public void makeDiscoverable(Activity activity) {



        pData.BTVisible=true;

        this.pumpData = new PumpData(activity);

        IntentFilter filter = new IntentFilter("android.bluetooth.device.action.PAIRING_REQUEST");
        pairingReciever = new PairingRequest(activity, handler);
        activity.registerReceiver(pairingReciever, filter);

        Intent discoverableIntent = new Intent("android.bluetooth.adapter.action.REQUEST_DISCOVERABLE");
        discoverableIntent.putExtra("android.bluetooth.adapter.extra.DISCOVERABLE_DURATION", 240);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE);
        activity.startActivity(discoverableIntent);



        handler.log("Bevore BT Sock create");

        BluetoothServerSocket srvSock = null;
        try {
            srvSock = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("SerialLink", UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"));
        } catch (IOException e) {
            handler.fail("socket listen() failed");
            return;
        }
        handler.log("After BT Sock creation");

        if (srvSock!=null) {
            handler.log("BTConnection create Socket for Serial Link successfull ->" + srvSock.toString());
        } else {
            handler.log("BTConnection create Socket for Serial Link NOT successfull ->" + srvSock.toString());
        }


        IntentFilter filter_ACL_CONNECTED = new IntentFilter("android.bluetooth.device.action.ACL_CONNECTED");
        connectReceiver = new ConnectReceiver(handler);
        activity.registerReceiver(connectReceiver, filter_ACL_CONNECTED);


        /*
        // final BluetoothServerSocket lSock = srvSock;
        listen = new ListenThread(srvSock);
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool( 1 );
        scheduler.execute(listen);
        */


        startDiscoverthread(activity);

    }


    public void stopDiscoverable() {
        shouldbevisible=false;
        pData.BTVisible=false;
        if(listen!=null)
        {
            listen.halt();
        }
        if(bluetoothAdapter.isDiscovering())
        {
            bluetoothAdapter.cancelDiscovery();
        }
    }

    private void startDiscoverthread(Activity activity) {
        shouldbevisible=true;
        new Thread() {
            @Override
            public void run() {
                while (shouldbevisible==true) {
                    try {
                        sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if(bluetoothAdapter.getScanMode() == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE)
                    {
                        Log.d("startDiscoverthread" , "Phone ist visible");
                    } else {
                        Log.e("startDiscoverthread" , "Phone ist not visible");

                    }

                }

            }

        }.start();
    }


    public void connect(BluetoothDevice device) {
        connect(device.getAddress(), 4);
    }

    public void connect(PumpData pumpData, int retries)
    {
        this.pumpData = pumpData;
        connect(pumpData.getPumpMac(),retries);
    }

    private int state = 0;

    private void connect(String deviceAddress, int retry) {
        if(state!=0)
        {
            handler.log("in connect! wrong state="+state);
            return;
        }
        state=1;

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter==null) {
            handler.fail("in connect -> Can't connect to BT Lib !!");
            state=0;
            return;
        }

        int btretry = 1;
        while (!bluetoothAdapter.isEnabled() && btretry<=3) {
            btretry++;
            if (!bluetoothAdapter.isEnabled()) {
                handler.log("in connect! Bluetooth ist ausgeschaltet schalte ein");
                bluetoothAdapter.enable();
                try {
                    sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }


        handler.log("in connect! (" + deviceAddress + ") retry -> (" + retry + ")");

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);

        // TODO Connect läuft hier nicht immer ab mehr bebug infos's einbauen wo es hackt.
        BluetoothSocket tmp = null;
        try {
            try {
                sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            tmp = device.createInsecureRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"));
            handler.log("BTConnection.java create socket successfull");
        } catch (IOException e) {
            state=0;
            handler.fail("socket create() failed: "+e.getMessage());
        }
        // TODO hier kommt der code schon nicht mehr hin ?


        if(tmp != null) {
            handler.log("BTConnection.java activate connection !=null");
            stopDiscoverable();
            activateConnection(tmp);
        }
        else
        {
            handler.log("BTConnection.java activate connection == null");
            handler.log("failed the pump connection( retries left: "+retry+")");
            try {
                sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if(retry>0)
            {
                state=0;
                connect(deviceAddress,retry-1);
            }
            else
            {
                state=0;
                handler.fail("Failed to connect");
            }
        }
    }

    private void startReadThread() {
        new Thread() {
            @Override
            public void run() {
                try {
                    if (currentConnection!=null) {
                        currentConnection.connect();//This method will block until a connection is made or the connection fails. If this method returns without an exception then this socket is now connected.
                        currentInput = currentConnection.getInputStream();
                        currentOutput = currentConnection.getOutputStream();
                        connect = true;
                        state = 2;
                    } else {
                        state = 0;
                        handler.fail("no connection possible: currentConnection is invalid");
                        return;
                    }
                } catch (IOException e) {
                    //e.printStackTrace();
                    state = 0;
                    handler.fail("no connection possible: " + e.getMessage());
                    return;

                    //??????????
                    //state=1;
                    //return;
                }
                try {
                    pumpData.getActivity().unregisterReceiver(connectReceiver);
                }catch(Exception e){/*ignore*/}
                try {
                    pumpData.getActivity().unregisterReceiver(pairingReciever);
                }catch(Exception e){/*ignore*/}

                //here check if really connected!
                //this will start thread to write
                handler.deviceConnected();//in ruffy.java


                try {
                    sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                byte[] buffer = new byte[512];
                while (connect==true) {
                    try {
                        int bytes = currentInput.read(buffer);
                        handler.log("read "+bytes+": "+ Utils.byteArrayToHexString(buffer,bytes));
                        handler.handleRawData(buffer,bytes);
                    } catch (Exception e) {
                        //e.printStackTrace();
                        //do not fail here as we maybe just closed the socket..

                        if (connect==true) {
                            handler.log("got error in read");
                        } else {
                            state = 0;
                        }

                        return;
                    }
                }
                state=0;
            }
        }.start();
    }

    public void writeCommand(byte[] key) {
        List<Byte> out = new LinkedList<Byte>();
        for(Byte b : key)
            out.add(b);
        for (Byte n : pumpData.getNonceTx())
            out.add(n);
        Utils.addCRC(out);

        List<Byte> temp = Frame.frameEscape(out);

        byte[] ro = new byte[temp.size()];
        int i = 0;
        for(byte b : temp)
            ro[i++]=b;

        StringBuilder sb = new StringBuilder();
        for (i = 0; i < key.length; i++) {
            sb.append(String.format("%02X ", key[i]));
        }
        if (state>1) {
            handler.log("writing command: " + sb.toString());
            write(ro);
        } else {
            handler.log("writing command: but no connection established");
        }
    }

    private void activateConnection(BluetoothSocket newConnection){

        if(this.currentConnection!=null)
        {
            try {
                this.currentOutput.close();
            } catch (Exception e) {/*ignore*/}
            try {
                this.currentInput.close();
            } catch (Exception e) {/*ignore*/}
            try {
                this.currentConnection.close();
            } catch (Exception e) {/*ignore*/}
            this.currentInput=null;
            this.currentOutput=null;
            this.currentConnection=null;
            handler.log("activateConnection() -> closed current Connection");
        }
        // TODO: Wieso Handler Fail ?
        // handler.fail("got new Connection: "+newConnection);
        handler.log("got new Connection: "+newConnection);
        this.currentConnection = newConnection;
        if(newConnection!=null)
        {
            startReadThread();
        }
    }

    public void write(byte[] ro){

        handler.log("!!!write!!!");

        if(this.currentConnection==null)
        {
            handler.fail("unable to write: no socket");
            return;
        }
        try {
            currentOutput.write(ro);
            handler.log("wrote "+ro.length+" bytes: "+ Utils.byteArrayToHexString(ro,ro.length));
        }catch(Exception e)
        {
            //e.printStackTrace();
            state=0;
            connect=false;
            handler.fail("failed write of "+ro.length+" bytes!");
        }
    }

    public void log(String s) {
        if(handler!=null)
            handler.log(s);
    }

    public void disconnect() {
        connect=false;
        try {
            this.currentOutput.close();
        } catch (Exception e) {/*ignore*/}
        try {
            this.currentInput.close();
        } catch (Exception e) {/*ignore*/}
        try {
            this.currentConnection.close();
        } catch (Exception e) {/*ignore*/}
        this.currentInput=null;
        this.currentOutput=null;
        this.currentConnection=null;
        this.pumpData = null;

        state=0;
        handler.log("disconnect() closed current Connection state=0");
    }

    public PumpData getPumpData() {
        return pumpData;
    }

    public boolean isConnected() {
        return this.currentConnection != null && this.currentConnection.isConnected();
    }
}
