package com.yuhbui.comicapp.ui.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.yuhbui.comicapp.R;
import com.yuhbui.comicapp.data.model.Notification;
import java.util.ArrayList;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotifViewHolder> {

    private List<Notification> list = new ArrayList<>();
    private OnNotifClickListener listener;

    public interface OnNotifClickListener {
        void onNotifClick(Notification notification);
    }

    public void setData(List<Notification> list, OnNotifClickListener listener) {
        this.list = list;
        this.listener = listener;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotifViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotifViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull NotifViewHolder holder, int position) {
        Notification notif = list.get(position);

        holder.tvTitle.setText(notif.getTitle());
        holder.tvMessage.setText(notif.getMessage());
        holder.tvTime.setText(notif.getCreatedAt() != null ? notif.getCreatedAt().substring(0, 10) : "");

        if (!notif.isRead()) {
            holder.viewDot.setVisibility(View.VISIBLE);
            holder.itemView.setBackgroundColor(Color.parseColor("#F5F9FF")); // Nền xanh nhạt nếu chưa đọc
        } else {
            holder.viewDot.setVisibility(View.GONE);
            holder.itemView.setBackgroundColor(Color.parseColor("#FFFFFF"));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onNotifClick(notif);
        });
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class NotifViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvMessage, tvTime;
        View viewDot;
        public NotifViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvNotifTitle);
            tvMessage = itemView.findViewById(R.id.tvNotifMessage);
            tvTime = itemView.findViewById(R.id.tvNotifTime);
            viewDot = itemView.findViewById(R.id.viewUnreadDot);
        }
    }
}