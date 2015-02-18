package com.plugin.gcm;

import android.app.NotificationManager;

import com.google.android.gcm.GCMRegistrar;

import com.appgyver.event.EventService;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.TimeZone;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.DateFormat;

/**
 * Notification Service - Handles Push Notification and deliver the messages to all web views
 * that have registered callbacks.
 */
public class NotificationService {

    public static final String USER_ACTION = "userAction";

    private static String TAG = "PushPlugin-NotificationService";

    public static final String REG_ID = "regID";

    public static final String FOREGROUND = "foreground";

    public static final String COLDSTART = "coldstart";

    public static final String FROM = "from";

    public static final String COLLAPSE_KEY = "collapse_key";

    public static final String MESSAGE = "message";

    public static final String MSGCNT = "msgcnt";

    public static final String SOUNDNAME = "soundname";

    public static final String JSON_START_PREFIX = "{";

    public static final String JSON_ARRAY_START_PREFIX = "[";

    public static final String PAYLOAD = "payload";

    public static final String TIMESTAMP = "timestamp";

    public static final String KEY_UUID = "uuid";

    private static NotificationService sInstance;

    private boolean mIsActive = true;

    private final Context mContext;

    private String mSenderID;

    private List<WebViewReference> mWebViewReferences = new ArrayList<WebViewReference>();

    private String mRegistrationID = null;

    private List<JSONObject> mNotifications = new ArrayList<JSONObject>();

    private boolean mForeground = false;

    public NotificationService(Context context) {
        mContext = context;

        EventService.getInstance().addEventListener(EventService.STEROIDS_APPLICATION_STARTED,
                new EventService.EventHandler() {
                    @Override
                    public void call(Object eventContext) {
                        mIsActive = true;
                    }
                });

        EventService.getInstance().addEventListener(EventService.STEROIDS_APPLICATION_ENDED,
                new EventService.EventHandler() {
                    @Override
                    public void call(Object eventContext) {
                        cleanUp();
                        mIsActive = false;
                    }
                });
    }

    public boolean isActive() {
        return mIsActive;
    }

