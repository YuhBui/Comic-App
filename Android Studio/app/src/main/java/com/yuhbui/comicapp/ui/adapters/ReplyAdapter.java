package com.yuhbui.comicapp.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.yuhbui.comicapp.R;
import com.yuhbui.comicapp.data.model.Comment;
import java.util.ArrayList;
import java.util.List;

public class ReplyAdapter extends RecyclerView.Adapter<ReplyAdapter.ReplyViewHolder> {
    private List<Comment> replies = new ArrayList<>();

    public void setReplies(List<Comment> replies) {
        this.replies = replies;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReplyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reply, parent, false);
        return new ReplyViewHolder(view);
    }

    // --- INTERFACE BẮT SỰ KIỆN CLICK PHẢN HỒI BÌNH LUẬN CON ---
    public interface OnReplyToReplyClickListener {
        void onReplyToReplyClick(Comment childComment);
    }

    private OnReplyToReplyClickListener replyListener;

    public void setOnReplyToReplyClickListener(OnReplyToReplyClickListener listener) {
        this.replyListener = listener;
    }

    @Override
    public void onBindViewHolder(@NonNull ReplyViewHolder holder, int position) {
        Comment reply = replies.get(position);
        holder.tvUserReply.setText("Thành viên #" + reply.getUserId());
        holder.tvReplyContent.setText(reply.getContent());

        // Hiển thị số lượt Like/Dislike hiện tại của bình luận con
        holder.btnLike.setText("👍 Thích (" + reply.getLikeCount() + ")");
        holder.btnDislike.setText("👎 Ghét (" + reply.getDislikeCount() + ")");

        int currentUserId = com.yuhbui.comicapp.utils.SharedPrefsManager.getUserId(holder.itemView.getContext());

        // 1. Xử lý sự kiện bấm LIKE bình luận con
        holder.btnLike.setOnClickListener(v -> {
            if (currentUserId == -1) {
                android.widget.Toast.makeText(holder.itemView.getContext(), "Vui lòng đăng nhập!", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            executeInteraction(holder, reply.getCommentId(), currentUserId, 1);
        });

        // 2. Xử lý sự kiện bấm DISLIKE bình luận con
        holder.btnDislike.setOnClickListener(v -> {
            if (currentUserId == -1) {
                android.widget.Toast.makeText(holder.itemView.getContext(), "Vui lòng đăng nhập!", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            executeInteraction(holder, reply.getCommentId(), currentUserId, -1);
        });

        // 3. Xử lý sự kiện bấm PHẢN HỒI bình luận con
        holder.btnReplyToReply.setOnClickListener(v -> {
            if (replyListener != null) {
                replyListener.onReplyToReplyClick(reply);
            }
        });

        // 4. Xử lý sự kiện bấm BÁO CÁO bình luận con
        holder.btnReport.setOnClickListener(v -> {
            if (currentUserId == -1) {
                android.widget.Toast.makeText(holder.itemView.getContext(), "Vui lòng đăng nhập để báo cáo!", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            showReportDialog(holder, reply.getCommentId(), currentUserId);
        });
    }

    // Hàm gọi API Like/Dislike
    private void executeInteraction(ReplyViewHolder holder, int commentId, int userId, int type) {
        com.yuhbui.comicapp.data.api.ApiClient.getApiService().interactWithComment(commentId, userId, type)
                .enqueue(new retrofit2.Callback<Comment>() {
                    @Override
                    public void onResponse(retrofit2.Call<Comment> call, retrofit2.Response<Comment> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Comment updatedReply = response.body();
                            holder.btnLike.setText("👍 Thích (" + updatedReply.getLikeCount() + ")");
                            holder.btnDislike.setText("👎 Ghét (" + updatedReply.getDislikeCount() + ")");
                        }
                    }
                    @Override
                    public void onFailure(retrofit2.Call<Comment> call, Throwable t) {}
                });
    }

    // Hàm hiển thị Dialog nhập lý do báo cáo
    private void showReportDialog(ReplyViewHolder holder, int commentId, int userId) {
        android.view.View dialogView = android.view.LayoutInflater.from(holder.itemView.getContext()).inflate(R.layout.dialog_report, null);
        android.widget.EditText edtReason = dialogView.findViewById(R.id.edtReportReason);

        new android.app.AlertDialog.Builder(holder.itemView.getContext())
                .setTitle("Báo cáo phản hồi xấu")
                .setView(dialogView)
                .setPositiveButton("Gửi", (dialog, which) -> {
                    String reason = edtReason.getText().toString().trim();
                    if (!reason.isEmpty()) {
                        sendReport(commentId, userId, reason, holder.itemView.getContext());
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    // Hàm gọi API gửi Báo cáo
    private void sendReport(int commentId, int userId, String reason, android.content.Context context) {
        com.yuhbui.comicapp.data.api.ApiClient.getApiService().reportComment(commentId, userId, reason)
                .enqueue(new retrofit2.Callback<String>() {
                    @Override
                    public void onResponse(retrofit2.Call<String> call, retrofit2.Response<String> response) {
                        if (response.isSuccessful()) {
                            android.widget.Toast.makeText(context, "Cảm ơn bạn đã báo cáo phản hồi này!", android.widget.Toast.LENGTH_SHORT).show();
                        } else {
                            android.widget.Toast.makeText(context, "Bạn đã báo cáo phản hồi này rồi.", android.widget.Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(retrofit2.Call<String> call, Throwable t) {}
                });
    }

    @Override
    public int getItemCount() { return replies.size(); }

    // LỚP VIEW HOLDER CẬP NHẬT ĐẦY ĐỦ ÁNH XẠ
    static class ReplyViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserReply, tvReplyContent;
        TextView btnLike, btnDislike, btnReport;
        TextView btnReplyToReply; // Thêm biến nút Phản hồi con

        public ReplyViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserReply = itemView.findViewById(R.id.tvUserReply);
            tvReplyContent = itemView.findViewById(R.id.tvReplyContent);
            btnLike = itemView.findViewById(R.id.btnLikeReply);
            btnDislike = itemView.findViewById(R.id.btnDislikeReply);
            btnReport = itemView.findViewById(R.id.btnReportReply);
            btnReplyToReply = itemView.findViewById(R.id.btnReplyToReply); // Thực hiện ánh xạ View
        }
    }
}