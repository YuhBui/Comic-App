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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdminCommentAdapter extends RecyclerView.Adapter<AdminCommentAdapter.CommentViewHolder> {

    private List<Map<String, Object>> commentList = new ArrayList<>();
    private OnCommentAdminActionListener listener;

    public interface OnCommentAdminActionListener {
        void onReply(Map<String, Object> comment);
        void onDelete(int commentId, int position);
    }

    public AdminCommentAdapter(OnCommentAdminActionListener listener) {
        this.listener = listener;
    }

    public void setData(List<Map<String, Object>> list) {
        this.commentList = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Map<String, Object> comment = commentList.get(position);

        holder.tvUser.setText((String) comment.get("username"));
        holder.tvContent.setText((String) comment.get("content"));

        // HÀM ÉP KIỂU SỐ AN TOÀN TUYỆT ĐỐI CHỐNG SẬP ỨNG DỤNG
        holder.tvLikes.setText("👍 " + getSafeLong(comment.get("likes")));
        holder.tvDislikes.setText("👎 " + getSafeLong(comment.get("dislikes")));
        holder.tvReports.setText("⚠️ Báo cáo (" + getSafeLong(comment.get("reports")) + ")");

        Glide.with(holder.itemView.getContext())
                .load((String) comment.get("avatarUrl"))
                .placeholder(android.R.drawable.sym_def_app_icon)
                .circleCrop()
                .into(holder.imgAvatar);

        holder.btnReply.setOnClickListener(v -> listener.onReply(comment));

        holder.btnDelete.setOnClickListener(v -> {
            int commentId = getSafeInt(comment.get("commentId"));
            listener.onDelete(commentId, position);
        });
    }

    private long getSafeLong(Object obj) {
        if (obj instanceof Number) return ((Number) obj).longValue();
        return 0L;
    }

    private int getSafeInt(Object obj) {
        if (obj instanceof Number) return ((Number) obj).intValue();
        return 0;
    }

    @Override
    public int getItemCount() { return commentList.size(); }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView tvUser, tvContent, tvLikes, tvDislikes, tvReports;
        TextView btnReply, btnDelete;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgAdminCommentAvatar);
            tvUser = itemView.findViewById(R.id.tvAdminCommentUser);
            tvContent = itemView.findViewById(R.id.tvAdminCommentContent);
            tvLikes = itemView.findViewById(R.id.tvAdminCommentLikes);
            tvDislikes = itemView.findViewById(R.id.tvAdminCommentDislikes);
            tvReports = itemView.findViewById(R.id.tvAdminCommentReports);
            btnReply = itemView.findViewById(R.id.btnAdminCommentReply);
            btnDelete = itemView.findViewById(R.id.btnAdminCommentDelete);
        }
    }
}