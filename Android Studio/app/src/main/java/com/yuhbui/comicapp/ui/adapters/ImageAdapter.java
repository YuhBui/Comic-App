package com.yuhbui.comicapp.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.yuhbui.comicapp.R;
import com.yuhbui.comicapp.data.model.ChapterImage;
import java.util.ArrayList;
import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {

    private List<ChapterImage> imageList = new ArrayList<>();

    public void setImages(List<ChapterImage> images) {
        this.imageList = images;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        ChapterImage image = imageList.get(position);
        Glide.with(holder.itemView.getContext())
                .load(image.getImageUrl())
                .dontAnimate() // Tắt animation chuyển cảnh để ảnh hiển thị ngay lập tức, không gây lag chiều cao layout
                .placeholder(android.R.drawable.progress_horizontal) // Hiện thanh tiến trình chờ tải
                .into(holder.imgPage);
    }

    @Override
    public int getItemCount() {
        return imageList != null ? imageList.size() : 0;
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imgPage;
        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imgPage = itemView.findViewById(R.id.imgPage);
        }
    }
}