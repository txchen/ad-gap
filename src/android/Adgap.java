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
import java.util.Map;
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

import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.AdListener;
import com.facebook.ads.AdSize;
import com.facebook.ads.AdView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;

import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubView;

import com.millennialmedia.InlineAd;
import com.millennialmedia.MMException;
import com.millennialmedia.MMSDK;

import com.inmobi.ads.InMobiAdRequestStatus;
import com.inmobi.ads.InMobiBanner;
import com.inmobi.sdk.InMobiSdk;

public class Adgap extends CordovaPlugin {
    private static final String LOG_TAG = "Adgap";

    private Object _currentBannerObj = null;
    private RelativeLayout _bannerContainer = null;
    private CallbackContext _bannerCallbackContext;
    private CallbackContext _initCallbackContext;

    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {
        if (action.equals("getSystemInfo")) { // Get imei, packageName, versionName, versionCode
            return getSystemInfo(callbackContext);
        } else if (action.equals("showBanner")) {
            return startBanner(callbackContext, data);
        } else if (action.equals("stopBanner")) {
            return stopBanner(callbackContext);
        } else if (action.equals("init")) {
            initAdgap(callbackContext, data.optString(0), data.optBoolean(1), data.optBoolean(2), data.optBoolean(3));
            return true;
        } else {
            return false;
        }
    }

