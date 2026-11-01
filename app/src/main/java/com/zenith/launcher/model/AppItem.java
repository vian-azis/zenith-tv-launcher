package com.zenith.launcher.model;

import android.graphics.drawable.Drawable;

public class AppItem {
    private final String packageName;
    private final String activityName;
    private final String label;
    private final Drawable icon;
    private final Drawable banner;
    private final String tagline;
    private final String category;
    private boolean isFavorite;
    private AppActionCapabilities capabilities;

    public AppItem(String packageName, String activityName, String label, Drawable icon, Drawable banner, String tagline, String category, boolean isFavorite) {
        this(packageName, activityName, label, icon, banner, tagline, category, isFavorite, null);
    }

    public AppItem(String packageName, String activityName, String label, Drawable icon, Drawable banner, String tagline, String category, boolean isFavorite, AppActionCapabilities capabilities) {
        this.packageName = packageName != null ? packageName : "";
        this.activityName = activityName != null ? activityName : "";
        this.label = label != null ? label : "";
        this.icon = icon;
        this.banner = banner;
        this.tagline = tagline != null ? tagline : "";
        this.category = category != null ? category : "";
        this.isFavorite = isFavorite;
        this.capabilities = capabilities;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getActivityName() {
        return activityName;
    }

    public String getLabel() {
        return label;
    }

    public Drawable getIcon() {
        return icon;
    }

    public Drawable getBanner() {
        return banner;
    }

    public String getTagline() {
        return tagline;
    }

    public String getCategory() {
        return category;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    public AppActionCapabilities getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(AppActionCapabilities capabilities) {
        this.capabilities = capabilities;
    }
}
