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
 * Adapter hiển thị danh sách thể loại dạng hàng ngang cuộn động.
 * Đã nâng cấp: Hỗ trợ chọn nhiều thể loại song song, bấm lại để hủy chọn.
 */
public class CategoryFilterAdapter extends RecyclerView.Adapter<CategoryFilterAdapter.CatViewHolder> {

    private List<Category> categories = new ArrayList<>();
    private final List<Integer> selectedCategoryIds = new ArrayList<>();
    private final OnCatClickListener listener;

    public interface OnCatClickListener {
        void onCatClick(List<Integer> selectedCategoryIds);
    }

    public CategoryFilterAdapter(OnCatClickListener listener) {
        this.listener = listener;
        // Mặc định khi vừa mở bộ lọc, hệ thống sẽ tự động kích hoạt chọn nút "Tất cả" (ID = 0)
        this.selectedCategoryIds.add(0);
    }

    public void setCategories(List<Category> categories) {
        this.categories = categories;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new CatViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CatViewHolder holder, int position) {
        Category cat = categories.get(position);
        holder.tvName.setText(cat.getName());

        // Định dạng giao diện các ô thẻ thể loại (Chips)
        holder.tvName.setPadding(26, 14, 26, 14);
        holder.tvName.setTextSize(12f);
        holder.tvName.setGravity(android.view.Gravity.CENTER);
        holder.tvName.setMaxLines(2);

        // ĐÃ SỬA: Lấy ra ID của thể loại hiện tại để đối sánh trực tiếp
        int catId = cat.getCategoryId();

        // Màu sắc theo trạng thái đa chọn (Kiểm tra xem mảng ID có chứa catId này không)
        if (selectedCategoryIds.contains(catId)) {
            holder.tvName.setBackgroundResource(R.drawable.bg_category_chip_active);
            holder.tvName.setTextColor(Color.WHITE);
        } else {
            holder.tvName.setBackgroundResource(R.drawable.bg_category_chip);
            holder.tvName.setTextColor(Color.parseColor("#333333"));
        }

        holder.itemView.setOnClickListener(v -> {
            if (catId == 0) {
                // TH 1: Nếu bấm nút "Tất cả" -> Xóa sạch các bộ lọc thể loại cụ thể khác đang chọn
                selectedCategoryIds.clear();
                selectedCategoryIds.add(0);
            } else {
                // TH 2: Bấm vào một thể loại cụ thể -> Gỡ bỏ trạng thái của nút "Tất cả"
                selectedCategoryIds.remove((Integer) 0);

                if (selectedCategoryIds.contains(catId)) {
                    // Nhấn thêm 1 lần nữa vào ô đã chọn sáng màu -> Hủy bỏ chọn (Xóa khỏi danh sách)
                    selectedCategoryIds.remove((Integer) catId);
                } else {
                    // Nhấn vào ô chưa chọn -> Thêm ID này vào danh sách bộ lọc đa chọn
                    selectedCategoryIds.add(catId);
                }
            }

            // Phòng hờ nếu người dùng tắt hết toàn bộ các nút -> Tự động quay về chọn nút "Tất cả"
            if (selectedCategoryIds.isEmpty()) {
                selectedCategoryIds.add(0);
            }

            notifyDataSetChanged(); // Vẽ lại giao diện đổi màu sắc các ô thẻ
            listener.onCatClick(selectedCategoryIds); // Báo danh sách ID mới về cho Màn hình gọi xử lý
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