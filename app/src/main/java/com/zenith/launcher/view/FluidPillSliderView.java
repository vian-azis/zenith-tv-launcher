package com.zenith.launcher.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Outline;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import com.zenith.launcher.R;

/**
 * Enterprise Integrated Fluid Pill Slider (tvOS / iOS Control Center style).
 * Full-Row fluid fill without separate seekbar boxes or circle thumbs.
 * Zero-scale focus with instant tabular percentage and seamless D-pad controls.
 */
public class FluidPillSliderView extends FrameLayout {

    public interface OnSliderChangeListener {
        void onProgressChanged(FluidPillSliderView slider, int progress, boolean fromUser);
    }

    private View mFillView;
    private View mIndicatorView;
    private ImageView mIconView;
    private TextView mLabelView;
    private TextView mValueView;

    private int mProgress = 0;
    private OnSliderChangeListener mListener;

    public FluidPillSliderView(Context context) {
        super(context);
        init(context);
    }

    public FluidPillSliderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public FluidPillSliderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setFocusable(true);
        setFocusableInTouchMode(true);
        setClickable(true);

        final int cornerRadiusPx = dpToPx(14);

        // Enforce rounded capsule outline clipping
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), cornerRadiusPx);
                }
            });
            setClipToOutline(true);
        }

        setBackgroundResource(R.drawable.bg_fluid_slider_idle);

        // Layer 2: Fluid Progress Fill Bar Container
        FrameLayout fillContainer = new FrameLayout(context);
        LayoutParams fillContainerParams = new LayoutParams(0, LayoutParams.MATCH_PARENT);
        fillContainerParams.gravity = Gravity.START | Gravity.TOP;
        addView(fillContainer, fillContainerParams);
        mFillView = fillContainer;
        mFillView.setBackgroundResource(R.drawable.bg_fluid_slider_fill_idle);

        // Indicator line on the right edge of fill
        mIndicatorView = new View(context);
        mIndicatorView.setBackgroundResource(R.drawable.bg_fluid_slider_indicator);
        LayoutParams indicatorParams = new LayoutParams(dpToPx(3), dpToPx(18));
        indicatorParams.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
        indicatorParams.rightMargin = dpToPx(6);
        fillContainer.addView(mIndicatorView, indicatorParams);
        mIndicatorView.setVisibility(View.GONE);

        // Layer 3: Synchronized Content (Icon + Label + Percentage Value)
        LinearLayout contentLayout = new LinearLayout(context);
        contentLayout.setOrientation(LinearLayout.HORIZONTAL);
        contentLayout.setGravity(Gravity.CENTER_VERTICAL);
        contentLayout.setPadding(dpToPx(16), 0, dpToPx(16), 0);
        LayoutParams contentParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        addView(contentLayout, contentParams);

        // Icon
        mIconView = new ImageView(context);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dpToPx(20), dpToPx(20));
        mIconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        mIconView.setImageTintList(ColorStateList.valueOf(0xFFF4F4F5));
        contentLayout.addView(mIconView, iconParams);

        // Label
        mLabelView = new TextView(context);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        labelParams.leftMargin = dpToPx(12);
        mLabelView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        mLabelView.setLetterSpacing(0.01f);
        mLabelView.setIncludeFontPadding(false);
        mLabelView.setTextColor(0xFFF4F4F5);
        Typeface tfSemiBold = loadFont(R.font.poppins_medium);
        if (tfSemiBold != null) mLabelView.setTypeface(tfSemiBold);
        contentLayout.addView(mLabelView, labelParams);

        // Flexible spacer
        View spacer = new View(context);
        LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(
                0, LayoutParams.MATCH_PARENT, 1.0f);
        contentLayout.addView(spacer, spacerParams);

        // Percentage Value (Right aligned tabular)
        mValueView = new TextView(context);
        LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        mValueView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        mValueView.setFontFeatureSettings("tnum");
        mValueView.setIncludeFontPadding(false);
        mValueView.setMinWidth(dpToPx(40));
        mValueView.setTextColor(0xFFA1A1AA);
        mValueView.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        Typeface tfBold = loadFont(R.font.poppins_semibold);
        if (tfBold != null) mValueView.setTypeface(tfBold);
        mValueView.setText("0%");
        contentLayout.addView(mValueView, valueParams);

        // Enforce 100% Zero-Scale
        setScaleX(1.0f);
        setScaleY(1.0f);
    }

    private Typeface loadFont(int fontRes) {
        try {
            return ResourcesCompat.getFont(getContext(), fontRes);
        } catch (Exception e) {
            return Typeface.SANS_SERIF;
        }
    }

    public void setSliderIcon(int drawableRes) {
        if (mIconView != null) {
            mIconView.setImageResource(drawableRes);
        }
    }

    public void setSliderLabel(String label) {
        if (mLabelView != null) {
            mLabelView.setText(label);
        }
    }

    public void setOnSliderChangeListener(OnSliderChangeListener listener) {
        mListener = listener;
    }

    public int getProgress() {
        return mProgress;
    }

    public void setProgress(int progress) {
        int clamped = Math.max(0, Math.min(100, progress));
        mProgress = clamped;
        if (mValueView != null) {
            mValueView.setText(clamped + "%");
        }
        updateFillWidth();
    }

    private void updateFillWidth() {
        if (mFillView == null) return;
        int width = getWidth();
        if (width <= 0) {
            post(this::updateFillWidth);
            return;
        }
        int targetWidth = (int) Math.round(width * (mProgress / 100.0f));
        LayoutParams params = (LayoutParams) mFillView.getLayoutParams();
        params.width = targetWidth;
        mFillView.setLayoutParams(params);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateFillWidth();
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);

        // ATURAN 1: 100% Zero-Scale Focus
        setScaleX(1.0f);
        setScaleY(1.0f);
        setTranslationZ(0);

        if (gainFocus) {
            setBackgroundResource(R.drawable.bg_fluid_slider_focused);
            if (mFillView != null) {
                mFillView.setBackgroundResource(R.drawable.bg_fluid_slider_fill_focused);
            }
            if (mIndicatorView != null) {
                mIndicatorView.setVisibility(View.VISIBLE);
            }
            if (mIconView != null) {
                mIconView.setImageTintList(ColorStateList.valueOf(0xFF18181B));
            }
            if (mLabelView != null) {
                mLabelView.setTextColor(0xFF18181B);
            }
            if (mValueView != null) {
                mValueView.setTextColor(0xFF18181B);
            }
        } else {
            setBackgroundResource(R.drawable.bg_fluid_slider_idle);
            if (mFillView != null) {
                mFillView.setBackgroundResource(R.drawable.bg_fluid_slider_fill_idle);
            }
            if (mIndicatorView != null) {
                mIndicatorView.setVisibility(View.GONE);
            }
            if (mIconView != null) {
                mIconView.setImageTintList(ColorStateList.valueOf(0xFFF4F4F5));
            }
            if (mLabelView != null) {
                mLabelView.setTextColor(0xFFF4F4F5);
            }
            if (mValueView != null) {
                mValueView.setTextColor(0xFFA1A1AA);
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                int next = Math.max(0, mProgress - 5);
                setProgress(next);
                if (mListener != null) {
                    mListener.onProgressChanged(this, next, true);
                }
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                int next = Math.min(100, mProgress + 5);
                setProgress(next);
                if (mListener != null) {
                    mListener.onProgressChanged(this, next, true);
                }
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }
}

