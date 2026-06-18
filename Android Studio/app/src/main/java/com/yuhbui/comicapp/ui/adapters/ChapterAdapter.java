package com.yuhbui.comicapp.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.yuhbui.comicapp.R;
import com.yuhbui.comicapp.data.model.Chapter;
import java.util.ArrayList;
import java.util.List;

public class ChapterAdapter extends RecyclerView.Adapter<ChapterAdapter.ChapterViewHolder> {

    private List<Chapter> chapterList = new ArrayList<>();
    private boolean isOfflineMode = false; // Quản lý xem đang mở truyện ở chế độ offline hay online
    private List<Integer> downloadedChapterIds = new ArrayList<>(); // Lưu trữ các ID chương đã tải
    private OnChapterActionListener actionListener;

    // Định nghĩa Interface để giao tiếp ngược lại với ComicDetailActivity
    public interface OnChapterActionListener {
        void onDownloadClick(Chapter chapter);
        void onDeleteClick(Chapter chapter);
    }

    public void setChapters(List<Chapter> chapters) {
        this.chapterList = chapters;
        notifyDataSetChanged();
    }

    // Thiết lập trạng thái chế độ Offline cho Adapter
    public void setOfflineMode(boolean offlineMode) {
        this.isOfflineMode = offlineMode;
        notifyDataSetChanged();
    }

    // Đồng bộ danh sách các chương đã tải từ Room Database lên Giao diện
    public void setDownloadedChapterIds(List<Integer> downloadedIds) {
        if (downloadedIds != null) {
            this.downloadedChapterIds = downloadedIds;
            notifyDataSetChanged();
        }
    }

    public void setOnChapterActionListener(OnChapterActionListener listener) {
        this.actionListener = listener;
    }

    @NonNull
    @Override
    public ChapterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chapter, parent, false);
        return new ChapterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChapterViewHolder holder, int position) {
        Chapter chapter = chapterList.get(position);
        holder.tvChapterName.setText("Chương " + chapter.getChapterNumber() + (chapter.getTitle() != null ? " - " + chapter.getTitle() : ""));

        // Xử lý hiển thị giao diện của Nút bấm dựa vào chế độ mạng
        if (isOfflineMode) {
            // NẾU LÀ OFFLINE: Biến đổi nút vuông thành nút XÓA (Thùng rác hệ thống)
            holder.btnChapterAction.setVisibility(View.VISIBLE);
            holder.btnChapterAction.setImageResource(android.R.drawable.ic_menu_delete);
            holder.btnChapterAction.setAlpha(1.0f);
            holder.btnChapterAction.setEnabled(true);
            holder.btnChapterAction.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onDeleteClick(chapter);
            });
        } else {
            // NẾU LÀ ONLINE: Kiểm tra xem chương này đã từng được tải về máy chưa
            if (downloadedChapterIds.contains(chapter.getChapterId())) {
                // ĐÃ TẢI: Ẩn hoàn toàn nút tải xuống đi
                holder.btnChapterAction.setVisibility(View.GONE);
            } else {
                // CHƯA TẢI: Hiện nút vuông hình mũi tên tải xuống bình thường
                holder.btnChapterAction.setVisibility(View.VISIBLE);
                holder.btnChapterAction.setImageResource(android.R.drawable.stat_sys_download);
                holder.btnChapterAction.setEnabled(true);
                holder.btnChapterAction.setAlpha(1.0f);
                holder.btnChapterAction.setOnClickListener(v -> {
                    if (actionListener != null) actionListener.onDownloadClick(chapter);
                });
            }
        }

        holder.itemView.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(holder.itemView.getContext(), com.yuhbui.comicapp.ui.ReaderActivity.class);

            // Gửi dữ liệu cốt lõi sang Reader
            intent.putExtra("CHAPTER_ID", chapter.getChapterId());
            intent.putExtra("COMIC_ID", chapter.getComicId());

            // THÊM DÒNG NÀY: Gửi trạng thái Offline để Reader biết đường ẩn phần bình luận đi
            intent.putExtra("IS_OFFLINE_MODE", isOfflineMode);

            holder.itemView.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return chapterList != null ? chapterList.size() : 0;
    }

    static class ChapterViewHolder extends RecyclerView.ViewHolder {
        TextView tvChapterName;
        ImageButton btnChapterAction; // Thêm khai báo nút vuông hành động

        public ChapterViewHolder(@NonNull View itemView) {
            super(itemView);
            tvChapterName = itemView.findViewById(R.id.tvChapterName);
            btnChapterAction = itemView.findViewById(R.id.btnChapterAction); // Ánh xạ từ XML mới
        }
    }
}