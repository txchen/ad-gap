/*global cordova, module*/

function deepmerge(target, src) {
  var array = Array.isArray(src);
  var dst = array && [] || {};

  if (array) {
    target = target || [];
    dst = dst.concat(target);
    src.forEach(function (e, i) {
      if (typeof dst[i] === 'undefined') {
        dst[i] = e;
      } else if (typeof e === 'object') {
        dst[i] = deepmerge(target[i], e);
      } else {
        if (target.indexOf(e) === -1) {
          dst.push(e);
        }
      }
    });
  } else {
    if (target && typeof target === 'object') {
      Object.keys(target).forEach(function (key) {
        dst[key] = target[key];
      })
    }
    Object.keys(src).forEach(function (key) {
      if (typeof src[key] !== 'object' || !src[key]) {
        dst[key] = src[key];
      }
      else {
        if (!target[key]) {
          dst[key] = src[key];
        } else {
          dst[key] = deepmerge(target[key], src[key]);
        }
      }
    });
  }
  return dst;
}

function formatDate(d) {
  return d.getFullYear() + ('0' + (d.getMonth() + 1)).slice(-2) + ('0' + d.getDate()).slice(-2) + '-' +
    ('0' + d.getHours()).slice(-2) + ':' + ('0' + d.getMinutes()).slice(-2) + ':' + ('0' + d.getSeconds()).slice(-2) +
    '.' + ('0' + d.getMilliseconds()).slice(-3)
}

