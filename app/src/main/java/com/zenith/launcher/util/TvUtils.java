package com.zenith.launcher.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.provider.Settings;

import com.zenith.launcher.model.AppItem;
import com.zenith.launcher.model.AppActionCapabilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TvUtils {

    // Curated priority order for Zenith TV Favorites dock
    public static final List<String> FAVORITE_PACKAGES = Arrays.asList(
        "com.spotify.tv.android",
        "com.google.android.youtube.tv",
        "com.netflix.ninja",
        "com.bstar.intl.tv",
        "com.bstar.bilibili.global",
        "com.vidio.android.tv",
        "com.vuclip.viu.tv",
        "com.vuclip.viu",
        "com.amazon.amazonvideo.livingroom",
        "org.videolan.vlc",
        "com.google.android.youtube.tvmusic"
    );

    private static final String PREF_NAME = "tv_favorites_pref";
    private static final String KEY_FAVORITES = "user_fav_packages";
    private static final String KEY_FAVORITES_ORDER = "user_fav_packages_order";
    private static final String KEY_ALL_APPS_ORDER = "user_all_apps_order";

    public static List<String> getAllAppsOrder(Context context) {
        android.content.SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String saved = sp.getString(KEY_ALL_APPS_ORDER, null);
        List<String> list = new ArrayList<>();
        if (saved != null && !saved.trim().isEmpty()) {
            String[] tokens = saved.split(",");
            for (String t : tokens) {
                if (!t.trim().isEmpty() && !list.contains(t.trim())) {
                    list.add(t.trim());
                }
            }
        }
        return list;
    }

    public static void saveAllAppsOrder(Context context, List<String> orderedPackages) {
        android.content.SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < orderedPackages.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(orderedPackages.get(i));
        }
        sp.edit()
            .putString(KEY_ALL_APPS_ORDER, sb.toString())
            .apply();
    }

    public static List<String> getFavoritePackagesOrder(Context context) {
        android.content.SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String saved = sp.getString(KEY_FAVORITES_ORDER, null);
        List<String> list = new ArrayList<>();
        if (saved != null && !saved.trim().isEmpty()) {
            String[] tokens = saved.split(",");
            for (String t : tokens) {
                if (!t.trim().isEmpty() && !list.contains(t.trim())) {
                    list.add(t.trim());
                }
            }
        }
        if (list.isEmpty()) {
            list.addAll(FAVORITE_PACKAGES);
        }
        return list;
    }

    public static void saveFavoritesOrder(Context context, List<String> orderedPackages) {
        android.content.SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < orderedPackages.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(orderedPackages.get(i));
        }
        Set<String> set = new HashSet<>(orderedPackages);
        sp.edit()
            .putString(KEY_FAVORITES_ORDER, sb.toString())
            .putStringSet(KEY_FAVORITES, set)
            .apply();
    }

    public static void removeFavorite(Context context, String pkg) {
        List<String> list = getFavoritePackagesOrder(context);
        list.remove(pkg);
        saveFavoritesOrder(context, list);
    }

    public static void addFavorite(Context context, String pkg) {
        List<String> list = getFavoritePackagesOrder(context);
        if (!list.contains(pkg)) {
            list.add(pkg);
            saveFavoritesOrder(context, list);
        }
    }

    public static Set<String> getUserFavorites(Context context) {
        return new HashSet<>(getFavoritePackagesOrder(context));
    }

    public static boolean isFavorite(Context context, String pkg) {
        return getUserFavorites(context).contains(pkg);
    }

    public static boolean toggleFavorite(Context context, String pkg) {
        List<String> list = getFavoritePackagesOrder(context);
        boolean added;
        if (list.contains(pkg)) {
            list.remove(pkg);
            added = false;
        } else {
            list.add(pkg);
            added = true;
        }
        saveFavoritesOrder(context, list);
        return added;
    }

    public static List<AppItem> loadInstalledApps(Context context) {
        PackageManager pm = context.getPackageManager();
        List<AppItem> apps = new ArrayList<>();
        Set<String> seenPackages = new HashSet<>();

        // 1. Query Leanback Launcher apps
        Intent leanbackIntent = new Intent(Intent.ACTION_MAIN, null);
        leanbackIntent.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        List<ResolveInfo> leanbackApps = pm.queryIntentActivities(leanbackIntent, 0);

        Set<String> userFavs = getUserFavorites(context);

        for (ResolveInfo info : leanbackApps) {
            String pkg = info.activityInfo.packageName;
            if (pkg.equals(context.getPackageName()) || seenPackages.contains(pkg)) {
                continue;
            }
            seenPackages.add(pkg);
            apps.add(createAppItem(context, pm, info, userFavs.contains(pkg)));
        }

        // 2. Query Standard Launcher apps for completeness
        Intent launcherIntent = new Intent(Intent.ACTION_MAIN, null);
        launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> standardApps = pm.queryIntentActivities(launcherIntent, 0);

        for (ResolveInfo info : standardApps) {
            String pkg = info.activityInfo.packageName;
            if (pkg.equals(context.getPackageName()) || seenPackages.contains(pkg)) {
                continue;
            }
            if (pkg.startsWith("com.android.providers") || pkg.contains("keychain")) {
                continue;
            }
            seenPackages.add(pkg);
            apps.add(createAppItem(context, pm, info, userFavs.contains(pkg)));
        }

        final List<String> favOrder = getFavoritePackagesOrder(context);
        final List<String> allAppsOrder = getAllAppsOrder(context);
        final Set<String> finalFavs = userFavs;
        // Sort by Favorite custom order first, then remaining apps by user order (or label)
        Collections.sort(apps, new Comparator<AppItem>() {
            @Override
            public int compare(AppItem o1, AppItem o2) {
                boolean fav1 = finalFavs.contains(o1.getPackageName());
                boolean fav2 = finalFavs.contains(o2.getPackageName());
                if (fav1 && fav2) {
                    int idx1 = favOrder.indexOf(o1.getPackageName());
                    int idx2 = favOrder.indexOf(o2.getPackageName());
                    if (idx1 != -1 && idx2 != -1) return Integer.compare(idx1, idx2);
                    if (idx1 != -1) return -1;
                    if (idx2 != -1) return 1;
                }
                if (fav1 && !fav2) return -1;
                if (!fav1 && fav2) return 1;

                if (!allAppsOrder.isEmpty()) {
                    int idx1 = allAppsOrder.indexOf(o1.getPackageName());
                    int idx2 = allAppsOrder.indexOf(o2.getPackageName());
                    if (idx1 != -1 && idx2 != -1) return Integer.compare(idx1, idx2);
                    if (idx1 != -1) return -1;
                    if (idx2 != -1) return 1;
                }
                return o1.getLabel().compareToIgnoreCase(o2.getLabel());
            }
        });

        return apps;
    }

    private static AppItem createAppItem(Context context, PackageManager pm, ResolveInfo info, boolean isFavorite) {
        ActivityInfo actInfo = info.activityInfo;
        ApplicationInfo appInfo = actInfo.applicationInfo;
        String pkg = actInfo.packageName;
        String label = info.loadLabel(pm).toString();

        Drawable icon = info.loadIcon(pm);
        Drawable banner = null;

        if (actInfo.banner != 0) {
            banner = pm.getDrawable(pkg, actInfo.banner, appInfo);
        } else if (appInfo.banner != 0) {
            banner = pm.getDrawable(pkg, appInfo.banner, appInfo);
        }

        String tagline = getAppTagline(pkg, label);
        String category = getAppCategory(pkg);

        AppActionCapabilities caps = AppActionCapabilities.evaluate(context, pkg);

        return new AppItem(pkg, actInfo.name, label, icon, banner, tagline, category, isFavorite, caps);
    }

    public static String getAppTagline(String pkg, String label) {
        if (pkg.contains("youtube.tvmusic")) return "Jelajahi jutaan lagu, playlist, dan album favorit Anda dalam kualitas audio jernih.";
        if (pkg.contains("youtube")) return "Tonton video trending, musik, podcast, dan konten kreator terbaik dunia dalam resolusi Ultra HD 4K.";
        if (pkg.contains("netflix")) return "Streaming serial film pemenang penghargaan, drama eksklusif, dan dokumenter orisinal.";
        if (pkg.contains("spotify")) return "Dengarkan musik tanpa jeda, audio Hi-Fi, dan jutaan podcast pilihan dari seluruh dunia.";
        if (pkg.contains("vidio")) return "Live sports terlengkap: Premier League, BRI Liga 1, UCL, tayangan sinetron, dan Vidio Originals.";
        if (pkg.contains("viu")) return "Nonton drama Korea terbaru, anime Jepang, dan serial Asia terpopuler dengan subtitle resmi Indonesia.";
        if (pkg.contains("bstar") || pkg.contains("bilibili")) return "Pusat anime terbesar, pop culture, video kreator, dan live streaming seru.";
        if (pkg.contains("amazonvideo")) return "Tonton film blockbuster bioskop, Amazon Originals, dan serial drama kelas dunia.";
        if (pkg.contains("vlc")) return "Pemutar media serbaguna berperforma tinggi untuk memutar video lokal, USB, dan jaringan.";
        return "Buka aplikasi " + label + " di Smart TV.";
    }

    public static String getAppCategory(String pkg) {
        if (pkg.contains("spotify") || pkg.contains("music")) return "MUSIK";
        if (pkg.contains("youtube") || pkg.contains("netflix") || pkg.contains("viu") || pkg.contains("vidio") || pkg.contains("amazonvideo") || pkg.contains("bilibili")) return "STREAMING";
        if (pkg.contains("vlc") || pkg.contains("mxplayer")) return "MEDIA";
        return "APLIKASI";
    }

    public static void openDashboard(Context context) {
        openQuickSettings(context);
    }

    public static void openQuickSettings(Context context) {
        try {
            GlobalSidebarOverlayManager.getInstance(context).toggleSidebar();
        } catch (Exception e) {
            try {
                Intent intent = new Intent(context, com.zenith.launcher.QuickSettingsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                context.startActivity(intent);
            } catch (Exception e2) {
                openSettings(context);
            }
        }
    }

    public static void startScreensaver(Context context) {
        try {
            Class<?> sandmanClass = Class.forName("android.service.dreams.Sandman");
            java.lang.reflect.Method startDream = sandmanClass.getMethod("startDreamByUser", Context.class, boolean.class);
            startDream.invoke(null, context, true);
            return;
        } catch (Throwable ignored) {}

        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setClassName("com.google.android.apps.tv.dreamx", "android.service.dreams.DreamActivity");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Throwable ignored) {}
    }

    public static void openPictureSettings(Context context) {
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.android.tv.settings", "com.android.tv.settings.partnercustomizer.picture.PictureActivity"));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            openSettings(context);
        }
    }

    public static void openSoundSettings(Context context) {
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.android.tv.settings", "com.android.tv.settings.device.sound.SoundActivity"));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            openSettings(context);
        }
    }

    public static void openBluetoothSettings(Context context) {
        try {
            Intent intent = new Intent();
            intent.setAction("com.google.android.intent.action.CONNECT_INPUT");

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception e2) {
                openSettings(context);
            }
        }
    }

    public static void openAccessibilitySettings(Context context) {
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.android.tv.settings", "com.android.tv.settings.oemlink.AccessibilitySettingsActivity"));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception e2) {
                openSettings(context);
            }
        }
    }

    public static void openVendorInputOverlay(Context context) {
        try {
            Intent intent = new Intent("com.android.tv.action.VIEW_INPUTS");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            try {
                Intent intent = new Intent("com.tcl.suspension.action.main");
                intent.setComponent(new ComponentName("com.tcl.suspension", "com.tcl.suspension.ui.main.MainActivity"));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception e2) {
                openInputs(context);
            }
        }
    }
    public static void openNetworkSettings(Context context) {
        try {
            Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e1) {
            try {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName("com.android.tv.settings", "com.android.tv.settings.connectivity.NetworkActivity"));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception e2) {
                openSettings(context);
            }
        }
    }

    public static void openSettings(Context context) {
        try {
            Intent intent = new Intent(Settings.ACTION_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e1) {
            try {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName("com.android.tv.settings", "com.android.tv.settings.MainSettings"));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception ignored) {
            }
        }
    }

    public static void switchToInput(Context context, String inputId) {
        try {
            android.net.Uri uri = android.media.tv.TvContract.buildChannelUriForPassthroughInput(inputId);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(android.net.Uri.parse("content://android.media.tv/passthrough/" + android.net.Uri.encode(inputId)));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception e2) {
                openInputs(context);
            }
        }
    }

    public static void switchToHdmi1(Context context) {
        switchToInput(context, "com.mediatek.tvinput/.hdmi.HDMIInputService/HW5");
    }

    public static void switchToHdmi2(Context context) {
        switchToInput(context, "com.mediatek.tvinput/.hdmi.HDMIInputService/HW6");
    }

    public static void switchToAv(Context context) {
        switchToInput(context, "com.mediatek.tvinput/.composite.CompositeInputService/HW2");
    }

    public static void openLiveTv(Context context) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setComponent(new ComponentName("com.mediatek.wwtv.tvcenter", "com.mediatek.wwtv.tvcenter.nav.TurnkeyUiMainActivity"));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            try {
                switchToInput(context, "com.mediatek.tvinput/.tuner.TunerInputService/HW0");
            } catch (Exception e2) {
                openInputs(context);
            }
        }
    }

    public static void openInputs(Context context) {
        try {
            Intent intent = new Intent("com.mediatek.tv.action.SELECT_SOURCE");
            intent.setComponent(new ComponentName("com.mediatek.wwtv.tvcenter", "com.mediatek.wwtv.tvcenter.nav.TurnkeyUiMainActivity"));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e1) {
            try {
                Intent intent = new Intent("android.intent.action.VIEW");
                intent.setData(android.net.Uri.parse("content://android.media.tv/passthrough"));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception ignored) {
                openSettings(context);
            }
        }
    }

    public static void searchMediaOnTv(Context context, String query) {
        try {
            Intent intent = new Intent(Intent.ACTION_SEARCH);
            intent.setPackage("com.google.android.youtube.tv");
            intent.putExtra("query", query + " trailer");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e1) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(android.net.Uri.parse("https://www.youtube.com/results?search_query=" + android.net.Uri.encode(query + " trailer")));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception ignored) {
            }
        }
    }

    public static void openVoiceSearch(Context context) {
        try {
            Intent intent = new Intent(Intent.ACTION_VOICE_COMMAND);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Intent.ACTION_SEARCH);
                intent.setPackage("com.google.android.katniss");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception ignored) {
            }
        }
    }

    public static boolean launchPackage(Context context, String pkg) {
        try {
            Intent intent = context.getPackageManager().getLaunchIntentForPackage(pkg);
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                context.startActivity(intent);
                return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }
}
