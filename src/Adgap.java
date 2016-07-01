package com.txchen.adgap;

import org.apache.cordova.*;
import android.content.Context;
import android.telephony.TelephonyManager;
import android.content.BroadcastReceiver;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Timer;
import java.util.TimerTask;
import android.util.Log;

public class Adgap extends CordovaPlugin {
    private static final String LOG_TAG = "Adgap";

    BroadcastReceiver receiver;

    private CallbackContext _delayedCallbackContext;

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
        } else if (action.equals("delayedEvent")) {
            _delayedCallbackContext = callbackContext;
            Log.e(LOG_TAG, "I am here1!!");
            new Timer().schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        Log.e(LOG_TAG, "I am here2!!");
                        PluginResult result = new PluginResult(PluginResult.Status.OK, buildDummyEvent(54321));
                        result.setKeepCallback(true);
                        _delayedCallbackContext.sendPluginResult(result);

                        Log.e(LOG_TAG, "I am here3!!");
                        result = new PluginResult(PluginResult.Status.OK, buildDummyEvent(98765));
                        result.setKeepCallback(true);
                        _delayedCallbackContext.sendPluginResult(result);
                    }
                },
                3000
            );
            // dont return any result now
            PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
            pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
            return true;
        } else {
            return false;
        }
    }

    private JSONObject buildDummyEvent(int data) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("val", data);
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
        }
        return obj;
    }
}
