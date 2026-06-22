package com.yuhbui.comicapp.ui.adapters;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.yuhbui.comicapp.R;
import com.yuhbui.comicapp.data.model.User;
import java.util.ArrayList;
import java.util.List;

public class AdminUserAdapter extends RecyclerView.Adapter<AdminUserAdapter.UserViewHolder> {

    private List<User> userList = new ArrayList<>();
    private OnUserAdminActionListener listener;

    public interface OnUserAdminActionListener {
        void onItemClick(User user);
        void onToggleBan(User user, int position);
        void onEdit(User user, int position);
        void onDelete(User user, int position);
    }

    public AdminUserAdapter(OnUserAdminActionListener listener) {
        this.listener = listener;
    }

    public void setData(List<User> list) {
        this.userList = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_user, parent, false);
        return new UserViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);

        holder.tvName.setText(user.getDisplayName());
        holder.tvEmail.setText(user.getEmail());

        // ĐỔI: Chuyển nút Ban thành biểu tượng cấm (Hệ thống vòng tròn gạch ngang báo bận/cấm mặc định)
        holder.btnBan.setImageResource(android.R.drawable.presence_busy);

        // Lấy thông số cấu hình mật độ pixel để quy đổi chuẩn kích thước px từ CSS sang dp
        float density = holder.itemView.getContext().getResources().getDisplayMetrics().density;
        int strokeWidth = Math.round(1 * density); // border: 1px
        int itemRadius = Math.round(4 * density);  // border-radius: 4px
        int badgeRadius = Math.round(12 * density); // border-radius: 12px

        // 1. XỬ LÝ MÀU THẺ USER (USER ITEM CARD): Đồng bộ theo 3 trạng thái của CSS
        GradientDrawable itemBg = new GradientDrawable();
        itemBg.setShape(GradientDrawable.RECTANGLE);
        itemBg.setColor(Color.parseColor("#1E1E1E")); // background: #1E1E1E
        itemBg.setCornerRadius(itemRadius);

        if ("Banned".equalsIgnoreCase(user.getStatus())) {
            // Trạng thái User Card 7 (Banned): border 1px solid #FF383C, Icon màu #FF0000
            itemBg.setStroke(strokeWidth, Color.parseColor("#FF383C"));
            holder.btnBan.setColorFilter(Color.parseColor("#FF0000"));
        } else {
            // Icon Ban mặc định khi tài khoản hoạt động bình thường có màu vàng cát #DBC2B0
            holder.btnBan.setColorFilter(Color.parseColor("#DBC2B0"));

            if ("Admin".equalsIgnoreCase(user.getRole())) {
                // Trạng thái User Card 5 (Admin): border 1px solid rgba(85, 67, 54, 0.3)
                itemBg.setStroke(strokeWidth, Color.argb(76, 85, 67, 54)); // 76 tương đương với 30% alpha
            } else {
                // Trạng thái User Card 2 (User thường): border 1px solid #0C1322
                itemBg.setStroke(strokeWidth, Color.parseColor("#0C1322"));
            }
        }
        holder.itemView.setBackground(itemBg);

        // 2. XỬ LÝ MÀU NHÃN PHÂN QUYỀN (ROLE BADGE): Đồng bộ theo thiết kế Bento của CSS
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setShape(GradientDrawable.RECTANGLE);
        badgeBg.setCornerRadius(badgeRadius);

        if ("Admin".equalsIgnoreCase(user.getRole())) {
            holder.tvRole.setText("ADMIN");
            holder.tvRole.setTextColor(Color.parseColor("#166534")); // color: #166534
            badgeBg.setColor(Color.parseColor("#34C759"));           // background: #34C759
            badgeBg.setStroke(strokeWidth, Color.parseColor("#166534")); // border: 1px solid #166534
        } else {
            holder.tvRole.setText("USER");
            holder.tvRole.setTextColor(Color.parseColor("#6155F5")); // color: #6155F5
            badgeBg.setColor(Color.parseColor("#00C0E8"));           // background: #00C0E8
            badgeBg.setStroke(strokeWidth, Color.parseColor("#6155F5")); // border: 1px solid #6155F5
        }
        holder.tvRole.setBackground(badgeBg);

        // Tải ảnh đại diện bằng Glide
        Glide.with(holder.itemView.getContext())
                .load(user.getAvatarUrl())
                .placeholder(android.R.drawable.sym_def_app_icon)
                .circleCrop()
                .into(holder.imgAvatar);

        // Đăng ký toàn bộ sự kiện xử lý tương tác
        holder.itemView.setOnClickListener(v -> listener.onItemClick(user));
        holder.btnBan.setOnClickListener(v -> listener.onToggleBan(user, position));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(user, position));
    }

    @Override
    public int getItemCount() { return userList != null ? userList.size() : 0; }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView tvName, tvEmail, tvRole;
        ImageView btnBan, btnEdit, btnDelete;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgAdminUserAvatar);
            tvName = itemView.findViewById(R.id.tvAdminUserDisplayName);
            tvEmail = itemView.findViewById(R.id.tvAdminUserEmail);
            tvRole = itemView.findViewById(R.id.tvAdminUserRoleBadge);
            btnBan = itemView.findViewById(R.id.btnAdminBanUser);
            btnDelete = itemView.findViewById(R.id.btnAdminDeleteUser);
        }
    }

    private String formatToDateOnly(String rawDateTime) {
        if (rawDateTime == null || rawDateTime.trim().isEmpty()) {
            return "Chưa cập nhật";
        }
        try {
            String datePart = rawDateTime.contains("T") ? rawDateTime.split("T")[0] : rawDateTime.split(" ")[0];
            String[] parts = datePart.split("-");
            if (parts.length == 3) {
                return parts[2] + "/" + parts[1] + "/" + parts[0];
            }
            return datePart;
        } catch (Exception e) {
            return rawDateTime;
        }
    }
}