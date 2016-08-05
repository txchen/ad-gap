/*global cordova, module*/

module.exports = {
  sendBroadcast: function (action, extras) {
    cordova.exec(function() {}, function() {}, 'Intenthelper', 'sendBroadcast', [action, extras])
  },
  getAdsInfo: function (successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, 'Intenthelper', 'getAdsInfo', [])
  },
  checkPackage: function (packageName, resultCallback, errorCallback) {
    cordova.exec(resultCallback, errorCallback, 'Intenthelper', 'checkPackageInstalled', [packageName])
  },
  getSharedPref: function (prefName, resultCallback, errorCallback) {
    cordova.exec(resultCallback, errorCallback, 'Intenthelper', 'getSharedPref', [prefName])
  }
}
