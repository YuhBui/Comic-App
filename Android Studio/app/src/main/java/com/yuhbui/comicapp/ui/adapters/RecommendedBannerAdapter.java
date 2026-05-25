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
 * Adapter dùng cho ViewPager2 hiển thị banner truyện đề cử
 * Mỗi item là 1 ảnh lớn với thông tin truyện chồng phía dưới (gradient overlay)
 */
public class RecommendedBannerAdapter extends RecyclerView.Adapter<RecommendedBannerAdapter.BannerViewHolder> {

    private List<Comic> comicList = new ArrayList<>();

    public void setComics(List<Comic> comics) {
        this.comicList = comics;
        notifyDataSetChanged();
    }

    public List<Comic> getComics() {
        return comicList;
    }

    @NonNull
    @Override
    public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_banner_recommended, parent, false);
        return new BannerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
        Comic comic = comicList.get(position);

        // Tiêu đề
        holder.tvTitle.setText(comic.getTitle());

        // Chương mới nhất
        String chapter = comic.getLatestChapterNumber() != null
                ? "Chương " + comic.getLatestChapterNumber()
                : "Chương -";
        holder.tvChapter.setText(chapter);

        // Thống kê tương tác
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

    static class BannerViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCover;
        TextView tvTitle, tvChapter, tvViews, tvLikes, tvComments;

        public BannerViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCover    = itemView.findViewById(R.id.imgBannerCover);
            tvTitle     = itemView.findViewById(R.id.tvBannerTitle);
            tvChapter   = itemView.findViewById(R.id.tvBannerChapter);
            tvViews     = itemView.findViewById(R.id.tvBannerViews);
            tvLikes     = itemView.findViewById(R.id.tvBannerLikes);
            tvComments  = itemView.findViewById(R.id.tvBannerComments);
        }
    }
}
