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
        // ĐÃ SỬA: Nạp layout chứa nút xóa dành riêng cho quản trị
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_chapter, parent, false);
        return new ChapterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChapterViewHolder holder, int position) {
        Map<String, Object> chapter = list.get(position);

        String titleStr = (String) chapter.get("title");
        Number num = (Number) chapter.get("chapterNumber");
        double chapterNum = num != null ? num.doubleValue() : 0.0;

        if (titleStr != null && !titleStr.isEmpty()) {
            holder.tvChapterName.setText("Chương " + chapterNum + ": " + titleStr);
        } else {
            holder.tvChapterName.setText("Chương " + chapterNum);
        }

        holder.itemView.setOnClickListener(v -> listener.onClick(chapter));

        // Sự kiện xử lý bấm nút Xóa chương trực tiếp hàng ngang
        holder.btnDelete.setOnClickListener(v -> {
            if (chapter.get("chapterId") != null) {
                int chapterId = ((Double) chapter.get("chapterId")).intValue();
                listener.onDelete(chapterId, position);
            }
        });
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class ChapterViewHolder extends RecyclerView.ViewHolder {
        TextView tvChapterName, btnDelete;

        public ChapterViewHolder(@NonNull View itemView) {
            super(itemView);
            tvChapterName = itemView.findViewById(R.id.tvAdminChapterName);
            btnDelete = itemView.findViewById(R.id.btnAdminDeleteChapterItem);
        }
    }
}