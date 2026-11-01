package com.zenith.launcher;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.media.tv.TvInputManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.zenith.launcher.util.TvUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 1:1 Pixel-Perfect & Functional Clone of Native Google TV Quick Settings Drawer.
 * Slides in from the right edge with native decelerate curve, dark translucent card,
 * 2-column tiles, D-pad focus highlight, and functional system sub-menus.
 */
public class QuickSettingsActivity extends AppCompatActivity {

    private FrameLayout sheetContainer;
    private View scrim;
    private LinearLayout mainPage;

    // Sub-panels
    private LinearLayout subDisplayPage;
    private LinearLayout subInputPage;
    private LinearLayout subWifiPage;

    // Header views
    private TextView textDate;
    private TextView textClock;
    private FrameLayout btnSettings;
        private TextView labelSettings;

    // Tiles
    private LinearLayout tileScreensaver;
    private LinearLayout tileInput;
    private LinearLayout tileDisplay;
    private LinearLayout tileSound;
    private LinearLayout tileWifi;
    private LinearLayout tileBluetooth;
    private LinearLayout tileAccessibility;

    // Dynamic tile texts
    private TextView subInputText;
    private TextView subWifiText;
    private TextView textConnectedSsid;

    // Display sub-panel controls
    private LinearLayout rowBacklight;
    private TextView valBacklight;
    private SeekBar seekBacklight;

    private LinearLayout rowBrightness;
    private TextView valBrightness;
    private SeekBar seekBrightness;

    private LinearLayout rowContrast;
    private TextView valContrast;
    private SeekBar seekContrast;

    private final Handler timeHandler = new Handler(Looper.getMainLooper());
    private final Runnable timeRunnable = new Runnable() {
        @Override
        public void run() {
            updateTimeAndDate();
            timeHandler.postDelayed(this, 10000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quick_settings);

        initViews();
        setupFocusEffects();
        setupClickListeners();
        updateTimeAndDate();
        updateNetworkStatus();

        animateOpen();
    }

    private void initViews() {
        sheetContainer = findViewById(R.id.qs_sheet_container);
        scrim = findViewById(R.id.qs_scrim);
        mainPage = findViewById(R.id.qs_main_page);

        subDisplayPage = findViewById(R.id.qs_sub_display);
        subInputPage = findViewById(R.id.qs_sub_input_page);
        subWifiPage = findViewById(R.id.qs_sub_wifi_page);

        textDate = findViewById(R.id.qs_text_date);
        textClock = findViewById(R.id.qs_text_clock);
        btnSettings = findViewById(R.id.qs_btn_settings);
                labelSettings = findViewById(R.id.qs_label_settings);

        tileScreensaver = findViewById(R.id.qs_tile_screensaver);
        tileInput = findViewById(R.id.qs_tile_input);
        tileDisplay = findViewById(R.id.qs_tile_display);
        tileSound = findViewById(R.id.qs_tile_sound);
        tileWifi = findViewById(R.id.qs_tile_wifi);
        tileBluetooth = findViewById(R.id.qs_tile_bluetooth);
        tileAccessibility = findViewById(R.id.qs_tile_accessibility);

        subInputText = findViewById(R.id.qs_sub_input);
        subWifiText = findViewById(R.id.qs_sub_wifi);
        textConnectedSsid = findViewById(R.id.qs_text_connected_ssid);

        // Display controls
        rowBacklight = findViewById(R.id.qs_row_backlight);
        valBacklight = findViewById(R.id.qs_val_backlight);
        seekBacklight = findViewById(R.id.qs_seek_backlight);

        rowBrightness = findViewById(R.id.qs_row_brightness);
        valBrightness = findViewById(R.id.qs_val_brightness);
        seekBrightness = findViewById(R.id.qs_seek_brightness);

        rowContrast = findViewById(R.id.qs_row_contrast);
        valContrast = findViewById(R.id.qs_val_contrast);
        seekContrast = findViewById(R.id.qs_seek_contrast);

        // Initialize display brightness from system
        try {
            int curBright = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 102);
            int pct = (int) Math.round((curBright / 255.0) * 100);
            seekBrightness.setProgress(pct);
            valBrightness.setText(String.valueOf(pct));
            seekBacklight.setProgress(pct);
            valBacklight.setText(String.valueOf(pct));
        } catch (Exception ignored) {}
    }

