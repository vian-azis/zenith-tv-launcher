package com.zenith.launcher.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewParent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.zenith.launcher.R;

/**
 * Enterprise Android TV Optimized Horizontal Grid:
 * - Active/focused card draws completely unclipped above parent dock.
 * - Non-focused cards remain strictly clipped inside the parent dock capsule.
 * - Focused card is always drawn last in child drawing order so it sits above neighbors.
 * - High-speed repeat & fluid single-tap navigation with zero focus drop:
 *   Holding or rapidly tapping DPAD_RIGHT seamlessly navigates through all cards
 *   (revealing hidden cards on the right smoothly) without ever getting stuck or jumping to sidebar.
 * - Exit left to sidebar navigation rail ONLY from position 0.
 * - Last card in row firmly locks focus without leaking to window or parent.
 */
public class TvHorizontalGridView extends RecyclerView {

    /**
     * TV Optimized LayoutManager with 1200px off-screen prefetch & extra layout space.
     * Guarantees off-screen cards are pre-bound and instantly focusable before scrolling.
     */
    public static class TvHorizontalLayoutManager extends LinearLayoutManager {
        public TvHorizontalLayoutManager(Context context) {
            super(context, HORIZONTAL, false);
            setItemPrefetchEnabled(true);
            setInitialPrefetchItemCount(8);
        }

        public TvHorizontalLayoutManager(Context context, int orientation, boolean reverseLayout) {
            super(context, orientation, reverseLayout);
            setItemPrefetchEnabled(true);
            setInitialPrefetchItemCount(8);
        }

        @Override
        protected void calculateExtraLayoutSpace(@NonNull State state, @NonNull int[] extraLayoutSpace) {
            // Pre-layout 600px left and 1200px right (full screen width!)
            extraLayoutSpace[0] = 600;
            extraLayoutSpace[1] = 1200;
        }
    }

    private final Path dockClipPath = new Path();
    private final RectF dockRect = new RectF();
    private int currentTargetPos = NO_POSITION;
    private long lastNavTime = 0;
    private static final long NAV_REPEAT_INTERVAL_MS = 60; // 60ms fluid repeat interval

    public TvHorizontalGridView(@NonNull Context context) {
        super(context);
        init();
    }

    public TvHorizontalGridView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TvHorizontalGridView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
        setHasFixedSize(true);
        setClipChildren(false);
        setClipToPadding(false);
        setItemAnimator(null);
        setChildrenDrawingOrderEnabled(true);

        if (getLayoutManager() == null) {
            setLayoutManager(new TvHorizontalLayoutManager(getContext()));
        }

