# adgap
ads plugin for cordova, aggregates multiple networks. Only supports banner ad format.

Now adgap supports the following networks:

* Facebook Audience Network
* Mopub
* Admob
* mMedia
* InMobi

## Usage

First, add the plugin to your cordova project:

```
cordova plugin add cordova-plugin-adgap --save
```

Since mMedia requires minSDKVer to be GE 16, if you get build error, then add the following to your config.xml

```
<platform name="android">
    <preference name="android-minSdkVersion" value="16" />   <------ add this
    <allow-intent href="market:*" />
</platform>
```

### Show banner ads

Then, write code to show ads, after cordova is ready, call `configBanner` fisrt:

```js
adgap.configBanner({
  reloadSec: 26,
  networks: {
    fban: { name: 'fban', pid: 'YOUR_FBAN_PLACEMENT_ID', weight: 100, reloadSec: 25 },
    admob: { name: 'admob', pid: 'YOUR_ADMOB_PLACEMENT_ID', weight: 100, reloadSec: 25 },
    mopub: { name: 'mopub', pid: 'YOUR_MOPUD_PLACEMENT_ID', weight: 100, reloadSec: 25 },
    mm: { name: 'mm', pid: 'YOUR_MMEDIA_PLACEMENT_ID', weight: 100, reloadSec: 25 },
    inmobi: { name: 'inmobi', pid: 'YOUR_INMOBI_PLACEMENT_ID', acctid: 'YOUR_INMOBI_ACCOUNT_ID', weight: 100, reloadSec: 25 },
  }
})
```

Once configuration is done, call startBanner to show the banner at bottom of your app:

```js
adgap.startBanner()
```

The plugin will auto reload banner every X seconds, so when you create placement at network, don't set the reload interval.

When the banner is loaded, or fail to load, events will be published and you can subscribe them:

```js
window.addEventListener('adgap_event', function (adsEvent) {
    // HERE, cannot use JSON.stringify on the info, Event is created by cordova, and cannot be serialized.
    // adsEvent contains ads_type, network_name, event_name, event_detail
    // ads_type = banner
    // network_name = fban
    // event_name = LOAD_OK | LOAD_ERROR | CLICKED
    // event_detail = when error happens, error code like '1001'
  }, false);
```

If you want to pause the banner loop for a while, let it stop loading new ads for some time, you can:

```js
adgap.snoozeBanner(15000) // will pause the banner loop for 15000 ms
```

### Send broadcast

```js
intenthelper.sendBroadcast('com.xzy.abc.ACTION_NAME',
  { extraName1: 'extraValue1', extraName2: 'extraValue2' })
```

### Get ads info

```js
intenthelper.getAdsInfo(
  function (info) {
    // now get info.adsid and info.adslimittracking
  },
  function (error) {

  }
)
```

### Check if a package is installed or not:

```js
intenthelper.checkPackage('com.awesome.pkg', function (installed) {
  // installed is boolean
})
```
