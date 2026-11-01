package com.zenith.launcher;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

import com.zenith.launcher.util.GlobalSidebarOverlayManager;
import com.zenith.launcher.util.DimmerOverlayManager;

public class ZenithAccessibilityService extends AccessibilityService {

    private static final String TAG = "ZenithA11y";
    private static ZenithAccessibilityService sInstance;
    private long mLastRedirectTime = 0;
    private long mLastSettingsKeyTime = 0;
    private String mCurrentLauncherXActivity = "";

    private final BroadcastReceiver mSidebarBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;
            String action = intent.getAction();
            Log.d(TAG, "Received sidebar action broadcast: " + action);
            if ("com.zenith.launcher.ACTION_TOGGLE_SIDEBAR".equals(action)) {
                GlobalSidebarOverlayManager.getInstance(ZenithAccessibilityService.this).toggleSidebar();
            } else if ("com.zenith.launcher.ACTION_OPEN_SIDEBAR".equals(action)) {
                GlobalSidebarOverlayManager.getInstance(ZenithAccessibilityService.this).showSidebar();
            } else if ("com.zenith.launcher.ACTION_CLOSE_SIDEBAR".equals(action)) {
                GlobalSidebarOverlayManager.getInstance(ZenithAccessibilityService.this).closeSidebar();
            }
        }
    };

    public static ZenithAccessibilityService getInstance() {
        return sInstance;
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        sInstance = this;
        try {
            android.accessibilityservice.AccessibilityServiceInfo info = getServiceInfo();
            if (info == null) {
                info = new android.accessibilityservice.AccessibilityServiceInfo();
            }
            info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
            info.feedbackType = android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_GENERIC;
            info.flags |= android.accessibilityservice.AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
                    | android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
                    | android.accessibilityservice.AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
            setServiceInfo(info);
            Log.d(TAG, "ZenithAccessibilityService connected with FILTER_KEY_EVENTS & RETRIEVE_WINDOWS");

            // Attach Accessibility Service to Global Managers
            GlobalSidebarOverlayManager.getInstance(this).setAccessibilityService(this);
            DimmerOverlayManager.getInstance(this).attachWithAccessibilityService(this);

            // Register global sidebar broadcast receiver
            IntentFilter filter = new IntentFilter();
            filter.addAction("com.zenith.launcher.ACTION_TOGGLE_SIDEBAR");
            filter.addAction("com.zenith.launcher.ACTION_OPEN_SIDEBAR");
            filter.addAction("com.zenith.launcher.ACTION_CLOSE_SIDEBAR");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(mSidebarBroadcastReceiver, filter, RECEIVER_EXPORTED);
            } else {
                registerReceiver(mSidebarBroadcastReceiver, filter);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error configuring service info", e);
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        try {
            unregisterReceiver(mSidebarBroadcastReceiver);
        } catch (Exception ignored) {}
        sInstance = null;
        return super.onUnbind(intent);
    }

    public boolean openNotifications() {
        return performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS);
    }

    public boolean openQuickSettings() {
        GlobalSidebarOverlayManager.getInstance(this).toggleSidebar();
        return true;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        CharSequence pkg = event.getPackageName();
        CharSequence cls = event.getClassName();

        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (pkg != null) {
                String pkgStr = pkg.toString();


            }
        }

        if (pkg != null && "com.google.android.apps.tv.launcherx".contentEquals(pkg)) {
            if (cls != null) {
                String clsName = cls.toString();

                if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                    mCurrentLauncherXActivity = clsName;
                }

                if (clsName.contains("Dashboard") 
                        || mCurrentLauncherXActivity.contains("Dashboard")
                        || clsName.contains("Notification")
                        || mCurrentLauncherXActivity.contains("Notification")
                        || clsName.contains("Setting")
                        || clsName.contains("Profile")
                        || clsName.contains("Account")
                        || clsName.contains("Reauth")) {
                    return;
                }

                if (clsName.contains(".home.HomeActivity") || clsName.contains(".home.VanillaModeHomeActivity")) {
                    long now = System.currentTimeMillis();
                    if (now - mLastRedirectTime > 300) {
                        mLastRedirectTime = now;
                        Log.d(TAG, "Redirecting Google TV Home to Zenith TV Launcher: " + clsName);
                        Intent home = new Intent(this, MainActivity.class);
                        home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK 
                                | Intent.FLAG_ACTIVITY_CLEAR_TOP 
                                | Intent.FLAG_ACTIVITY_SINGLE_TOP 
                                | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        startActivity(home);
                    }
                    return;
                }
            }
        }
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (event == null) return false;
        int keyCode = event.getKeyCode();
        int action = event.getAction();
        Log.d(TAG, "onKeyEvent: keyCode=" + keyCode + ", action=" + action);

        // Intercept Gear / Quick Settings buttons (Global across all apps)
        // Key 83: KEYCODE_NOTIFICATION (TCL RC813A Remote Gear/Quick Settings key)
        if (keyCode == 83
                || keyCode == KeyEvent.KEYCODE_NOTIFICATION
                || keyCode == KeyEvent.KEYCODE_SETTINGS
                || keyCode == 176
                || keyCode == 227) {

            if (action == KeyEvent.ACTION_DOWN) {
                long now = System.currentTimeMillis();
                if (now - mLastSettingsKeyTime > 350) {
                    mLastSettingsKeyTime = now;
                    Log.d(TAG, "Gear/Settings key (" + keyCode + ") intercepted! Toggling global sidebar overlay");
                    GlobalSidebarOverlayManager.getInstance(this).toggleSidebar();
                }
            }
            return true;
        }

        // Intercept Back button when sidebar is showing
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (GlobalSidebarOverlayManager.getInstance(this).isShowing()) {
                if (action == KeyEvent.ACTION_DOWN) {
                    Log.d(TAG, "Back key intercepted to close sidebar");
                    GlobalSidebarOverlayManager.getInstance(this).closeSidebar();
                }
                return true;
            }
        }

        if (action == KeyEvent.ACTION_DOWN) {

            // Intercept Home button
            if (keyCode == KeyEvent.KEYCODE_HOME) {
                if (GlobalSidebarOverlayManager.getInstance(this).isShowing()) {
                    GlobalSidebarOverlayManager.getInstance(this).closeSidebar();
                }
                Intent home = new Intent(this, MainActivity.class);
                home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK 
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP 
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP 
                        | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(home);
                return true;
            }
        }
        return super.onKeyEvent(event);
    }

    @Override
    public void onInterrupt() {
    }
}
