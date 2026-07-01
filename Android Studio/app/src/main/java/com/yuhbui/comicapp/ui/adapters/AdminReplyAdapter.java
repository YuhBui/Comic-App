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
import com.yuhbui.comicapp.data.api.ApiClient;
import com.yuhbui.comicapp.data.model.Comment;
import com.yuhbui.comicapp.utils.SharedPrefsManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Adapter hiển thị danh sách phản hồi con (reply) trong màn hình Admin.
 * Khác với ReplyAdapter (phía User), adapter này:
 *  - Report icon → xem danh sách người báo cáo (không phải form submit)
 *  - Delete icon → xóa phản hồi bất kỳ (quyền Admin)
 *  - Không có text label "Phản hồi" / "Báo cáo", chỉ dùng icon
 */
public class AdminReplyAdapter extends RecyclerView.Adapter<AdminReplyAdapter.AdminReplyViewHolder> {

    private List<Comment> replies = new ArrayList<>();
    private final int parentCommentId;
    private final AdminCommentAdapter.OnCommentAdminActionListener listener;

    public AdminReplyAdapter(int parentCommentId, AdminCommentAdapter.OnCommentAdminActionListener listener) {
        this.parentCommentId = parentCommentId;
        this.listener = listener;
    }

    public void setReplies(List<Comment> replies) {
        this.replies = replies;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AdminReplyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_reply, parent, false);
        return new AdminReplyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminReplyViewHolder holder, int position) {
        Comment reply = replies.get(position);
        int replyId = reply.getCommentId();
        int currentUserId = SharedPrefsManager.getUserId(holder.itemView.getContext());

        // ---- Avatar ----
        Glide.with(holder.itemView.getContext())
                .load(reply.getUserAvatarUrl())
                .placeholder(R.drawable.ic_launcher_background)
                .circleCrop()
                .into(holder.imgAvatar);

        holder.imgAvatar.setOnClickListener(v ->
                AdminCommentAdapter.navigateToUserDetail(v.getContext(), reply.getUserId()));

        // ---- Tên người dùng ----
        String displayName = (reply.getUserDisplayName() != null && !reply.getUserDisplayName().isEmpty())
                ? reply.getUserDisplayName()
                : "Thành viên #" + reply.getUserId();
        holder.tvUsername.setText(displayName);

        // ---- Nội dung (highlight @mention) ----
        String content = reply.getContent();
        if (content != null && content.trim().startsWith("@")) {
            int spaceIdx = content.indexOf(" ");
            if (spaceIdx != -1) {
                String tagPart = content.substring(0, spaceIdx);
                String mainText = content.substring(spaceIdx);
                holder.tvContent.setText(Html.fromHtml(
                        "<b><font color='#FFB77D'>" + tagPart + "</font></b>" + mainText,
                        Html.FROM_HTML_MODE_COMPACT));
            } else {
                holder.tvContent.setText(content);
            }
        } else {
            holder.tvContent.setText(content);
        }

        // ---- Like / Dislike count ----
        holder.tvLikeCount.setText(String.valueOf(reply.getLikeCount()));
        holder.tvDislikeCount.setText(String.valueOf(reply.getDislikeCount()));

        // ---- Report count badge ----
        int reportCount = reply.getReportCount();
        if (reportCount > 0) {
            holder.tvReportCount.setVisibility(View.VISIBLE);
            holder.tvReportCount.setText(String.valueOf(reportCount));
        } else {
            holder.tvReportCount.setVisibility(View.GONE);
        }

        // ---- Trạng thái fill màu Like / Dislike ----
        updateLikeDislikeUI(holder, reply);

        // ---- Nút Like ----
        holder.layoutLike.setOnClickListener(v -> {
            if (reply.getUserId() == currentUserId && currentUserId != -1) {
                Toast.makeText(holder.itemView.getContext(), "Không thể tự thích phản hồi của mình!", Toast.LENGTH_SHORT).show();
                return;
            }
            executeInteraction(holder, reply, currentUserId, 1);
        });

        // ---- Nút Dislike ----
        holder.layoutDislike.setOnClickListener(v -> {
            if (reply.getUserId() == currentUserId && currentUserId != -1) {
                Toast.makeText(holder.itemView.getContext(), "Không thể tự ghét phản hồi của mình!", Toast.LENGTH_SHORT).show();
                return;
            }
            executeInteraction(holder, reply, currentUserId, -1);
        });

        // ---- Nút Phản hồi → gọi lại Activity để điền @username vào EditText ----
        holder.layoutReply.setOnClickListener(v -> {
            if (listener != null) {
                Map<String, Object> ghostMap = new HashMap<>();
                // Giữ parentCommentId gốc để reply lồng nhau vẫn thuộc thread chính
                ghostMap.put("commentId", parentCommentId);
                ghostMap.put("userId", reply.getUserId());
                ghostMap.put("username", displayName);
                listener.onReply(ghostMap);
            }
        });

        // ---- Nút Report → Admin xem danh sách người báo cáo ----
        holder.layoutReport.setOnClickListener(v -> {
            if (listener != null) listener.onShowReports(replyId);
        });

        // ---- Nút Xóa → Admin xóa bất kỳ phản hồi ----
        holder.layoutDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(replyId, holder.getAdapterPosition());
        });
    }
    private void executeInteraction(AdminReplyViewHolder holder, Comment reply, int userId, int type) {
        ApiClient.getApiService().interactWithComment(reply.getCommentId(), userId, type)
                .enqueue(new Callback<Comment>() {
                    @Override
                    public void onResponse(Call<Comment> call, Response<Comment> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Comment updated = response.body();
                            reply.setLikeCount(updated.getLikeCount());
                            reply.setDislikeCount(updated.getDislikeCount());
                            reply.setLiked(updated.isLiked());
                            reply.setDisliked(updated.isDisliked());
                            holder.tvLikeCount.setText(String.valueOf(reply.getLikeCount()));
                            holder.tvDislikeCount.setText(String.valueOf(reply.getDislikeCount()));
                            updateLikeDislikeUI(holder, reply);
                        }
                    }
                    @Override public void onFailure(Call<Comment> call, Throwable t) {}
                });
    }

    private void updateLikeDislikeUI(AdminReplyViewHolder holder, Comment reply) {
        if (reply.isLiked()) {
            holder.imgLikeIcon.setImageResource(R.drawable.ic_thumb_up_filled);
            holder.imgLikeIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#FFB77D")));
            holder.tvLikeCount.setTextColor(Color.parseColor("#FFB77D"));
        } else {
            holder.imgLikeIcon.setImageResource(R.drawable.ic_thumb_up_outline);
            holder.imgLikeIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#DBC2B0")));
            holder.tvLikeCount.setTextColor(Color.parseColor("#DBC2B0"));
        }

        if (reply.isDisliked()) {
            holder.imgDislikeIcon.setImageResource(R.drawable.ic_thumb_down_filled);
            holder.imgDislikeIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#E91E63")));
            holder.tvDislikeCount.setTextColor(Color.parseColor("#E91E63"));
        } else {
            holder.imgDislikeIcon.setImageResource(R.drawable.ic_thumb_down_outline);
            holder.imgDislikeIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#DBC2B0")));
            holder.tvDislikeCount.setTextColor(Color.parseColor("#DBC2B0"));
        }
    }

    @Override
    public int getItemCount() { return replies.size(); }

    // ViewHolder
    static class AdminReplyViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar, imgLikeIcon, imgDislikeIcon;
        TextView tvUsername, tvContent, tvLikeCount, tvDislikeCount, tvReportCount;
        View layoutLike, layoutDislike, layoutReply, layoutReport, layoutDelete;

        public AdminReplyViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar      = itemView.findViewById(R.id.imgAdminReplyAvatar);
            tvUsername     = itemView.findViewById(R.id.tvAdminReplyUsername);
            tvContent      = itemView.findViewById(R.id.tvAdminReplyContent);
            layoutLike     = itemView.findViewById(R.id.layoutAdminReplyLike);
            layoutDislike  = itemView.findViewById(R.id.layoutAdminReplyDislike);
            layoutReply    = itemView.findViewById(R.id.layoutAdminReplyReply);
            layoutReport   = itemView.findViewById(R.id.layoutAdminReplyReport);
            layoutDelete   = itemView.findViewById(R.id.layoutAdminReplyDelete);
            tvLikeCount    = itemView.findViewById(R.id.tvAdminReplyLikeCount);
            tvDislikeCount = itemView.findViewById(R.id.tvAdminReplyDislikeCount);
            tvReportCount  = itemView.findViewById(R.id.tvAdminReplyReportCount);
            imgLikeIcon    = itemView.findViewById(R.id.imgAdminReplyLikeIcon);
            imgDislikeIcon = itemView.findViewById(R.id.imgAdminReplyDislikeIcon);
        }
    }
}
