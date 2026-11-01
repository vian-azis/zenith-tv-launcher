package com.zenith.launcher.model;

import org.json.JSONObject;

public class MovieItem {
    private final String title;
    private final String year;
    private final String rating;
    private final String duration;
    private final String genres;
    private final String quality;
    private final String description;
    private final String backdropUrl;
    private final int fallbackResId;
    private final String targetPackage;
    private final String searchKeyword;

    public MovieItem(String title, String year, String rating, String duration, String genres,
                     String quality, String description, String backdropUrl, int fallbackResId,
                     String targetPackage, String searchKeyword) {
        this.title = title;
        this.year = year;
        this.rating = rating;
        this.duration = duration;
        this.genres = genres;
        this.quality = quality;
        this.description = description;
        this.backdropUrl = backdropUrl;
        this.fallbackResId = fallbackResId;
        this.targetPackage = targetPackage;
        this.searchKeyword = searchKeyword;
    }

    public String getTitle() { return title; }
    public String getYear() { return year; }
    public String getRating() { return rating; }
    public String getDuration() { return duration; }
    public String getGenres() { return genres; }
    public String getQuality() { return quality; }
    public String getDescription() { return description; }
    public String getBackdropUrl() { return backdropUrl; }
    public int getFallbackResId() { return fallbackResId; }
    public String getTargetPackage() { return targetPackage; }
    public String getSearchKeyword() { return searchKeyword; }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("title", title);
            obj.put("year", year);
            obj.put("rating", rating);
            obj.put("duration", duration);
            obj.put("genres", genres);
            obj.put("quality", quality);
            obj.put("description", description);
            obj.put("backdropUrl", backdropUrl);
            obj.put("fallbackResId", fallbackResId);
            obj.put("targetPackage", targetPackage);
            obj.put("searchKeyword", searchKeyword);
        } catch (Exception ignored) {}
        return obj;
    }

    public static MovieItem fromJson(JSONObject obj) {
        if (obj == null) return null;
        return new MovieItem(
            obj.optString("title", ""),
            obj.optString("year", "2024"),
            obj.optString("rating", "8.0"),
            obj.optString("duration", "2j 0m"),
            obj.optString("genres", "Drama"),
            obj.optString("quality", "4K ULTRA HD"),
            obj.optString("description", ""),
            obj.optString("backdropUrl", ""),
            obj.optInt("fallbackResId", 0),
            obj.optString("targetPackage", "com.google.android.youtube.tv"),
            obj.optString("searchKeyword", obj.optString("title", ""))
        );
    }
}
