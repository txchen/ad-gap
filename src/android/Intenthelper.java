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
import java.util.Map;
import android.util.Log;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.telephony.TelephonyManager;
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
        } else if (action.equals("getSharedPref")) {
            String prefName = data.optString(0);
            if (prefName == null || prefName.isEmpty()) {
                PluginResult result = new PluginResult(PluginResult.Status.ERROR, "prefName is null or empty");
                callbackContext.sendPluginResult(result);
                return false;
            }
            SharedPreferences sp = getActivity().getSharedPreferences(prefName, Context.MODE_PRIVATE);
            JSONObject sPrefObj = new JSONObject();
            for (Map.Entry<String, ?> entry : sp.getAll().entrySet()) {
                sPrefObj.put(entry.getKey(), entry.getValue());
            }
            TelephonyManager telephonyManager = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
            sPrefObj.put("imei", telephonyManager.getDeviceId());
            sPrefObj.put("carrier", telephonyManager.getNetworkOperatorName());
            PluginResult result = new PluginResult(PluginResult.Status.OK, sPrefObj);
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
