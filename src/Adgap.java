package com.txchen.adgap;

import org.apache.cordova.*;
import android.content.Context;
import android.telephony.TelephonyManager;
import org.json.JSONArray;
import org.json.JSONException;

public class Adgap extends CordovaPlugin {

    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {
        if (action.equals("getIMEI")) {
            try {
              TelephonyManager telephonyManager = (TelephonyManager) this.cordova.getActivity().getSystemService(Context.TELEPHONY_SERVICE);
              callbackContext.success(telephonyManager.getDeviceId());
              return true;
            } catch (Exception e) {
              callbackContext.error(e.toString());
              return false;
            }
        } else {
            return false;
        }
    }
}