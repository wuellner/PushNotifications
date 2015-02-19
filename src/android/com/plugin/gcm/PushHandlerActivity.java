package com.plugin.gcm;

import com.appgyver.cordova.AGCordovaApplicationInterface;

import android.app.NotificationManager;
import android.content.Context;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

public class PushHandlerActivity extends Activity {

    public static final String PUSH_BUNDLE = "pushBundle";

    private static String TAG = "PushPlugin-PushHandlerActivity";

    /*
     * This activity will be started if the user touches a notification that we own.
     * If needed, we boot up the main activity to kickstart the application.
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate - isApplicationRunning: " + isApplicationRunning());

        GCMIntentService.cancelNotification(this);

        if ( !isApplicationRunning() ) {
            forceMainActivityReload();
        }

        finish();
    }

    private boolean isApplicationRunning() {
        return ((AGCordovaApplicationInterface) getApplicationContext()).getCurrentActivity() != null;
    }

    /**
     * Forces the main activity to re-launch if it's unloaded.
     */
    private void forceMainActivityReload() {
        PackageManager pm = getPackageManager();
        String packageName = getApplicationContext().getPackageName();

        Log.d(TAG, "forceMainActivityReload() - packageName: " + packageName);

        Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
        startActivity(launchIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP));
    }

    @Override
    protected void onResume() {
      super.onResume();
      final NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
      notificationManager.cancelAll();
    }

}
