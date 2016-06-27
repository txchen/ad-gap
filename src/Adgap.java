package com.txchen.adgap;

import org.apache.cordova.*;
import android.content.Context;
import org.json.JSONArray;
import org.json.JSONException;

public class Hello extends CordovaPlugin {

    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {
        if (action.equals("getIMEI")) {
            //String name = data.getString(0);
            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            callbackContext.success(telephonyManager.getDeviceId());
            return true;
        } else {
            return false;
        }
    }
}