package com.yuhbui.comicapp.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.yuhbui.comicapp.R;
import com.yuhbui.comicapp.data.model.Notification;
import java.util.ArrayList;
import java.util.List;

public class AdminNotificationAdapter extends RecyclerView.Adapter<AdminNotificationAdapter.AdminNotifViewHolder> {

    private List<Notification> list = new ArrayList<>();
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

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(notif);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(notif, holder.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class AdminNotifViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvMessage;
        Button btnEdit, btnDelete;
        public AdminNotifViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvAdminNotifTitle);
            tvMessage = itemView.findViewById(R.id.tvAdminNotifMessage);
            btnEdit = itemView.findViewById(R.id.btnAdminEditNotif);
            btnDelete = itemView.findViewById(R.id.btnAdminDeleteNotif);
        }
    }
}