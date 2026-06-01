package com.yuhbui.comicapp.ui.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.yuhbui.comicapp.R;
import com.yuhbui.comicapp.data.model.Comic;
import com.yuhbui.comicapp.ui.admin.AdminComicDetailActivity;
import java.util.ArrayList;
import java.util.List;

public class AdminComicAdapter extends RecyclerView.Adapter<AdminComicAdapter.AdminViewHolder> {

    private List<Comic> list = new ArrayList<>();
    private OnComicActionListener listener;

    // Interface định nghĩa các hành động: Sửa và Xóa (Thay đổi từ ẩn sang xóa hoàn toàn)
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
        holder.tvAuthor.setText("Tác giả: " + comic.getAuthor());
        holder.tvStatus.setText(comic.getStatus());

        // Hiển thị ảnh bìa bằng Glide
        Glide.with(holder.itemView.getContext())
                .load(comic.getCoverImageUrl())
                .placeholder(R.drawable.ic_launcher_background)
                .into(holder.imgCover);

        // THÊM: Sự kiện click vào cả dòng truyện để xem chi tiết quản trị và quản lý bình luận
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(holder.itemView.getContext(), AdminComicDetailActivity.class);
            intent.putExtra("COMIC_ID", comic.getComicId());
            holder.itemView.getContext().startActivity(intent);
        });

        // Sự kiện click nút Sửa
        holder.btnEdit.setOnClickListener(v -> listener.onEdit(comic));

        // Sự kiện click nút Xóa mới (Gọi sang callback onDelete của Activity)
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(comic, position));
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    static class AdminViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCover;
        TextView tvTitle, tvAuthor, tvStatus;
        Button btnEdit, btnDelete;
        LinearLayout layoutItem;

        public AdminViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCover = itemView.findViewById(R.id.imgAdminComicCover);
            tvTitle = itemView.findViewById(R.id.tvAdminComicTitle);
            tvAuthor = itemView.findViewById(R.id.tvAdminComicAuthor);
            tvStatus = itemView.findViewById(R.id.tvAdminComicStatus);
            btnEdit = itemView.findViewById(R.id.btnAdminEditComic);
            btnDelete = itemView.findViewById(R.id.btnAdminDeleteComic); // Ánh xạ sang ID nút Xóa mới
            layoutItem = itemView.findViewById(R.id.layoutAdminComicItem);
        }
    }
}