    // start of top level methods
    private void initAdgap(CallbackContext callbackContext, final String inmobiAccountId,
            final boolean inmobiEnabled, final boolean admobEnabled, final boolean mmEnabled) {
        _initCallbackContext = callbackContext;
        final Activity activity = getActivity();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                createBannerContainer();

                // init ads sdks
                if (admobEnabled) {
                    MobileAds.initialize(activity); // admob
                    Log.i(LOG_TAG, "admob sdk inited");
                }
                if (mmEnabled) {
                    MMSDK.initialize(activity); // mm
                    Log.i(LOG_TAG, "mm sdk inited");
                }

                // Inmobi
                if (inmobiEnabled && inmobiAccountId != null && !"".equals(inmobiAccountId)) {
                    InMobiSdk.setLogLevel(InMobiSdk.LogLevel.ERROR);
                    InMobiSdk.init(getActivity(), inmobiAccountId);
                    Log.i(LOG_TAG, "inmobi sdk inited");
                }

                PluginResult result = new PluginResult(PluginResult.Status.OK, new JSONObject());
                result.setKeepCallback(false);
                _initCallbackContext.sendPluginResult(result);
            }
        });

        String installerPackageName = getInstallerPackageName();
        if (!"com.android.vending".equals(installerPackageName)) {
            Log.w(LOG_TAG, String.format("installerPackageName = '%s', not from google play", installerPackageName));
        } else {
            Log.w(LOG_TAG, "installerPackageName is 'com.android.vending', seems this app is from google play");
        }
    }

    private boolean startBanner(CallbackContext callbackContext, JSONArray data) {
        _bannerCallbackContext = callbackContext;
        String networkName = data.optString(0);
        String pid = data.optString(1);

        loadAndShowBanner(networkName, pid);

        PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
        pluginResult.setKeepCallback(true);
        callbackContext.sendPluginResult(pluginResult);
        return true;
    }

    private boolean stopBanner(CallbackContext callbackContext) {
        hideBanner();

        PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
        pluginResult.setKeepCallback(false);
        callbackContext.sendPluginResult(pluginResult);
        return true;
    }

    private boolean getSystemInfo(CallbackContext callbackContext) {
        try {
            JSONObject obj = new JSONObject();
            TelephonyManager telephonyManager = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
            Activity myActivity = getActivity();
            PackageManager packageManager = myActivity.getPackageManager();
            String packageName = myActivity.getPackageName();
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
            Context context = myActivity.getBaseContext();
            int appNameResId = context.getApplicationInfo().labelRes;
            String appName = context.getString(appNameResId);
            // Local IP address V4
            WifiManager wm = (WifiManager) myActivity.getSystemService("wifi");
            String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
            // display info
            DisplayMetrics displayMetrics = myActivity.getBaseContext().getResources().getDisplayMetrics();

            obj.put("imei", telephonyManager.getDeviceId());
            obj.put("carrier", telephonyManager.getNetworkOperatorName());
            obj.put("packagename", packageName);
            obj.put("appname", appName);
            obj.put("installerpackagename", getInstallerPackageName());
            obj.put("versionname", packageInfo.versionName);
            obj.put("versioncode", packageInfo.versionCode);
            obj.put("localip", ip);
            obj.put("screenwidth", displayMetrics.widthPixels);
            obj.put("screenheight", displayMetrics.heightPixels);
            obj.put("displaydensity", displayMetrics.density);
            obj.put("useragent", System.getProperty("http.agent")); // http.agent

            PluginResult result = new PluginResult(PluginResult.Status.OK, obj);
            callbackContext.sendPluginResult(result);
            return true;
        } catch (Exception e) {
            callbackContext.error(e.toString());
            return false;
        }
    }
    // end of top level methods

    private void createBannerContainer() {
        if (_bannerContainer != null) {
            return;
        }
        Activity activity = getActivity();
        Log.w(LOG_TAG, "AdsContainer is null, creating that");
        RelativeLayout bigLayout = new RelativeLayout(activity);
        // rootView is Framelayout, add a big one to fill the parent
        getRootView().addView(bigLayout, new RelativeLayout.LayoutParams(-1, -1));
        bigLayout.bringToFront();

        RelativeLayout rl = new RelativeLayout(activity);
        DisplayMetrics displayMetrics = activity.getBaseContext().getResources().getDisplayMetrics();
        int containerHeight = Math.round(50 * displayMetrics.density);

        Log.i(LOG_TAG, String.format("containerHeight=%d", containerHeight));
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, containerHeight);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);

        bigLayout.addView(rl, params);
        rl.bringToFront();
        _bannerContainer = rl;
    }

    private String getInstallerPackageName() {
        String installerPackageName = "";
        try {
            Activity myActivity = getActivity();
            PackageManager packageManager = myActivity.getPackageManager();
            installerPackageName = packageManager.getInstallerPackageName(myActivity.getPackageName());
        } catch (Exception e) {
            Log.e(LOG_TAG, "failed to get installerPackageName", e);
        }
        return installerPackageName;
    }

    private void loadAndShowBanner(final String networkName, final String pid) {
        final Activity activity = getActivity();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.w(LOG_TAG, String.format("loading banner from %s, pid = %s", networkName, pid));
                if ("fban".equals(networkName)) {
                    loadFBBanner(pid);
                } else if ("admob".equals(networkName)) {
                    loadAdmobBanner(pid);
                } else if ("mopub".equals(networkName)) {
                    loadMopubBanner(pid);
                } else if ("mm".equals(networkName)) {
                    loadMMBanner(pid);
                } else if ("inmobi".equals(networkName)) {
                    loadInmobiBanner(pid);
                } else {
                    Log.e(LOG_TAG, "currently not supporting " + networkName);
                }
            }
        });
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

    private void destroyAdView(View adView) {
        if (adView != null) {
            if (adView instanceof com.facebook.ads.AdView) {
                ((com.facebook.ads.AdView) adView).destroy();
            }
        }
    }

    private void showBannerView(final View adView, final Object bannerObject) {
        final Activity activity = getActivity();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                hideBanner(); // clear the current banner first
                try {
                    if (adView != null) {
                        _bannerContainer.addView(adView, new RelativeLayout.LayoutParams(-1, -1));
                    }
                } catch (Exception e) {
                    Log.e(LOG_TAG, "!!!! Failed to showAd", e);
                }
                if (bannerObject != null) {
                    _currentBannerObj = bannerObject;
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
                    _bannerContainer.removeAllViews();
                    destroyBannerObject();
                } catch (Exception e) {
                    Log.e(LOG_TAG, "!!!! Failed to hideBanner", e);
                }
            }
        });
    }

    private void destroyBannerObject() {
        if (_currentBannerObj != null) {
            if (_currentBannerObj instanceof com.facebook.ads.AdView) {
                Log.i(LOG_TAG, "destroying fb banner");
                ((com.facebook.ads.AdView) _currentBannerObj).destroy();
            }

            if (_currentBannerObj instanceof com.google.android.gms.ads.AdView) {
                Log.i(LOG_TAG, "destroying admob banner");
                ((com.google.android.gms.ads.AdView) _currentBannerObj).destroy();
            }

            if (_currentBannerObj instanceof MoPubView) {
                Log.i(LOG_TAG, "destroying mopub banner");
                ((MoPubView) _currentBannerObj).destroy();
            }

            if (_currentBannerObj instanceof InlineAd) {
                Log.i(LOG_TAG, "destroying mm banner");
                // actually nothing to do
            }

            if (_currentBannerObj instanceof InMobiBanner) {
                Log.i(LOG_TAG, "destroying inmobi banner");
                // actually nothing to do
            }
            _currentBannerObj = null;
        }
    }

    private void loadInmobiBanner(String pid) {
        Log.w(LOG_TAG, "try to load inmobi banner: " + pid);
        Long pidLong = Long.parseLong(pid);
        final InMobiBanner imBanner = new InMobiBanner(getActivity(), pidLong);
        int width = toPixelUnits(320);
        int height = toPixelUnits(50);
        RelativeLayout.LayoutParams bannerLayoutParams =
                new RelativeLayout.LayoutParams(width, height);
        bannerLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        bannerLayoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);

        imBanner.setLayoutParams(bannerLayoutParams);
        imBanner.setListener(new InMobiBanner.BannerAdListener() {
            @Override
            public void onAdLoadSucceeded(InMobiBanner inMobiBanner) {
                Log.w(LOG_TAG, "inmobi banner loaded");
                showBannerView(imBanner, imBanner);
                PluginResult result = new PluginResult(PluginResult.Status.OK,
                    buildAdsEvent("inmobi", "LOAD_OK", ""));
                result.setKeepCallback(true);
                _bannerCallbackContext.sendPluginResult(result);
            }

            @Override
            public void onAdLoadFailed(InMobiBanner inMobiBanner, InMobiAdRequestStatus inMobiAdRequestStatus) {
                Log.w(LOG_TAG, "failed to load inmobi banner " + String.valueOf(inMobiAdRequestStatus.getStatusCode()));
                PluginResult result = new PluginResult(PluginResult.Status.OK,
                    buildAdsEvent("inmobi", "LOAD_ERROR", String.valueOf(inMobiAdRequestStatus.getStatusCode())));
                result.setKeepCallback(true);
                _bannerCallbackContext.sendPluginResult(result);
            }

            @Override
            public void onAdDisplayed(InMobiBanner inMobiBanner) { }

            @Override
            public void onAdDismissed(InMobiBanner inMobiBanner) { }

            @Override
            public void onAdInteraction(InMobiBanner inMobiBanner, Map<Object, Object> map) { }

            @Override
            public void onUserLeftApplication(InMobiBanner inMobiBanner) {
                // left app ~= clicked
                Log.w(LOG_TAG, "inmobi banner clicked");
                PluginResult result = new PluginResult(PluginResult.Status.OK,
                    buildAdsEvent("inmobi", "CLICKED", ""));
                result.setKeepCallback(true);
                _bannerCallbackContext.sendPluginResult(result);
            }

            @Override
            public void onAdRewardActionCompleted(InMobiBanner inMobiBanner, Map<Object, Object> map) { }
        });
        // NOTE: if banner not added to container, inmobi banner cannot load, so here must add.
        _bannerContainer.addView(imBanner);
        imBanner.load();
    }

    private void loadMMBanner(String pid) {
        Log.w(LOG_TAG, "try to load mm banner: " + pid);
        final InlineAd.InlineAdMetadata inlineAdMetadata = new InlineAd.InlineAdMetadata().
                setAdSize(InlineAd.AdSize.BANNER);
        final ViewGroup mmBannerView = new RelativeLayout(getActivity());
        InlineAd inlineAd;
        try {
            inlineAd = InlineAd.createInstance(pid, mmBannerView);
        } catch (MMException e) {
            e.printStackTrace();
            return;
        }
        inlineAd.setListener(new InlineAd.InlineListener() {
            @Override
            public void onRequestSucceeded(InlineAd inlineAd) {
                Log.w(LOG_TAG, "mm banner loaded");
                showBannerView(mmBannerView, inlineAd);
                PluginResult result = new PluginResult(PluginResult.Status.OK,
                    buildAdsEvent("mm", "LOAD_OK", ""));
                result.setKeepCallback(true);
                _bannerCallbackContext.sendPluginResult(result);
            }

            @Override
            public void onRequestFailed(InlineAd inlineAd, InlineAd.InlineErrorStatus errorStatus) {
                Log.w(LOG_TAG, "failed to load mm banner " + String.valueOf(errorStatus.getErrorCode()));
                PluginResult result = new PluginResult(PluginResult.Status.OK,
                    buildAdsEvent("mm", "LOAD_ERROR", String.valueOf(errorStatus.getErrorCode())));
                result.setKeepCallback(true);
                _bannerCallbackContext.sendPluginResult(result);
            }

            @Override
            public void onClicked(InlineAd inlineAd) {
                Log.w(LOG_TAG, "mm banner clicked");
                PluginResult result = new PluginResult(PluginResult.Status.OK,
                    buildAdsEvent("mm", "CLICKED", ""));
                result.setKeepCallback(true);
                _bannerCallbackContext.sendPluginResult(result);
            }

            @Override
            public void onResize(InlineAd inlineAd, int i, int i1) { }
            @Override
            public void onResized(InlineAd inlineAd, int i, int i1, boolean b) { }
            @Override
            public void onExpanded(InlineAd inlineAd) { }
            @Override
            public void onCollapsed(InlineAd inlineAd) { }
            @Override
            public void onAdLeftApplication(InlineAd inlineAd) { }
        });
        inlineAd.request(inlineAdMetadata);
    }

    private void loadMopubBanner(String pid) {
        Log.w(LOG_TAG, "try to load mopub banner: " + pid);
        final MoPubView mopubView = new MoPubView(getActivity());
        mopubView.setAdUnitId(pid);
        mopubView.setBannerAdListener(new MoPubView.BannerAdListener() {
            @Override
            public void onBannerLoaded(MoPubView banner) {
                Log.w(LOG_TAG, "mopub banner loaded");
                showBannerView(mopubView, mopubView);
                PluginResult result = new PluginResult(PluginResult.Status.OK,
                    buildAdsEvent("mopub", "LOAD_OK", ""));
                result.setKeepCallback(true);
                _bannerCallbackContext.sendPluginResult(result);
            }

            @Override
            public void onBannerFailed(MoPubView banner, MoPubErrorCode errorCode) {
                Log.w(LOG_TAG, "failed to load mopub banner " + errorCode.toString());
                mopubView.destroy();
                PluginResult result = new PluginResult(PluginResult.Status.OK,
                    buildAdsEvent("mopub", "LOAD_ERROR", String.valueOf(errorCode)));
                result.setKeepCallback(true);
                _bannerCallbackContext.sendPluginResult(result);
            }

            @Override
            public void onBannerClicked(MoPubView banner) {
                Log.w(LOG_TAG, "mopub banner clicked");
                PluginResult result = new PluginResult(PluginResult.Status.OK,
                    buildAdsEvent("mopub", "CLICKED", ""));
                result.setKeepCallback(true);
                _bannerCallbackContext.sendPluginResult(result);
            }

            @Override
            public void onBannerExpanded(MoPubView banner) { }
            @Override
            public void onBannerCollapsed(MoPubView banner) { }
        });
        mopubView.loadAd();
    }

    private void loadAdmobBanner(String pid) {
        Log.w(LOG_TAG, "try to load admob banner: " + pid);
        AdRequest adRequest = new AdRequest.Builder().build();
        final com.google.android.gms.ads.AdView adView = new com.google.android.gms.ads.AdView(getActivity());
        adView.setAdSize(com.google.android.gms.ads.AdSize.BANNER);
        adView.setAdUnitId(pid);
        adView.setAdListener(new com.google.android.gms.ads.AdListener() {
            @Override
            public void onAdLoaded() {
                Log.w(LOG_TAG, "admob banner loaded");
                showBannerView(adView, adView);
                PluginResult result = new PluginResult(PluginResult.Status.OK,
                    buildAdsEvent("admob", "LOAD_OK", ""));
                result.setKeepCallback(true);
                _bannerCallbackContext.sendPluginResult(result);
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                Log.w(LOG_TAG, "failed to load admob banner " + String.valueOf(errorCode));
                PluginResult result = new PluginResult(PluginResult.Status.OK,
                    buildAdsEvent("admob", "LOAD_ERROR", String.valueOf(errorCode)));
                result.setKeepCallback(true);
                _bannerCallbackContext.sendPluginResult(result);
            }

            @Override
            public void onAdLeftApplication() {
                // left app ~= clicked
                Log.w(LOG_TAG, "admob banner clicked");
                PluginResult result = new PluginResult(PluginResult.Status.OK,
                    buildAdsEvent("admob", "CLICKED", ""));
                result.setKeepCallback(true);
                _bannerCallbackContext.sendPluginResult(result);
            }
        });
        adView.loadAd(adRequest);
    }

    private void loadFBBanner(String pid) {
        Log.w(LOG_TAG, "try to load fb banner: " + pid);
        final AdView fbAdView = new AdView(getActivity(), pid, AdSize.BANNER_320_50);
        fbAdView.setAdListener(new AdListener() {
            @Override
            public void onError(Ad ad, AdError error) {
                Log.w(LOG_TAG, "failed to load fb banner " + error.getErrorCode());
                PluginResult result = new PluginResult(PluginResult.Status.OK,
                    buildAdsEvent("fban", "LOAD_ERROR", String.valueOf(error.getErrorCode())));
                result.setKeepCallback(true);
                _bannerCallbackContext.sendPluginResult(result);
            }

            @Override
            public void onAdLoaded(Ad ad) {
                Log.w(LOG_TAG, "fb banner loaded");
                showBannerView(fbAdView, fbAdView);
                PluginResult result = new PluginResult(PluginResult.Status.OK,
                    buildAdsEvent("fban", "LOAD_OK", ""));
                result.setKeepCallback(true);
                _bannerCallbackContext.sendPluginResult(result);
            }

            @Override
            public void onAdClicked(Ad ad) {
                Log.w(LOG_TAG, "fb banner clicked");
                PluginResult result = new PluginResult(PluginResult.Status.OK,
                    buildAdsEvent("fban", "CLICKED", ""));
                result.setKeepCallback(true);
                _bannerCallbackContext.sendPluginResult(result);
            }
        });
        // Request to load an ad
        fbAdView.loadAd();
    }

    private JSONObject buildAdsEvent(String networkName, String eventName, String eventDetail) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("ads_type", "banner");
            obj.put("network_name", networkName);
            obj.put("event_name", eventName);
            obj.put("event_detail", eventDetail);
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            return null;
        }
        return obj;
    }

    private int toPixelUnits(int dipUnit) {
        float density = getActivity().getBaseContext().getResources().getDisplayMetrics().density;
        return Math.round(dipUnit * density);
    }
}
