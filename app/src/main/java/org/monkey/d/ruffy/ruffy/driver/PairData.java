package org.monkey.d.ruffy.ruffy.driver;

import android.app.Activity;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Created by Admin on 26.09.2017.
 */

public class PairData {
    private static byte[] pin;
    private static Boolean pinokay;
    private static Boolean paired;
    public static boolean BTVisible;
    public static boolean BTPaired;
    public static boolean ROCHEPAIRED;

public byte[] getPin() {
    Log.v("PairData", "getPin->"+pin);
    return pin;
}

public void setPin(byte[] val) {
    Log.v("PairData", "setPin->"+val);
    pin = val;
}
public boolean getPinokay() {
    Log.v("PairData", "getPinokay->"+pinokay);
    return pinokay;
}

public void setPinokay(boolean val) {
    Log.v("PairData", "setPinokay->"+val);
    pinokay = val;
}

}
