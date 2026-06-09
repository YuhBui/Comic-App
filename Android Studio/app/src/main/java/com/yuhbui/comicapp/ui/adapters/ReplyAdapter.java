package com.yuhbui.comicapp.ui.adapters;

import android.text.Html; // Thêm để xử lý định dạng màu cho chữ @tag
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

import retrofit2.Call;

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

        // ĐÃ SỬA: Hiển thị Tên người dùng thực tế nếu có thay vì ép ID thô cứng
        if (reply.getUserDisplayName() != null && !reply.getUserDisplayName().isEmpty()) {
            holder.tvUserReply.setText(reply.getUserDisplayName());
        } else {
            holder.tvUserReply.setText("Thành viên #" + reply.getUserId());
        }

        // ĐÃ SỬA: Tô màu xanh dương nổi bật cho thẻ tag @tên_user của bình luận cháu
        String content = reply.getContent();
        if (content != null && content.trim().startsWith("@")) {
            int firstSpaceIndex = content.indexOf(" ");
            if (firstSpaceIndex != -1) {
                String tagPart = content.substring(0, firstSpaceIndex);
                String mainText = content.substring(firstSpaceIndex);
                holder.tvReplyContent.setText(Html.fromHtml("<b><font color='#1E88E5'>" + tagPart + "</font></b>" + mainText, Html.FROM_HTML_MODE_COMPACT));
            } else {
                holder.tvReplyContent.setText(content);
            }
        } else {
            holder.tvReplyContent.setText(content);
        }

        holder.btnLike.setText("👍 Thích (" + reply.getLikeCount() + ")");
        holder.btnDislike.setText("👎 Ghét (" + reply.getDislikeCount() + ")");

        int currentUserId = com.yuhbui.comicapp.utils.SharedPrefsManager.getUserId(holder.itemView.getContext());

        holder.btnLike.setOnClickListener(v -> {
            if (currentUserId == -1) {
                android.widget.Toast.makeText(holder.itemView.getContext(), "Vui lòng đăng nhập!", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            executeInteraction(holder, reply.getCommentId(), currentUserId, 1);
        });

        holder.btnDislike.setOnClickListener(v -> {
            if (currentUserId == -1) {
                android.widget.Toast.makeText(holder.itemView.getContext(), "Vui lòng đăng nhập!", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            executeInteraction(holder, reply.getCommentId(), currentUserId, -1);
        });

        holder.btnReplyToReply.setOnClickListener(v -> {
            if (replyListener != null) {
                replyListener.onReplyToReplyClick(reply);
            }
        });

        holder.btnReport.setOnClickListener(v -> {
            if (currentUserId == -1) {
                android.widget.Toast.makeText(holder.itemView.getContext(), "Vui lòng đăng nhập để báo cáo!", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            showReportDialog(holder, reply.getCommentId(), currentUserId);
        });
    }

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
                    @Override public void onFailure(Call<Comment> call, Throwable t) {}
                });
    }

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

    static class ReplyViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserReply, tvReplyContent;
        TextView btnLike, btnDislike, btnReport;
        TextView btnReplyToReply;

        public ReplyViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserReply = itemView.findViewById(R.id.tvUserReply);
            tvReplyContent = itemView.findViewById(R.id.tvReplyContent);
            btnLike = itemView.findViewById(R.id.btnLikeReply);
            btnDislike = itemView.findViewById(R.id.btnDislikeReply);
            btnReport = itemView.findViewById(R.id.btnReportReply);
            btnReplyToReply = itemView.findViewById(R.id.btnReplyToReply);
        }
    }
}