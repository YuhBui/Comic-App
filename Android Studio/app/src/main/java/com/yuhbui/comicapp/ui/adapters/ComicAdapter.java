package com.yuhbui.comicapp.ui.adapters;

import android.content.Intent;
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

/**
 * ComicAdapter - Adapter đa năng cho danh sách truyện
 * Dùng layout item_comic_full_default: ảnh bìa + stats overlay + tiêu đề + chương + thời gian
 * Sử dụng cho phần Truyện Mới Cập Nhật (GridLayout 2 cột)
 */
public class ComicAdapter extends RecyclerView.Adapter<ComicAdapter.ComicViewHolder> {

    private List<Comic> comicList = new ArrayList<>();
    private boolean isListView = false;

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

        holder.tvViews.setText("👁 " + formatNumber(comic.getViewCount()));
        holder.tvLikes.setText("❤ " + formatNumber(comic.getFollowCount()));
        holder.tvComments.setText("💬 " + formatNumber(comic.getCommentCount()));

        Glide.with(holder.itemView.getContext())
                .load(comic.getCoverImageUrl())
                .placeholder(R.drawable.ic_launcher_background)
                .centerCrop()
                .into(holder.imgCover);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(holder.itemView.getContext(), ComicDetailActivity.class);
            intent.putExtra("COMIC_ID", comic.getComicId());
            holder.itemView.getContext().startActivity(intent);
        });
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
        ImageView imgCover;
        TextView tvTitle, tvLatestChapter, tvTimeUpdate;
        TextView tvViews, tvLikes, tvComments;

        public ComicViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCover        = itemView.findViewById(R.id.imgItemCover);
            tvTitle         = itemView.findViewById(R.id.tvItemTitle);
            tvLatestChapter = itemView.findViewById(R.id.tvItemLatestChapter);
            tvTimeUpdate    = itemView.findViewById(R.id.tvItemTimeUpdate);
            tvViews         = itemView.findViewById(R.id.tvItemViews);
            tvLikes         = itemView.findViewById(R.id.tvItemLikes);
            tvComments      = itemView.findViewById(R.id.tvItemComments);
        }
    }
}