module.exports = {
  ///////////////   misc APIs
  getSystemInfo: function (successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, 'Adgap', 'getSystemInfo', [])
  },
  ///////////////   misc APIs

  /////////  states
  _bannerOptions: {
    reloadSec: 26, // global reloadSec
    cooldownSec: 10,
    networks: {
      fban: { name: 'fban', pid: null, weight: 100, reloadSec: 25 },
      admob: { name: 'admob', pid: null, weight: 100, reloadSec: 25 },
      mopub: { name: 'mopub', pid: null, weight: 100, reloadSec: 25 },
    },
  },
  _bannerStates: {
    lastLoadedTime: 0,
    lastAttemptTime: 0,
    lastAttemptNetwork: null,
    lastLoadedNetwork: null,
    networks: {
      fban: { lastLoadedTime: 0, lastAttemptTime: 0 },
      admob: { lastLoadedTime: 0, lastAttemptTime: 0 },
      mopub: { lastLoadedTime: 0, lastAttemptTime: 0 },
    },
  },
  _bannerLoopFunc: null,
  _isInBackground: false,
  _inited: false,
  _snoozeBannerUntil: 0, // timestamp, if current time < _snoozeUntil, then it should snooze
  /////////  states

  // PRIVATE METHODS:
  _bannerLogic: function (now) {
    if (this._isInBackground) {
      console.log('[verb] app in background, bypass bannerlogic ' + formatDate(now))
      return
    }
    if ((new Date()).getTime() < this._snoozeBannerUntil) {
      console.log('[info] app is in snooze, bypass bannerlogic ' + formatDate(now))
      return
    }

    // Now the actual banner logic
    console.log('[verb] executing banner logic ' + formatDate(now))
    // 0) check if we need to loadAds or not
    //    check if it is too fast to make another attempt
    if ((now - this._bannerStates.lastAttemptTime) < 1000 * this._bannerOptions.cooldownSec) {
      console.log('[verb] attempt cooldown not passed, end the logic')
      return
    }
    //    check if it is too frequent to load an ad
    if ((now - this._bannerStates.lastLoadedTime) < 1000 * this._bannerOptions.reloadSec) {
      console.log('[verb] no need to load ads now, since reloadSec has not passed')
      return
    }
    // 1) get all the available networks, and remove un-ready (cooldown) ones
    var networks = []
    var networkNames = []
    for (var n in this._bannerOptions.networks) {
      var networkConfig = this._bannerOptions.networks[n]
      // if network level reloadSec is not provided, use the global one instead
      var networkReloadSec = networkConfig.reloadSec || this._bannerOptions.reloadSec
      // pid must be available, and lastLoadedTime must be early enough
      if (networkConfig.pid && (now - this._bannerStates.networks[n].lastLoadedTime) > networkReloadSec) {
        //console.log('network ' + n + ' is an available candidate to load')
        networks.push(networkConfig)
        networkNames.push(n)
      }
    }
    if (!networks.length) {
      console.log('[warn] there are no available networks, return now')
      return
    }
    console.log('[info] ' + networks.length + ' networks are available to pick up. They are: ' + networkNames)
    // 2) then random by weight
    var totalWeight = 0
    for (var i in networks) {
      totalWeight += networks[i].weight
    }
    var network = networks[networks.length - 1]
    var rand = Math.floor(Math.random() * totalWeight)
    for (var i in networks) {
      if (rand < networks[i].weight) {
        network = networks[i]
        break
      }
      rand -= networks[i].weight
    }
    // 3) finally, call java to load that ads
    this._bannerStates.lastAttemptNetwork = network.name
    this._bannerStates.lastAttemptTime = now.getTime()
    this._bannerStates.networks[network.name].lastAttemptTime = now.getTime()

    cordova.exec(
      function (adsEvent) {
        if (adsEvent) {
          // adsEvent contains ads_type, network_name, event_name, event_detail
          cordova.fireWindowEvent("adgap_event", adsEvent)
        }
      },
      function (err) {
        console.log('[error] failed to show banner', err)
        cordova.fireWindowEvent("adgap_show_banner_failure",
          { type: 'show_banner_failure', error: err })
      }, 'Adgap', 'showBanner', [network.name, network.pid])
    console.log('[info] call java to load banner from network - ' + network.name)
    // HACK: should set lastLoaded in callback from Java
    console.log('[warn] ' + network.name + ' banner loaded')
    this._bannerStates.lastLoadedTime = now.getTime()
    this._bannerStates.lastLoadedNetwork = network.name
    this._bannerStates.networks[network.name].lastLoadedTime = now.getTime()
  },

  // PUBLIC APIs:
  // TODO: returns error, if there is something wrong
  configBanner: function (options) {
    if (!this._inited) {
      console.log('[info] setting up onPause onResume logic')
      var self = this
      document.addEventListener('pause', function () {
          console.log('[info] detected cordova pause event, current isInBackgrond=' + self._isInBackground)
          self._isInBackground = true
        }, false)
      document.addEventListener('resume', function () {
          console.log('[info] detected cordova resume event, current isInBackgrond=' + self._isInBackground)
          self._isInBackground = false
        }, false)
      this._inited = true
    }
    // TODO: validate the input, and returns error
    this._bannerOptions = deepmerge(this._bannerOptions, options)
    console.log('[info] The new banner config: ' + JSON.stringify(this._bannerOptions, null, 2))
  },

  startBanner: function () {
    if (!this._bannerLoopFunc) {
      this._bannerLoopFunc = setInterval(
        (function (self) {
          return function () {
            self._bannerLogic(new Date())
          }
        })(this),
        2000)

      this._bannerLogic(new Date())
    }
  },

  stopBanner: function () {
    if (this._bannerLoopFunc) {
      clearInterval(this._bannerLoopFunc)
      cordova.exec(
        function () {
          console.log('[warn] banner stopped')
          cordova.fireWindowEvent("adgap_banner_stopped", { type: 'banner_stopped' })
        },
        function (err) {
          console.log('[error] failed to stop banner', err)
          cordova.fireWindowEvent("adgap_banner_stop_failure", { type: 'banner_stop_failure', error: err })
        }, 'Adgap', 'stopBanner', [])
      this._bannerLoopFunc = null
    }
  },

  snoozeBanner: function (snoozeMs) {
    this._snoozeBannerUntil = (new Date()).getTime() + snoozeMs
    console.log('[warn] banner logic will snooze for ' + snoozeMs + ' ms')
  },
}
