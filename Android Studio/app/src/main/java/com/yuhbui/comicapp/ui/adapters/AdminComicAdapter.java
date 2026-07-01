package com.yuhbui.comicapp.ui.adapters;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.yuhbui.comicapp.R;
import com.yuhbui.comicapp.data.model.Comic;
import com.yuhbui.comicapp.ui.admin.AdminComicDetailActivity;
import java.util.ArrayList;
import java.util.List;

public class AdminComicAdapter extends RecyclerView.Adapter<AdminComicAdapter.AdminViewHolder> {

    private List<Comic> list = new ArrayList<>();
    private OnComicActionListener listener;

    public interface OnComicActionListener {
        void onEdit(Comic comic);
        void onDelete(Comic comic, int position);
    }

    public AdminComicAdapter(OnComicActionListener listener) {
        this.listener = listener;
    }

    public void setData(List<Comic> list) {
        this.list = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AdminViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_comic, parent, false);
        return new AdminViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminViewHolder holder, int position) {
        Comic comic = list.get(position);

        holder.tvTitle.setText(comic.getTitle());

        String latestChapter = comic.getLatestChapterNumber();
        if (latestChapter != null && !latestChapter.trim().isEmpty()) {
            holder.tvLatestChapter.setText("Chương " + latestChapter);
        } else {
            holder.tvLatestChapter.setText("Chưa có chương");
        }

        String timeUpdate = comic.getTimeUpdated();
        holder.tvTimeUpdate.setText(timeUpdate != null ? formatToDateOnly(timeUpdate) : "Đang cập nhật");

        holder.tvViews.setText(formatNumber(comic.getViewCount()));
        holder.tvLikes.setText(formatNumber(comic.getFollowCount()));
        holder.tvComments.setText(formatNumber(comic.getCommentCount()));

        float density = holder.itemView.getContext().getResources().getDisplayMetrics().density;
        GradientDrawable itemBg = new GradientDrawable();
        itemBg.setShape(GradientDrawable.RECTANGLE);
        itemBg.setColor(Color.parseColor("#1E1E1E"));
        itemBg.setCornerRadius(4 * density);
        itemBg.setStroke(Math.round(1 * density), Color.argb(76, 85, 67, 54)); // border: 1px solid rgba(85,67,54,0.3)
        holder.itemView.setBackground(itemBg);

        Glide.with(holder.itemView.getContext())
                .load(comic.getCoverImageUrl())
                .placeholder(R.drawable.ic_launcher_background)
                .into(holder.imgCover);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(holder.itemView.getContext(), AdminComicDetailActivity.class);
            intent.putExtra("COMIC_ID", comic.getComicId());
            holder.itemView.getContext().startActivity(intent);
        });

        holder.btnEdit.setOnClickListener(v -> listener.onEdit(comic));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(comic, position));
    }

    private String formatToDateOnly(String rawDateTime) {
        if (rawDateTime == null || rawDateTime.trim().isEmpty()) {
            return "Đang cập nhật";
        }
        try {
            String datePart = rawDateTime.contains("T") ? rawDateTime.split("T")[0] : rawDateTime.split(" ")[0];
            String[] parts = datePart.split("-");
            if (parts.length == 3) {
                return parts[2] + "/" + parts[1] + "/" + parts[0];
            }
            return datePart;
        } catch (Exception e) {
            return rawDateTime;
        }
    }

    @Override
    public int getItemCount() { return list != null ? list.size() : 0; }

    private String formatNumber(long number) {
        if (number >= 1_000_000) {
            return String.format("%.1fM", (double) number / 1_000_000).replace(".0", "");
        } else if (number >= 1_000) {
            return String.format("%.1fK", (double) number / 1_000).replace(".0", "");
        }
        return String.valueOf(number);
    }

    static class AdminViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCover;
        TextView tvTitle, tvLatestChapter, tvTimeUpdate;
        TextView tvViews, tvLikes, tvComments;
        ImageView btnEdit, btnDelete;

        public AdminViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCover = itemView.findViewById(R.id.imgAdminComicCover);
            tvTitle = itemView.findViewById(R.id.tvAdminComicTitle);
            tvLatestChapter = itemView.findViewById(R.id.tvAdminComicLatestChapter);
            tvTimeUpdate = itemView.findViewById(R.id.tvAdminComicTimeUpdate);
            tvViews = itemView.findViewById(R.id.tvAdminComicViews);
            tvLikes = itemView.findViewById(R.id.tvAdminComicLikes);
            tvComments = itemView.findViewById(R.id.tvAdminComicComments);
            btnEdit = itemView.findViewById(R.id.btnAdminEditComic);
            btnDelete = itemView.findViewById(R.id.btnAdminDeleteComic);
        }
    }
}