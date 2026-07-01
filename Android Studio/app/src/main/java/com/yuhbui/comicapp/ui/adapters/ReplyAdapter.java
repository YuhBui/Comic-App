package com.yuhbui.comicapp.ui.adapters;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.yuhbui.comicapp.R;
import com.yuhbui.comicapp.data.model.Comment;
import com.yuhbui.comicapp.utils.SharedPrefsManager;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReplyAdapter extends RecyclerView.Adapter<ReplyAdapter.ReplyViewHolder> {
    private List<Comment> replies = new ArrayList<>();
    private OnReplyToReplyClickListener replyListener;

    public interface OnReplyToReplyClickListener {
        void onReplyToReplyClick(Comment childComment);
    }

    public void setOnReplyToReplyClickListener(OnReplyToReplyClickListener listener) {
        this.replyListener = listener;
    }

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

    @Override
    public void onBindViewHolder(@NonNull ReplyViewHolder holder, int position) {
        Comment reply = replies.get(position);

        android.graphics.drawable.GradientDrawable bubbleBg = new android.graphics.drawable.GradientDrawable();
        bubbleBg.setCornerRadius(24f);
        bubbleBg.setColor(Color.parseColor("#1E1E1E"));
        holder.itemView.setBackground(bubbleBg);

        if (holder.itemView.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) holder.itemView.getLayoutParams();
            params.setMargins(0, 6, 24, 6);
            holder.itemView.setLayoutParams(params);
        }

        holder.itemView.setPadding(32, 18, 32, 18);

        Glide.with(holder.itemView.getContext())
                .load(reply.getUserAvatarUrl())
                .placeholder(R.drawable.ic_launcher_background)
                .circleCrop()
                .into(holder.imgReplyAvatar);

        if (reply.getChapterName() != null && !reply.getChapterName().isEmpty()) {
            holder.tvReplyChapterTag.setVisibility(View.VISIBLE);
            holder.tvReplyChapterTag.setText("• " + reply.getChapterName());
        } else {
            holder.tvReplyChapterTag.setVisibility(View.GONE);
        }

        if (reply.getUserDisplayName() != null && !reply.getUserDisplayName().isEmpty()) {
            holder.tvUserReply.setText(reply.getUserDisplayName());
        } else {
            holder.tvUserReply.setText("Thành viên #" + reply.getUserId());
        }

        String content = reply.getContent();
        if (content != null && content.trim().startsWith("@")) {
            int firstSpaceIndex = content.indexOf(" ");
            if (firstSpaceIndex != -1) {
                String tagPart = content.substring(0, firstSpaceIndex);
                String mainText = content.substring(firstSpaceIndex);
                holder.tvReplyContent.setText(Html.fromHtml("<b><font color='#FFB77D'>" + tagPart + "</font></b>" + mainText, Html.FROM_HTML_MODE_COMPACT));
            } else {
                holder.tvReplyContent.setText(content);
            }
        } else {
            holder.tvReplyContent.setText(content);
        }

        holder.tvLikeCount.setText("(" + reply.getLikeCount() + ")");
        holder.tvDislikeCount.setText("(" + reply.getDislikeCount() + ")");

        if (reply.isLiked()) {
            holder.imgLikeIcon.setImageResource(R.drawable.ic_thumb_up_filled);
            holder.imgLikeIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#FFB77D")));
        } else {
            holder.imgLikeIcon.setImageResource(R.drawable.ic_thumb_up_outline);
            holder.imgLikeIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#666666")));
        }

        if (reply.isDisliked()) {
            holder.imgDislikeIcon.setImageResource(R.drawable.ic_thumb_down_filled);
            holder.imgDislikeIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#E91E63")));
        } else {
            holder.imgDislikeIcon.setImageResource(R.drawable.ic_thumb_down_outline);
            holder.imgDislikeIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#666666")));
        }

        int currentUserId = SharedPrefsManager.getUserId(holder.itemView.getContext());

        // CHỐNG TỰ TƯƠNG TÁC CHO BÌNH LUẬN REPLIES CHÍNH CHỦ
        if (reply.getUserId() == currentUserId && currentUserId != -1) {
            holder.layoutLike.setVisibility(View.VISIBLE);
            holder.layoutDislike.setVisibility(View.VISIBLE);

            holder.layoutLike.setOnClickListener(v ->
                    Toast.makeText(holder.itemView.getContext(), "Bạn không thể tự thích phản hồi của chính mình!", Toast.LENGTH_SHORT).show()
            );

            holder.layoutDislike.setOnClickListener(v ->
                    Toast.makeText(holder.itemView.getContext(), "Bạn không thể tự ghét phản hồi của chính mình!", Toast.LENGTH_SHORT).show()
            );

            // Chuyển đổi Icon sang dạng xóa cho tài khoản chính chủ
            holder.imgReport.setImageResource(R.drawable.ic_delete);
            holder.imgReport.setImageTintList(ColorStateList.valueOf(Color.parseColor("#F44336")));

            holder.layoutReport.setOnClickListener(v -> new android.app.AlertDialog.Builder(holder.itemView.getContext())
                    .setTitle("Xóa phản hồi")
                    .setMessage("Bạn có chắc chắn muốn xóa phản hồi này không?")
                    .setPositiveButton("Xóa", (dialog, which) -> {
                        com.yuhbui.comicapp.data.api.ApiClient.getApiService().deleteComment(reply.getCommentId(), currentUserId).enqueue(new Callback<Comment>() {
                            @Override
                            public void onResponse(Call<Comment> call, Response<Comment> response) {
                                if (response.isSuccessful()) {
                                    Toast.makeText(holder.itemView.getContext(), "Đã xóa phản hồi!", Toast.LENGTH_SHORT).show();
                                    replies.remove(holder.getAdapterPosition());
                                    notifyItemRemoved(holder.getAdapterPosition());
                                }
                            }
                            @Override public void onFailure(Call<Comment> call, Throwable t) {}
                        });
                    })
                    .setNegativeButton("Hủy", null)
                    .show());
        } else {
            holder.layoutLike.setVisibility(View.VISIBLE);
            holder.layoutDislike.setVisibility(View.VISIBLE);

            // Thiết lập Icon tố cáo mặc định đối với tài khoản người dùng khác
            holder.imgReport.setImageResource(R.drawable.ic_report);
            holder.imgReport.setImageTintList(ColorStateList.valueOf(Color.parseColor("#E91E63")));

            holder.layoutLike.setOnClickListener(v -> {
                if (currentUserId == -1) {
                    Toast.makeText(holder.itemView.getContext(), "Vui lòng đăng nhập!", Toast.LENGTH_SHORT).show();
                    return;
                }
                executeInteraction(holder, reply, currentUserId, 1);
            });

            holder.layoutDislike.setOnClickListener(v -> {
                if (currentUserId == -1) {
                    Toast.makeText(holder.itemView.getContext(), "Vui lòng đăng nhập!", Toast.LENGTH_SHORT).show();
                    return;
                }
                executeInteraction(holder, reply, currentUserId, -1);
            });

            holder.layoutReport.setOnClickListener(v -> {
                if (currentUserId == -1) {
                    Toast.makeText(holder.itemView.getContext(), "Vui lòng đăng nhập để báo cáo!", Toast.LENGTH_SHORT).show();
                    return;
                }
                showReportDialog(holder, reply.getCommentId(), currentUserId);
            });
        }

        holder.layoutReplyToReply.setOnClickListener(v -> {
            if (replyListener != null) {
                replyListener.onReplyToReplyClick(reply);
            }
        });
    }

    private void executeInteraction(ReplyViewHolder holder, Comment reply, int userId, int type) {
        com.yuhbui.comicapp.data.api.ApiClient.getApiService().interactWithComment(reply.getCommentId(), userId, type)
                .enqueue(new Callback<Comment>() {
                    @Override
                    public void onResponse(Call<Comment> call, Response<Comment> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Comment updatedReply = response.body();

                            reply.setLikeCount(updatedReply.getLikeCount());
                            reply.setDislikeCount(updatedReply.getDislikeCount());
                            reply.setLiked(updatedReply.isLiked());
                            reply.setDisliked(updatedReply.isDisliked());

                            holder.tvLikeCount.setText("(" + reply.getLikeCount() + ")");
                            holder.tvDislikeCount.setText("(" + reply.getDislikeCount() + ")");

                            updateReplyLikeDislikeUI(holder, reply);
                        }
                    }
                    @Override public void onFailure(Call<Comment> call, Throwable t) {}
                });
    }

    private void showReportDialog(ReplyViewHolder holder, int commentId, int userId) {
        View dialogView = LayoutInflater.from(holder.itemView.getContext()).inflate(R.layout.dialog_report, null);
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
                .enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(context, "Cảm ơn bạn đã báo cáo phản hồi này!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, "Bạn đã báo cáo phản hồi này rồi.", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<String> call, Throwable t) {}
                });
    }

    @Override
    public int getItemCount() { return replies.size(); }

    static class ReplyViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserReply, tvReplyContent, tvLikeCount, tvDislikeCount;
        TextView tvReplyChapterTag;
        View layoutLike, layoutDislike, layoutReplyToReply, layoutReport;
        ImageView imgReport, imgLikeIcon, imgDislikeIcon;
        ImageView imgReplyAvatar;

        public ReplyViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserReply = itemView.findViewById(R.id.tvUserReply);
            tvReplyContent = itemView.findViewById(R.id.tvReplyContent);

            imgReplyAvatar = itemView.findViewById(R.id.imgReplyAvatar);
            tvReplyChapterTag = itemView.findViewById(R.id.tvReplyChapterTag);

            layoutLike = itemView.findViewById(R.id.layoutLikeReply);
            layoutDislike = itemView.findViewById(R.id.layoutDislikeReply);
            layoutReplyToReply = itemView.findViewById(R.id.layoutReplyToReply);
            layoutReport = itemView.findViewById(R.id.layoutReportReply);

            tvLikeCount = itemView.findViewById(R.id.tvLikeReplyCount);
            tvDislikeCount = itemView.findViewById(R.id.tvDislikeReplyCount);
            imgReport = itemView.findViewById(R.id.imgReportReply);

            imgLikeIcon = itemView.findViewById(R.id.imgLikeReplyIcon);
            imgDislikeIcon = itemView.findViewById(R.id.imgDislikeReplyIcon);
        }
    }

    private void updateReplyLikeDislikeUI(ReplyViewHolder holder, Comment reply) {
        if (reply.isLiked()) {
            holder.imgLikeIcon.setImageResource(R.drawable.ic_thumb_up_filled);
            holder.imgLikeIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#FFB77D")));
            holder.tvLikeCount.setTextColor(Color.parseColor("#FFB77D"));
        } else {
            holder.imgLikeIcon.setImageResource(R.drawable.ic_thumb_up_outline);
            holder.imgLikeIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#666666")));
            holder.tvLikeCount.setTextColor(Color.parseColor("#666666"));
        }

        if (reply.isDisliked()) {
            holder.imgDislikeIcon.setImageResource(R.drawable.ic_thumb_down_filled);
            holder.imgDislikeIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#E91E63")));
            holder.tvDislikeCount.setTextColor(Color.parseColor("#E91E63"));
        } else {
            holder.imgDislikeIcon.setImageResource(R.drawable.ic_thumb_down_outline);
            holder.imgDislikeIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#666666")));
            holder.tvDislikeCount.setTextColor(Color.parseColor("#666666"));
        }
    }
}