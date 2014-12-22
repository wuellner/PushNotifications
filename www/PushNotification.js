
  var exec = require('cordova/exec');

  var PushNotification = function() {};

  // Call this to register for push notifications. Content of [options] depends on whether we are working with APNS (iOS) or GCM (Android)
  PushNotification.prototype.register = function(successCallback, errorCallback, options) {
    errorCallback = errorCallback || function() {};

    if (typeof errorCallback != "function")  {
      console.log("PushNotification.register failure: failure parameter not a function");
      return
    }

    if (typeof successCallback != "function") {
      console.log("PushNotification.register failure: success callback parameter must be a function");
      return
    }

    exec(successCallback, errorCallback, "PushPlugin", "register", [options]);
  };

  // Call this to receive notification messages while the app is in foreground
  PushNotification.prototype.onMessageInForeground = function(successCallback, errorCallback) {
    errorCallback = errorCallback || function() {};

    if (typeof errorCallback != "function")  {
      console.log("PushNotification.onMessageInForeground failure: failure parameter not a function");
      return
    }

    if (typeof successCallback != "function") {
      console.log("PushNotification.onMessageInForeground failure: success callback parameter must be a function");
      return
    }

    exec(successCallback, errorCallback, "PushPlugin", "onMessageInForeground", []);
  };

  // Call this to receive notification messages while the app is in the background
  PushNotification.prototype.onMessageInBackground = function(successCallback, errorCallback) {
    errorCallback = errorCallback || function() {};

    if (typeof errorCallback != "function")  {
      console.log("PushNotification.onMessageInBackground failure: failure parameter not a function");
      return
    }

    if (typeof successCallback != "function") {
      console.log("PushNotification.onMessageInBackground failure: success callback parameter must be a function");
      return
    }

    exec(successCallback, errorCallback, "PushPlugin", "onMessageInBackground", []);
  };

  // Call this to unregister for push notifications
  PushNotification.prototype.unregister = function(successCallback, errorCallback) {
    errorCallback = errorCallback || function() {};

    if (typeof errorCallback != "function")  {
      console.log("PushNotification.unregister failure: failure parameter not a function");
      return
    }

    if (typeof successCallback != "function") {
      console.log("PushNotification.unregister failure: success callback parameter must be a function");
      return
    }

    exec(successCallback, errorCallback, "PushPlugin", "unregister", []);
  };


  // Call this to set the application icon badge
  PushNotification.prototype.setApplicationIconBadgeNumber = function(successCallback, errorCallback, badge) {
    errorCallback = errorCallback || function() {};

    if (typeof errorCallback != "function")  {
      console.log("PushNotification.setApplicationIconBadgeNumber failure: failure parameter not a function");
      return
    }

    if (typeof successCallback != "function") {
      console.log("PushNotification.setApplicationIconBadgeNumber failure: success callback parameter must be a function");
      return
    }

    exec(successCallback, errorCallback, "PushPlugin", "setApplicationIconBadgeNumber", [{badge: badge}]);
  };

  //-------------------------------------------------------------------

  if(!window.plugins) {
    window.plugins = {};
  }
  if (!window.plugins.pushNotification) {
    window.plugins.pushNotification = new PushNotification();
  }

  module.exports = PushNotification;
