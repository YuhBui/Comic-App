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

/**
 * Adapter hiển thị danh sách thể loại trong popup bộ lọc
 * Dùng cho dialog_category_filter (grid 3 cột) và hiển thị dạng chip
 */
public class CategoryFilterAdapter extends RecyclerView.Adapter<CategoryFilterAdapter.CatViewHolder> {

    private List<Category> categories = new ArrayList<>();
    private int selectedPosition = -1;
    private final OnCatClickListener listener;

    public interface OnCatClickListener {
        void onCatClick(Category category); // null = xóa lọc
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
        // Dùng simple_list_item_1 của Android làm base, style bằng code
        View v = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new CatViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CatViewHolder holder, int position) {
        Category cat = categories.get(position);
        holder.tvName.setText(cat.getName());

        // Style chip: padding, bo góc, căn giữa
        holder.tvName.setPadding(16, 14, 16, 14);
        holder.tvName.setTextSize(12f);
        holder.tvName.setGravity(android.view.Gravity.CENTER);
        holder.tvName.setMaxLines(2);

        final int currentPos = holder.getAdapterPosition();

        // Màu sắc theo trạng thái chọn
        if (selectedPosition == currentPos) {
            holder.tvName.setBackgroundResource(R.drawable.bg_category_chip_active);
            holder.tvName.setTextColor(Color.WHITE);
        } else {
            holder.tvName.setBackgroundResource(R.drawable.bg_category_chip);
            holder.tvName.setTextColor(Color.parseColor("#333333"));
        }

        holder.itemView.setOnClickListener(v -> {
            if (selectedPosition == currentPos) {
                // Bấm lại lần nữa thì bỏ lọc
                selectedPosition = -1;
                listener.onCatClick(null);
            } else {
                selectedPosition = currentPos;
                listener.onCatClick(cat);
            }
            notifyDataSetChanged();
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class CatViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        public CatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(android.R.id.text1);
        }
    }
}