package com.zenith.launcher.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.zenith.launcher.R;
import com.zenith.launcher.model.AppItem;
import com.zenith.launcher.view.SquircleCardView;

import java.util.List;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.ViewHolder> {

    public interface OnAppFocusListener {
        void onAppFocused(AppItem app);
    }

    public interface OnAppLongClickListener {
        void onAppLongClicked(AppItem app, int position, boolean isFavorite);
    }

    private final Context context;
    private final List<AppItem> items;
    private final boolean isFavoriteRow;
    private final OnAppFocusListener focusListener;
    private final OnAppLongClickListener longClickListener;
    private int reorderingPosition = -1;

    public AppAdapter(Context context, List<AppItem> items, boolean isFavoriteRow, OnAppFocusListener focusListener, OnAppLongClickListener longClickListener) {
        this.context = context;
        this.items = items;
        this.isFavoriteRow = isFavoriteRow;
        this.focusListener = focusListener;
        this.longClickListener = longClickListener;
    }

    public void setReorderingPosition(int pos) {
        int oldPos = this.reorderingPosition;
        this.reorderingPosition = pos;
        if (pos == -1) {
            notifyDataSetChanged();
        } else {
            if (oldPos != -1 && oldPos < getItemCount()) {
                notifyItemChanged(oldPos);
            }
            if (pos >= 0 && pos < getItemCount()) {
                notifyItemChanged(pos);
            }
        }
    }

    public int getReorderingPosition() {
        return reorderingPosition;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_app_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppItem item = items.get(position);

        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) holder.itemView.getLayoutParams();
        if (params != null) {
            int marginEnd = (position == getItemCount() - 1) ? 0 : (int) (12 * context.getResources().getDisplayMetrics().density);
            if (params.rightMargin != marginEnd) {
                params.rightMargin = marginEnd;
                params.setMarginEnd(marginEnd);
                holder.itemView.setLayoutParams(params);
            }
        }

        if (item.getBanner() != null) {
            holder.appIcon.setImageDrawable(item.getBanner());
            holder.appIcon.setPadding(0, 0, 0, 0);
            holder.appTitle.setVisibility(View.GONE);
        } else if (item.getIcon() != null) {
            holder.appIcon.setImageDrawable(item.getIcon());
            int p = (int) ((isFavoriteRow ? 10 : 14) * context.getResources().getDisplayMetrics().density);
            holder.appIcon.setPadding(p, p, p, p);
            holder.appTitle.setVisibility(View.GONE);
        } else {
            holder.appIcon.setImageDrawable(null);
            holder.appTitle.setText(item.getLabel());
            holder.appTitle.setVisibility(View.VISIBLE);
        }

        boolean isMoving = (position == reorderingPosition);
        boolean isFocused = holder.cardRoot.hasFocus() || isMoving;

        float targetScale = isMoving ? 1.10f : (isFocused ? 1.08f : 1.0f);
        holder.cardRoot.setScaleX(targetScale);
        holder.cardRoot.setScaleY(targetScale);
        holder.cardRoot.setAlpha(isMoving ? 1.0f : (isFocused ? 1.0f : 0.50f));
        holder.appIcon.setScaleX(1.0f);
        holder.appIcon.setScaleY(1.0f);

        holder.cardRoot.setCardFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && focusListener != null) {
                focusListener.onAppFocused(item);
            }
        });

        holder.cardRoot.setOnClickListener(v -> {
            if (reorderingPosition != -1 || com.zenith.launcher.MainActivity.isGlobalReorderActive() || com.zenith.launcher.MainActivity.wasRecentlyReordering() || com.zenith.launcher.MainActivity.wasDialogRecentlyDismissed()) {
                // If clicked while reordering or right after dialog dismissal, ignore standard app launch!
                return;
            }
            Intent intent = context.getPackageManager().getLaunchIntentForPackage(item.getPackageName());
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                context.startActivity(intent);
            }
        });

        holder.cardRoot.setOnLongClickListener(v -> {
            if (reorderingPosition != -1 || com.zenith.launcher.MainActivity.isGlobalReorderActive() || com.zenith.launcher.MainActivity.wasRecentlyReordering() || com.zenith.launcher.MainActivity.wasDialogRecentlyDismissed()) {
                return true;
            }
            if (longClickListener != null) {
                int pos = holder.getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    longClickListener.onAppLongClicked(item, pos, isFavoriteRow);
                    return true;
                }
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final SquircleCardView cardRoot;
        final ImageView appIcon;
        final TextView appTitle;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.card_root);
            appIcon = itemView.findViewById(R.id.app_icon);
            appTitle = itemView.findViewById(R.id.app_title);
        }
    }
}
