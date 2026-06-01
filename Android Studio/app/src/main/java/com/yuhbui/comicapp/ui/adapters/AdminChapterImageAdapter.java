package com.yuhbui.comicapp.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.yuhbui.comicapp.R;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdminChapterImageAdapter extends RecyclerView.Adapter<AdminChapterImageAdapter.ImageViewHolder> {

    private List<Map<String, Object>> list = new ArrayList<>();
    private OnPageDeleteClickListener listener;

    public interface OnPageDeleteClickListener {
        void onDelete(int imageId, int position);
    }

    public AdminChapterImageAdapter(OnPageDeleteClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<Map<String, Object>> list) {
        this.list = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_chapter_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        Map<String, Object> page = list.get(position);

        // Hiển thị số trang bằng cách ép kiểu an toàn
        if (page.get("pageNumber") != null) {
            holder.tvPageNum.setText("Trang " + ((Double) page.get("pageNumber")).intValue());
        }

        // Tải ảnh trang truyện bằng Glide
        Glide.with(holder.itemView.getContext())
                .load((String) page.get("imageUrl"))
                .placeholder(R.drawable.ic_launcher_background)
                .into(holder.imgPreview);

        // Sự kiện xóa trang truyện phạm quy
        holder.btnDelete.setOnClickListener(v -> {
            if (page.get("imageId") != null) {
                int imgId = ((Double) page.get("imageId")).intValue();
                listener.onDelete(imgId, position);
            }
        });
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imgPreview, btnDelete;
        TextView tvPageNum;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imgPreview = itemView.findViewById(R.id.imgAdminPagePreview);
            btnDelete = itemView.findViewById(R.id.btnAdminDeletePage);
            tvPageNum = itemView.findViewById(R.id.tvAdminPageNumber);
        }
    }
}