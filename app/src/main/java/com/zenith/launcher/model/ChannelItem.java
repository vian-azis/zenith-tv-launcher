package com.zenith.launcher.model;

import android.content.Intent;
import android.graphics.drawable.Drawable;

public class ChannelItem {
    private final String title;
    private final String subtitle;
    private final String badge;
    private final Drawable thumbnail;
    private final String imageUrl;
    private final Intent launchIntent;

    public ChannelItem(String title, String subtitle, String badge, Drawable thumbnail, String imageUrl, Intent launchIntent) {
        this.title = title;
        this.subtitle = subtitle;
        this.badge = badge;
        this.thumbnail = thumbnail;
        this.imageUrl = imageUrl;
        this.launchIntent = launchIntent;
    }

    public ChannelItem(String title, String subtitle, String badge, Drawable thumbnail, Intent launchIntent) {
        this(title, subtitle, badge, thumbnail, null, launchIntent);
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getBadge() {
        return badge;
    }

    public Drawable getThumbnail() {
        return thumbnail;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Intent getLaunchIntent() {
        return launchIntent;
    }
}
