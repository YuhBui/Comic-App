package com.yuhbui.comicapp.ui.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.yuhbui.comicapp.R;
import com.yuhbui.comicapp.data.api.ApiClient;
import com.yuhbui.comicapp.data.model.Comment;
import com.yuhbui.comicapp.utils.SharedPrefsManager;
import java.util.ArrayList;
import java.util.Collections; // Bổ sung để sắp xếp dữ liệu
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private List<Comment> commentList = new ArrayList<>();
    private OnCommentClickListener replyListener;

    private final Map<Integer, List<Comment>> repliesCache = new HashMap<>();
    private final Map<Integer, Integer> displayedCountCache = new HashMap<>();

    public interface OnCommentClickListener {
        void onReplyClick(Comment parentComment);
    }

    public void setOnCommentClickListener(OnCommentClickListener listener) {
        this.replyListener = listener;
    }

    public void setComments(List<Comment> comments) {
        this.commentList = comments;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = commentList.get(position);
        Context context = holder.itemView.getContext();
        int commentId = comment.getCommentId();

        if (comment.getUserDisplayName() != null && !comment.getUserDisplayName().isEmpty()) {
            holder.tvUserComment.setText(comment.getUserDisplayName());
        } else {
            holder.tvUserComment.setText("Thành viên ẩn danh");
        }

        if (comment.getChapterName() != null && !comment.getChapterName().isEmpty()) {
            holder.tvCommentChapterTag.setVisibility(View.VISIBLE);
            holder.tvCommentChapterTag.setText("• " + comment.getChapterName());
        } else {
            holder.tvCommentChapterTag.setVisibility(View.GONE);
        }

        Glide.with(context)
                .load(comment.getUserAvatarUrl())
                .placeholder(R.drawable.ic_launcher_background)
                .circleCrop()
                .into(holder.imgUserAvatar);

        holder.tvCommentContent.setText(comment.getContent());

        holder.btnLike.setText("👍 Thích (" + comment.getLikeCount() + ")");
        holder.btnDislike.setText("👎 Ghét (" + comment.getDislikeCount() + ")");
        holder.btnReply.setText("💬 Phản hồi (" + comment.getReplyCount() + ")");

        holder.rvReplies.setLayoutManager(new LinearLayoutManager(context));
        ReplyAdapter replyAdapter = new ReplyAdapter();
        holder.rvReplies.setAdapter(replyAdapter);

        if (!repliesCache.containsKey(commentId)) {
            repliesCache.put(commentId, new ArrayList<>());
            displayedCountCache.put(commentId, 0);
        }

        List<Comment> cachedReplies = repliesCache.get(commentId);
        int currentDisplayedCount = displayedCountCache.get(commentId);
        int totalRepliesCount = comment.getReplyCount();

        if (currentDisplayedCount > 0 && !cachedReplies.isEmpty()) {
            holder.rvReplies.setVisibility(View.VISIBLE);
            int endBound = Math.min(currentDisplayedCount, cachedReplies.size());
            replyAdapter.setReplies(new ArrayList<>(cachedReplies.subList(0, endBound)));

            int remaining = totalRepliesCount - currentDisplayedCount;
            if (remaining > 0) {
                holder.tvLoadMoreReplies.setText("—— Xem thêm " + Math.min(10, remaining) + " phản hồi ——");
            } else {
                holder.tvLoadMoreReplies.setText("—— Thu gọn phản hồi ——");
            }
            holder.tvLoadMoreReplies.setVisibility(View.VISIBLE);
        } else {
            holder.rvReplies.setVisibility(View.GONE);
            replyAdapter.setReplies(new ArrayList<>());

            // ĐÃ SỬA: Bình luận cha không có phản hồi thì ẩn hoàn toàn vùng text phản hồi
            if (totalRepliesCount > 0) {
                holder.tvLoadMoreReplies.setVisibility(View.VISIBLE);
                holder.tvLoadMoreReplies.setText("—— Xem phản hồi (" + totalRepliesCount + ") ——");
            } else {
                holder.tvLoadMoreReplies.setVisibility(View.GONE);
            }
        }

        // ĐÃ SỬA: Bắt sự kiện phản hồi của bình luận con (Cháu) -> Truyền thêm tên để chèn `@tên_user`
        replyAdapter.setOnReplyToReplyClickListener(childComment -> {
            if (replyListener != null) {
                Comment ghostComment = new Comment();
                ghostComment.setCommentId(comment.getCommentId()); // Gốc luồng vẫn ăn theo cha lớn nhất
                ghostComment.setUserId(childComment.getUserId());

                String validName = (childComment.getUserDisplayName() != null && !childComment.getUserDisplayName().isEmpty())
                        ? childComment.getUserDisplayName() : "Thành viên #" + childComment.getUserId();
                ghostComment.setUserDisplayName(validName); // Gán tên hiển thị đích danh

                replyListener.onReplyClick(ghostComment);
            }
        });

        int currentUserId = SharedPrefsManager.getUserId(context);

        holder.btnLike.setOnClickListener(v -> {
            if (currentUserId == -1) {
                Toast.makeText(context, "Vui lòng đăng nhập để tương tác!", Toast.LENGTH_SHORT).show();
                return;
            }
            executeInteraction(holder, comment.getCommentId(), currentUserId, 1);
        });

        holder.btnDislike.setOnClickListener(v -> {
            if (currentUserId == -1) {
                Toast.makeText(context, "Vui lòng đăng nhập để tương tác!", Toast.LENGTH_SHORT).show();
                return;
            }
            executeInteraction(holder, comment.getCommentId(), currentUserId, -1);
        });

        holder.btnReply.setOnClickListener(v -> {
            if (replyListener != null) {
                replyListener.onReplyClick(comment);
            }
        });

        if (comment.getUserId() == currentUserId && currentUserId != -1) {
            holder.btnReport.setText("🗑️ Xóa");
            holder.btnReport.setTextColor(Color.parseColor("#F44336"));
            holder.btnReport.setOnClickListener(v -> new AlertDialog.Builder(context)
                    .setTitle("Xóa bình luận")
                    .setMessage("Bạn có chắc chắn muốn xóa bình luận này không?")
                    .setPositiveButton("Xóa", (dialog, which) -> executeDeleteComment(comment.getCommentId(), currentUserId, context, holder.getAdapterPosition()))
                    .setNegativeButton("Hủy", null)
                    .show());
        } else {
            holder.btnReport.setText("⚠️ Báo cáo");
            holder.btnReport.setTextColor(Color.parseColor("#E91E63"));
            holder.btnReport.setOnClickListener(v -> {
                if (currentUserId == -1) {
                    Toast.makeText(context, "Vui lòng đăng nhập để báo cáo!", Toast.LENGTH_SHORT).show();
                    return;
                }
                View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_report, null);
                EditText edtReason = dialogView.findViewById(R.id.edtReportReason);
                new AlertDialog.Builder(context)
                        .setTitle("Báo cáo bình luận xấu")
                        .setView(dialogView)
                        .setPositiveButton("Gửi", (dialog, which) -> {
                            String reason = edtReason.getText().toString().trim();
                            if (!reason.isEmpty()) {
                                executeReport(comment.getCommentId(), currentUserId, reason, context);
                            } else {
                                Toast.makeText(context, "Lý do không được để trống!", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            });
        }

        holder.tvLoadMoreReplies.setOnClickListener(v -> {
            List<Comment> currentList = repliesCache.get(commentId);
            if (currentList == null || currentList.isEmpty()) {
                ApiClient.getApiService().getRepliesByParentId(commentId)
                        .enqueue(new Callback<List<Comment>>() {
                            @Override
                            public void onResponse(Call<List<Comment>> call, Response<List<Comment>> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    List<Comment> serverReplies = response.body();

                                    // 1. SẮP XẾP: Luôn hiển thị từ CŨ đến MỚI (Tăng dần theo commentId/Thời gian)
                                    Collections.sort(serverReplies, (c1, c2) -> Integer.compare(c1.getCommentId(), c2.getCommentId()));

                                    // 2. THUẬT TOÁN GẮN TAG: Quét tìm comment cháu để bổ sung chuỗi @tên_user
                                    Map<Integer, Comment> lookupMap = new HashMap<>();
                                    for (Comment r : serverReplies) {
                                        lookupMap.put(r.getCommentId(), r);
                                    }
                                    for (Comment r : serverReplies) {
                                        // Nếu parentCommentId không trùng với ID gốc, nghĩa là r là câu trả lời cho 1 comment con khác (Cháu)
                                        if (r.getParentCommentId() != null && r.getParentCommentId() != commentId) {
                                            Comment immediateParent = lookupMap.get(r.getParentCommentId());
                                            if (immediateParent != null) {
                                                String parentName = (immediateParent.getUserDisplayName() != null && !immediateParent.getUserDisplayName().isEmpty())
                                                        ? immediateParent.getUserDisplayName() : "Thành viên #" + immediateParent.getUserId();

                                                if (r.getContent() != null && !r.getContent().trim().startsWith("@")) {
                                                    r.setContent("@" + parentName + " " + r.getContent());
                                                }
                                            }
                                        }
                                    }

                                    repliesCache.put(commentId, serverReplies);
                                    paginateReplies(commentId, holder, replyAdapter, totalRepliesCount);
                                }
                            }
                            @Override public void onFailure(Call<List<Comment>> call, Throwable t) {}
                        });
            } else {
                paginateReplies(commentId, holder, replyAdapter, totalRepliesCount);
            }
        });
    }

    private void paginateReplies(int commentId, CommentViewHolder holder, ReplyAdapter replyAdapter, int totalCount) {
        List<Comment> fullList = repliesCache.get(commentId);
        int currentCount = displayedCountCache.get(commentId);

        if (currentCount >= fullList.size()) {
            displayedCountCache.put(commentId, 0);
            holder.rvReplies.setVisibility(View.GONE);
            replyAdapter.setReplies(new ArrayList<>());
            holder.tvLoadMoreReplies.setText("—— Xem phản hồi (" + totalCount + ") ——");
            return;
        }

        currentCount += 10;
        if (currentCount > fullList.size()) {
            currentCount = fullList.size();
        }
        displayedCountCache.put(commentId, currentCount);

        holder.rvReplies.setVisibility(View.VISIBLE);
        replyAdapter.setReplies(new ArrayList<>(fullList.subList(0, currentCount)));

        int remaining = totalCount - currentCount;
        if (remaining > 0) {
            holder.tvLoadMoreReplies.setText("—— Xem thêm " + Math.min(10, remaining) + " phản hồi ——");
        } else {
            holder.tvLoadMoreReplies.setText("—— Thu gọn phản hồi ——");
        }
    }

    private void executeInteraction(CommentViewHolder holder, int commentId, int userId, int type) {
        ApiClient.getApiService().interactWithComment(commentId, userId, type)
                .enqueue(new Callback<Comment>() {
                    @Override
                    public void onResponse(Call<Comment> call, Response<Comment> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Comment updatedComment = response.body();
                            holder.btnLike.setText("👍 Thích (" + updatedComment.getLikeCount() + ")");
                            holder.btnDislike.setText("👎 Ghét (" + updatedComment.getDislikeCount() + ")");
                        }
                    }
                    @Override public void onFailure(Call<Comment> call, Throwable t) {}
                });
    }

    private void executeDeleteComment(int commentId, int userId, Context context, int position) {
        ApiClient.getApiService().deleteComment(commentId, userId)
                .enqueue(new Callback<Comment>() {
                    @Override
                    public void onResponse(Call<Comment> call, Response<Comment> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(context, "Đã xóa bình luận!", Toast.LENGTH_SHORT).show();
                            if (position != RecyclerView.NO_POSITION && position < commentList.size()) {
                                commentList.remove(position);
                                notifyItemRemoved(position);
                                notifyItemRangeChanged(position, commentList.size());
                            }
                        }
                    }
                    @Override public void onFailure(Call<Comment> call, Throwable t) {}
                });
    }

    private void executeReport(int commentId, int userId, String reason, Context context) {
        ApiClient.getApiService().reportComment(commentId, userId, reason)
                .enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(context, "Cảm ơn bạn đã báo cáo!", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override public void onFailure(Call<String> call, Throwable t) {}
                });
    }

    @Override
    public int getItemCount() {
        return commentList != null ? commentList.size() : 0;
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserComment, tvCommentContent, btnLike, btnDislike, btnReply, btnReport, tvCommentChapterTag, tvLoadMoreReplies;
        ImageView imgUserAvatar;
        RecyclerView rvReplies;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserComment = itemView.findViewById(R.id.tvUserComment);
            tvCommentContent = itemView.findViewById(R.id.tvCommentContent);
            btnLike = itemView.findViewById(R.id.btnLikeComment);
            btnDislike = itemView.findViewById(R.id.btnDislikeComment);
            btnReply = itemView.findViewById(R.id.btnReplyComment);
            btnReport = itemView.findViewById(R.id.btnReportComment);
            imgUserAvatar = itemView.findViewById(R.id.imgUserAvatar);
            tvCommentChapterTag = itemView.findViewById(R.id.tvCommentChapterTag);
            rvReplies = itemView.findViewById(R.id.recyclerViewReplies);
            tvLoadMoreReplies = itemView.findViewById(R.id.tvLoadMoreReplies);
        }
    }

    public void resetRepliesCache(int parentCommentId) {
        repliesCache.remove(parentCommentId);
        displayedCountCache.put(parentCommentId, 0);
        for (int i = 0; i < commentList.size(); i++) {
            if (commentList.get(i).getCommentId() == parentCommentId) {
                notifyItemChanged(i);
                break;
            }
        }
    }
}