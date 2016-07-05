/*global cordova, module*/

module.exports = {
  _configObject: null,
  _bannerLoop: null,

  configBanner: function (configObj, errCallback) {

  },

  // return Error if config is not valid
  validateConfig: function (configObj) {

  },

  // start the gapBanner, it will auto refresh
  startBanner: function () {

  },

  // stop the banner loop, dismiss the banner
  stopBanner: function () {

  },

  getIMEI: function (successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, 'Adgap', 'getIMEI', [])
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
}
