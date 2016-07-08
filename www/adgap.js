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
  ///////////////   misc functions
  getSystemInfo: function (successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, 'Adgap', 'getSystemInfo', [])
  },

  delayedEvent: function () {
    cordova.exec(function (status) {
      if (status) {
        console.log('got delayedEvent from java')
        console.log(status)
        cordova.fireWindowEvent("delayedevent", status);
      } else {
        console.log('got empty status from java')
      }
    },
      function (e) {
        console.log('Error setup delayed event ' + e)
      },
      'Adgap',
      'delayedEvent',
      []
    )
  },
  ///////////////   misc functions

  _bannerOptions: {
    reloadSec: 25, // global reloadSec
    cooldownSec: 10,
    networks: {
      fban: { name: 'fban', pid: null, weight: 100, reloadSec: 26 },
      flurry: { name: 'flurry', pid: null, weight: 100, reloadSec: 20 },
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
      flurry: { lastLoadedTime: 0, lastAttemptTime: 0 },
      mopub: { lastLoadedTime: 0, lastAttemptTime: 0 },
    },
  },
  _bannerLoopFunc: null,

  // PRIVATE METHODS:
  _bannerLogic: function (now) {
    console.log('[verb] executing banner logic ' + formatDate(now))
    // 0) check if we need to loadAds or not
    //    check if it is too fast to make another attempt
    if ((now - this._bannerStates.lastAttemptTime) < 1000 * this._bannerOptions.cooldownSec) {
      console.log('attempt cooldown not passed, end the logic')
      return
    }
    //    check if it is too frequent to load an ad
    if ((now - this._bannerStates.lastLoadedTime) < 1000 * this._bannerOptions.reloadSec) {
      console.log('no need to load ads now, since reloadSec has not passed')
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
      console.log('there are no available networks, return now')
      return
    }
    console.log(networks.length + ' networks are available to pick up. They are: ' + networkNames)
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
    // TODO: implement java
    cordova.exec(
      function (adsEvent) {
        if (adsEvent) {
          this._processAdsEvent(adsEvent)
        }
      },
      function (err) {

      }, 'Adgap', 'showBanner', [network.name, network.pid])
    console.log('[info] call java to load banner from network - ' + network.name)
    // HACK: should set lastLoaded in callback from Java
    console.log('[warn] ' + network.name + ' loaded')
    this._bannerStates.lastLoadedTime = now.getTime()
    this._bannerStates.lastLoadedNetwork = network.name
    this._bannerStates.networks[network.name].lastLoadedTime = now.getTime()
  },

  _processAdsEvent: function (adsEvent) {

  },

  // PUBLIC METHODS:

  // returns error, if there is something wrong
  configBanner: function (options) {
    // TODO: validate the input, and returns error
    this._bannerOptions = deepmerge(this._bannerOptions, options)
    console.log('The new banner config: ' + JSON.stringify(this._bannerOptions, null, 2))
  },

  // returns error, if there is something wrong
  startBanner: function () {
    if (!this._bannerLoopFunc) {
      this._bannerLoopFunc = setInterval(
        (function (self) {
          return function () {
            self._bannerLogic(new Date())
          }
        })(this),
        2000)
    }
  },

  stopBanner: function () {
    if (this._bannerLoopFunc) {
      clearInterval(this._bannerLoopFunc)
      cordova.exec(
        function () {
        },
        function (err) {

        }, 'Adgap', 'stopBanner', [])
      this._bannerLoopFunc = null
    }
  },

}
