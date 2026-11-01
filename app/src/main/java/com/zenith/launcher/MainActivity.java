/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.app.Dialog
 *  android.content.BroadcastReceiver
 *  android.content.Context
 *  android.content.Intent
 *  android.content.IntentFilter
 *  android.content.pm.LauncherApps
 *  android.content.pm.LauncherApps$Callback
 *  android.content.pm.PackageInfo
 *  android.graphics.Color
 *  android.graphics.Outline
 *  android.graphics.drawable.ColorDrawable
 *  android.graphics.drawable.Drawable
 *  android.net.Uri
 *  android.os.Build$VERSION
 *  android.os.Bundle
 *  android.os.Handler
 *  android.os.Looper
 *  android.os.SystemClock
 *  android.os.UserHandle
 *  android.util.DisplayMetrics
 *  android.util.Log
 *  android.util.TypedValue
 *  android.view.KeyEvent
 *  android.view.View
 *  android.view.ViewGroup
 *  android.view.ViewGroup$LayoutParams
 *  android.view.ViewGroup$MarginLayoutParams
 *  android.view.ViewOutlineProvider
 *  android.view.ViewParent
 *  android.view.ViewTreeObserver$OnGlobalLayoutListener
 *  android.widget.FrameLayout
 *  android.widget.ImageView
 *  android.widget.TextView
 *  android.widget.Toast
 *  androidx.appcompat.app.AppCompatActivity
 *  androidx.fragment.app.FragmentActivity
 *  androidx.recyclerview.widget.LinearLayoutManager
 *  androidx.recyclerview.widget.RecyclerView$LayoutManager
 *  androidx.recyclerview.widget.RecyclerView$ViewHolder
 *  com.bumptech.glide.Glide
 *  com.bumptech.glide.RequestBuilder
 *  com.bumptech.glide.TransitionOptions
 *  com.bumptech.glide.load.engine.DiskCacheStrategy
 *  com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
 *  com.zenith.launcher.R$drawable
 *  com.zenith.launcher.R$id
 *  com.zenith.launcher.R$layout
 */
