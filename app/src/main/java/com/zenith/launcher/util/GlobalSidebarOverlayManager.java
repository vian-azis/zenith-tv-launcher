package com.zenith.launcher.util;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.zenith.launcher.R;
import com.zenith.launcher.view.FluidPillSliderView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Global Spatial Frosted Glass Quick Settings Sidebar Overlay Manager.
 * Mounts directly to WindowManager as TYPE_ACCESSIBILITY_OVERLAY / TYPE_APPLICATION_OVERLAY
 * so the sidebar slides in flawlessly over ANY active app (YouTube, Netflix, TV, Launcher).
 */
public class GlobalSidebarOverlayManager {

    private static final String TAG = "GlobalSidebar";
    private static GlobalSidebarOverlayManager sInstance;

    public static volatile boolean sAllowSystemSettingsOnce = false;

    private final Context mContext;
    private WindowManager mWindowManager;
    private WindowManager mActiveWindowManager;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final DimmerOverlayManager mDimmerManager;

    private AccessibilityService mAccessibilityService;
    private View mRootView;
    private FrameLayout mDrawer;
    private View mScrim;

    // Header views
    private TextView mTextDate;
    private TextView mTextClock;
    private FrameLayout mBtnSettings;
    
    // Integrated Fluid Pill Sliders
    private FluidPillSliderView mSliderBrightness;
    private FluidPillSliderView mSliderWarmth;
    private FluidPillSliderView mSliderVolume;

    // Action Tiles
    private LinearLayout mTileScreensaver;
    private LinearLayout mTileInput;
    private LinearLayout mTileDisplay;
    private LinearLayout mTileSound;
    private LinearLayout mTileWifi;
    private LinearLayout mTileBluetooth;
    private TextView mSubWifi;

    private boolean mIsShowing = false;
    private boolean mIsAttached = false;
    private long mLastToggleTime = 0;

