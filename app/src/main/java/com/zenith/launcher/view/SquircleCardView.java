package com.zenith.launcher.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.zenith.launcher.R;

public class SquircleCardView extends FrameLayout {

    public interface OnCardFocusChangeListener {
        void onCardFocusChange(View view, boolean hasFocus);
    }

    private final DecelerateInterpolator interpolator = new DecelerateInterpolator(2.0f);
    private float cornerRadius = 0f;
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF borderRect = new RectF();
    private final RectF innerRect = new RectF();
    private float density = 1f;
    private OnCardFocusChangeListener cardFocusChangeListener;

    public SquircleCardView(@NonNull Context context) {
        super(context);
        init();
    }

    public SquircleCardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SquircleCardView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setFocusable(true);
        setFocusableInTouchMode(true);
        setClickable(true);
        setLongClickable(true);
        setClipChildren(false);

        density = getResources().getDisplayMetrics().density;
        // 14dp rounded squircle corners matching Zenith TV reference
        cornerRadius = 14f * density;

        borderPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStyle(Paint.Style.STROKE);

        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), cornerRadius);
            }
        });
        setClipToOutline(true);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            setOutlineSpotShadowColor(0x90000000);
            setOutlineAmbientShadowColor(0x50000000);
        }

        // 50% opacity in idle/unfocused state as requested
        setAlpha(0.50f);
    }

    public void setCardFocusChangeListener(OnCardFocusChangeListener listener) {
        this.cardFocusChangeListener = listener;
    }

    private View findInnerMediaView() {
        View v = findViewById(R.id.app_icon);
        if (v != null) return v;
        v = findViewById(R.id.channel_thumb);
        if (v != null) return v;
        if (getChildCount() > 0) return getChildAt(0);
        return null;
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, @Nullable Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);

        View innerMedia = findInnerMediaView();

        if (gainFocus) {
            android.view.ViewParent p = getParent();
            if (p instanceof android.view.ViewGroup) {
                ((android.view.ViewGroup) p).bringToFront();
            }
            if (!(p instanceof android.widget.LinearLayout)) {
                bringToFront();
            }
            animate()
                .scaleX(1.08f)
                .scaleY(1.08f)
                .alpha(1.0f)
                .translationZ(18f * density)
                .setDuration(220)
                .setInterpolator(interpolator)
                .start();
        } else {
            animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(0.50f)
                .translationZ(0f)
                .setDuration(180)
                .setInterpolator(interpolator)
                .start();
        }

        invalidate();

        if (cardFocusChangeListener != null) {
            cardFocusChangeListener.onCardFocusChange(this, gainFocus);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
        if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER || keyCode == android.view.KeyEvent.KEYCODE_ENTER) {
            event.startTracking();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, android.view.KeyEvent event) {
        if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER || keyCode == android.view.KeyEvent.KEYCODE_ENTER) {
            return performLongClick();
        }
        return super.onKeyLongPress(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, android.view.KeyEvent event) {
        if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER || keyCode == android.view.KeyEvent.KEYCODE_ENTER) {
            if ((event.getFlags() & android.view.KeyEvent.FLAG_CANCELED_LONG_PRESS) == 0) {
                return performClick();
            }
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        if (hasFocus()) {
            // Sophisticated Zenith TV frosted specular rim (NOT a harsh opaque stroke)
            float inset = 0.75f * density;
            borderRect.set(inset, inset, getWidth() - inset, getHeight() - inset);
            borderPaint.setStrokeWidth(1.2f * density);
            borderPaint.setColor(0x38FFFFFF);
            canvas.drawRoundRect(borderRect, cornerRadius - inset, cornerRadius - inset, borderPaint);
        } else {
            // Subtle hairline in idle state
            float inset = 0.5f * density;
            borderRect.set(inset, inset, getWidth() - inset, getHeight() - inset);
            borderPaint.setStrokeWidth(0.6f * density);
            borderPaint.setColor(0x0EFFFFFF);
            canvas.drawRoundRect(borderRect, cornerRadius - inset, cornerRadius - inset, borderPaint);
        }
    }
}
