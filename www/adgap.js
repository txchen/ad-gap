/*global cordova, module*/

module.exports = {
    getIMEI: function (name, successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "Adgap", "getIMEI", []);
    }
};