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
        holder.tvRole.setText(user.getRole());
        holder.tvStatus.setText(user.getStatus());

        // Khôi phục trạng thái hiển thị và cập nhật viền đỏ cảnh báo tài khoản bị Ban
        if ("Banned".equalsIgnoreCase(user.getStatus())) {
            holder.tvStatus.setTextColor(android.graphics.Color.RED);
            holder.btnBan.setText("✅ Unban");
            holder.itemView.setBackgroundResource(R.drawable.bg_banned_user_border);
        } else {
            holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50"));
            holder.btnBan.setText("🚷 Ban");
            holder.itemView.setBackgroundResource(R.drawable.bg_normal_user);
        }

        Glide.with(holder.itemView.getContext())
                .load(user.getAvatarUrl())
                .placeholder(android.R.drawable.sym_def_app_icon)
                .circleCrop()
                .into(holder.imgAvatar);

        // Đăng ký toàn bộ sự kiện click
        holder.itemView.setOnClickListener(v -> listener.onItemClick(user));
        holder.btnBan.setOnClickListener(v -> listener.onToggleBan(user, position));
        holder.btnEdit.setOnClickListener(v -> listener.onEdit(user, position));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(user, position));
    }

    @Override
    public int getItemCount() { return userList != null ? userList.size() : 0; }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView tvName, tvEmail, tvRole, tvStatus, btnBan, btnEdit, btnDelete;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgItemUserAvatar);
            tvName = itemView.findViewById(R.id.tvItemUserDisplayName);
            tvEmail = itemView.findViewById(R.id.tvItemUserEmail);
            tvRole = itemView.findViewById(R.id.tvItemUserRole);
            tvStatus = itemView.findViewById(R.id.tvItemUserStatus);
            btnBan = itemView.findViewById(R.id.btnItemUserBan);
            btnEdit = itemView.findViewById(R.id.btnItemUserEdit);
            btnDelete = itemView.findViewById(R.id.btnItemUserDelete);
        }
    }
}