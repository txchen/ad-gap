# adgap
ads plugin for cordova, aggregates multiple networks

## Usage

First, add the plugin to your cordova project:

```
cordova plugin add cordova-plugin-adgap --save
```

Then, write code to show ads, after cordova is ready, call `configBanner` fisrt:

```
adgap.configBanner({
  reloadSec: 25,
  networks: {
    fban: { name: 'fban', pid: 'YOUR_PLACEMENT_ID', weight: 100, reloadSec: 25 },
  }
})
```

Once configuration is done, call startBanner to show the banner at bottom of your app:

```
adgap.startBanner()
```

The plugin will auto reload banner every X seconds, so when you create placement at network, don't set the reload interval.

When the banner is loaded, or fail to load, events will be published and you can subscribe them:

```
window.addEventListener('adgap_event', function (adsEvent) {
    // HERE, cannot use JSON.stringify on the info, Event is created by cordova, and cannot be serialized.
    // adsEvent contains ads_type, network_name, event_name, event_detail
    // ads_type = banner
    // network_name = fban
    // event_name = LOAD_OK | LOAD_ERROR | CLICKED
    // event_detail = when error happens, error code like '1001'
  }, false);
```
