/*global cordova, module*/

module.exports = {
    getIMEI: function (successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "Adgap", "getIMEI", []);
    }
};