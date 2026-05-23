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
import com.yuhbui.comicapp.data.model.Comic;
import java.util.ArrayList;
import java.util.List;

public class ComicAdapter extends RecyclerView.Adapter<ComicAdapter.ComicViewHolder> {

    private List<Comic> comicList = new ArrayList<>();

    // Hàm này giúp Activity truyền dữ liệu mới vào Adapter
    public void setComics(List<Comic> comics) {
        this.comicList = comics;
        notifyDataSetChanged(); // Lệnh yêu cầu RecyclerView vẽ lại màn hình
    }

    @NonNull
    @Override
    public ComicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Lấy cái khung item_comic.xml bạn vừa tạo ở Bước 1
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comic, parent, false);
        return new ComicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ComicViewHolder holder, int position) {
        Comic comic = comicList.get(position);

        holder.tvTitle.setText(comic.getTitle());

        Glide.with(holder.itemView.getContext())
                .load(comic.getCoverImageUrl())
                .into(holder.imgCover);

        // --- ĐOẠN MÃ MỚI THÊM VÀO ĐÂY ---
        // Bắt sự kiện khi người dùng bấm vào một item truyện
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Tạo một "Chuyến xe" (Intent) để chuyển sang màn hình Chi tiết
                android.content.Intent intent = new android.content.Intent(holder.itemView.getContext(), com.yuhbui.comicapp.ui.ComicDetailActivity.class);

                // Gửi kèm ID và Tên của bộ truyện sang màn hình mới
                intent.putExtra("COMIC_ID", comic.getComicId());
                intent.putExtra("COMIC_TITLE", comic.getTitle());

                // Bắt đầu khởi hành
                holder.itemView.getContext().startActivity(intent);
            }
        });
        // ---------------------------------
    }

    @Override
    public int getItemCount() {
        return comicList != null ? comicList.size() : 0;
    }

    // Lớp giữ các thành phần giao diện (ViewHolder)
    static class ComicViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCover;
        TextView tvTitle;

        public ComicViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCover = itemView.findViewById(R.id.imgCover);
            tvTitle = itemView.findViewById(R.id.tvTitle);
        }
    }
}