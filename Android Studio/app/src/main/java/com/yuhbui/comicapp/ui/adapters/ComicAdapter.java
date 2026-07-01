package com.yuhbui.comicapp.ui.adapters;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.yuhbui.comicapp.R;
import com.yuhbui.comicapp.data.model.Comic;
import com.yuhbui.comicapp.ui.ComicDetailActivity;
import java.util.ArrayList;
import java.util.List;

public class ComicAdapter extends RecyclerView.Adapter<ComicAdapter.ComicViewHolder> {

    private List<Comic> comicList = new ArrayList<>();
    private boolean isListView = false;
    private boolean isDownloadMode = false;
    private OnItemClickListener onItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(Comic comic);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public void setDownloadMode(boolean isDownloadMode) {
        this.isDownloadMode = isDownloadMode;
        notifyDataSetChanged();
    }

    public ComicAdapter() {
        this.isListView = false;
    }

    public ComicAdapter(boolean isListView) {
        this.isListView = isListView;
    }

    public void setComics(List<Comic> comics) {
        this.comicList = comics;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ComicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = isListView ? R.layout.item_comic_full : R.layout.item_comic_full_default;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new ComicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ComicViewHolder holder, int position) {
        Comic comic = comicList.get(position);

        holder.tvTitle.setText(comic.getTitle());
        holder.tvLatestChapter.setText("Chương " + (comic.getLatestChapterNumber() != null ? comic.getLatestChapterNumber() : "0"));
        holder.tvTimeUpdate.setText(comic.getTimeUpdated() != null ? comic.getTimeUpdated() : "Vừa xong");

        holder.tvViews.setText(formatNumber(comic.getViewCount()));
        holder.tvLikes.setText(formatNumber(comic.getFollowCount()));
        holder.tvComments.setText(formatNumber(comic.getCommentCount()));

        if (holder.imgLikesIcon != null) {
            if (comic.isFollowed()) {
                holder.imgLikesIcon.setImageResource(R.drawable.ic_heart_filled);
                holder.imgLikesIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#CC003C")));
            } else {
                holder.imgLikesIcon.setImageResource(R.drawable.ic_heart_outline);
                holder.imgLikesIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#B2FFFFFF")));
            }
        }

        Glide.with(holder.itemView.getContext())
                .load(comic.getCoverImageUrl())
                .placeholder(R.drawable.ic_launcher_background)
                .centerCrop()
                .into(holder.imgCover);

        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(comic);
            } else {
                Intent intent = new Intent(holder.itemView.getContext(), ComicDetailActivity.class);
                intent.putExtra("COMIC_ID", comic.getComicId());
                intent.putExtra("COMIC_TITLE", comic.getTitle());
                holder.itemView.getContext().startActivity(intent);
            }
        });

        // XỬ LÝ ẨN/HIỆN THÔNG TIN TRUYỆN TRANH THEO CHẾ ĐỘ
        if (isDownloadMode) {
            // Ẩn chương và thời gian cập nhật
            holder.tvLatestChapter.setVisibility(View.GONE);
            holder.tvTimeUpdate.setVisibility(View.GONE);

            // Ẩn toàn bộ thanh container chứa thông số thống kê
            if (holder.layoutItemStats != null) {
                holder.layoutItemStats.setVisibility(View.GONE);
            }

            // Ẩn bồi thêm các thành phần con để tránh lỗi hiển thị sót biểu tượng
            holder.tvViews.setVisibility(View.GONE);
            holder.tvLikes.setVisibility(View.GONE);
            holder.tvComments.setVisibility(View.GONE);
            if (holder.imgLikesIcon != null) {
                holder.imgLikesIcon.setVisibility(View.GONE);
            }
        } else {
            holder.tvLatestChapter.setVisibility(comic.getLatestChapterNumber() != null ? View.VISIBLE : View.GONE);
            holder.tvTimeUpdate.setVisibility(View.VISIBLE);

            if (holder.layoutItemStats != null) {
                holder.layoutItemStats.setVisibility(View.VISIBLE);
            }
            holder.tvViews.setVisibility(View.VISIBLE);
            holder.tvLikes.setVisibility(View.VISIBLE);
            holder.tvComments.setVisibility(View.VISIBLE);
            if (holder.imgLikesIcon != null) {
                holder.imgLikesIcon.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return comicList != null ? comicList.size() : 0;
    }

    private String formatNumber(long number) {
        if (number >= 1000000) return String.format("%.1fM", number / 1000000.0);
        if (number >= 1000) return String.format("%.1fK", number / 1000.0);
        return String.valueOf(number);
    }

    static class ComicViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCover, imgLikesIcon;
        TextView tvTitle, tvLatestChapter, tvTimeUpdate;
        TextView tvViews, tvLikes, tvComments;
        View layoutItemStats;

        public ComicViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCover        = itemView.findViewById(R.id.imgItemCover);
            imgLikesIcon    = itemView.findViewById(R.id.imgItemLikesIcon);
            tvTitle         = itemView.findViewById(R.id.tvItemTitle);
            tvLatestChapter = itemView.findViewById(R.id.tvItemLatestChapter);
            tvTimeUpdate    = itemView.findViewById(R.id.tvItemTimeUpdate);
            tvViews         = itemView.findViewById(R.id.tvItemViews);
            tvLikes         = itemView.findViewById(R.id.tvItemLikes);
            tvComments      = itemView.findViewById(R.id.tvItemComments);
            layoutItemStats = itemView.findViewById(R.id.layoutItemStats);
        }
    }
}