    private final BroadcastReceiver mVolumeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateVolumeSlider();
        }
    };

    public static synchronized GlobalSidebarOverlayManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new GlobalSidebarOverlayManager(context.getApplicationContext());
        }
        if (sInstance.mAccessibilityService == null && com.zenith.launcher.ZenithAccessibilityService.getInstance() != null) {
            sInstance.setAccessibilityService(com.zenith.launcher.ZenithAccessibilityService.getInstance());
        }
        return sInstance;
    }

    private GlobalSidebarOverlayManager(Context context) {
        mContext = context;
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mActiveWindowManager = mWindowManager;
        mDimmerManager = DimmerOverlayManager.getInstance(context);

        initViews();
        registerVolumeReceiver();
    }

    public void setAccessibilityService(AccessibilityService service) {
        mAccessibilityService = service;
        if (service != null) {
            try {
                WindowManager a11yWm = (WindowManager) service.getSystemService(Context.WINDOW_SERVICE);
                if (a11yWm != null) {
                    mWindowManager = a11yWm;
                    mActiveWindowManager = a11yWm;
                }
            } catch (Exception ignored) {}
            if (mDimmerManager != null) {
                mDimmerManager.attachWithAccessibilityService(service);
            }
        }
    }

    @SuppressLint("InflateParams")
    private void initViews() {
        try {
            mRootView = LayoutInflater.from(mContext).inflate(R.layout.layout_global_sidebar_overlay, null);
            mDrawer = mRootView.findViewById(R.id.global_sidebar_drawer);
            mScrim = mRootView.findViewById(R.id.global_sidebar_scrim);

            mTextDate = mRootView.findViewById(R.id.sidebar_text_date);
            mTextClock = mRootView.findViewById(R.id.sidebar_text_clock);
            mBtnSettings = mRootView.findViewById(R.id.sidebar_btn_settings);
            
            mSliderBrightness = mRootView.findViewById(R.id.sidebar_slider_brightness);
            mSliderWarmth = mRootView.findViewById(R.id.sidebar_slider_warmth);
            mSliderVolume = mRootView.findViewById(R.id.sidebar_slider_volume);

            if (mSliderBrightness != null) {
                mSliderBrightness.setSliderIcon(R.drawable.ic_qs_brightness);
                mSliderBrightness.setSliderLabel("Kecerahan");
                mSliderBrightness.setOnSliderChangeListener((slider, progress, fromUser) -> {
                    mDimmerManager.setBrightness(progress);
                });
            }

            if (mSliderWarmth != null) {
                mSliderWarmth.setSliderIcon(R.drawable.ic_qs_nightlight);
                mSliderWarmth.setSliderLabel("Kenyamanan");
                mSliderWarmth.setOnSliderChangeListener((slider, progress, fromUser) -> {
                    mDimmerManager.setWarmth(progress);
                });
            }

            if (mSliderVolume != null) {
                mSliderVolume.setSliderIcon(R.drawable.ic_qs_volume);
                mSliderVolume.setSliderLabel("Volume");
                mSliderVolume.setOnSliderChangeListener((slider, progress, fromUser) -> {
                    setSystemVolumePercentage(progress);
                });
            }

            mTileScreensaver = mRootView.findViewById(R.id.sidebar_tile_screensaver);
            mTileInput = mRootView.findViewById(R.id.sidebar_tile_input);
            mTileDisplay = mRootView.findViewById(R.id.sidebar_tile_display);
            mTileSound = mRootView.findViewById(R.id.sidebar_tile_sound);
            mTileWifi = mRootView.findViewById(R.id.sidebar_tile_wifi);
            mTileBluetooth = mRootView.findViewById(R.id.sidebar_tile_bluetooth);
            mSubWifi = mRootView.findViewById(R.id.sidebar_sub_wifi);

            setupFocusInversionAndAntiClip();
            setupClickAndDpadActions();
        } catch (Exception e) {
            Log.e(TAG, "Error inflating global sidebar layout", e);
        }
    }

    private void setupFocusInversionAndAntiClip() {
        View[] interactiveViews = new View[]{
                mBtnSettings,
                mTileScreensaver, mTileInput, mTileDisplay,
                mTileSound, mTileWifi, mTileBluetooth
        };

        for (View v : interactiveViews) {
            if (v == null) continue;
            v.setScaleX(1.0f);
            v.setScaleY(1.0f);
            v.setOnFocusChangeListener((view, hasFocus) -> {
                applyFocusInversionAndAntiClip(view, hasFocus);
            });
        }
    }

    /**
     * Guarantees 0% white-on-white text blending and 100% Zero-Scale focus.
     * All items stay static at scale 1.0f with strict border alignment.
     */
    private void applyFocusInversionAndAntiClip(View view, boolean hasFocus) {
        if (view == null) return;

        // ATURAN 1: Strict Zero-Scale Focus (100% scale = 1.0f, no zoom!)
        view.setScaleX(1.0f);
        view.setScaleY(1.0f);
        view.setTranslationZ(hasFocus ? 6f : 0f);

        // Contrast Inversion colors:
        // Focused: Obsidian Black text (#0F172A), Dark Slate subtext (#334155), Obsidian icon (#0F172A)
        // Idle: White text (#FFFFFF), Slate subtext (#94A3B8), Soft White icon (#F8FAFC)
        int textColor = hasFocus ? 0xFF18181B : 0xFFF4F4F5;
        int subColor = hasFocus ? 0xFF52525B : 0xFFA1A1AA;
        int iconColor = hasFocus ? 0xFF18181B : 0xFFF4F4F5;

        applyColorsRecursively(view, textColor, subColor, iconColor, hasFocus);
    }

    private void applyColorsRecursively(View view, int textColor, int subColor, int iconColor, boolean hasFocus) {
        if (view instanceof TextView) {
            TextView tv = (TextView) view;
            int id = tv.getId();
            if (id == R.id.sidebar_title_screensaver
                    || id == R.id.sidebar_title_input
                    || id == R.id.sidebar_title_display
                    || id == R.id.sidebar_title_sound
                    || id == R.id.sidebar_title_wifi
                    || id == R.id.sidebar_title_bluetooth) {
                tv.setTextColor(textColor);
            } else {
                tv.setTextColor(subColor);
            }
        } else if (view instanceof ImageView) {
            ImageView iv = (ImageView) view;
            iv.setImageTintList(ColorStateList.valueOf(iconColor));
        } else if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                applyColorsRecursively(vg.getChildAt(i), textColor, subColor, iconColor, hasFocus);
            }
        }
    }

    private void setupClickAndDpadActions() {
        if (mScrim != null) {
            mScrim.setOnClickListener(v -> closeSidebar());
        }

        // Header Actions
        if (mBtnSettings != null) {
            mBtnSettings.setOnClickListener(v -> {
                closeSidebarImmediately();
                TvUtils.openSettings(mContext);
            });
        }
        
        // Action Tiles
        if (mTileScreensaver != null) {
            mTileScreensaver.setOnClickListener(v -> {
                closeSidebarImmediately();
                TvUtils.startScreensaver(mContext);
            });
        }
        if (mTileInput != null) {
            mTileInput.setOnClickListener(v -> {
                closeSidebarImmediately();
                TvUtils.openVendorInputOverlay(mContext);
            });
        }
        if (mTileDisplay != null) {
            mTileDisplay.setOnClickListener(v -> {
                closeSidebarImmediately();
                TvUtils.openPictureSettings(mContext);
            });
        }
        if (mTileSound != null) {
            mTileSound.setOnClickListener(v -> {
                closeSidebarImmediately();
                TvUtils.openSoundSettings(mContext);
            });
        }
        if (mTileWifi != null) {
            mTileWifi.setOnClickListener(v -> {
                closeSidebarImmediately();
                TvUtils.openNetworkSettings(mContext);
            });
        }
        if (mTileBluetooth != null) {
            mTileBluetooth.setOnClickListener(v -> {
                closeSidebarImmediately();
                TvUtils.openBluetoothSettings(mContext);
            });
        }
    }

    private void setSystemVolumePercentage(int pct) {
        AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        if (am == null) return;
        try {
            int max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int target = Math.round((pct / 100f) * max);
            am.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0);
        } catch (Exception ignored) {}
    }

    private void updateVolumeSlider() {
        AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        if (am == null) return;
        try {
            int max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int current = am.getStreamVolume(AudioManager.STREAM_MUSIC);
            int pct = Math.round((current / (float) max) * 100f);
            if (mSliderVolume != null) {
                mSliderVolume.setProgress(pct);
            }
        } catch (Exception ignored) {}
    }

    private void updateTimeAndDate() {
        Locale locale = Locale.getDefault();
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, d MMM", locale);
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH.mm", locale);

        Date now = new Date();
        String dateStr = dateFormat.format(now);
        if (dateStr.length() > 0) {
            dateStr = Character.toUpperCase(dateStr.charAt(0)) + dateStr.substring(1);
        }
        if (mTextDate != null) mTextDate.setText(dateStr);
        if (mTextClock != null) mTextClock.setText(timeFormat.format(now));
    }

    private void updateNetworkStatus() {
        try {
            ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                Network network = cm.getActiveNetwork();
                NetworkCapabilities caps = cm.getNetworkCapabilities(network);
                if (caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                    WifiManager wm = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    if (wm != null) {
                        WifiInfo wifiInfo = wm.getConnectionInfo();
                        String ssid = wifiInfo.getSSID();
                        if (ssid != null && !ssid.isEmpty() && !ssid.equals("<unknown ssid>")) {
                            ssid = ssid.replace("\"", "");
                            if (mSubWifi != null) mSubWifi.setText(ssid);
                            return;
                        }
                    }
                    if (mSubWifi != null) mSubWifi.setText("Terhubung");
                    return;
                }
            }
            if (mSubWifi != null) mSubWifi.setText("Tidak terhubung");
        } catch (Exception e) {
            if (mSubWifi != null) mSubWifi.setText("Wi-Fi");
        }
    }

    private void registerVolumeReceiver() {
        try {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.media.VOLUME_CHANGED_ACTION");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                mContext.registerReceiver(mVolumeReceiver, filter, Context.RECEIVER_EXPORTED);
            } else {
                mContext.registerReceiver(mVolumeReceiver, filter);
            }
        } catch (Exception ignored) {}
    }

    public synchronized void toggleSidebar() {
        long now = System.currentTimeMillis();
        if (now - mLastToggleTime < 350) {
            return;
        }
        mLastToggleTime = now;
        if (mIsShowing) {
            closeSidebar();
        } else {
            showSidebar();
        }
    }

    public synchronized boolean isShowing() {
        return mIsShowing;
    }

    public synchronized void closeSidebarImmediately() {
        mIsShowing = false;
        mMainHandler.post(() -> {
            try {
                if (mRootView != null) {
                    mRootView.clearFocus();
                    mRootView.setVisibility(View.GONE);
                    if (mRootView.getParent() != null && mActiveWindowManager != null) {
                        mActiveWindowManager.removeViewImmediate(mRootView);
                    }
                }
                mIsAttached = false;
            } catch (Exception ignored) {}
        });
    }

    public synchronized void showSidebar() {
        if (mIsShowing || mRootView == null) return;
        mMainHandler.post(() -> {
            try {
                // Update live values
                updateTimeAndDate();
                updateNetworkStatus();
                updateVolumeSlider();

                int bright = mDimmerManager.getBrightness();
                if (mSliderBrightness != null) mSliderBrightness.setProgress(bright);

                int warm = mDimmerManager.getWarmth();
                if (mSliderWarmth != null) mSliderWarmth.setProgress(warm);

                int windowType;
                WindowManager targetWm = mWindowManager;
                if (mAccessibilityService != null) {
                    windowType = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
                    try {
                        WindowManager a11yWm = (WindowManager) mAccessibilityService.getSystemService(Context.WINDOW_SERVICE);
                        if (a11yWm != null) targetWm = a11yWm;
                    } catch (Exception ignored) {}
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    windowType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
                } else {
                    windowType = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
                }

                mActiveWindowManager = targetWm;

                WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT,
                        windowType,
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                        PixelFormat.TRANSLUCENT
                );
                params.gravity = Gravity.TOP | Gravity.START;

                if (mRootView.getParent() != null) {
                    try {
                        if (mActiveWindowManager != null) {
                            mActiveWindowManager.removeViewImmediate(mRootView);
                        }
                    } catch (Exception ignored) {}
                    mIsAttached = false;
                }

                if (!mIsAttached) {
                    mActiveWindowManager.addView(mRootView, params);
                    mIsAttached = true;
                }

                mRootView.setVisibility(View.VISIBLE);
                mIsShowing = true;

                // Animate entry: drawer slides from right, scrim fades in
                mDrawer.setTranslationX(450f);
                mDrawer.setAlpha(0f);
                mScrim.setAlpha(0f);

                mScrim.animate().alpha(1f).setDuration(220).start();
                mDrawer.animate()
                        .translationX(0f)
                        .alpha(1f)
                        .setDuration(260)
                        .setInterpolator(new DecelerateInterpolator())
                        .withEndAction(() -> {
                            if (mSliderBrightness != null) {
                                mSliderBrightness.requestFocus();
                            }
                        })
                        .start();

                Log.d(TAG, "Global Quick Settings sidebar displayed successfully!");
            } catch (Exception e) {
                Log.e(TAG, "Failed showing sidebar overlay", e);
            }
        });
    }

    public synchronized void closeSidebar() {
        if (!mIsShowing || mRootView == null) return;
        mIsShowing = false;
        mMainHandler.post(() -> {
            try {
                if (mRootView != null) {
                    mRootView.clearFocus();
                }
                mScrim.animate().alpha(0f).setDuration(180).start();
                mDrawer.animate()
                        .translationX(450f)
                        .alpha(0f)
                        .setDuration(200)
                        .setInterpolator(new AccelerateInterpolator())
                        .withEndAction(() -> {
                            try {
                                if (mRootView != null) {
                                    mRootView.setVisibility(View.GONE);
                                    if (mRootView.getParent() != null && mActiveWindowManager != null) {
                                        mActiveWindowManager.removeView(mRootView);
                                    }
                                }
                                mIsAttached = false;
                            } catch (Exception ignored) {}
                        })
                        .start();
                Log.d(TAG, "Global Quick Settings sidebar closed");
            } catch (Exception e) {
                Log.e(TAG, "Error closing sidebar overlay", e);
            }
        });
    }
}