package com.zenith.launcher;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.LauncherApps;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.TransitionOptions;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.zenith.launcher.R;
import com.zenith.launcher.adapter.AppAdapter;
import com.zenith.launcher.adapter.ChannelAdapter;
import com.zenith.launcher.model.AppActionCapabilities;
import com.zenith.launcher.model.AppItem;
import com.zenith.launcher.model.ChannelItem;
import com.zenith.launcher.model.MovieItem;
import com.zenith.launcher.util.MovieCatalog;
import com.zenith.launcher.util.TvUtils;
import com.zenith.launcher.view.TvHorizontalGridView;
import com.zenith.launcher.view.TvNestedScrollView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity
extends AppCompatActivity
implements AppAdapter.OnAppFocusListener,
AppAdapter.OnAppLongClickListener,
ChannelAdapter.OnChannelFocusListener {
    private ImageView backdropImage1;
    private ImageView backdropImage2;
    private TextView heroTitle;
    private TextView heroGenres;
    private TextView badgeRating;
    private TextView badgeQuality;
    private TextView badgeAudio;
    private TextView heroDuration;
    private TextView heroTagline;
    private View btnWatchNow;
    private View btnNextMovie;
    private TextView clockTime;
    private TextView clockDate;
    private long lastSettingsPressTime = 0L;
    private View navBtnHome;
    private ImageView navIconHome;
    private View navLineHome;
    private View navBtnSearch;
    private ImageView navIconSearch;
    private View navLineSearch;
    private View navBtnSettings;
    private ImageView navIconSettings;
    private View navLineSettings;
    private TextView sidebarTooltip;
    private View topBar;
    private View heroContainer;
    private TvNestedScrollView mainScrollView;
    private TvHorizontalGridView recyclerFavorites;
    private TvHorizontalGridView recyclerRecommended;
    private TvHorizontalGridView recyclerAllApps;
    private final List<AppItem> favoriteApps = new ArrayList<AppItem>();
    private final List<AppItem> remainingApps = new ArrayList<AppItem>();
    private AppAdapter favAdapter;
    private AppAdapter allAdapter;
    private LauncherApps launcherApps;
    private LauncherApps.Callback launcherAppsCallback;
    private BroadcastReceiver packageReceiver;
    private final ExecutorService appSyncExecutor = Executors.newSingleThreadExecutor();
    private volatile boolean isReconciling = false;
    private static MainActivity sInstance;
    public static MainActivity getInstance() { return sInstance; }
    private volatile boolean isActivityResumed = false;
    private boolean isInitialLaunch = true;
    public static boolean isLauncherResumed() { return sInstance != null && sInstance.isActivityResumed; }
    private static long lastReorderEndTime;
    private boolean isReorderMode = false;
    private boolean isReorderFavorites = true;
    private int reorderingAppIndex = -1;
    private AppItem reorderingApp = null;
    private View reorderHud;
    private TextView reorderHudText;
    private long reorderStartTime = 0L;
    private static long lastActionDismissTime;
    private final Random random = new Random();
    private static final long AUTO_SHUFFLE_INTERVAL_MS = 300000L;
    private final Runnable autoShuffleRunnable = new Runnable(){

        @Override
        public void run() {
            if (!(MainActivity.this.isReorderMode || MainActivity.this.isFinishing() || MainActivity.this.isDestroyed())) {
                MainActivity.this.shuffleRecommendationMovie(true);
            }
            MainActivity.this.handler.postDelayed((Runnable)this, 300000L);
        }
    };
    private View anchorFavorit;
    private View labelRecommended;
    private View anchorAllApps;
    private View labelGlanceHub;
    private View glanceHubContainer;
    private View cardWeather;
    private View cardTvInputs;
    private View containerFavoritesDock;
    private View containerAllAppsDock;
    public static final int SECTION_HOME = 0;
    public static final int SECTION_MOVIES = 1;
    public static final int SECTION_DASHBOARD = 2;
    public static final int SECTION_APPS = 3;
    public static final int SECTION_SETTINGS = 4;
    private int activeSection = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isBackdrop1Visible = true;
    private int currentMovieIndex = 0;
    private MovieItem currentMovie;
    private static final long SCREENSAVER_TIMEOUT_MS = 300000L;
    private final Runnable screensaverRunnable = () -> TvUtils.startScreensaver((Context)this);
    private final Runnable hideTooltipRunnable = () -> {
        if (this.sidebarTooltip != null) {
            this.sidebarTooltip.animate().alpha(0.0f).setDuration(200L).start();
        }
    };
    private final Runnable timeRunnable = new Runnable(){

        @Override
        public void run() {
            MainActivity.this.updateClock();
            MainActivity.this.handler.postDelayed((Runnable)this, 30000L);
        }
    };

    public static boolean isGlobalReorderActive() {
        return sInstance != null && MainActivity.sInstance.isReorderMode;
    }

    public static boolean wasRecentlyReordering() {
        return SystemClock.uptimeMillis() - lastReorderEndTime < 600L;
    }

    public static boolean wasDialogRecentlyDismissed() {
        return SystemClock.uptimeMillis() - lastActionDismissTime < 500L;
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sInstance = this;
        com.zenith.launcher.receiver.BootReceiver.ensureAccessibilityServiceEnabled(this);
        this.hideSystemUI();
        this.setContentView(R.layout.activity_main);
        this.bindViews();
        this.setupClock();
        this.setupSidebar();
        this.setupHeroActions();
        this.loadCatalogAndApps();
        this.setupDynamicSpacing();
        this.setupShelfAutoSnap();
        this.setupScreensaverWatchdog();
        this.restartAutoShuffleTimer();
        this.setupPackageSyncListeners();
        this.findViewById(R.id.root_view).post(() -> {
            long now;
            this.focusRow1();
            if (this.getIntent() != null && "com.zenith.launcher.ACTION_OPEN_SETTINGS_DRAWER".equals(this.getIntent().getAction()) && (now = System.currentTimeMillis()) - this.lastSettingsPressTime > 350L) {
                this.lastSettingsPressTime = now;
                TvUtils.openQuickSettings((Context)this);
            }
        });
    }

    private void hideSystemUI() {
        View decorView = this.getWindow().getDecorView();
        decorView.setSystemUiVisibility(5382);
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            this.hideSystemUI();
            if (this.isInitialLaunch) {
                this.focusRow1();
            } else {
                this.handler.postDelayed(this::ensureFocus, 100L);
            }
        }
    }

    private void bindViews() {
        this.backdropImage1 = (ImageView)this.findViewById(R.id.backdrop_image_1);
        this.backdropImage2 = (ImageView)this.findViewById(R.id.backdrop_image_2);
        this.heroTitle = (TextView)this.findViewById(R.id.hero_title);
        this.heroGenres = (TextView)this.findViewById(R.id.hero_genres);
        this.badgeRating = (TextView)this.findViewById(R.id.badge_rating);
        this.badgeQuality = (TextView)this.findViewById(R.id.badge_quality);
        this.badgeAudio = (TextView)this.findViewById(R.id.badge_audio);
        this.heroDuration = (TextView)this.findViewById(R.id.hero_duration);
        this.heroTagline = (TextView)this.findViewById(R.id.hero_tagline);
        this.btnWatchNow = this.findViewById(R.id.btn_watch_now);
        this.btnNextMovie = this.findViewById(R.id.btn_next_movie);
        this.clockTime = (TextView)this.findViewById(R.id.clock_time);
        this.clockDate = (TextView)this.findViewById(R.id.clock_date);
        this.navBtnHome = this.findViewById(R.id.nav_btn_home);
        this.navIconHome = (ImageView)this.findViewById(R.id.nav_icon_home);
        this.navLineHome = this.findViewById(R.id.nav_line_home);
        this.navBtnSearch = this.findViewById(R.id.nav_btn_search);
        this.navIconSearch = (ImageView)this.findViewById(R.id.nav_icon_search);
        this.navLineSearch = this.findViewById(R.id.nav_line_search);
        this.navBtnSettings = this.findViewById(R.id.nav_btn_settings);
        this.navIconSettings = (ImageView)this.findViewById(R.id.nav_icon_settings);
        this.navLineSettings = this.findViewById(R.id.nav_line_settings);
        this.sidebarTooltip = (TextView)this.findViewById(R.id.sidebar_tooltip);
        this.topBar = this.findViewById(R.id.top_bar);
        this.heroContainer = this.findViewById(R.id.hero_container);
        this.reorderHud = this.findViewById(R.id.reorder_hud);
        this.reorderHudText = (TextView)this.findViewById(R.id.reorder_hud_text);
        this.mainScrollView = (TvNestedScrollView)this.findViewById(R.id.main_scroll_view);
        this.recyclerFavorites = (TvHorizontalGridView)this.findViewById(R.id.recycler_favorites);
        this.recyclerRecommended = (TvHorizontalGridView)this.findViewById(R.id.recycler_recommended);
        this.recyclerAllApps = (TvHorizontalGridView)this.findViewById(R.id.recycler_all_apps);
        this.anchorFavorit = this.findViewById(R.id.anchor_favorit);
        this.labelRecommended = this.findViewById(R.id.label_recommended);
        this.anchorAllApps = this.findViewById(R.id.anchor_all_apps);
        this.labelGlanceHub = this.findViewById(R.id.label_glance_hub);
        this.glanceHubContainer = this.findViewById(R.id.glance_hub_container);
        this.cardWeather = this.findViewById(R.id.card_weather);
        this.cardTvInputs = this.findViewById(R.id.card_tv_inputs);
        this.containerFavoritesDock = this.findViewById(R.id.container_favorites_dock);
        this.containerAllAppsDock = this.findViewById(R.id.container_all_apps_dock);
        this.ensureDockClipping(this.containerFavoritesDock);
        this.ensureDockClipping(this.containerAllAppsDock);
        this.setupGlanceCardFocus(this.cardWeather, () -> Toast.makeText((Context)this, (CharSequence)"Cuaca Real-Time: 28\u00b0C Cerah Berawan di Jakarta", (int)0).show());
        this.setupGlanceCardFocus(this.cardTvInputs, this::showTvInputDialog);

    }

    private void setupGlanceCardFocus(View card, Runnable onClick) {
        if (card == null) {
            return;
        }
        card.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                v.animate().scaleX(1.04f).scaleY(1.04f).setDuration(200L).start();
            } else {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(180L).start();
            }
        });
        card.setOnClickListener(v -> {
            if (onClick != null) {
                onClick.run();
            }
        });
    }

    private void setupClock() {
        this.updateClock();
        this.syncInstalledApps(false);
        this.handler.post(this.timeRunnable);
    }

    private void updateClock() {
        Date now = new Date();
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.US);
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, d MMMM", new Locale("id", "ID"));
        if (this.clockTime != null) {
            this.clockTime.setText((CharSequence)timeFormat.format(now));
        }
        if (this.clockDate != null) {
            this.clockDate.setText((CharSequence)dateFormat.format(now));
        }
    }

    private void displayMovie(MovieItem movie, boolean animate) {
        int fallback;
        if (movie == null) {
            return;
        }
        this.currentMovie = movie;
        this.currentMovieIndex = MovieCatalog.getIndexOf(movie);
        this.heroTitle.setText((CharSequence)movie.getTitle());
        if (this.heroGenres != null) {
            this.heroGenres.setText((CharSequence)movie.getGenres());
        }
        if (this.badgeRating != null) {
            this.badgeRating.setText((CharSequence)(movie.getRating() + " IMDb"));
        }
        if (this.badgeQuality != null) {
            this.badgeQuality.setVisibility(8);
        }
        if (this.badgeAudio != null) {
            this.badgeAudio.setVisibility(8);
        }
        if (this.heroDuration != null) {
            this.heroDuration.setText((CharSequence)movie.getDuration());
        }
        this.heroTagline.setText((CharSequence)movie.getDescription());
        if (animate) {
            this.heroTitle.setAlpha(0.0f);
            this.heroTitle.setTranslationY(20.0f);
            this.heroTitle.animate().alpha(1.0f).translationY(0.0f).setDuration(350L).start();
            this.heroTagline.setAlpha(0.0f);
            this.heroTagline.setTranslationY(15.0f);
            this.heroTagline.animate().alpha(1.0f).translationY(0.0f).setDuration(400L).start();
        }
        ImageView incoming = this.isBackdrop1Visible ? this.backdropImage2 : this.backdropImage1;
        ImageView outgoing = this.isBackdrop1Visible ? this.backdropImage1 : this.backdropImage2;
        int n = fallback = movie.getFallbackResId() != 0 ? movie.getFallbackResId() : R.drawable.bg_wallpaper_joker;
        if (movie.getBackdropUrl() != null && !movie.getBackdropUrl().isEmpty()) {
            ((RequestBuilder)((RequestBuilder)((RequestBuilder)((RequestBuilder)Glide.with((FragmentActivity)this).load(movie.getBackdropUrl()).placeholder(fallback)).error(fallback)).diskCacheStrategy(DiskCacheStrategy.ALL)).transition((TransitionOptions)DrawableTransitionOptions.withCrossFade((int)400)).centerCrop()).into(incoming);
        } else {
            incoming.setImageResource(fallback);
        }
        if (animate) {
            incoming.setScaleX(1.04f);
            incoming.setScaleY(1.04f);
            incoming.animate().alpha(1.0f).scaleX(1.0f).scaleY(1.0f).setDuration(550L).start();
            outgoing.animate().alpha(0.0f).setDuration(550L).start();
            this.isBackdrop1Visible = !this.isBackdrop1Visible;
        } else {
            incoming.setAlpha(1.0f);
            outgoing.setAlpha(0.0f);
            this.isBackdrop1Visible = !this.isBackdrop1Visible;
        }
    }

    public void focusRow1() {
        if (this.mainScrollView != null) {
            this.mainScrollView.scrollTo(0, 0);
        }
        this.activeSection = 0;
        this.updateSidebarActiveState();
        if (this.recyclerFavorites != null) {
            this.recyclerFavorites.scrollToPosition(0);
            if (this.tryFocusFavoriteFirstItem()) {
                return;
            }
            this.recyclerFavorites.post(() -> {
                if (this.tryFocusFavoriteFirstItem()) return;
                this.recyclerFavorites.postDelayed(() -> {
                    if (this.tryFocusFavoriteFirstItem()) return;
                    this.recyclerFavorites.postDelayed(this::tryFocusFavoriteFirstItem, 150L);
                }, 80L);
            });
        }
    }

    private boolean tryFocusFavoriteFirstItem() {
        if (!this.isActivityResumed || this.isFinishing() || this.isDestroyed() || this.recyclerFavorites == null) {
            return false;
        }
        if (this.recyclerFavorites.getChildCount() > 0) {
            View child = this.recyclerFavorites.getChildAt(0);
            if (child != null) {
                View card = child.findViewById(R.id.card_root);
                if (card != null && card.requestFocus()) {
                    if (this.mainScrollView != null && this.mainScrollView.getScrollY() != 0) {
                        this.mainScrollView.scrollTo(0, 0);
                    }
                    this.isInitialLaunch = false;
                    return true;
                }
                if (child.requestFocus()) {
                    if (this.mainScrollView != null && this.mainScrollView.getScrollY() != 0) {
                        this.mainScrollView.scrollTo(0, 0);
                    }
                    this.isInitialLaunch = false;
                    return true;
                }
            }
        }
        return false;
    }

    private void setupSidebar() {
        this.bindSidebarItem(this.navBtnHome, this.navIconHome, this.navLineHome, "Beranda", 0, () -> {
            this.focusRow1();
        });
        this.bindSidebarItem(this.navBtnSearch, this.navIconSearch, this.navLineSearch, "Pencarian", 1, () -> {
            this.triggerSystemSearch();
        });
        this.bindSidebarItem(this.navBtnSettings, this.navIconSettings, this.navLineSettings, "Pengaturan TV", 2, () -> {
            if (wasDialogRecentlyDismissed() || wasRecentlyReordering()) return;
            long now = System.currentTimeMillis();
            if (now - this.lastSettingsPressTime > 350L) {
                this.lastSettingsPressTime = now;
                TvUtils.openQuickSettings((Context)this);
            }
        });
        this.updateSidebarActiveState();
    }

    private void bindSidebarItem(View button, ImageView icon, View line, String tooltipText, int section, Runnable onClick) {
        if (button == null) {
            return;
        }
        button.setOnFocusChangeListener((v, hasFocus) -> {
            this.updateSidebarActiveState();
            if (hasFocus) {
                if (icon != null) {
                    icon.animate().scaleX(1.18f).scaleY(1.18f).setDuration(180L).start();
                }
                if (this.sidebarTooltip != null) {
                    this.handler.removeCallbacks(this.hideTooltipRunnable);
                    this.sidebarTooltip.setText((CharSequence)tooltipText);
                    this.sidebarTooltip.setY(button.getY() + this.getSidebarTooltipOffset());
                    this.sidebarTooltip.animate().alpha(1.0f).translationX(0.0f).setDuration(180L).start();
                    this.handler.postDelayed(this.hideTooltipRunnable, 2500L);
                }
            } else if (this.sidebarTooltip != null) {
                this.handler.removeCallbacks(this.hideTooltipRunnable);
                this.sidebarTooltip.animate().alpha(0.0f).setDuration(140L).start();
            }
        });
        button.setOnClickListener(v -> {
            if (onClick != null) {
                onClick.run();
            }
        });
    }

    private void updateSidebarActiveState() {
        this.updateNavItemVisual(this.navBtnHome, this.navIconHome, this.navLineHome, this.activeSection == 0);
        this.updateNavItemVisual(this.navBtnSearch, this.navIconSearch, this.navLineSearch, this.activeSection == 1);
        this.updateNavItemVisual(this.navBtnSettings, this.navIconSettings, this.navLineSettings, this.activeSection == 2);
    }

    private void updateNavItemVisual(View btn, ImageView icon, View line, boolean isActive) {
        float targetAlpha;
        if (btn == null) {
            return;
        }
        boolean hasFocus = btn.hasFocus();
        if (icon != null) {
            float f = targetAlpha = hasFocus || isActive ? 1.0f : 0.45f;
            float targetScale = hasFocus ? 1.18f : (isActive ? 1.05f : 1.0f);
            icon.animate().alpha(targetAlpha).scaleX(targetScale).scaleY(targetScale).setDuration(160L).start();
        }
        if (line != null) {
            targetAlpha = hasFocus || isActive ? 1.0f : 0.0f;
            line.animate().alpha(targetAlpha).setDuration(160L).start();
        }
    }

    private boolean isSidebarView(View v) {
        if (v == null) {
            return false;
        }
        return v == this.navBtnHome || v == this.navBtnSearch || v == this.navBtnSettings
                || v.getId() == R.id.nav_btn_home || v.getId() == R.id.nav_btn_search || v.getId() == R.id.nav_btn_settings;
    }

    private void moveToSidebar(int section) {
        View target = this.navBtnHome;
        if (section == 1) {
            target = this.navBtnSearch;
        } else if (section >= 2) {
            target = this.navBtnSettings;
        }
        if (target != null) {
            target.requestFocus();
        }
    }

    private void returnFocusToActiveContent() {
        this.focusRow1();
    }

    private boolean shouldMoveToSidebar(View focused) {
        if (focused == null) {
            return false;
        }
        try {
            if (focused == this.btnWatchNow) {
                return true;
            }
            if (focused == this.cardWeather) {
                return true;
            }
            if (this.recyclerFavorites != null && this.isChildOf(focused, (View)this.recyclerFavorites)) {
                View itemView = this.recyclerFavorites.findContainingItemView(focused);
                if (itemView != null) {
                    int pos = this.recyclerFavorites.getChildAdapterPosition(itemView);
                    return pos == 0;
                }
                View firstChild = this.recyclerFavorites.getChildAt(0);
                return firstChild != null && this.isChildOf(focused, firstChild);
            }
            if (this.recyclerRecommended != null && this.isChildOf(focused, (View)this.recyclerRecommended)) {
                View itemView = this.recyclerRecommended.findContainingItemView(focused);
                if (itemView != null) {
                    int pos = this.recyclerRecommended.getChildAdapterPosition(itemView);
                    return pos == 0;
                }
                View firstChild = this.recyclerRecommended.getChildAt(0);
                return firstChild != null && this.isChildOf(focused, firstChild);
            }
            if (this.recyclerAllApps != null && this.isChildOf(focused, (View)this.recyclerAllApps)) {
                View itemView = this.recyclerAllApps.findContainingItemView(focused);
                if (itemView != null) {
                    int pos = this.recyclerAllApps.getChildAdapterPosition(itemView);
                    return pos == 0;
                }
                View firstChild = this.recyclerAllApps.getChildAt(0);
                return firstChild != null && this.isChildOf(focused, firstChild);
            }
        }
        catch (Exception e) {
            Log.e((String)"MainActivity", (String)("Error in shouldMoveToSidebar: " + e.getMessage()), (Throwable)e);
        }
        return false;
    }

    private void showTvInputDialog() {
        Dialog dialog = new Dialog((Context)this);
        dialog.requestWindowFeature(1);
        dialog.setContentView(R.layout.dialog_tv_inputs);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable((Drawable)new ColorDrawable(0));
        }
        TextView btnHdmi1 = (TextView)dialog.findViewById(R.id.btn_input_hdmi1);
        TextView btnHdmi2 = (TextView)dialog.findViewById(R.id.btn_input_hdmi2);
        TextView btnLiveTv = (TextView)dialog.findViewById(R.id.btn_input_livetv);
        TextView btnAv = (TextView)dialog.findViewById(R.id.btn_input_av);
        TextView btnSettings = (TextView)dialog.findViewById(R.id.btn_input_settings);
        TextView btnCancel = (TextView)dialog.findViewById(R.id.btn_input_cancel);
        btnHdmi1.setOnClickListener(v -> {
            dialog.dismiss();
            TvUtils.switchToHdmi1((Context)this);
        });
        btnHdmi2.setOnClickListener(v -> {
            dialog.dismiss();
            TvUtils.switchToHdmi2((Context)this);
        });
        btnLiveTv.setOnClickListener(v -> {
            dialog.dismiss();
            TvUtils.openLiveTv((Context)this);
        });
        btnAv.setOnClickListener(v -> {
            dialog.dismiss();
            TvUtils.switchToAv((Context)this);
        });
        btnSettings.setOnClickListener(v -> {
            dialog.dismiss();
            TvUtils.openInputs((Context)this);
        });
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        btnHdmi1.requestFocus();
    }

    private float dpToPx(float dp) {
        return dp * this.getResources().getDisplayMetrics().density;
    }

    private float getSidebarTooltipOffset() {
        return this.dpToPx(28.0f);
    }

    private void setupHeroActions() {
        ViewOutlineProvider pillOutline = new ViewOutlineProvider(){

            public void getOutline(View view, Outline outline) {
                float r = (view.getHeight() > 0) ? ((float)view.getHeight() / 2.0f) : (20.0f * MainActivity.this.getResources().getDisplayMetrics().density);
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), r);
            }
        };
        if (this.btnWatchNow != null) {
            this.btnWatchNow.setOutlineProvider(pillOutline);
            this.btnWatchNow.setClipToOutline(false);
            if (Build.VERSION.SDK_INT >= 28) {
                this.btnWatchNow.setOutlineAmbientShadowColor(0x30000000);
                this.btnWatchNow.setOutlineSpotShadowColor(0x40000000);
            }
            this.btnWatchNow.setOnFocusChangeListener((v, hasFocus) -> v.animate().scaleX(hasFocus ? 1.05f : 1.0f).scaleY(hasFocus ? 1.05f : 1.0f).translationZ(hasFocus ? 8.0f * this.getResources().getDisplayMetrics().density : 0.0f).setDuration(160L).start());
            this.btnWatchNow.setOnClickListener(v -> {
                if (this.currentMovie == null) {
                    return;
                }
                boolean launched = false;
                if (this.currentMovie.getTargetPackage() != null) {
                    launched = TvUtils.launchPackage((Context)this, this.currentMovie.getTargetPackage());
                }
                if (!launched) {
                    TvUtils.searchMediaOnTv((Context)this, this.currentMovie.getSearchKeyword());
                }
            });
        }
        if (this.btnNextMovie != null) {
            this.btnNextMovie.setOutlineProvider(pillOutline);
            this.btnNextMovie.setClipToOutline(false);
            if (Build.VERSION.SDK_INT >= 28) {
                this.btnNextMovie.setOutlineAmbientShadowColor(0x20000000);
                this.btnNextMovie.setOutlineSpotShadowColor(0x30000000);
            }
            this.btnNextMovie.setOnFocusChangeListener((v, hasFocus) -> v.animate().scaleX(hasFocus ? 1.05f : 1.0f).scaleY(hasFocus ? 1.05f : 1.0f).translationZ(hasFocus ? 8.0f * this.getResources().getDisplayMetrics().density : 0.0f).setDuration(160L).start());
            this.btnNextMovie.setOnClickListener(v -> this.shuffleRecommendationMovie(true));
        }
    }

    private void shuffleRecommendationMovie(boolean animate) {
        int newIndex;
        List<MovieItem> all = MovieCatalog.getAllMovies();
        if (all == null || all.isEmpty()) {
            return;
        }
        if (all.size() == 1) {
            this.displayMovie(all.get(0), animate);
            return;
        }
        while ((newIndex = this.random.nextInt(all.size())) == this.currentMovieIndex && all.size() > 1) {
        }
        this.currentMovieIndex = newIndex;
        this.displayMovie(all.get(this.currentMovieIndex), animate);
        this.restartAutoShuffleTimer();
    }

    private void restartAutoShuffleTimer() {
        this.handler.removeCallbacks(this.autoShuffleRunnable);
        this.handler.postDelayed(this.autoShuffleRunnable, 300000L);
    }

    private void loadCatalogAndApps() {
        MovieCatalog.initializeWithCache((Context)this);
        if (this.currentMovie == null) {
            this.shuffleRecommendationMovie(false);
        }
        List<AppItem> allApps = TvUtils.loadInstalledApps((Context)this);
        List<String> favOrder = TvUtils.getFavoritePackagesOrder((Context)this);
        this.favoriteApps.clear();
        this.remainingApps.clear();
        HashMap<String, AppItem> appMap = new HashMap<String, AppItem>();
        for (AppItem app : allApps) {
            appMap.put(app.getPackageName(), app);
        }
        for (String pkg : favOrder) {
            AppItem app = (AppItem)appMap.get(pkg);
            if (app == null) continue;
            app.setFavorite(true);
            this.favoriteApps.add(app);
        }
        for (AppItem app : allApps) {
            if (this.favoriteApps.contains(app)) continue;
            app.setFavorite(false);
            this.remainingApps.add(app);
        }
        this.recyclerFavorites.setLayoutManager((RecyclerView.LayoutManager)new LinearLayoutManager((Context)this, 0, false));
        this.favAdapter = new AppAdapter((Context)this, this.favoriteApps, true, this, this);
        this.recyclerFavorites.setAdapter(this.favAdapter);
        this.recyclerRecommended.setLayoutManager((RecyclerView.LayoutManager)new LinearLayoutManager((Context)this, 0, false));
        List<ChannelItem> movieCards = this.buildMovieCards();
        ChannelAdapter movieAdapter = new ChannelAdapter((Context)this, movieCards, this);
        this.recyclerRecommended.setAdapter(movieAdapter);
        this.recyclerAllApps.setLayoutManager((RecyclerView.LayoutManager)new LinearLayoutManager((Context)this, 0, false));
        this.allAdapter = new AppAdapter((Context)this, this.remainingApps.isEmpty() ? allApps : this.remainingApps, false, this, this);
        this.recyclerAllApps.setAdapter(this.allAdapter);
        MovieCatalog.fetchOnlineCatalog((Context)this, updatedList -> {
            if (this.isActivityResumed && !this.isFinishing() && !this.isDestroyed()) {
                View currentFocus = this.getCurrentFocus();
                boolean wasRow1 = currentFocus != null && (this.isChildOf(currentFocus, (View)this.recyclerFavorites) || currentFocus == this.containerFavoritesDock || currentFocus == this.btnWatchNow);
                List<ChannelItem> updatedCards = this.buildMovieCards();
                this.recyclerRecommended.setAdapter(new ChannelAdapter((Context)this, updatedCards, this));
                if (wasRow1 || this.isInitialLaunch || this.activeSection == 0) {
                    this.focusRow1();
                }
            }
        });
    }

    private List<ChannelItem> buildMovieCards() {
        ArrayList<ChannelItem> cards = new ArrayList<ChannelItem>();
        List<MovieItem> movies = MovieCatalog.getAllMovies();
        for (MovieItem movie : movies) {
            Intent intent = new Intent("android.intent.action.SEARCH");
            intent.setPackage("com.google.android.youtube.tv");
            intent.putExtra("query", movie.getSearchKeyword() + " trailer");
            cards.add(new ChannelItem(movie.getTitle(), movie.getGenres(), "\u2605 " + movie.getRating(), null, movie.getBackdropUrl(), intent));
        }
        return cards;
    }

    @Override
    public void onAppFocused(AppItem app) {
        this.resetScreensaverTimer();
    }

    @Override
    public void onAppLongClicked(AppItem app, int position, boolean isFavorite) {
        this.showAppActionDialog(app, position, isFavorite);
    }

    private void showAppActionDialog(AppItem app, int position, boolean isFavorite) {
        View[] dialogButtons;
        View rootDialogView;
        Dialog dialog = new Dialog((Context)this);
        dialog.requestWindowFeature(1);
        dialog.setContentView(R.layout.dialog_app_actions);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable((Drawable)new ColorDrawable(0));
            dialog.getWindow().setDimAmount(0.65f);
        }
        if ((rootDialogView = dialog.findViewById(R.id.dialog_app_actions_root)) != null) {
            rootDialogView.setOutlineProvider(new ViewOutlineProvider(){

                public void getOutline(View view, Outline outline) {
                    float r = 28.0f * MainActivity.this.getResources().getDisplayMetrics().density;
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), r);
                }
            });
            rootDialogView.setClipToOutline(true);
        }
        ImageView appIcon = (ImageView)dialog.findViewById(R.id.dialog_app_icon);
        TextView appName = (TextView)dialog.findViewById(R.id.dialog_app_name);
        TextView appVersion = (TextView)dialog.findViewById(R.id.dialog_app_version);
        TextView appBadge = (TextView)dialog.findViewById(R.id.dialog_app_badge);
        TextView appPkg = (TextView)dialog.findViewById(R.id.dialog_app_package);
        View btnOpen = dialog.findViewById(R.id.btn_action_open);
        TextView textOpen = (TextView)dialog.findViewById(R.id.text_action_open);
        TextView subOpen = (TextView)dialog.findViewById(R.id.sub_action_open);
        View btnFav = dialog.findViewById(R.id.btn_action_fav);
        TextView textFav = (TextView)dialog.findViewById(R.id.text_action_fav);
        TextView subFav = (TextView)dialog.findViewById(R.id.sub_action_fav);
        View btnMove = dialog.findViewById(R.id.btn_action_move);
        TextView textMove = (TextView)dialog.findViewById(R.id.text_action_move);
        TextView subMove = (TextView)dialog.findViewById(R.id.sub_action_move);
        View btnInfo = dialog.findViewById(R.id.btn_action_info);
        TextView textInfo = (TextView)dialog.findViewById(R.id.text_action_info);
        TextView subInfo = (TextView)dialog.findViewById(R.id.sub_action_info);
        View btnUninstall = dialog.findViewById(R.id.btn_action_uninstall);
        TextView textUninstall = (TextView)dialog.findViewById(R.id.text_action_uninstall);
        TextView subUninstall = (TextView)dialog.findViewById(R.id.sub_action_uninstall);
        View btnCancel = dialog.findViewById(R.id.btn_action_cancel);
        TextView textCancel = (TextView)dialog.findViewById(R.id.text_action_cancel);
        TextView subCancel = (TextView)dialog.findViewById(R.id.sub_action_cancel);
        AppActionCapabilities caps = AppActionCapabilities.evaluate((Context)this, app.getPackageName());
        app.setCapabilities(caps);
        if (app.getIcon() != null) {
            appIcon.setImageDrawable(app.getIcon());
        }
        appName.setText((CharSequence)app.getLabel());
        appPkg.setText((CharSequence)app.getPackageName());
        if (appVersion != null) {
            try {
                PackageInfo pInfo = this.getPackageManager().getPackageInfo(app.getPackageName(), 0);
                if (pInfo != null && pInfo.versionName != null && !pInfo.versionName.isEmpty()) {
                    appVersion.setText((CharSequence)("v" + pInfo.versionName));
                } else {
                    appVersion.setText((CharSequence)"v1.0");
                }
            }
            catch (Exception e) {
                appVersion.setText((CharSequence)"v1.0");
            }
        }
        if (appBadge != null) {
            if (caps.canUninstallUpdates()) {
                appBadge.setText((CharSequence)"Pembaruan Sistem");
                appBadge.setTextColor(Color.parseColor((String)"#FBBF24"));
            } else if (caps.isSystemApp()) {
                appBadge.setText((CharSequence)"Aplikasi Sistem");
                appBadge.setTextColor(Color.parseColor((String)"#94A3B8"));
            } else {
                appBadge.setText((CharSequence)"Aplikasi Pengguna");
                appBadge.setTextColor(Color.parseColor((String)"#38BDF8"));
            }
        }
        if (caps.canOpen()) {
            btnOpen.setVisibility(0);
            btnOpen.setOnClickListener(v -> {
                dialog.dismiss();
                Intent intent = this.getPackageManager().getLaunchIntentForPackage(app.getPackageName());
                if (intent != null) {
                    intent.addFlags(0x10200000);
                    this.startActivity(intent);
                }
            });
        } else {
            btnOpen.setVisibility(8);
        }
        btnFav.setVisibility(0);
        if (textFav != null) {
            textFav.setText((CharSequence)(isFavorite ? "Hapus dari Favorit" : "Sematkan ke Favorit"));
        }
        if (subFav != null) {
            subFav.setText((CharSequence)(isFavorite ? "Hapus dari baris 1" : "Akses cepat"));
        }
        btnFav.setOnClickListener(v -> {
            lastActionDismissTime = SystemClock.uptimeMillis();
            dialog.dismiss();
            int originalPos = position;
            boolean wasFav = isFavorite;
            if (wasFav) {
                this.favoriteApps.remove(app);
                TvUtils.removeFavorite((Context)this, app.getPackageName());
                app.setFavorite(false);
                if (!this.remainingApps.contains(app)) {
                    this.remainingApps.add(app);
                }
                if (this.favAdapter != null) {
                    this.favAdapter.notifyDataSetChanged();
                }
                if (this.allAdapter != null) {
                    this.allAdapter.notifyDataSetChanged();
                }
                Toast.makeText((Context)this, (CharSequence)(app.getLabel() + " dihapus dari Favorit"), (int)0).show();
                this.handler.postDelayed(() -> {
                    if (this.isFinishing() || this.isDestroyed()) {
                        return;
                    }
                    if (this.recyclerFavorites != null) {
                        int targetPos = Math.min(originalPos, Math.max(0, this.favoriteApps.size() - 1));
                        this.recyclerFavorites.scrollToPosition(targetPos);
                        this.recyclerFavorites.postDelayed(() -> {
                            RecyclerView.ViewHolder vh = this.recyclerFavorites.findViewHolderForAdapterPosition(targetPos);
                            if (vh != null) {
                                vh.itemView.requestFocus();
                            } else {
                                this.ensureFocus();
                            }
                        }, 50L);
                    } else {
                        this.ensureFocus();
                    }
                }, 80L);
            } else {
                if (!this.favoriteApps.contains(app)) {
                    this.favoriteApps.add(app);
                    app.setFavorite(true);
                    TvUtils.addFavorite((Context)this, app.getPackageName());
                    this.remainingApps.remove(app);
                }
                if (this.favAdapter != null) {
                    this.favAdapter.notifyDataSetChanged();
                }
                if (this.allAdapter != null) {
                    this.allAdapter.notifyDataSetChanged();
                }
                Toast.makeText((Context)this, (CharSequence)(app.getLabel() + " ditambahkan ke Favorit"), (int)0).show();
                this.handler.postDelayed(() -> {
                    if (this.isFinishing() || this.isDestroyed()) {
                        return;
                    }
                    if (this.recyclerAllApps != null) {
                        int targetPos = Math.min(originalPos, Math.max(0, this.remainingApps.size() - 1));
                        this.recyclerAllApps.scrollToPosition(targetPos);
                        this.recyclerAllApps.postDelayed(() -> {
                            RecyclerView.ViewHolder vh = this.recyclerAllApps.findViewHolderForAdapterPosition(targetPos);
                            if (vh != null) {
                                vh.itemView.requestFocus();
                            } else {
                                this.ensureFocus();
                            }
                        }, 50L);
                    } else {
                        this.ensureFocus();
                    }
                }, 80L);
            }
        });
        btnMove.setVisibility(0);
        if (textMove != null) {
            textMove.setText((CharSequence)"Pindahkan Posisi");
        }
        if (subMove != null) {
            subMove.setText((CharSequence)(isFavorite ? "Atur urutan di Favorit" : "Atur urutan di Semua Aplikasi"));
        }
        btnMove.setOnClickListener(v -> {
            dialog.dismiss();
            this.handler.postDelayed(() -> {
                if (!this.isFinishing() && !this.isDestroyed()) {
                    this.startReorderMode(app, position, isFavorite);
                }
            }, 80L);
        });
        btnInfo.setVisibility(0);
        if (textInfo != null) {
            textInfo.setText((CharSequence)"Info Aplikasi");
        }
        if (subInfo != null) {
            subInfo.setText((CharSequence)"Izin & memori");
        }
        btnInfo.setOnClickListener(v -> {
            dialog.dismiss();
            try {
                Intent intent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS");
                intent.setData(Uri.parse((String)("package:" + app.getPackageName())));
                intent.addFlags(0x10000000);
                this.startActivity(intent);
            }
            catch (Exception exception) {
                // empty catch block
            }
        });
        if (caps.canUninstall()) {
            btnUninstall.setVisibility(0);
            if (textUninstall != null) {
                textUninstall.setText((CharSequence)"Copot Pemasangan");
            }
            if (subUninstall != null) {
                subUninstall.setText((CharSequence)"Hapus dari TV");
            }
            btnUninstall.setOnClickListener(v -> {
                dialog.dismiss();
                this.performAppUninstall(app.getPackageName());
            });
        } else if (caps.canUninstallUpdates()) {
            btnUninstall.setVisibility(0);
            if (textUninstall != null) {
                textUninstall.setText((CharSequence)"Hapus Pembaruan");
            }
            if (subUninstall != null) {
                subUninstall.setText((CharSequence)"Kembali ke versi pabrik");
            }
            btnUninstall.setOnClickListener(v -> {
                dialog.dismiss();
                this.performAppUninstall(app.getPackageName());
            });
        } else {
            btnUninstall.setVisibility(8);
        }
        btnCancel.setOnClickListener(v -> {
            lastActionDismissTime = SystemClock.uptimeMillis();
            dialog.dismiss();
            this.handler.postDelayed(this::ensureFocus, 80L);
        });
        dialog.setOnDismissListener(d -> {
            lastActionDismissTime = SystemClock.uptimeMillis();
        });
        for (View btn : dialogButtons = new View[]{btnFav, btnMove, btnInfo, btnUninstall, btnOpen, btnCancel}) {
            if (btn == null) continue;
            btn.setOnFocusChangeListener((v, hasFocus) -> {
                v.setScaleX(1.0f);
                v.setScaleY(1.0f);
                v.setTranslationZ(hasFocus ? (4.0f * this.getResources().getDisplayMetrics().density) : 0.0f);
            });
        }
        dialog.setOnShowListener(d -> {
            if (btnFav != null && btnFav.getVisibility() == 0) {
                btnFav.requestFocus();
            } else if (btnMove != null && btnMove.getVisibility() == 0) {
                btnMove.requestFocus();
            } else if (btnInfo != null && btnInfo.getVisibility() == 0) {
                btnInfo.requestFocus();
            } else if (btnOpen != null && btnOpen.getVisibility() == 0) {
                btnOpen.requestFocus();
            }
        });
        dialog.show();
    }

    private void performAppUninstall(String packageName) {
        try {
            Intent intent = new Intent("android.intent.action.UNINSTALL_PACKAGE");
            intent.setData(Uri.fromParts((String)"package", (String)packageName, null));
            intent.putExtra("android.intent.extra.RETURN_RESULT", true);
            intent.addFlags(0x10000000);
            this.startActivity(intent);
        }
        catch (Exception e) {
            try {
                Intent intent = new Intent("android.intent.action.DELETE");
                intent.setData(Uri.fromParts((String)"package", (String)packageName, null));
                intent.addFlags(0x10000000);
                this.startActivity(intent);
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
    }

    private void setupPackageSyncListeners() {
        try {
            this.launcherApps = (LauncherApps)this.getSystemService("launcherapps");
            if (this.launcherApps != null) {
                this.launcherAppsCallback = new LauncherApps.Callback(){

                    public void onPackageAdded(String packageName, UserHandle user) {
                        Log.d((String)"ZenithSync", (String)("LauncherApps onPackageAdded: " + packageName));
                        MainActivity.this.triggerRealtimeAppSync(packageName);
                    }

                    public void onPackageRemoved(String packageName, UserHandle user) {
                        Log.d((String)"ZenithSync", (String)("LauncherApps onPackageRemoved: " + packageName));
                        MainActivity.this.triggerRealtimeAppSync(packageName);
                    }

                    public void onPackageChanged(String packageName, UserHandle user) {
                        Log.d((String)"ZenithSync", (String)("LauncherApps onPackageChanged: " + packageName));
                        MainActivity.this.triggerRealtimeAppSync(packageName);
                    }

                    public void onPackagesAvailable(String[] packageNames, UserHandle user, boolean replacing) {
                        MainActivity.this.triggerRealtimeAppSync(null);
                    }

                    public void onPackagesUnavailable(String[] packageNames, UserHandle user, boolean replacing) {
                        MainActivity.this.triggerRealtimeAppSync(null);
                    }

                    public void onPackagesSuspended(String[] packageNames, UserHandle user) {
                        MainActivity.this.triggerRealtimeAppSync(null);
                    }

                    public void onPackagesUnsuspended(String[] packageNames, UserHandle user) {
                        MainActivity.this.triggerRealtimeAppSync(null);
                    }
                };
                this.launcherApps.registerCallback(this.launcherAppsCallback);
                Log.d((String)"ZenithSync", (String)"LauncherApps.Callback registered successfully");
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        try {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.PACKAGE_ADDED");
            filter.addAction("android.intent.action.PACKAGE_REMOVED");
            filter.addAction("android.intent.action.PACKAGE_REPLACED");
            filter.addAction("android.intent.action.PACKAGE_CHANGED");
            filter.addDataScheme("package");
            this.packageReceiver = new BroadcastReceiver(){

                public void onReceive(Context context, Intent intent) {
                    if (intent == null || intent.getData() == null) {
                        return;
                    }
                    String packageName = intent.getData().getSchemeSpecificPart();
                    Log.d((String)"ZenithSync", (String)("Broadcast received: " + intent.getAction() + " for " + packageName));
                    MainActivity.this.triggerRealtimeAppSync(packageName);
                }
            };
            if (Build.VERSION.SDK_INT >= 33) {
                this.registerReceiver(this.packageReceiver, filter, 2);
            } else {
                this.registerReceiver(this.packageReceiver, filter);
            }
            Log.d((String)"ZenithSync", (String)"Dynamic package BroadcastReceiver registered with RECEIVER_EXPORTED");
        }
        catch (Exception e) {
            Log.e((String)"ZenithSync", (String)"Error registering BroadcastReceiver", (Throwable)e);
        }
    }

    private void teardownPackageSyncListeners() {
        if (this.launcherApps != null && this.launcherAppsCallback != null) {
            try {
                this.launcherApps.unregisterCallback(this.launcherAppsCallback);
            }
            catch (Exception exception) {
                // empty catch block
            }
            this.launcherAppsCallback = null;
        }
        if (this.packageReceiver != null) {
            try {
                this.unregisterReceiver(this.packageReceiver);
            }
            catch (Exception exception) {
                // empty catch block
            }
            this.packageReceiver = null;
        }
    }

    private void triggerRealtimeAppSync(String changedPkg) {
        this.handler.post(() -> this.syncInstalledApps(true));
    }

    public synchronized void syncInstalledApps(boolean forceReload) {
        if (this.isFinishing() || this.isDestroyed() || this.isReconciling || this.isReorderMode) {
            return;
        }
        this.isReconciling = true;
        this.appSyncExecutor.execute(() -> {
            try {
                List<AppItem> freshApps = TvUtils.loadInstalledApps((Context)this);
                List<String> favOrder = TvUtils.getFavoritePackagesOrder((Context)this);
                if (!forceReload && !this.hasAppListChanged(freshApps, favOrder)) {
                    this.isReconciling = false;
                    return;
                }
                ArrayList<AppItem> newFavs = new ArrayList<AppItem>();
                ArrayList<AppItem> newRemaining = new ArrayList<AppItem>();
                HashMap<String, AppItem> appMap = new HashMap<String, AppItem>();
                for (AppItem app : freshApps) {
                    appMap.put(app.getPackageName(), app);
                }
                for (String pkg : favOrder) {
                    AppItem app = (AppItem)appMap.get(pkg);
                    if (app == null) continue;
                    app.setFavorite(true);
                    newFavs.add(app);
                }
                for (AppItem app : freshApps) {
                    if (newFavs.contains(app)) continue;
                    app.setFavorite(false);
                    newRemaining.add(app);
                }
                this.runOnUiThread(() -> {
                    if (!this.isActivityResumed || this.isFinishing() || this.isDestroyed()) {
                        this.isReconciling = false;
                        return;
                    }
                    this.favoriteApps.clear();
                    this.favoriteApps.addAll(newFavs);
                    Log.d((String)"ZenithSync", (String)("UI sync applied: favs=" + newFavs.size() + " remaining=" + newRemaining.size()));
                    this.remainingApps.clear();
                    this.remainingApps.addAll(newRemaining);
                    if (this.favAdapter != null) {
                        this.favAdapter.notifyDataSetChanged();
                    }
                    if (this.allAdapter != null) {
                        this.allAdapter.notifyDataSetChanged();
                    }
                    this.isReconciling = false;
                });
            }
            catch (Exception e) {
                this.isReconciling = false;
            }
        });
    }

    private boolean hasAppListChanged(List<AppItem> freshApps, List<String> favOrder) {
        if (freshApps == null) {
            return false;
        }
        int totalCurrent = this.favoriteApps.size() + this.remainingApps.size();
        if (freshApps.size() != totalCurrent) {
            return true;
        }
        HashMap<String, Long> currentVersions = new HashMap<String, Long>();
        for (AppItem a : this.favoriteApps) {
            currentVersions.put(a.getPackageName(), a.getCapabilities() != null ? a.getCapabilities().getVersionCode() : 0L);
        }
        for (AppItem a : this.remainingApps) {
            currentVersions.put(a.getPackageName(), a.getCapabilities() != null ? a.getCapabilities().getVersionCode() : 0L);
        }
        for (AppItem fresh : freshApps) {
            Long v = (Long)currentVersions.get(fresh.getPackageName());
            if (v == null) {
                return true;
            }
            long freshV = fresh.getCapabilities() != null ? fresh.getCapabilities().getVersionCode() : 0L;
            if (v.equals(freshV)) continue;
            return true;
        }
        return false;
    }

    private void startReorderMode(AppItem app, int position, boolean isFavorite) {
        AppAdapter targetAdapter;
        this.isReorderMode = true;
        this.isReorderFavorites = isFavorite;
        this.reorderingApp = app;
        this.reorderingAppIndex = position;
        this.reorderStartTime = SystemClock.uptimeMillis();
        if (this.reorderHud != null) {
            this.reorderHud.setVisibility(0);
            if (this.reorderHudText != null) {
                if (isFavorite) {
                    this.reorderHudText.setText((CharSequence)"Geser: \u25c4 Kiri / Kanan \u25ba untuk atur Favorit  \u2022  Tekan OK untuk selesai");
                } else {
                    this.reorderHudText.setText((CharSequence)"Geser: \u25c4 Kiri / Kanan \u25ba untuk atur Semua Aplikasi  \u2022  Tekan OK untuk selesai");
                }
            }
        }
        TvHorizontalGridView targetRecycler = isFavorite ? this.recyclerFavorites : this.recyclerAllApps;
        AppAdapter appAdapter = targetAdapter = isFavorite ? this.favAdapter : this.allAdapter;
        if (isFavorite) {
            if (this.allAdapter != null) {
                this.allAdapter.setReorderingPosition(-1);
            }
            if (this.favAdapter != null) {
                this.favAdapter.setReorderingPosition(position);
            }
            if (this.containerFavoritesDock != null) {
                this.scrollToCenterRow(this.anchorFavorit, this.containerFavoritesDock);
            }
        } else {
            if (this.favAdapter != null) {
                this.favAdapter.setReorderingPosition(-1);
            }
            if (this.allAdapter != null) {
                this.allAdapter.setReorderingPosition(position);
            }
            if (this.containerAllAppsDock != null) {
                this.scrollToCenterRow(this.anchorAllApps, this.containerAllAppsDock);
            }
        }
        if (targetRecycler != null) {
            targetRecycler.scrollToPosition(position);
            targetRecycler.postDelayed(() -> {
                RecyclerView.ViewHolder vh = targetRecycler.findViewHolderForAdapterPosition(position);
                if (vh != null) {
                    vh.itemView.requestFocus();
                } else {
                    targetRecycler.requestFocus();
                }
            }, 100L);
        }
    }

    private void stopReorderMode() {
        if (!this.isReorderMode) {
            return;
        }
        this.isReorderMode = false;
        lastReorderEndTime = SystemClock.uptimeMillis();
        if (this.reorderHud != null) {
            this.reorderHud.setVisibility(8);
        }
        if (this.favAdapter != null) {
            this.favAdapter.setReorderingPosition(-1);
        }
        if (this.allAdapter != null) {
            this.allAdapter.setReorderingPosition(-1);
        }
        int finalPos = this.reorderingAppIndex;
        boolean wasFav = this.isReorderFavorites;
        if (wasFav) {
            List<String> newOrder = new ArrayList<String>();
            for (AppItem a : this.favoriteApps) {
                newOrder.add(a.getPackageName());
            }
            TvUtils.saveFavoritesOrder((Context)this, newOrder);
            Toast.makeText((Context)this, (CharSequence)"Urutan favorit berhasil disimpan", (int)0).show();
        } else {
            List<String> newOrder = new ArrayList<String>();
            for (AppItem a : this.remainingApps) {
                newOrder.add(a.getPackageName());
            }
            TvUtils.saveAllAppsOrder((Context)this, newOrder);
            Toast.makeText((Context)this, (CharSequence)"Urutan semua aplikasi berhasil disimpan", (int)0).show();
        }
        TvHorizontalGridView targetRecycler = wasFav ? this.recyclerFavorites : this.recyclerAllApps;
        int listCount = wasFav ? this.favoriteApps.size() : this.remainingApps.size();
        int safePos = Math.min(Math.max(0, finalPos), Math.max(0, listCount - 1));
        if (targetRecycler != null && safePos >= 0) {
            targetRecycler.scrollToPosition(safePos);
            targetRecycler.postDelayed(() -> {
                if (this.isFinishing() || this.isDestroyed()) {
                    return;
                }
                RecyclerView.ViewHolder vh = targetRecycler.findViewHolderForAdapterPosition(safePos);
                if (vh != null) {
                    vh.itemView.requestFocus();
                } else if (targetRecycler.getChildCount() > 0) {
                    View child = targetRecycler.getChildAt(0);
                    if (child != null) {
                        child.requestFocus();
                    }
                } else {
                    this.ensureFocus();
                }
            }, 80L);
        } else {
            this.ensureFocus();
        }
        this.reorderingApp = null;
        this.reorderingAppIndex = -1;
    }

    private boolean handleReorderKey(int keyCode) {
        TvHorizontalGridView targetRecycler;
        if (!this.isReorderMode) {
            return false;
        }
        List<AppItem> targetList = this.isReorderFavorites ? this.favoriteApps : this.remainingApps;
        AppAdapter targetAdapter = this.isReorderFavorites ? this.favAdapter : this.allAdapter;
        TvHorizontalGridView tvHorizontalGridView = targetRecycler = this.isReorderFavorites ? this.recyclerFavorites : this.recyclerAllApps;
        if (targetList == null || targetList.isEmpty()) {
            this.stopReorderMode();
            return true;
        }
        if (keyCode == 21) {
            if (this.reorderingAppIndex > 0 && this.reorderingAppIndex < targetList.size()) {
                int from = this.reorderingAppIndex;
                int to = this.reorderingAppIndex - 1;
                Collections.swap(targetList, from, to);
                if (targetAdapter != null) {
                    targetAdapter.notifyItemMoved(from, to);
                    targetAdapter.setReorderingPosition(to);
                }
                this.reorderingAppIndex = to;
                if (targetRecycler != null) {
                    targetRecycler.scrollToPosition(this.reorderingAppIndex);
                    targetRecycler.postDelayed(() -> {
                        RecyclerView.ViewHolder vh = targetRecycler.findViewHolderForAdapterPosition(this.reorderingAppIndex);
                        if (vh != null) {
                            vh.itemView.requestFocus();
                        }
                    }, 50L);
                }
            }
            return true;
        }
        if (keyCode == 22) {
            if (this.reorderingAppIndex >= 0 && this.reorderingAppIndex < targetList.size() - 1) {
                int from = this.reorderingAppIndex;
                int to = this.reorderingAppIndex + 1;
                Collections.swap(targetList, from, to);
                if (targetAdapter != null) {
                    targetAdapter.notifyItemMoved(from, to);
                    targetAdapter.setReorderingPosition(to);
                }
                this.reorderingAppIndex = to;
                if (targetRecycler != null) {
                    targetRecycler.scrollToPosition(this.reorderingAppIndex);
                    targetRecycler.postDelayed(() -> {
                        RecyclerView.ViewHolder vh = targetRecycler.findViewHolderForAdapterPosition(this.reorderingAppIndex);
                        if (vh != null) {
                            vh.itemView.requestFocus();
                        }
                    }, 50L);
                }
            }
            return true;
        }
        if (keyCode == 23 || keyCode == 66 || keyCode == 4) {
            if (SystemClock.uptimeMillis() - this.reorderStartTime > 400L) {
                this.stopReorderMode();
            }
            return true;
        }
        return keyCode == 19 || keyCode == 20;
    }

    public void ensureFocus() {
        if (!this.isActivityResumed || this.isFinishing() || this.isDestroyed()) {
            return;
        }
        View focused = this.getCurrentFocus();
        if (focused != null && focused.isShown() && focused.isAttachedToWindow() && !this.isTopBarOrSettingsView(focused)) {
            return;
        }
        this.focusRow1();
    }

    private boolean isTopBarOrSettingsView(View view) {
        if (view == null) {
            return false;
        }
        int id2 = view.getId();
        return id2 == R.id.nav_btn_settings || id2 == R.id.nav_btn_search || id2 == R.id.top_bar;
    }

    private void ensureSettingsAtEndOfRemainingApps() {
        AppItem settingsApp = null;
        for (int i = 0; i < this.remainingApps.size(); ++i) {
            AppItem item = this.remainingApps.get(i);
            String pkg = item.getPackageName().toLowerCase();
            String label = item.getLabel().toLowerCase();
            if (!pkg.contains("settings") && !label.equals("settings") && !label.equals("setelan") && !label.equals("pengaturan")) continue;
            settingsApp = this.remainingApps.remove(i);
            break;
        }
        if (settingsApp != null) {
            this.remainingApps.add(settingsApp);
        }
    }

    private void showControlCenterDialog() {
        Dialog dialog = new Dialog((Context)this);
        dialog.requestWindowFeature(1);
        dialog.setContentView(R.layout.dialog_control_center);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable((Drawable)new ColorDrawable(0));
        }
        TextView btnSettings = (TextView)dialog.findViewById(R.id.btn_cc_settings);
        TextView btnNetwork = (TextView)dialog.findViewById(R.id.btn_cc_network);
        TextView btnInputs = (TextView)dialog.findViewById(R.id.btn_cc_inputs);
        TextView btnSleep = (TextView)dialog.findViewById(R.id.btn_cc_sleep);
        TextView btnClose = (TextView)dialog.findViewById(R.id.btn_cc_close);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                dialog.dismiss();
                long now = System.currentTimeMillis();
                if (now - this.lastSettingsPressTime > 350L) {
                    this.lastSettingsPressTime = now;
                    TvUtils.openQuickSettings((Context)this);
                }
            });
        }
        if (btnNetwork != null) {
            btnNetwork.setOnClickListener(v -> {
                dialog.dismiss();
                TvUtils.openNetworkSettings((Context)this);
            });
        }
        if (btnInputs != null) {
            btnInputs.setOnClickListener(v -> {
                dialog.dismiss();
                this.showTvInputDialog();
            });
        }
        if (btnSleep != null) {
            btnSleep.setOnClickListener(v -> {
                dialog.dismiss();
                TvUtils.startScreensaver((Context)this);
            });
        }
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }
        dialog.show();
    }

    private void triggerSystemSearch() {
        try {
            Intent intent = new Intent("android.intent.action.ASSIST");
            intent.addFlags(0x10000000);
            this.startActivity(intent);
            return;
        }
        catch (Throwable intent) {
            try {
                Intent intent22 = new Intent("android.search.action.GLOBAL_SEARCH");
                intent22.addFlags(0x10000000);
                this.startActivity(intent22);
                return;
            }
            catch (Throwable t1) {
                try {
                    TvUtils.openVoiceSearch((Context)this);
                    return;
                }
                catch (Throwable t2) {
                    try {
                        Intent intent3 = new Intent("android.intent.action.SEARCH");
                        intent3.setPackage("com.google.android.youtube.tv");
                        intent3.addFlags(0x10000000);
                        this.startActivity(intent3);
                    }
                    catch (Throwable e) {
                        Toast.makeText((Context)this, (CharSequence)"Pencarian tidak tersedia", (int)0).show();
                    }
                    return;
                }
            }
        }
    }

    private void ensureDockClipping(View dockView) {
        if (dockView != null) {
            final float density = this.getResources().getDisplayMetrics().density;
            dockView.setOutlineProvider(new ViewOutlineProvider(){

                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), 24.0f * density);
                }
            });
            dockView.setClipToOutline(true);
            if (dockView instanceof ViewGroup) {
                ((ViewGroup)dockView).setClipChildren(true);
            }
        }
    }

    private void setupDynamicSpacing() {
        if (this.mainScrollView == null) {
            return;
        }
        this.mainScrollView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener(){

            public void onGlobalLayout() {
                ViewGroup.LayoutParams rawLp;
                View targetDock;
                MainActivity.this.mainScrollView.getViewTreeObserver().removeOnGlobalLayoutListener((ViewTreeObserver.OnGlobalLayoutListener)this);
                int screenHeight = MainActivity.this.getResources().getDisplayMetrics().heightPixels;
                int topBarMarginTop = 0;
                if (MainActivity.this.topBar != null && MainActivity.this.topBar.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                    topBarMarginTop = ((ViewGroup.MarginLayoutParams)MainActivity.this.topBar.getLayoutParams()).topMargin;
                }
                int topBarH = (MainActivity.this.topBar != null ? MainActivity.this.topBar.getHeight() : 0) + topBarMarginTop;
                int heroMarginTop = 0;
                if (MainActivity.this.heroContainer != null && MainActivity.this.heroContainer.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                    heroMarginTop = ((ViewGroup.MarginLayoutParams)MainActivity.this.heroContainer.getLayoutParams()).topMargin;
                }
                int heroH = (MainActivity.this.heroContainer != null ? MainActivity.this.heroContainer.getHeight() : 0) + heroMarginTop;
                int favH = MainActivity.this.containerFavoritesDock != null ? MainActivity.this.containerFavoritesDock.getHeight() : (MainActivity.this.recyclerFavorites != null ? MainActivity.this.recyclerFavorites.getHeight() : 0);
                int desiredBottomMargin = (int)TypedValue.applyDimension((int)1, (float)18.0f, (DisplayMetrics)MainActivity.this.getResources().getDisplayMetrics());
                int minMarginTop = (int)TypedValue.applyDimension((int)1, (float)20.0f, (DisplayMetrics)MainActivity.this.getResources().getDisplayMetrics());
                int maxMarginTop = (int)TypedValue.applyDimension((int)1, (float)180.0f, (DisplayMetrics)MainActivity.this.getResources().getDisplayMetrics());
                int calculatedMarginTop = screenHeight - topBarH - heroH - favH - desiredBottomMargin;
                int finalMarginTop = Math.max(minMarginTop, Math.min(calculatedMarginTop, maxMarginTop));
                Log.d((String)"TV_SNAP", (String)("setupDynamicSpacing: screenH=" + screenHeight + " topBarH=" + topBarH + " heroH=" + heroH + " favH=" + favH + " desiredBottomMargin=" + desiredBottomMargin + " calculatedMarginTop=" + calculatedMarginTop + " finalMarginTop=" + finalMarginTop));
                targetDock = MainActivity.this.containerFavoritesDock != null ? MainActivity.this.containerFavoritesDock : MainActivity.this.recyclerFavorites;
                if (targetDock != null && (rawLp = targetDock.getLayoutParams()) instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams)rawLp;
                    if (lp.topMargin != finalMarginTop) {
                        lp.topMargin = finalMarginTop;
                        targetDock.setLayoutParams((ViewGroup.LayoutParams)lp);
                    }
                }
            }
        });
    }

    private void scrollToCenterRow(View label, View content) {
        if (this.mainScrollView == null) {
            return;
        }
        int screenH = this.mainScrollView.getHeight();
        if (screenH <= 0) {
            screenH = this.getResources().getDisplayMetrics().heightPixels;
        }
        int top = label != null ? label.getTop() : (content != null ? content.getTop() : 0);
        int bottom = content != null ? content.getBottom() : (label != null ? label.getBottom() : top);
        int rowH = bottom - top;
        int targetScrollY = top + rowH / 2 - screenH / 2;
        if (targetScrollY < 0) {
            targetScrollY = 0;
        }
        Log.d((String)"TV_SNAP", (String)("scrollToCenterRow: label=" + (label != null ? label.getClass().getSimpleName() : "null") + " content=" + (content != null ? content.getClass().getSimpleName() : "null") + " top=" + top + " bottom=" + bottom + " rowH=" + rowH + " screenH=" + screenH + " targetScrollY=" + targetScrollY + " currY=" + this.mainScrollView.getScrollY()));
        this.mainScrollView.smoothScrollTo(0, targetScrollY);
    }

    private void setupShelfAutoSnap() {
        if (this.mainScrollView == null) {
            return;
        }
        this.mainScrollView.getViewTreeObserver().addOnGlobalFocusChangeListener((oldFocus, newFocus) -> {
            boolean isAllApps;
            boolean isFavorites;
            if (newFocus == null) {
                return;
            }
            boolean bl = isFavorites = this.isChildOf(newFocus, (View)this.recyclerFavorites) || newFocus == this.containerFavoritesDock;
            if (this.containerFavoritesDock != null) {
                this.containerFavoritesDock.setBackgroundResource(isFavorites ? R.drawable.bg_favorites_dock_active : R.drawable.bg_favorites_dock);
                if (isFavorites) {
                    this.containerFavoritesDock.setClipToOutline(false);
                    if (this.containerFavoritesDock instanceof ViewGroup) {
                        ((ViewGroup)this.containerFavoritesDock).setClipChildren(false);
                    }
                } else {
                    this.ensureDockClipping(this.containerFavoritesDock);
                }
            }
            if (this.isSidebarView(newFocus)) {
                return;
            }
            if (this.isChildOf(newFocus, (View)this.recyclerFavorites) || this.isChildOf(newFocus, this.heroContainer) || this.isChildOf(newFocus, this.topBar) || this.isChildOf(newFocus, this.containerFavoritesDock)) {
                this.activeSection = 0;
                this.updateSidebarActiveState();
                if (this.mainScrollView.getScrollY() != 0) {
                    this.mainScrollView.smoothScrollTo(0, 0);
                }
                return;
            }
            if (this.isChildOf(newFocus, (View)this.recyclerRecommended)) {
                this.activeSection = 1;
                this.updateSidebarActiveState();
                this.scrollToCenterRow(this.labelRecommended, (View)this.recyclerRecommended);
                return;
            }
            if (this.isChildOf(newFocus, this.glanceHubContainer)) {
                this.activeSection = 2;
                this.updateSidebarActiveState();
                this.scrollToCenterRow(this.labelGlanceHub, this.glanceHubContainer);
                return;
            }
            boolean bl2 = isAllApps = this.isChildOf(newFocus, (View)this.recyclerAllApps) || newFocus == this.containerAllAppsDock;
            if (this.containerAllAppsDock != null) {
                this.containerAllAppsDock.setBackgroundResource(isAllApps ? R.drawable.bg_favorites_dock_active : R.drawable.bg_favorites_dock);
                if (isAllApps) {
                    this.containerAllAppsDock.setClipToOutline(false);
                    if (this.containerAllAppsDock instanceof ViewGroup) {
                        ((ViewGroup)this.containerAllAppsDock).setClipChildren(false);
                    }
                } else {
                    this.ensureDockClipping(this.containerAllAppsDock);
                }
            }
            if (this.isChildOf(newFocus, (View)this.recyclerAllApps) || newFocus == this.containerAllAppsDock) {
                this.activeSection = 3;
                this.updateSidebarActiveState();
                this.scrollToCenterRow(this.anchorAllApps, (View)(this.containerAllAppsDock != null ? this.containerAllAppsDock : this.recyclerAllApps));
            }
        });
    }

    private boolean isChildOf(View child, View parent) {
        if (child == null || parent == null) {
            return false;
        }
        if (child == parent) {
            return true;
        }
        for (ViewParent p = child.getParent(); p != null; p = p.getParent()) {
            if (p != parent) continue;
            return true;
        }
        return false;
    }

    public void onBackPressed() {
        if (this.mainScrollView != null && this.mainScrollView.getScrollY() > 50) {
            this.focusRow1();
            return;
        }
        if (this.recyclerFavorites != null && this.recyclerFavorites.hasFocus()) {
            return;
        }
        this.focusRow1();
    }

    @Override
    public void onChannelFocused(ChannelItem channel) {
        this.resetScreensaverTimer();
        for (MovieItem m : MovieCatalog.getAllMovies()) {
            if (!m.getTitle().equalsIgnoreCase(channel.getTitle())) continue;
            this.displayMovie(m, true);
            break;
        }
    }

    protected void onNewIntent(Intent intent) {
        long now;
        super.onNewIntent(intent);
        this.setIntent(intent);
        if (intent != null && (Intent.ACTION_MAIN.equals(intent.getAction()) || intent.hasCategory(Intent.CATEGORY_HOME))) {
            this.focusRow1();
        }
        if (intent != null && "com.zenith.launcher.ACTION_OPEN_SETTINGS_DRAWER".equals(intent.getAction()) && (now = System.currentTimeMillis()) - this.lastSettingsPressTime > 350L) {
            this.lastSettingsPressTime = now;
            TvUtils.openQuickSettings((Context)this);
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        this.resetScreensaverTimer();
        if (ZenithAccessibilityService.isSettingsKey(keyCode)) {
            if (wasDialogRecentlyDismissed() || wasRecentlyReordering()) return true;
            long now = System.currentTimeMillis();
            if (now - this.lastSettingsPressTime > 350L) {
                this.lastSettingsPressTime = now;
                TvUtils.openQuickSettings((Context)this);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void setupScreensaverWatchdog() {
        this.resetScreensaverTimer();
    }

    private void resetScreensaverTimer() {
        this.handler.removeCallbacks(this.screensaverRunnable);
        this.handler.postDelayed(this.screensaverRunnable, 300000L);
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        this.resetScreensaverTimer();
        this.isInitialLaunch = false;
        int keyCode = event.getKeyCode();

        // 1. Dedicated remote Settings key interception
        if (ZenithAccessibilityService.isSettingsKey(keyCode)) {
            if (event.getAction() == 0) {
                if (this.isReorderMode) {
                    this.stopReorderMode();
                    return true;
                }
                if (wasDialogRecentlyDismissed() || wasRecentlyReordering()) {
                    return true;
                }
                long now = System.currentTimeMillis();
                if (now - this.lastSettingsPressTime > 350L) {
                    this.lastSettingsPressTime = now;
                    Log.d("MainActivity", "Settings key (" + keyCode + ") intercepted! Toggling quick settings");
                    TvUtils.openQuickSettings((Context)this);
                }
            }
            return true;
        }

        if (this.isReorderMode) {
            if (event.getAction() == 0) {
                this.handleReorderKey(keyCode);
            }
            if (keyCode == 21 || keyCode == 22 || keyCode == 19 || keyCode == 20 || keyCode == 23 || keyCode == 66 || keyCode == 4) {
                return true;
            }
        }
        if (!(SystemClock.uptimeMillis() - lastReorderEndTime >= 500L && SystemClock.uptimeMillis() - lastActionDismissTime >= 500L || keyCode != 23 && keyCode != 66)) {
            return true;
        }
        View currentFocused = this.getCurrentFocus();
        if (currentFocused == null || (wasDialogRecentlyDismissed() && this.isTopBarOrSettingsView(currentFocused))) {
            this.ensureFocus();
            if (keyCode == 23 || keyCode == 66) {
                return true;
            }
        }
        if (event.getAction() == 0) {
            View focused = this.getCurrentFocus();
            if (focused != null) {
                if (this.isSidebarView(focused) && keyCode == 22) {
                    this.returnFocusToActiveContent();
                    return true;
                }
                if (keyCode == 21 && !this.isSidebarView(focused) && this.shouldMoveToSidebar(focused)) {
                    this.moveToSidebar(this.activeSection);
                    return true;
                }
                if (keyCode == 20) {
                    if (this.recyclerAllApps != null && this.isChildOf(focused, (View)this.recyclerAllApps)) {
                        return true;
                    }
                    if (this.navBtnSettings != null && (focused == this.navBtnSettings || focused.getId() == R.id.nav_btn_settings)) {
                        return true;
                    }
                }
            }
            switch (keyCode) {
                case 4: {
                    if (this.isReorderMode) {
                        this.stopReorderMode();
                        return true;
                    }
                    this.onBackPressed();
                    return true;
                }
                case 178: {
                    TvUtils.openInputs((Context)this);
                    return true;
                }
                case 84: {
                    TvUtils.openVoiceSearch((Context)this);
                    return true;
                }
                case 183: {
                    this.shuffleRecommendationMovie(true);
                    return true;
                }
            }
        }
        return super.dispatchKeyEvent(event);
    }

    protected void onResume() {
        super.onResume();
        this.isActivityResumed = true;
        com.zenith.launcher.receiver.BootReceiver.ensureAccessibilityServiceEnabled(this);
        this.hideSystemUI();
        this.resetScreensaverTimer();
        this.restartAutoShuffleTimer();
        this.updateClock();
        this.syncInstalledApps(false);
        this.handler.postDelayed(() -> {
            if (this.isActivityResumed && !this.isReorderMode) {
                if (this.isInitialLaunch || this.activeSection == 0) {
                    this.focusRow1();
                } else {
                    this.ensureFocus();
                }
            }
        }, 100L);
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.isActivityResumed = false;
        this.handler.removeCallbacks(this.screensaverRunnable);
        this.handler.removeCallbacks(this.autoShuffleRunnable);
        this.handler.removeCallbacks(this.hideTooltipRunnable);
    }

    @Override
    protected void onStop() {
        super.onStop();
        this.isActivityResumed = false;
    }

    protected void onDestroy() {
        if (sInstance == this) {
            sInstance = null;
        }
        super.onDestroy();
        this.teardownPackageSyncListeners();
        this.appSyncExecutor.shutdownNow();
        this.handler.removeCallbacksAndMessages(null);
    }

    static {
        lastReorderEndTime = 0L;
        lastActionDismissTime = 0L;
    }
}
