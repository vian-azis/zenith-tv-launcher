package com.zenith.launcher.model;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Evaluates real-time application capabilities and flags directly from Android PackageManager.
 * Prevents invalid actions on system apps, updated system apps, and core protected components.
 */
public class AppActionCapabilities {

    public static final Set<String> CORE_PROTECTED_PACKAGES = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList(
            "com.android.tv.settings",
            "com.android.vending",
            "com.google.android.gms",
            "com.google.android.gsf",
            "com.android.systemui",
            "com.google.android.inputmethod.latin",
            "com.tcl.suspension",
            "com.tcl.partnercustomizer",
            "com.mediatek.tvinput",
            "com.mediatek.wwtv.tvcenter",
            "com.mediatek.tv.service",
            "com.google.android.apps.tv.dreamx",
            "com.dolby.android.audio.service",
            "com.android.bluetooth",
            "android"
        ))
    );

    private final String packageName;
    private final boolean isSystemApp;
    private final boolean isUpdatedSystemApp;
    private final boolean isStopped;
    private final boolean isEnabled;
    private final boolean isSuspended;
    private final boolean isActiveAdmin;
    private final boolean isSelfLauncher;
    private final boolean isCoreProtectedPackage;
    private final long versionCode;
    private final String versionName;

    public AppActionCapabilities(
            String packageName,
            boolean isSystemApp,
            boolean isUpdatedSystemApp,
            boolean isStopped,
            boolean isEnabled,
            boolean isSuspended,
            boolean isActiveAdmin,
            boolean isSelfLauncher,
            boolean isCoreProtectedPackage,
            long versionCode,
            String versionName) {
        this.packageName = packageName != null ? packageName : "";
        this.isSystemApp = isSystemApp;
        this.isUpdatedSystemApp = isUpdatedSystemApp;
        this.isStopped = isStopped;
        this.isEnabled = isEnabled;
        this.isSuspended = isSuspended;
        this.isActiveAdmin = isActiveAdmin;
        this.isSelfLauncher = isSelfLauncher;
        this.isCoreProtectedPackage = isCoreProtectedPackage;
        this.versionCode = versionCode;
        this.versionName = versionName != null ? versionName : "";
    }

    public static AppActionCapabilities evaluate(Context context, String packageName) {
        if (context == null || packageName == null || packageName.isEmpty()) {
            return new AppActionCapabilities("", false, false, false, false, false, false, false, false, 0, "");
        }

        PackageManager pm = context.getPackageManager();
        boolean isSelf = packageName.equals(context.getPackageName());
        boolean isCore = isCoreProtected(context, packageName);
        boolean isAdmin = false;

        try {
            DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            if (dpm != null) {
                try {
                    Method m = dpm.getClass().getMethod("packageHasActiveAdmins", String.class);
                    Object res = m.invoke(dpm, packageName);
                    if (res instanceof Boolean) {
                        isAdmin = (Boolean) res;
                    }
                } catch (Throwable fallback) {
                    List<ComponentName> admins = dpm.getActiveAdmins();
                    if (admins != null) {
                        for (ComponentName admin : admins) {
                            if (packageName.equals(admin.getPackageName())) {
                                isAdmin = true;
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        try {
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            PackageInfo pkgInfo = pm.getPackageInfo(packageName, 0);

            boolean isSystem = (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
            boolean isUpdatedSystem = (appInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0;
            boolean isStopped = (appInfo.flags & ApplicationInfo.FLAG_STOPPED) != 0;
            boolean isEnabled = appInfo.enabled;
            boolean isSuspended = (appInfo.flags & ApplicationInfo.FLAG_SUSPENDED) != 0;

            long vCode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P 
                ? pkgInfo.getLongVersionCode() 
                : pkgInfo.versionCode;
            String vName = pkgInfo.versionName != null ? pkgInfo.versionName : "";

            return new AppActionCapabilities(
                packageName,
                isSystem,
                isUpdatedSystem,
                isStopped,
                isEnabled,
                isSuspended,
                isAdmin,
                isSelf,
                isCore,
                vCode,
                vName
            );
        } catch (PackageManager.NameNotFoundException e) {
            return new AppActionCapabilities(
                packageName,
                false,
                false,
                false,
                false,
                false,
                isAdmin,
                isSelf,
                isCore,
                0,
                ""
            );
        }
    }

    public static boolean isCoreProtected(Context context, String pkg) {
        if (pkg == null) return false;
        if (context != null && pkg.equals(context.getPackageName())) return true;
        if (CORE_PROTECTED_PACKAGES.contains(pkg)) return true;
        if (pkg.startsWith("com.android.providers.")) return true;
        if (pkg.startsWith("com.android.cts.")) return true;
        if (pkg.contains("keychain")) return true;
        return false;
    }

    public String getPackageName() {
        return packageName;
    }

    public boolean isSystemApp() {
        return isSystemApp;
    }

    public boolean isUpdatedSystemApp() {
        return isUpdatedSystemApp;
    }

    public boolean isStopped() {
        return isStopped;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public boolean isSuspended() {
        return isSuspended;
    }

    public boolean isActiveAdmin() {
        return isActiveAdmin;
    }

    public boolean isSelfLauncher() {
        return isSelfLauncher;
    }

    public boolean isCoreProtectedPackage() {
        return isCoreProtectedPackage;
    }

    public long getVersionCode() {
        return versionCode;
    }

    public String getVersionName() {
        return versionName;
    }

    public boolean canOpen() {
        return isEnabled && !isSuspended && !isSelfLauncher;
    }

    public boolean canUninstall() {
        return !isSystemApp && !isUpdatedSystemApp && !isActiveAdmin && !isSelfLauncher && !isCoreProtectedPackage;
    }

    public boolean canUninstallUpdates() {
        return isSystemApp && isUpdatedSystemApp && !isSelfLauncher && !isCoreProtectedPackage;
    }

    public boolean isPureSystemApp() {
        return isSystemApp && !isUpdatedSystemApp;
    }

    public String getAppTypeBadge() {
        if (canUninstallUpdates()) {
            return "Aplikasi Sistem • Pembaruan Terpasang";
        } else if (isSystemApp) {
            return "Aplikasi Sistem Bawaan";
        } else {
            return "Aplikasi Pengguna";
        }
    }
}
