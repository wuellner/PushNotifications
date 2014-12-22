package com.plugin.gcm;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

/**
* Push Notifications Plugin
*/
public class PushPlugin extends CordovaPlugin {

  public static final String TAG = "PushPlugin";

  public static final String REGISTER = "register";

  public static final String UNREGISTER = "unregister";

  public static final String EXIT = "exit";

  public static final String ON_MESSAGE_FOREGROUND = "onMessageInForeground";

  public static final String ON_MESSAGE_BACKGROUND = "onMessageInBackground";

  public static final String SENDER_ID = "senderID";

  public static final String GCM_SENDER_ID = "gcm_senderid";

  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);

    readSenderIdFromCordovaConfig();
  }

  private void readSenderIdFromCordovaConfig() {
    Bundle extras = cordova.getActivity().getIntent().getExtras();
    if(extras.containsKey(GCM_SENDER_ID)) {
      String senderID = extras.getString(GCM_SENDER_ID);
      NotificationService
      .getInstance(getApplicationContext())
      .setSenderID(senderID);
    }
  }

  /**
  * Gets the application context from cordova's main activity.
  *
  * @return the application context
  */
  private Context getApplicationContext() {
    return this.cordova.getActivity().getApplicationContext();
  }

  private boolean handleRegister(JSONArray data, CallbackContext callbackContext) {
    try {
      JSONObject jo = data.getJSONObject(0);

      String senderID = (String) jo.get(SENDER_ID);

      if(senderID != null && senderID.trim().length() > 0) {
        NotificationService
        .getInstance(getApplicationContext())
        .setSenderID(senderID);
      }

      NotificationService
      .getInstance(getApplicationContext())
      .registerWebView(this.webView);

      NotificationService
      .getInstance(getApplicationContext())
      .addRegisterCallBack(this.webView, callbackContext);

      return true;

    }
    catch (Exception e) {
      Log.e(TAG, "execute: Got JSON Exception " + e.getMessage());
      callbackContext.error(e.getMessage());
      return false;
    }
  }

  @Override
  public boolean execute(String action, JSONArray data, CallbackContext callbackContext) {

    Log.v(TAG, "handleRegister -> data: " + data);

    boolean result = false;

    if (REGISTER.equals(action)) {

      result = handleRegister(data, callbackContext);

    }
    else if (ON_MESSAGE_FOREGROUND.equals(action)) {

      result = handleOnMessageForeground(data, callbackContext);

    }
    else if (ON_MESSAGE_BACKGROUND.equals(action)) {

      result = handleOnMessageBackground(data, callbackContext);

    }
    else if (UNREGISTER.equals(action)) {

      result = handleUnRegister(data, callbackContext);

    }
    else {
      result = false;
      Log.e(TAG, "Invalid action : " + action);
      callbackContext.error("Invalid action : " + action);
    }

    return result;
  }

  private boolean handleUnRegister(JSONArray data, CallbackContext callbackContext) {
    Log.v(TAG, "handleUnRegister() -> data: " + data);

    NotificationService
    .getInstance(getApplicationContext())
    .unRegister();

    callbackContext.success();
    return true;
  }

  private boolean handleOnMessageForeground(JSONArray data, CallbackContext callbackContext) {
    Log.v(TAG, "handleOnMessageForeground() -> data: " + data);

    NotificationService
    .getInstance(getApplicationContext())
    .addNotificationForegroundCallBack(this.webView, callbackContext);

    return true;
  }

  private boolean handleOnMessageBackground(JSONArray data, CallbackContext callbackContext) {
    Log.v(TAG, "handleOnMessageBackground() -> data: " + data);

    NotificationService
    .getInstance(getApplicationContext())
    .addNotificationBackgroundCallBack(this.webView, callbackContext);

    return true;
  }

  @Override
  public void onPause(boolean multitasking) {
    super.onPause(multitasking);

    Log.v(TAG, "onPause() -> webView: " + webView);

    NotificationService
    .getInstance(getApplicationContext())
    .setForeground(false);
  }

  @Override
  public void onResume(boolean multitasking) {
    super.onResume(multitasking);

    Log.v(TAG, "onResume() -> webView: " + webView);

    NotificationService
    .getInstance(getApplicationContext())
    .setForeground(true);
  }


  public void onDestroy() {

    Log.v(TAG, "onDestroy() -> webView: " + webView);

    NotificationService
    .getInstance(getApplicationContext())
    .removeWebView(this.webView);

    super.onDestroy();
  }
}