    private void setupFocusEffects() {
        View[] interactiveViews = new View[]{
            btnSettings,
            tileScreensaver, tileInput, tileDisplay,
            tileSound, tileWifi, tileBluetooth, tileAccessibility
        };

        for (View v : interactiveViews) {
            if (v == null) continue;
            v.setOnFocusChangeListener((view, hasFocus) -> {
                if (hasFocus) {
                    view.animate().scaleX(1.04f).scaleY(1.04f).setDuration(160).start();
                } else {
                    view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(160).start();
                }

                if (view == btnSettings) {
                    labelSettings.setVisibility(hasFocus ? View.VISIBLE : View.INVISIBLE);
                }
            });
        }
    }

    private void setupClickListeners() {
        scrim.setOnClickListener(v -> animateClose());

        // Header buttons
        btnSettings.setOnClickListener(v -> {
            TvUtils.openSettings(this);
            finish();
        });

        
        // Tile 1: Screensaver
        tileScreensaver.setOnClickListener(v -> {
            TvUtils.startScreensaver(this);
            finish();
        });

        // Tile 2: Input
        tileInput.setOnClickListener(v -> showSubPanel(subInputPage));

        // Tile 3: Tampilan (Display)
        tileDisplay.setOnClickListener(v -> showSubPanel(subDisplayPage));

        // Tile 4: Suara (Sound)
        tileSound.setOnClickListener(v -> {
            TvUtils.openSoundSettings(this);
            finish();
        });

        // Tile 5: Wi-Fi
        tileWifi.setOnClickListener(v -> showSubPanel(subWifiPage));

        // Tile 6: Bluetooth
        tileBluetooth.setOnClickListener(v -> {
            TvUtils.openBluetoothSettings(this);
            finish();
        });

        // Tile 7: Aksesibilitas
        tileAccessibility.setOnClickListener(v -> {
            TvUtils.openAccessibilitySettings(this);
            finish();
        });

        // Display Sub-panel actions
        findViewById(R.id.qs_sub_display_back).setOnClickListener(v -> returnToMainPage(tileDisplay));
        findViewById(R.id.qs_btn_system_picture).setOnClickListener(v -> {
            TvUtils.openPictureSettings(this);
            finish();
        });

        // Input Sub-panel actions
        findViewById(R.id.qs_sub_input_back).setOnClickListener(v -> returnToMainPage(tileInput));
        findViewById(R.id.qs_input_hdmi1).setOnClickListener(v -> {
            TvUtils.switchToHdmi1(this);
            finish();
        });
        findViewById(R.id.qs_input_hdmi2).setOnClickListener(v -> {
            TvUtils.switchToHdmi2(this);
            finish();
        });
        findViewById(R.id.qs_input_av).setOnClickListener(v -> {
            TvUtils.switchToAv(this);
            finish();
        });
        findViewById(R.id.qs_input_antena).setOnClickListener(v -> {
            TvUtils.openLiveTv(this);
            finish();
        });
        findViewById(R.id.qs_input_native_vendor).setOnClickListener(v -> {
            TvUtils.openVendorInputOverlay(this);
            finish();
        });

        // Wi-Fi Sub-panel actions
        findViewById(R.id.qs_sub_wifi_back).setOnClickListener(v -> returnToMainPage(tileWifi));
        findViewById(R.id.qs_btn_full_network_settings).setOnClickListener(v -> {
            TvUtils.openNetworkSettings(this);
            finish();
        });
    }

    private void showSubPanel(View subPanel) {
        if (subPanel == null) return;
        mainPage.setVisibility(View.GONE);
        subPanel.setVisibility(View.VISIBLE);
        subPanel.setAlpha(0f);
        subPanel.setTranslationX(40f);
        subPanel.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(200)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        // Focus first interactive element in sub-panel
        View backBtn = subPanel.findViewById(R.id.qs_sub_display_back);
        if (backBtn == null) backBtn = subPanel.findViewById(R.id.qs_sub_input_back);
        if (backBtn == null) backBtn = subPanel.findViewById(R.id.qs_sub_wifi_back);
        if (backBtn != null) {
            backBtn.requestFocus();
        }
    }

