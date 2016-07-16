package com.adgap;

import org.apache.cordova.*;
import android.app.Activity;
import android.util.DisplayMetrics;
import android.content.Context;
import android.telephony.TelephonyManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Timer;
import java.util.TimerTask;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import com.facebook.ads.*;

public class Adgap extends CordovaPlugin {
    private static final String LOG_TAG = "Adgap";
    private View _currentAdsView = null;
    private RelativeLayout _adsContainer = null;

    private CallbackContext _delayedCallbackContext;
    private CallbackContext _bannerCallbackContext;

    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {
        if (action.equals("getSystemInfo")) { // Get imei, packageName, versionName, versionCode
            try {
                JSONObject obj = new JSONObject();
                TelephonyManager telephonyManager = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
                Activity myActivity = getActivity();
                PackageManager packageManager = myActivity.getPackageManager();
                String packageName = myActivity.getPackageName();
                String installerPackageName = "";
                try {
                    installerPackageName = packageManager.getInstallerPackageName(packageName);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "failed to get installerPackageName", e);
                }
                PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
                // Local IP address V4
                WifiManager wm = (WifiManager) myActivity.getSystemService("wifi");
                String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
                // display info
                DisplayMetrics displayMetrics = myActivity.getBaseContext().getResources().getDisplayMetrics();

                obj.put("imei", telephonyManager.getDeviceId());
                obj.put("packagename", packageName);
                obj.put("installerpackagename", installerPackageName);
                obj.put("versionname", packageInfo.versionName);
                obj.put("versioncode", packageInfo.versionCode);
                obj.put("localip", ip);
                obj.put("screenwidth", displayMetrics.widthPixels);
                obj.put("screenheight", displayMetrics.heightPixels);
                obj.put("displaydensity", displayMetrics.density);
                obj.put("useragent", System.getProperty("http.agent")); // http.agent

                if (!"com.android.vending".equals(installerPackageName)) {
                    Log.w(LOG_TAG, String.format("installerPackageName = '%s', not from google play", installerPackageName));
                } else {
                    Log.w(LOG_TAG, "installerPackageName is 'com.android.vending', seems this app is from google play");
                }

                PluginResult result = new PluginResult(PluginResult.Status.OK, obj);
                callbackContext.sendPluginResult(result);
                return true;
            } catch (Exception e) {
                callbackContext.error(e.toString());
                return false;
            }
        } else if (action.equals("delayedEvent")) {
            _delayedCallbackContext = callbackContext;
            new Timer().schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        PluginResult result = new PluginResult(PluginResult.Status.OK, buildDummyEvent(54321));
                        result.setKeepCallback(true);
                        _delayedCallbackContext.sendPluginResult(result);

                        result = new PluginResult(PluginResult.Status.OK, buildDummyEvent(98765));
                        result.setKeepCallback(true);
                        _delayedCallbackContext.sendPluginResult(result);
                    }
                },
                3000
            );
            // dont return any result now, result will be returned later
            PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
            pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
            return true;
        } else if (action.equals("showBanner")) {
            _bannerCallbackContext = callbackContext;
            String networkName = data.optString(0);
            String pid = data.optString(1);
            loadAndShowBanner(networkName, pid);

            PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
            pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
            return true;
        } else if (action.equals("stopBanner")) {
            hideBanner();

            PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
            pluginResult.setKeepCallback(false);
            callbackContext.sendPluginResult(pluginResult);
            return true;
        } else {
            return false;
        }
    }

    private void loadAndShowBanner(String networkName, String pid) {
        Log.w(LOG_TAG, String.format("loading banner from %s, pid = %s", networkName, pid));
        if (!networkName.equals("fban")) {
            Log.e(LOG_TAG, "currently only support fban");
            return;
        }
        loadFBBanner(pid);
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

    private Activity getActivity() {
        return cordova.getActivity();
    }

    private ViewGroup getMainView() {
        return (ViewGroup) getRootView().getChildAt(0);
    }

    private ViewGroup getRootView() {
        return (ViewGroup) getActivity().getWindow().getDecorView().findViewById(android.R.id.content);
    }

    private void createAdsContainer(Activity activity) {
        if (_adsContainer != null) {
            return;
        }
        Log.w(LOG_TAG, "AdsContainer is null, creating that");
        RelativeLayout rl = new RelativeLayout(activity);
        getRootView().addView(rl, new RelativeLayout.LayoutParams(-1, -1));
        rl.bringToFront();
        _adsContainer = rl;
    }

    private void destroyAdView(View adView) {
        if (adView != null) {
            if (adView instanceof com.facebook.ads.AdView) {
                ((com.facebook.ads.AdView) adView).destroy();
            }
        }
    }

    private void showAdView(final View adView) {
        final View finalAdView = adView;
        final Activity activity = getActivity();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    createAdsContainer(activity);
                    _adsContainer.removeAllViews();
                    destroyAdView(_currentAdsView);
                    // Now add adView into the container
                    int wndWidth = getMainView().getWidth();
                    int wndHeight = getMainView().getHeight();

                    DisplayMetrics displayMetrics = activity.getBaseContext().getResources().getDisplayMetrics();
                    int bannerHeight = Math.round(50 * displayMetrics.density);

                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(wndWidth, bannerHeight);
                    params.topMargin = wndHeight - bannerHeight;

                    _adsContainer.addView(finalAdView, params);
                    _currentAdsView = finalAdView;
                } catch (Exception e) {
                    Log.e(LOG_TAG, "!!!! Failed to showAd", e);
                }
            }
        });
    }

    private void hideBanner() {
        final Activity activity = getActivity();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    createAdsContainer(activity);
                    _adsContainer.removeAllViews();
                    destroyAdView(_currentAdsView);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "!!!! Failed to hideAd", e);
                }
            }
        });
    }

    private void loadFBBanner(String pid) {
        Log.w(LOG_TAG, "try to load fb banner: " + pid);
        // now only load FB ads
        final AdView fbAdView = new AdView(getActivity(), pid, AdSize.BANNER_320_50);
        fbAdView.setAdListener(new AdListener() {
            @Override
            public void onError(Ad ad, AdError error) {
                Log.w(LOG_TAG, "failed to load fb banner " + error.getErrorCode());
                PluginResult result = new PluginResult(PluginResult.Status.ERROR,
                    buildFBBannerEvent("LOAD_ERROR", String.valueOf(error.getErrorCode())));
                _bannerCallbackContext.sendPluginResult(result);
            }

            @Override
            public void onAdLoaded(Ad ad) {
                Log.w(LOG_TAG, "fb banner loaded");
                showAdView(fbAdView);
                PluginResult result = new PluginResult(PluginResult.Status.OK,
                    buildFBBannerEvent("LOAD_OK", ""));
                result.setKeepCallback(true);
                _bannerCallbackContext.sendPluginResult(result);
            }

            @Override
            public void onAdClicked(Ad ad) {
                Log.w(LOG_TAG, "fb banner clicked");
                PluginResult result = new PluginResult(PluginResult.Status.OK,
                    buildFBBannerEvent("CLICKED", ""));
                result.setKeepCallback(true);
                _bannerCallbackContext.sendPluginResult(result);
            }
        });
        // Request to load an ad
        fbAdView.loadAd();
    }

    private JSONObject buildFBBannerEvent(String eventName, String eventDetail) {
        return buildAdsEvent("banner", "fban", eventName, eventDetail);
    }

    private JSONObject buildAdsEvent(String adsType, String networkName, String eventName, String eventDetail) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("ads_type", adsType);
            obj.put("network_name", networkName);
            obj.put("event_name", eventName);
            obj.put("event_detail", eventDetail);
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            return null;
        }
        return obj;
    }
}
