package com.zenith.launcher.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

/**
 * Dual-Action Dimmer & Warmth Overlay Manager.
 * Renders full-screen black dimmer and amber warmth filters as system overlays
 * so TV screen visibly dims and tints in real-time across ALL applications.
 */
public class DimmerOverlayManager {

    private static final String TAG = "DimmerOverlay";
    private static final String PREF_NAME = "zenith_display_prefs";
    private static final String KEY_BRIGHTNESS = "pref_brightness_pct";
    private static final String KEY_WARMTH = "pref_warmth_pct";

    private static DimmerOverlayManager sInstance;
    private final Context mContext;
    private final WindowManager mWindowManager;
    private final SharedPreferences mPrefs;

    private View mDimmerView;
    private View mWarmthView;
    private boolean mDimmerAttached = false;
    private boolean mWarmthAttached = false;

    private int mCurrentBrightness = 80;
    private int mCurrentWarmth = 0;

    public static synchronized DimmerOverlayManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new DimmerOverlayManager(context.getApplicationContext());
        }
        return sInstance;
    }

    private DimmerOverlayManager(Context context) {
        mContext = context;
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        mCurrentBrightness = mPrefs.getInt(KEY_BRIGHTNESS, 80);
        mCurrentWarmth = mPrefs.getInt(KEY_WARMTH, 0);

        initOverlays();
    }

    private void initOverlays() {
        if (mWindowManager == null) return;

        try {
            int windowType;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                windowType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                windowType = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
            }

            // 1. Black Dimmer View
            if (mDimmerView == null) {
                mDimmerView = new View(mContext);
                mDimmerView.setBackgroundColor(0xFF000000);
                float dimmerAlpha = calculateDimmerAlpha(mCurrentBrightness);
                mDimmerView.setAlpha(dimmerAlpha);

                WindowManager.LayoutParams paramsDimmer = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT,
                        windowType,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                        PixelFormat.TRANSLUCENT
                );
                paramsDimmer.gravity = Gravity.TOP | Gravity.START;

                try {
                    mWindowManager.addView(mDimmerView, paramsDimmer);
                    mDimmerAttached = true;
                    Log.d(TAG, "Dimmer overlay view attached successfully with alpha: " + dimmerAlpha);
                } catch (Exception e) {
                    Log.w(TAG, "Cannot attach dimmer overlay directly: " + e.getMessage());
                }
            }

            // 2. Amber Warmth View
            if (mWarmthView == null) {
                mWarmthView = new View(mContext);
                mWarmthView.setBackgroundColor(0xFFFF8C00); // Amber tint
                float warmthAlpha = calculateWarmthAlpha(mCurrentWarmth);
                mWarmthView.setAlpha(warmthAlpha);

                WindowManager.LayoutParams paramsWarmth = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT,
                        windowType,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                        PixelFormat.TRANSLUCENT
                );
                paramsWarmth.gravity = Gravity.TOP | Gravity.START;

                try {
                    mWindowManager.addView(mWarmthView, paramsWarmth);
                    mWarmthAttached = true;
                    Log.d(TAG, "Warmth overlay view attached successfully with alpha: " + warmthAlpha);
                } catch (Exception e) {
                    Log.w(TAG, "Cannot attach warmth overlay directly: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed initializing dimmer overlays", e);
        }
    }

    public void attachWithAccessibilityService(android.accessibilityservice.AccessibilityService service) {
        if (service == null) return;
        try {
            WindowManager wm = (WindowManager) service.getSystemService(Context.WINDOW_SERVICE);
            if (wm == null) return;

            // Ensure dimmer overlay attached via Accessibility overlay
            if (!mDimmerAttached) {
                if (mDimmerView == null) {
                    mDimmerView = new View(service);
                    mDimmerView.setBackgroundColor(0xFF000000);
                }
                mDimmerView.setAlpha(calculateDimmerAlpha(mCurrentBrightness));

                WindowManager.LayoutParams paramsDimmer = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                        PixelFormat.TRANSLUCENT
                );
                paramsDimmer.gravity = Gravity.TOP | Gravity.START;
                try {
                    wm.addView(mDimmerView, paramsDimmer);
                    mDimmerAttached = true;
                } catch (Exception ignored) {}
            }

            // Ensure warmth overlay attached via Accessibility overlay
            if (!mWarmthAttached) {
                if (mWarmthView == null) {
                    mWarmthView = new View(service);
                    mWarmthView.setBackgroundColor(0xFFFF8C00);
                }
                mWarmthView.setAlpha(calculateWarmthAlpha(mCurrentWarmth));

                WindowManager.LayoutParams paramsWarmth = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                        PixelFormat.TRANSLUCENT
                );
                paramsWarmth.gravity = Gravity.TOP | Gravity.START;
                try {
                    wm.addView(mWarmthView, paramsWarmth);
                    mWarmthAttached = true;
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in attachWithAccessibilityService", e);
        }
    }

    private float calculateDimmerAlpha(int brightnessPct) {
        // Brightness 100% -> alpha 0.0f (no dimming)
        // Brightness 0% -> alpha 0.75f (deep dark dimming)
        float clamped = Math.max(0, Math.min(100, brightnessPct));
        return 0.75f * (1.0f - (clamped / 100.0f));
    }

    private float calculateWarmthAlpha(int warmthPct) {
        // Warmth 0% -> alpha 0.0f (clean white)
        // Warmth 100% -> alpha 0.35f (comfort night amber)
        float clamped = Math.max(0, Math.min(100, warmthPct));
        return 0.35f * (clamped / 100.0f);
    }

    public int getBrightness() {
        return mCurrentBrightness;
    }

    public void setBrightness(int pct) {
        mCurrentBrightness = Math.max(0, Math.min(100, pct));
        mPrefs.edit().putInt(KEY_BRIGHTNESS, mCurrentBrightness).apply();

        float alpha = calculateDimmerAlpha(mCurrentBrightness);
        if (mDimmerView != null) {
            mDimmerView.setAlpha(alpha);
        }

        // Layer 1: Also write to Settings.System.SCREEN_BRIGHTNESS
        try {
            int sysVal = Math.round((mCurrentBrightness / 100.0f) * 255.0f);
            Settings.System.putInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, sysVal);
        } catch (Exception ignored) {}
    }

    public int getWarmth() {
        return mCurrentWarmth;
    }

    public void setWarmth(int pct) {
        mCurrentWarmth = Math.max(0, Math.min(100, pct));
        mPrefs.edit().putInt(KEY_WARMTH, mCurrentWarmth).apply();

        float alpha = calculateWarmthAlpha(mCurrentWarmth);
        if (mWarmthView != null) {
            mWarmthView.setAlpha(alpha);
        }
    }
}

