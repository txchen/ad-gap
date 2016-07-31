package com.adgap;

import org.apache.cordova.*;
import android.app.Activity;
import android.content.Intent;
import android.content.Context;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Timer;
import java.util.TimerTask;
import android.util.Log;
import android.content.pm.PackageManager;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;

public class Intenthelper extends CordovaPlugin {
    private static final String LOG_TAG = "Intenthelper";

    private CallbackContext _getAdsInfoCallbackContext;

    @Override
    public boolean execute(String action, JSONArray data, final CallbackContext callbackContext) throws JSONException {
        if (action.equals("sendBroadcast")) {
            String intentAction = data.optString(0);
            JSONObject extras = data.optJSONObject(1);

            Intent intent = new Intent(intentAction);
            if (extras != null) {
                for (int i = 0; i < extras.names().length(); i++) {
                    String keyName = extras.names().getString(i);
                    intent.putExtra(keyName, extras.getString(keyName));
                }
            }
            getActivity().getApplicationContext().sendBroadcast(intent);
            return true;
        } else if (action.equals("getAdsInfo")) {
            _getAdsInfoCallbackContext = callbackContext;
            Thread thr = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Context ctx = getActivity().getApplicationContext();
                        AdvertisingIdClient.Info adInfo = AdvertisingIdClient.getAdvertisingIdInfo(ctx);
                        Log.w(LOG_TAG, "gotAdInfo");
                        JSONObject adsObj = new JSONObject();
                        adsObj.put("adsid", adInfo.getId()); // google ads id
                        adsObj.put("adslimittracking", adInfo.isLimitAdTrackingEnabled()); // interested based ads or not
                        PluginResult result = new PluginResult(PluginResult.Status.OK, adsObj);
                        result.setKeepCallback(false);
                        _getAdsInfoCallbackContext.sendPluginResult(result);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            thr.start();
            // get ads must be done in async task
            PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
            pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
            return true;
        } else if (action.equals("checkPackageInstalled")) {
            PackageManager pm = getActivity().getPackageManager();
            boolean appInstalled;
            try {
                pm.getPackageInfo(data.optString(0), PackageManager.GET_ACTIVITIES);
                appInstalled = true;
            }
            catch (PackageManager.NameNotFoundException e) {
                appInstalled = false;
            }
            PluginResult result = new PluginResult(PluginResult.Status.OK, appInstalled);
            callbackContext.sendPluginResult(result);
            return true;
        } else {
            return false;
        }
    }

    private Activity getActivity() {
        return cordova.getActivity();
    }
}