    private void returnToMainPage(View targetFocusTile) {
        if (subDisplayPage != null) subDisplayPage.setVisibility(View.GONE);
        if (subInputPage != null) subInputPage.setVisibility(View.GONE);
        if (subWifiPage != null) subWifiPage.setVisibility(View.GONE);

        mainPage.setVisibility(View.VISIBLE);
        mainPage.setAlpha(0f);
        mainPage.setTranslationX(-30f);
        mainPage.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(200)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        if (targetFocusTile != null) {
            targetFocusTile.requestFocus();
        } else {
            tileScreensaver.requestFocus();
        }
    }

    private void animateOpen() {
        sheetContainer.setTranslationX(400f);
        sheetContainer.setAlpha(0f);
        scrim.setAlpha(0f);

        scrim.animate().alpha(1f).setDuration(250).start();
        sheetContainer.animate()
                .translationX(0f)
                .alpha(1f)
                .setDuration(280)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> {
                    if (tileScreensaver != null) {
                        tileScreensaver.requestFocus();
                    }
                })
                .start();
    }

    private void animateClose() {
        scrim.animate().alpha(0f).setDuration(180).start();
        sheetContainer.animate()
                .translationX(400f)
                .alpha(0f)
                .setDuration(200)
                .withEndAction(this::finish)
                .start();
    }

    private void updateTimeAndDate() {
        Locale locale = Locale.getDefault();
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, d MMM", locale);
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH.mm", locale);

        Date now = new Date();
        String dateStr = dateFormat.format(now);
        // Capitalize first letter (e.g. Sab, 31 Okt)
        if (dateStr.length() > 0) {
            dateStr = Character.toUpperCase(dateStr.charAt(0)) + dateStr.substring(1);
        }
        textDate.setText(dateStr);
        textClock.setText(timeFormat.format(now));
    }

    private void updateNetworkStatus() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                Network network = cm.getActiveNetwork();
                NetworkCapabilities caps = cm.getNetworkCapabilities(network);
                if (caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                    WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    if (wm != null) {
                        WifiInfo wifiInfo = wm.getConnectionInfo();
                        String ssid = wifiInfo.getSSID();
                        if (ssid != null && !ssid.isEmpty() && !ssid.equals("<unknown ssid>")) {
                            ssid = ssid.replace("\"", "");
                            subWifiText.setText(ssid);
                            textConnectedSsid.setText(ssid);
                            return;
                        }
                    }
                    subWifiText.setText("Terhubung");
                    textConnectedSsid.setText("Wi-Fi Aktif");
                    return;
                }
            }
            subWifiText.setText("Tidak terhubung");
            textConnectedSsid.setText("Tidak terhubung");
        } catch (Exception e) {
            subWifiText.setText("Wi-Fi");
            textConnectedSsid.setText("Wi-Fi");
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mainPage.getVisibility() != View.VISIBLE) {
                returnToMainPage(tileScreensaver);
                return true;
            }
            animateClose();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_SETTINGS || keyCode == 176) {
            animateClose();
            return true;
        }

        // DPAD adjustment on seekbars when focused
        View focused = getCurrentFocus();
        if (focused == rowBacklight || focused == rowBrightness || focused == rowContrast) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                adjustFocusedSeekbar(focused, keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ? 5 : -5);
                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    private void adjustFocusedSeekbar(View focusedRow, int delta) {
        SeekBar targetSeek = null;
        TextView targetVal = null;
        String settingKey = null;

        if (focusedRow == rowBacklight) {
            targetSeek = seekBacklight;
            targetVal = valBacklight;
        } else if (focusedRow == rowBrightness) {
            targetSeek = seekBrightness;
            targetVal = valBrightness;
            settingKey = Settings.System.SCREEN_BRIGHTNESS;
        } else if (focusedRow == rowContrast) {
            targetSeek = seekContrast;
            targetVal = valContrast;
        }

        if (targetSeek != null) {
            int newProg = Math.max(0, Math.min(100, targetSeek.getProgress() + delta));
            targetSeek.setProgress(newProg);
            targetVal.setText(String.valueOf(newProg));

            if (settingKey != null) {
                try {
                    int sysVal = (int) Math.round((newProg / 100.0) * 255);
                    Settings.System.putInt(getContentResolver(), settingKey, sysVal);
                } catch (Exception ignored) {}
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        timeHandler.post(timeRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        timeHandler.removeCallbacks(timeRunnable);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }
}

