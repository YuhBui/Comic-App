package com.yuhbui.comicapp.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.yuhbui.comicapp.R;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdminChapterAdapter extends RecyclerView.Adapter<AdminChapterAdapter.ChapterViewHolder> {

    private List<Map<String, Object>> list = new ArrayList<>();
    private OnChapterAdminActionListener listener;

    public interface OnChapterAdminActionListener {
        void onClick(Map<String, Object> chapter);
        void onEdit(Map<String, Object> chapter);
        void onDelete(int chapterId, int position);
    }

    public AdminChapterAdapter(OnChapterAdminActionListener listener) {
        this.listener = listener;
    }

    public void setData(List<Map<String, Object>> list) {
        this.list = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChapterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Nạp tệp layout item_chapter hợp lệ
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chapter, parent, false);
        return new ChapterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChapterViewHolder holder, int position) {
        Map<String, Object> chapter = list.get(position);

        String titleStr = (String) chapter.get("title");
        Number num = (Number) chapter.get("chapterNumber");
        double chapterNum = num != null ? num.doubleValue() : 0.0;

        // Đổ thông tin chương truyện vào ô hiển thị duy nhất
        if (titleStr != null && !titleStr.isEmpty()) {
            holder.tvChapterName.setText("Chương " + chapterNum + ": " + titleStr);
        } else {
            holder.tvChapterName.setText("Chương " + chapterNum);
        }

        // Bấm nhanh để mở màn hình quản lý nạp/xóa từng trang ảnh truyện
        holder.itemView.setOnClickListener(v -> listener.onClick(chapter));

        // Nhấn giữ lâu dòng chương để hiện Dialog Form Sửa hoặc Xóa chương vĩnh viễn
        holder.itemView.setOnLongClickListener(v -> {
            listener.onEdit(chapter);
            return true;
        });
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class ChapterViewHolder extends RecyclerView.ViewHolder {
        TextView tvChapterName; // Đã loại bỏ biến tvTime lỗi

        public ChapterViewHolder(@NonNull View itemView) {
            super(itemView);
            // Chỉ ánh xạ đúng ID duy nhất tồn tại trong file item_chapter.xml
            tvChapterName = itemView.findViewById(R.id.tvChapterName);
        }
    }
}