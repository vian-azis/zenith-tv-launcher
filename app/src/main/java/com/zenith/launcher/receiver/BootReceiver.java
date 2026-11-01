package com.zenith.launcher.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.zenith.launcher.MainActivity;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "ZenithBootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        Log.d(TAG, "Boot broadcast received: " + action);

        // Guard: Do NOT restart or steal focus if MainActivity is already running!
        // Android TV automatically launches the home launcher on boot.
        // Firing startActivity with CLEAR_TOP here would disrupt the user if they already opened an app!
        if (MainActivity.getInstance() != null) {
            Log.d(TAG, "MainActivity already running, skipping boot launch to prevent foreground stealing.");
            return;
        }

        Intent launch = new Intent(context, MainActivity.class);
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK 
                | Intent.FLAG_ACTIVITY_CLEAR_TOP 
                | Intent.FLAG_ACTIVITY_SINGLE_TOP 
                | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        try {
            context.startActivity(launch);
            Log.d(TAG, "MainActivity started immediately from BootReceiver");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start MainActivity from BootReceiver", e);
        }
    }
}

