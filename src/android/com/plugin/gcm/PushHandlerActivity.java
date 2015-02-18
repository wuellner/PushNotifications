package com.plugin.gcm;

import android.app.NotificationManager;
import android.content.Context;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

public class PushHandlerActivity extends Activity {

    public static final String COLDSTART = "coldstart";

    public static final String PUSH_BUNDLE = "pushBundle";

    private static String TAG = "PushPlugin-PushHandlerActivity";

    /*
     * this activity will be started if the user touches a notification that we own.
     * We send it's data off to the push plugin for processing.
     * If needed, we boot up the main activity to kickstart the application.
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean isPushPluginActive = NotificationService.getInstance(getApplicationContext())
                                                        .isActive();

        Log.v(TAG, "onCreate - isPushPluginActive: " + isPushPluginActive);

        processPushBundle(isPushPluginActive);

        GCMIntentService.cancelNotification(this);

        if (!isPushPluginActive) {
            forceMainActivityReload();
        }

        finish();
    }

    /**
     * Takes the pushBundle extras from the intent, and sends it through to the PushPlugin for
     * processing.
     */
    private void processPushBundle(boolean isPushPluginActive) {
        Bundle extras = getIntent().getExtras();

        if (extras != null) {

            Bundle originalExtras = extras.getBundle(PUSH_BUNDLE);

            if (!isPushPluginActive) {
                originalExtras.putBoolean(COLDSTART, true);
            }

            NotificationService.getInstance(getApplicationContext()).onMessage(originalExtras, true);
        }
    }


    /**
     * Forces the main activity to re-launch if it's unloaded.
     */
    private void forceMainActivityReload() {
        PackageManager pm = getPackageManager();
        String packageName = getApplicationContext().getPackageName();

        Log.v(TAG, "forceMainActivityReload() - packageName: " + packageName);

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
