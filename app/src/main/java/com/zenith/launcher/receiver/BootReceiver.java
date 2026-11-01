package com.zenith.launcher.receiver;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

import com.zenith.launcher.MainActivity;
import com.zenith.launcher.ZenithAccessibilityService;
import com.zenith.launcher.QuickSettingsAccessibilityService;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "ZenithBootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        Log.d(TAG, "Boot broadcast received: " + action);

        // Auto-heal and ensure Accessibility Service is permanently enabled for remote gear key interception
        ensureAccessibilityServiceEnabled(context);

        // Guard: Do NOT restart or steal focus if MainActivity is already running!
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

    public static void ensureAccessibilityServiceEnabled(Context context) {
        if (context == null) return;
        try {
            ContentResolver resolver = context.getContentResolver();
            String s1 = context.getPackageName() + "/" + ZenithAccessibilityService.class.getName();
            String s2 = context.getPackageName() + "/" + QuickSettingsAccessibilityService.class.getName();
            String s1Short = context.getPackageName() + "/.ZenithAccessibilityService";
            String s2Short = context.getPackageName() + "/.QuickSettingsAccessibilityService";

            String enabled = Settings.Secure.getString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (enabled == null || (!enabled.contains(s1) && !enabled.contains(s1Short) && !enabled.contains(s2) && !enabled.contains(s2Short))) {
                String target = s1Short + ":" + s2Short;
                String newServices = (enabled == null || enabled.trim().isEmpty()) ? target : enabled + ":" + target;
                Settings.Secure.putString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, newServices);
                Log.d(TAG, "Enabled accessibility services configured: " + newServices);
            }
            Settings.Secure.putInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, 1);
        } catch (Throwable t) {
            Log.e(TAG, "Could not configure accessibility settings: " + t.getMessage());
        }
    }
}