    public static NotificationService getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new NotificationService(context);
        }
        return sInstance;
    }

    public void setSenderID(String senderID) {
        if (senderID != null && senderID.trim().length() > 0) {
            mSenderID = senderID;
        }
    }

    public void addRegisterCallBack(CordovaWebView webView, CallbackContext callBack) {
        WebViewReference webViewReference = getWebViewReference(webView);
        webViewReference.setRegisterCallBack(callBack);

        if (isRegistered()) {
            webViewReference.notifyRegistered();
        } else {
            registerDevice();
        }
    }

    public void addNotificationForegroundCallBack(CordovaWebView webView,
                                                  CallbackContext callBack) {
        WebViewReference webViewReference = getWebViewReference(webView);
        webViewReference.setNotificationForegroundCallBack(callBack);

        flushNotificationToWebView(webViewReference);
    }

    public void addNotificationBackgroundCallBack(CordovaWebView webView,
                                                  CallbackContext callBack) {
        WebViewReference webViewReference = getWebViewReference(webView);
        webViewReference.setNotificationBackgroundCallBack(callBack);

        flushNotificationToWebView(webViewReference);
    }

    public void removeWebView(CordovaWebView webView) {
        WebViewReference webViewReference = findWebViewReference(webView);
        if (webViewReference != null) {
            mWebViewReferences.remove(webViewReference);
            webViewReference.destroy();

            Log.v(TAG, "removeWebView : " + webView + " - after remove -> mWebViewReferences: "
                    + mWebViewReferences);
        }
    }

    private WebViewReference findWebViewReference(CordovaWebView webView) {
        WebViewReference webViewReference = null;
        for (WebViewReference item : mWebViewReferences) {
            if (item.getWebView() == webView) {
                webViewReference = item;
                break;
            }
        }
        return webViewReference;
    }

    private WebViewReference getWebViewReference(CordovaWebView webView) {
        WebViewReference webViewReference = findWebViewReference(webView);
        if (webViewReference == null) {
            webViewReference = createWebViewReference(webView);
        }
        return webViewReference;
    }

    public void registerWebView(CordovaWebView webView) {
        getWebViewReference(webView);
    }

    private boolean isRegistered() {
        return mRegistrationID != null;
    }

    private WebViewReference createWebViewReference(CordovaWebView webView) {
        WebViewReference webViewReference = new WebViewReference(this, webView);
        mWebViewReferences.add(webViewReference);
        return webViewReference;
    }

    private void registerDevice() {
        if (mSenderID == null) {
            throw new IllegalArgumentException(
                    "sender ID is required in order to register this device for push notifications. You must specify the senderId in the ApplicationManifest or when calling the JavaScript API.");
        }
        GCMRegistrar.register(mContext, mSenderID);
    }

    public void onRegistered(String regId) {
        mRegistrationID = regId;
        notifyRegisteredToAllWebViews();
    }

    private void notifyRegisteredToAllWebViews() {
        for (WebViewReference webViewReference : mWebViewReferences) {
            webViewReference.notifyRegistered();
        }
    }

    public void onMessage(Bundle extras) {
        onMessage(extras, false);
    }

    /**
     * Deliver a notification message..
     *
     * @param extras
     * @param userAction -    When this flag is true we try to remove a duplicate notification.
     *                   This only happens when the IntentService delivers the notification
     *                   and the PushHandlerActivity also delivers the same notification
     *                   because the user tapped in the notification.
     */
    public void onMessage(Bundle extras, boolean userAction) {
        JSONObject notification = createNotificationJSON(extras);

        if (userAction) {
            tryToRemoveDuplicate(notification);

            try {
                notification.put(USER_ACTION, true);
            } catch (JSONException e) {
                /*no op*/
            }
        }

        Log.v(TAG, "onMessage() -> isForeground: " + isForeground() + " notification: "
                + notification);

        addNotification(notification);

        notifyAllWebViews();
    }

    private void tryToRemoveDuplicate(JSONObject notification) {
        int idx = 0;
        boolean found = false;
        for (idx = 0; idx < mNotifications.size(); idx++) {
            JSONObject item = mNotifications.get(idx);
            if (isEqual(notification, item)) {
                found = true;
                break;
            }
        }
        if (found) {
            Log.v(TAG, "tryToRemoveDuplicate() Duplicate found.. and removed.");
            mNotifications.remove(idx);
        }
    }

    private boolean isEqual(JSONObject from, JSONObject to) {
        boolean isEqual = false;

        if (from != null && to != null &&
                from.length() == to.length()) {

            try {
                Iterator<String> keys = from.keys();
                isEqual = true;
                while (keys.hasNext() && isEqual) {
                    String key = keys.next();

                    if (from.get(key) == null && to.get(key) == null) {
                        continue;
                    }

                    if (from.get(key) instanceof JSONObject) {
                        isEqual = isEqual((JSONObject) from.get(key), (JSONObject) to.get(key));
                    } else {
                        isEqual = from.get(key).equals(to.get(key));
                    }
                }
            } catch (Exception exp) {
                /*no op*/
            }
        } else {
            isEqual = false;
        }

        return isEqual;
    }


    private void notifyAllWebViews() {
        for (WebViewReference webViewReference : mWebViewReferences) {
            flushNotificationToWebView(webViewReference);
        }
    }

    private void flushNotificationToWebView(WebViewReference webViewReference) {
        Log.v(TAG, "flushNotificationToWebView() - Notifications.size(): " + mNotifications.size() + " -> webViewReference: " + webViewReference);

        for (JSONObject notification : mNotifications) {
            webViewReference.sendNotification(notification);
        }
    }

    private void addNotification(JSONObject notification) {
        mNotifications.add(notification);
    }

    private JSONObject createNotificationJSON(Bundle extras) {
        try {

            JSONObject notification = new JSONObject();
            JSONObject payload = new JSONObject();

            Iterator<String> it = extras.keySet().iterator();
            while (it.hasNext()) {
                String key = it.next();

                if (parseSystemData(key, notification, extras)) {
                    continue;
                }

                parseLegacyProperty(key, notification, extras);

                parseJsonProperty(key, notification, extras, payload);
            }

            notification.put(PAYLOAD, payload);

            notification.put(FOREGROUND, isForeground());

            notification.put(TIMESTAMP, getTimeStamp());

            notification.put(KEY_UUID, generateUUID());

            return notification;

        } catch (JSONException e) {
            Log.e(TAG, "extrasToJSON: JSON exception");
        }
        return null;
    }

    private String generateUUID() {
        UUID uuid = UUID.randomUUID();

        return uuid.toString();
    }

    private String getTimeStamp() {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");

        df.setTimeZone(tz);
        String timeAsISO = df.format(new Date());

        return timeAsISO;
    }

    // Try to figure out if the value is another JSON object or JSON Array
    private void parseJsonProperty(String key, JSONObject json, Bundle extras,
                                   JSONObject jsondata) throws JSONException {

        if (extras.get(key) instanceof String) {
            String strValue = extras.getString(key);

            if (strValue.startsWith(JSON_START_PREFIX)) {
                try {
                    JSONObject jsonObj = new JSONObject(strValue);
                    jsondata.put(key, jsonObj);
                } catch (Exception e) {
                    jsondata.put(key, strValue);
                }

            } else if (strValue.startsWith(JSON_ARRAY_START_PREFIX)) {
                try {
                    JSONArray jsonArray = new JSONArray(strValue);
                    jsondata.put(key, jsonArray);
                } catch (Exception e) {
                    jsondata.put(key, strValue);
                }
            } else {
                if (!json.has(key)) {
                    jsondata.put(key, strValue);
                }
            }
        }
    }

    // Maintain backwards compatibility
    private void parseLegacyProperty(String key, JSONObject json, Bundle extras)
            throws JSONException {
        if (key.equals(MESSAGE) || key.equals(MSGCNT) || key.equals(SOUNDNAME)) {
            json.put(key, extras.get(key));
        }
    }

    private boolean parseSystemData(String key, JSONObject json, Bundle extras)
            throws JSONException {

        boolean found = false;

        if (key.equals(FROM) || key.equals(COLLAPSE_KEY)) {
            json.put(key, extras.get(key));
            found = true;
        } else if (key.equals(COLDSTART)) {
            json.put(key, extras.getBoolean(COLDSTART));
            found = true;
        }

        return found;
    }

    public void setForeground(boolean foreground) {
        if (mForeground != foreground) {
            Log.v(TAG, "setForeground() -> oldValue: " + mForeground + " newValue: " + foreground);

            if (!foreground) {

                final NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancelAll();

            }
        }
        mForeground = foreground;

    }

    public boolean isForeground() {
        return mForeground;
    }

    public void onDestroy() {
        GCMRegistrar.onDestroy(mContext);
        cleanUp();
        sInstance = null;
    }

    public void unRegister() {
        Log.v(TAG, "unRegister");
        GCMRegistrar.unregister(mContext);
        mRegistrationID = null;
        cleanUp();
    }

    private void cleanUp() {
        Log.v(TAG, "Cleaning up");

        mWebViewReferences.clear();
        mNotifications.clear();
    }


    static class WebViewReference {

        private CordovaWebView mWebView;

        private CallbackContext mRegisterCallBack;

        private CallbackContext mNotificationForegroundCallBack;

        private CallbackContext mNotificationBackgroundCallBack;

        private NotificationService mNotificationService;

        private boolean mNotifiedOfRegistered = false;

        private List<JSONObject> mNotifications = new ArrayList<JSONObject>();

        public WebViewReference(NotificationService notificationService, CordovaWebView webView) {
            mNotificationService = notificationService;
            mWebView = webView;
        }

        public void destroy() {
            mWebView = null;
            mRegisterCallBack = null;
            mNotificationForegroundCallBack = null;
            mNotificationBackgroundCallBack = null;
            mNotificationService = null;

            mNotifications.clear();
        }

        public boolean hasNotifiedOfRegistered() {
            return mNotifiedOfRegistered;
        }

        public void setNotifiedOfRegistered(boolean notifiedOfRegistered) {
            mNotifiedOfRegistered = notifiedOfRegistered;
        }

        public CordovaWebView getWebView() {
            return mWebView;
        }

        public boolean hasNotification(JSONObject notification) {
            return mNotifications.contains(notification);
        }

        public void notifyRegistered() {
            if (hasNotifiedOfRegistered()) {
                Log.v(TAG,
                        "notifyRegistered() - Webview already notified of registration. skipping callback. webview: "
                                + getWebView());
                return;
            }

            if (getRegisterCallBack() != null) {
                setNotifiedOfRegistered(true);
                getRegisterCallBack().success(mNotificationService.mRegistrationID);
            } else {
                Log.v(TAG, "No Register callback - webview: " + getWebView());
            }
        }

        public void sendNotification(JSONObject notification) {
            if (hasNotification(notification)) {
                //Log.v(TAG,
                //        "sendNotification() - Webview already received this notification. skipping callback. webview: "
                //                + getWebView());
                return;
            }

            boolean isForeground = true;
            try {
                isForeground = notification.getBoolean(FOREGROUND);
            } catch (JSONException e) {
                /*no op*/
            }

            if (isForeground) {
                Log.v(TAG, "sendNotification() - foreground callback - webview: " + getWebView());
                sendNotification(getNotificationForegroundCallBack(), notification);
            } else {
                Log.v(TAG, "sendNotification() - background callback - webview: " + getWebView());
                sendNotification(getNotificationBackgroundCallBack(), notification);
            }
        }

        private void sendNotification(CallbackContext callBack,
                                      JSONObject notification) {

            if (callBack != null) {

                PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, notification);
                pluginResult.setKeepCallback(true);

                callBack.sendPluginResult(pluginResult);

                mNotifications.add(notification);
            } else {
                Log.v(TAG, "No Notification callback - webview: " + getWebView());
            }
        }

        public void setNotificationForegroundCallBack(CallbackContext callBack) {
            Log.v(TAG, "setNotificationForegroundCallBack() - webview: " + getWebView());
            mNotificationForegroundCallBack = callBack;
        }

        public void setNotificationBackgroundCallBack(CallbackContext callBack) {
            Log.v(TAG, "setNotificationBackgroundCallBack() - webview: " + getWebView());
            mNotificationBackgroundCallBack = callBack;
        }

        public CallbackContext getNotificationForegroundCallBack() {
            return mNotificationForegroundCallBack;
        }

        public CallbackContext getNotificationBackgroundCallBack() {
            return mNotificationBackgroundCallBack;
        }

        public void setRegisterCallBack(CallbackContext callBack) {
            mRegisterCallBack = callBack;
        }

        public CallbackContext getRegisterCallBack() {
            return mRegisterCallBack;
        }

        @Override
        public String toString() {
            String webViewStr = "empty";
            if (getWebView() != null) {
                webViewStr = getWebView().toString();
            }
            return "WebViewReference -> " + webViewStr;
        }

    }
}