        // Listener to safely restore focus if a focused child is ever detached
        addOnChildAttachStateChangeListener(new OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(@NonNull View view) {
            }

            @Override
            public void onChildViewDetachedFromWindow(@NonNull View view) {
                if (view.hasFocus()) {
                    post(() -> {
                        if (findFocus() == null) {
                            int count = getChildCount();
                            if (count > 0) {
                                View fallback = getChildAt(count - 1);
                                if (fallback != null) {
                                    focusCardAt(fallback);
                                }
                            }
                        }
                    });
                }
            }
        });
    }

    @Override
    public void setLayoutManager(@Nullable LayoutManager layout) {
        if (layout instanceof LinearLayoutManager && !(layout instanceof TvHorizontalLayoutManager)) {
            LinearLayoutManager original = (LinearLayoutManager) layout;
            layout = new TvHorizontalLayoutManager(getContext(), original.getOrientation(), original.getReverseLayout());
        }
        super.setLayoutManager(layout);
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        int focusedIndex = -1;
        for (int j = 0; j < childCount; j++) {
            View child = getChildAt(j);
            if (child != null && child.hasFocus()) {
                focusedIndex = j;
                break;
            }
        }
        if (focusedIndex == -1) {
            return i;
        }
        if (i == childCount - 1) {
            return focusedIndex;
        }
        if (i >= focusedIndex) {
            return i + 1;
        }
        return i;
    }

    @Override
    public boolean drawChild(Canvas canvas, View child, long drawingTime) {
        ViewParent parentView = getParent();
        if (parentView instanceof View) {
            int parentId = ((View) parentView).getId();
            if (parentId == R.id.container_favorites_dock || parentId == R.id.container_all_apps_dock) {
                View dock = (View) parentView;
                if (!child.hasFocus()) {
                    // Non-focused cards: strictly clipped inside the parent dock capsule
                    float left = -getLeft();
                    float top = -getTop();
                    float right = left + dock.getWidth();
                    float bottom = top + dock.getHeight();
                    float radius = 24f * getResources().getDisplayMetrics().density;

                    dockClipPath.reset();
                    dockRect.set(left, top, right, bottom);
                    dockClipPath.addRoundRect(dockRect, radius, radius, Path.Direction.CW);

                    int saveCount = canvas.save();
                    canvas.clipPath(dockClipPath);
                    boolean result = super.drawChild(canvas, child, drawingTime);
                    canvas.restoreToCount(saveCount);
                    return result;
                }
                // Active/focused card: unclipped, floats above parent dock
            }
        }
        return super.drawChild(canvas, child, drawingTime);
    }

    private void ensureChildVisible(View child) {
        if (child == null) return;
        int boxStart = getPaddingLeft();
        int boxEnd = getWidth() - getPaddingRight();

        int childLeft = child.getLeft();
        int childRight = child.getRight();

        int dx = 0;
        int extraMargin = dpToPx(24);

        if (childRight > boxEnd) {
            dx = (childRight - boxEnd) + extraMargin;
        } else if (childLeft < boxStart) {
            dx = (childLeft - boxStart) - extraMargin;
        }

        if (dx != 0) {
            smoothScrollBy(dx, 0);
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    public boolean requestChildRectangleOnScreen(View child, Rect rect, boolean immediate) {
        if (child == null) return false;
        ensureChildVisible(child);
        return true;
    }

    public int getCurrentFocusedPosition() {
        View focused = findFocus();
        if (focused != null) {
            View itemView = findContainingItemView(focused);
            if (itemView != null) {
                int pos = getChildAdapterPosition(itemView);
                if (pos != NO_POSITION) {
                    currentTargetPos = pos;
                    return pos;
                }
            }
        }
        if (currentTargetPos != NO_POSITION) {
            return currentTargetPos;
        }
        int childCount = getChildCount();
        if (childCount > 0) {
            View first = getChildAt(0);
            if (first != null) {
                int pos = getChildAdapterPosition(first);
                if (pos != NO_POSITION) {
                    currentTargetPos = pos;
                    return pos;
                }
            }
        }
        return NO_POSITION;
    }

    private View getCardRoot(View itemView) {
        if (itemView == null) return null;
        View card = itemView.findViewById(R.id.card_root);
        if (card == null) card = itemView.findViewById(R.id.channel_card_root);
        if (card == null) card = itemView;
        return card;
    }

    private void focusCardAt(View itemView) {
        if (itemView == null) return;
        View card = getCardRoot(itemView);
        if (card != null && !card.hasFocus()) {
            card.requestFocus();
        }
    }

    public void navigateToPosition(int targetPos) {
        if (getAdapter() == null) return;
        if (targetPos < 0 || targetPos >= getAdapter().getItemCount()) return;

        currentTargetPos = targetPos;

        ViewHolder vh = findViewHolderForAdapterPosition(targetPos);
        if (vh != null && vh.itemView != null) {
            focusCardAt(vh.itemView);
            ensureChildVisible(vh.itemView);
        } else {
            // Target view is currently off-screen: scroll to it immediately and focus upon arrival
            scrollToPosition(targetPos);
            post(() -> {
                ViewHolder scrolledVh = findViewHolderForAdapterPosition(targetPos);
                if (scrolledVh != null && scrolledVh.itemView != null) {
                    focusCardAt(scrolledVh.itemView);
                    ensureChildVisible(scrolledVh.itemView);
                }
            });
        }
    }

    public void smoothScrollToPositionSmooth(int position) {
        navigateToPosition(position);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int keyCode = event.getKeyCode();
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                int currentPos = getCurrentFocusedPosition();
                if (currentPos != NO_POSITION && getAdapter() != null) {
                    int total = getAdapter().getItemCount();
                    if (currentPos < total - 1) {
                        long now = SystemClock.uptimeMillis();
                        if (now - lastNavTime >= NAV_REPEAT_INTERVAL_MS) {
                            lastNavTime = now;
                            int nextPos = currentPos + 1;
                            navigateToPosition(nextPos);
                        }
                        return true; // Always consume: focus remains locked inside row
                    } else {
                        // At the very end of row: keep focus firmly locked on last item
                        currentTargetPos = total - 1;
                        return true; // Always consume: never leak to sidebar, top bar, or parent!
                    }
                }
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                int currentPos = getCurrentFocusedPosition();
                if (currentPos != NO_POSITION && getAdapter() != null) {
                    if (currentPos > 0) {
                        long now = SystemClock.uptimeMillis();
                        if (now - lastNavTime >= NAV_REPEAT_INTERVAL_MS) {
                            lastNavTime = now;
                            int prevPos = currentPos - 1;
                            navigateToPosition(prevPos);
                        }
                        return true; // Always consume: focus remains locked inside row
                    } else if (currentPos == 0) {
                        // At pos 0: reset target and allow exit left to sidebar navigation rail
                        currentTargetPos = NO_POSITION;
                        return super.dispatchKeyEvent(event);
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public View focusSearch(View focused, int direction) {
        if (direction == View.FOCUS_RIGHT) {
            int pos = getCurrentFocusedPosition();
            if (pos != NO_POSITION && getAdapter() != null) {
                int total = getAdapter().getItemCount();
                if (pos >= total - 1) {
                    return focused; // Lock focus at end of row
                }
                int nextPos = pos + 1;
                ViewHolder vh = findViewHolderForAdapterPosition(nextPos);
                if (vh != null && vh.itemView != null) {
                    View target = getCardRoot(vh.itemView);
                    if (target != null) {
                        return target;
                    }
                }
                navigateToPosition(nextPos);
                return focused;
            }
            return focused;
        } else if (direction == View.FOCUS_LEFT) {
            int pos = getCurrentFocusedPosition();
            if (pos != NO_POSITION && getAdapter() != null) {
                if (pos == 0) {
                    currentTargetPos = NO_POSITION;
                    return super.focusSearch(focused, direction); // Exit to sidebar
                } else if (pos > 0) {
                    int prevPos = pos - 1;
                    ViewHolder vh = findViewHolderForAdapterPosition(prevPos);
                    if (vh != null && vh.itemView != null) {
                        View target = getCardRoot(vh.itemView);
                        if (target != null) {
                            return target;
                        }
                    }
                    navigateToPosition(prevPos);
                    return focused;
                }
            }
            return focused;
        }
        return super.focusSearch(focused, direction);
    }
}
