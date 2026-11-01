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
import com.zenith.launcher.model.ChannelItem;
import com.zenith.launcher.view.SquircleCardView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import java.util.List;

public class ChannelAdapter extends RecyclerView.Adapter<ChannelAdapter.ViewHolder> {

    public interface OnChannelFocusListener {
        void onChannelFocused(ChannelItem channel);
    }

    private final Context context;
    private final List<ChannelItem> items;
    private final OnChannelFocusListener focusListener;

    public ChannelAdapter(Context context, List<ChannelItem> items, OnChannelFocusListener focusListener) {
        this.context = context;
        this.items = items;
        this.focusListener = focusListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_channel_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChannelItem item = items.get(position);

        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            Glide.with(context)
                .load(item.getImageUrl())
                .placeholder(item.getThumbnail())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .transition(DrawableTransitionOptions.withCrossFade())
                .centerCrop()
                .into(holder.channelThumb);
        } else {
            holder.channelThumb.setImageDrawable(item.getThumbnail());
        }

        holder.channelBadge.setText(item.getBadge());
        holder.videoTitle.setText(item.getTitle());
        holder.videoSubtitle.setText(item.getSubtitle());

        boolean isFocused = holder.cardRoot.hasFocus();
        holder.cardRoot.setScaleX(isFocused ? 1.08f : 1.0f);
        holder.cardRoot.setScaleY(isFocused ? 1.08f : 1.0f);
        holder.cardRoot.setAlpha(isFocused ? 1.0f : 0.70f);
        holder.channelThumb.setScaleX(isFocused ? 1.10f : 1.0f);
        holder.channelThumb.setScaleY(isFocused ? 1.10f : 1.0f);
        holder.videoTitle.setAlpha(isFocused ? 1.0f : 0.70f);
        holder.videoSubtitle.setAlpha(isFocused ? 0.90f : 0.50f);

        holder.cardRoot.setCardFocusChangeListener((v, hasFocus) -> {
            holder.videoTitle.animate().alpha(hasFocus ? 1.0f : 0.70f).setDuration(180).start();
            holder.videoSubtitle.animate().alpha(hasFocus ? 0.90f : 0.50f).setDuration(180).start();
            if (hasFocus && focusListener != null) {
                focusListener.onChannelFocused(item);
            }
        });

        holder.cardRoot.setOnClickListener(v -> {
            if (item.getLaunchIntent() != null) {
                item.getLaunchIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    context.startActivity(item.getLaunchIntent());
                } catch (Exception ignored) {
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final SquircleCardView cardRoot;
        final ImageView channelThumb;
        final TextView channelBadge;
        final TextView videoTitle;
        final TextView videoSubtitle;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.channel_card_root);
            channelThumb = itemView.findViewById(R.id.channel_thumb);
            channelBadge = itemView.findViewById(R.id.channel_badge);
            videoTitle = itemView.findViewById(R.id.video_title);
            videoSubtitle = itemView.findViewById(R.id.video_subtitle);
        }
    }
}
