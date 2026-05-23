package com.yuhbui.comicapp.ui.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.yuhbui.comicapp.R;
import com.yuhbui.comicapp.data.model.Category;
import java.util.ArrayList;
import java.util.List;

public class CategoryFilterAdapter extends RecyclerView.Adapter<CategoryFilterAdapter.CatViewHolder> {

    private List<Category> categories = new ArrayList<>();
    private int selectedPosition = -1; // Lưu vị trí đang được chọn để đổi màu nền công nghệ
    private OnCatClickListener listener;

    public interface OnCatClickListener {
        void onCatClick(Category category);
    }

    public CategoryFilterAdapter(OnCatClickListener listener) {
        this.listener = listener;
    }

    public void setCategories(List<Category> categories) {
        this.categories = categories;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        return new CatViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CatViewHolder holder, int position) {
        Category cat = categories.get(position);
        holder.tvName.setText(cat.getName());
        holder.tvName.setPadding(24, 12, 24, 12);
        holder.tvName.setTextSize(13);

        // Lấy vị trí thực tế an toàn của Holder hiện tại để tránh lỗi đồng bộ luồng Lambda
        final int currentPos = holder.getAdapterPosition();

        // So sánh vị trí được chọn để đổi màu nền
        if (selectedPosition == currentPos) {
            holder.tvName.setBackgroundColor(Color.parseColor("#FF9800"));
            holder.tvName.setTextColor(Color.WHITE);
        } else {
            holder.tvName.setBackgroundColor(Color.parseColor("#EEEEEE"));
            holder.tvName.setTextColor(Color.BLACK);
        }

        holder.itemView.setOnClickListener(v -> {
            if (selectedPosition == currentPos) {
                selectedPosition = -1; // Bấm lại lần nữa thì bỏ lọc
                listener.onCatClick(null);
            } else {
                selectedPosition = currentPos;
                listener.onCatClick(cat);
            }
            notifyDataSetChanged();
        });
    }

    @Override
    public int getItemCount() { return categories.size(); }

    static class CatViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        public CatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(android.R.id.text1);
        }
    }
}