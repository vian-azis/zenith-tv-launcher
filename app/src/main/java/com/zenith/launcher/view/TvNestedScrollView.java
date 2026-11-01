package com.zenith.launcher.view;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;

/**
 * Custom NestedScrollView for Android TV:
 * - Suppresses incremental micro-scrolling when navigating between horizontal rows.
 * - Allows deterministic, smooth snap-scrolling to rows/shelves.
 */
public class TvNestedScrollView extends NestedScrollView {

    public TvNestedScrollView(@NonNull Context context) {
        super(context);
    }

    public TvNestedScrollView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public TvNestedScrollView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean requestChildRectangleOnScreen(View child, Rect rectangle, boolean immediate) {
        // Suppress Android's default focus micro-scrolling so our smart TV shelf alignment can handle it cleanly
        return false;
    }
}

