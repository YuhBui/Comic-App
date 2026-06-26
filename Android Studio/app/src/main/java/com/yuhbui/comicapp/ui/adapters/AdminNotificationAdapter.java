package com.yuhbui.comicapp.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.yuhbui.comicapp.R;
import com.yuhbui.comicapp.data.model.Comic;
import com.yuhbui.comicapp.data.model.Notification;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminNotificationAdapter extends RecyclerView.Adapter<AdminNotificationAdapter.AdminNotifViewHolder> {

    private List<Notification> list = new ArrayList<>();
    private Map<Integer, String> comicMap = new HashMap<>();
    private OnAdminNotifActionListener listener;

    public interface OnAdminNotifActionListener {
        void onEdit(Notification notification);
        void onDelete(Notification notification, int position);
    }

    public void setData(List<Notification> list, OnAdminNotifActionListener listener) {
        this.list = list;
        this.listener = listener;
        notifyDataSetChanged();
    }

    // Nạp danh sách truyện để map ID sang Tên truyện công khai
    public void setComicList(List<Comic> comics) {
        if (comics != null) {
            this.comicMap.clear();
            for (Comic c : comics) {
                this.comicMap.put(c.getComicId(), c.getTitle());
            }
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public AdminNotifViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_notification, parent, false);
        return new AdminNotifViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminNotifViewHolder holder, int position) {
        Notification notif = list.get(position);
        holder.tvTitle.setText(notif.getTitle());
        holder.tvMessage.setText(notif.getMessage());

        // 1. ĐÃ SỬA: Định dạng thời gian tương đối bằng Date & SimpleDateFormat truyền thống (Không lo lỗi kén máy)
        if (notif.getCreatedAt() != null) {
            holder.tvTime.setText(formatRelativeTimeCompatible(notif.getCreatedAt().toString()));
        } else {
            holder.tvTime.setText("Vừa xong");
        }

        // 2. ĐÃ SỬA: Định dạng hiển thị "Truyện: Tên truyện" thay cho ID khô khan
        if (notif.getComicId() != null && notif.getComicId() > 0) {
            if (comicMap.containsKey(notif.getComicId())) {
                holder.tvTarget.setText("Truyện: " + comicMap.get(notif.getComicId()));
            } else {
                holder.tvTarget.setText("Truyện ID: #" + notif.getComicId());
            }
        } else {
            holder.tvTarget.setText("Tất cả");
        }

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(notif);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(notif, holder.getAdapterPosition());
        });
    }

    // Hàm tính khoảng thời gian tương thích ngược cho mọi phiên bản Android cũ/mới
    private String formatRelativeTimeCompatible(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) return "Vừa xong";
        try {
            String cleanStr = dateTimeStr.replace("T", " ");
            if (cleanStr.length() > 19) cleanStr = cleanStr.substring(0, 19);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            if (cleanStr.length() == 16) {
                sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            }

            Date dateNotif = sdf.parse(cleanStr);
            long diffInMillis = new Date().getTime() - dateNotif.getTime();
            long diffInSeconds = diffInMillis / 1000;

            if (diffInSeconds < 60) return "Vừa xong";

            long diffInMinutes = diffInSeconds / 60;
            if (diffInMinutes < 60) return diffInMinutes + " phút trước";

            long diffInHours = diffInMinutes / 60;
            if (diffInHours < 24) return diffInHours + " giờ trước";

            long diffInDays = diffInHours / 24;
            if (diffInDays < 30) return diffInDays + " ngày trước";

            return cleanStr.substring(0, 10); // Quá 1 tháng thì hiện ngày cố định
        } catch (Exception e) {
            return dateTimeStr;
        }
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class AdminNotifViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvMessage, tvTime, tvTarget;
        ImageView btnEdit, btnDelete;

        public AdminNotifViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvAdminNotifTitle);
            tvMessage = itemView.findViewById(R.id.tvAdminNotifMessage);
            tvTime = itemView.findViewById(R.id.tvAdminNotifTime);
            tvTarget = itemView.findViewById(R.id.tvAdminNotifTarget);
            btnEdit = itemView.findViewById(R.id.btnAdminEditNotif);
            btnDelete = itemView.findViewById(R.id.btnAdminDeleteNotif);
        }
    }
}