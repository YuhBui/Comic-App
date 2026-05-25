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
 * Adapter cho danh sách Bảng Xếp Hạng Top 10
 * Mỗi item hiển thị: Số thứ hạng | Ảnh bìa | Tiêu đề + Chương + Stats
 */
public class RankingAdapter extends RecyclerView.Adapter<RankingAdapter.RankViewHolder> {

    private List<Comic> comicList = new ArrayList<>();

    public void setComics(List<Comic> comics) {
        this.comicList = comics;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RankViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_rank_comic, parent, false);
        return new RankViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RankViewHolder holder, int position) {
        Comic comic = comicList.get(position);

        // Số thứ hạng (1-indexed)
        holder.tvRankNumber.setText(String.valueOf(position + 1));

        // Đổi màu badge cho top 3
        if (position == 0) {
            holder.tvRankNumber.setBackgroundResource(R.drawable.bg_rank_badge_gold);
        } else if (position == 1) {
            holder.tvRankNumber.setBackgroundResource(R.drawable.bg_rank_badge_silver);
        } else if (position == 2) {
            holder.tvRankNumber.setBackgroundResource(R.drawable.bg_rank_badge_bronze);
        } else {
            holder.tvRankNumber.setBackgroundResource(R.drawable.bg_rank_badge);
        }

        // Tiêu đề và chương mới
        holder.tvTitle.setText(comic.getTitle());
        String chapter = comic.getLatestChapterNumber() != null
                ? "Chương " + comic.getLatestChapterNumber()
                : "Chương -";
        holder.tvLatestChapter.setText(chapter);

        // Thống kê
        holder.tvViews.setText("👁 " + formatNumber(comic.getViewCount()));
        holder.tvLikes.setText("❤ " + formatNumber(comic.getFollowCount()));
        holder.tvComments.setText("💬 " + formatNumber(comic.getCommentCount()));

        // Tải ảnh bìa
        Glide.with(holder.itemView.getContext())
                .load(comic.getCoverImageUrl())
                .placeholder(R.drawable.ic_launcher_background)
                .centerCrop()
                .into(holder.imgCover);

        // Click chuyển sang chi tiết
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

    static class RankViewHolder extends RecyclerView.ViewHolder {
        TextView tvRankNumber, tvTitle, tvLatestChapter, tvViews, tvLikes, tvComments;
        ImageView imgCover;

        public RankViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRankNumber     = itemView.findViewById(R.id.tvRankNumber);
            imgCover         = itemView.findViewById(R.id.imgRankCover);
            tvTitle          = itemView.findViewById(R.id.tvRankTitle);
            tvLatestChapter  = itemView.findViewById(R.id.tvRankLatestChapter);
            tvViews          = itemView.findViewById(R.id.tvRankViews);
            tvLikes          = itemView.findViewById(R.id.tvRankLikes);
            tvComments       = itemView.findViewById(R.id.tvRankComments);
        }
    }
}
