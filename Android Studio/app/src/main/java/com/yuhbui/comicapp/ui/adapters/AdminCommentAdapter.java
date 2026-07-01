package com.yuhbui.comicapp.ui.adapters;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.yuhbui.comicapp.R;
import com.yuhbui.comicapp.data.api.ApiClient;
import com.yuhbui.comicapp.data.model.Comment;
import com.yuhbui.comicapp.ui.admin.AdminUserDetailActivity;
import com.yuhbui.comicapp.utils.SharedPrefsManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminCommentAdapter extends RecyclerView.Adapter<AdminCommentAdapter.CommentViewHolder> {

    private List<Map<String, Object>> commentList = new ArrayList<>();
    private final OnCommentAdminActionListener listener;
    private final Map<Integer, List<Comment>> repliesCache = new HashMap<>();
    private final Map<Integer, Integer> displayedCountCache = new HashMap<>();

    public interface OnCommentAdminActionListener {
        void onReply(Map<String, Object> comment);
        void onDelete(int commentId, int position);
        void onInteract(int commentId, int type, int position);
        void onShowReports(int commentId);
    }

    public AdminCommentAdapter(OnCommentAdminActionListener listener) {
        this.listener = listener;
    }

    public void setData(List<Map<String, Object>> list) {
        this.commentList = new ArrayList<>(list);
        notifyDataSetChanged();
    }

    /**
     * Sau khi gửi reply thành công: xóa cache của comment cha để buộc fetch lại từ server,
     * và giữ displayedCount hiện tại (không ẫn replies) để sau khi reload data reply mới hiện ngay.
     */
    public void prepareReloadAfterReply(Integer parentCommentId) {
        if (parentCommentId == null) return;
        // Xóa cached replies của comment cha để buộc re-fetch từ server
        repliesCache.remove(parentCommentId);
        // Đặt displayedCount = 10 (tối thiểu) nếu hiện tại đang mở
        Integer currentCount = displayedCountCache.get(parentCommentId);
        if (currentCount == null || currentCount == 0) {
            displayedCountCache.put(parentCommentId, 10);
        }
        // Notify item để trigger re-bind và auto-fetch replies từ server
        for (int i = 0; i < commentList.size(); i++) {
            if (getSafeInt(commentList.get(i).get("commentId")) == parentCommentId) {
                notifyItemChanged(i);
                break;
            }
        }
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
        int commentId = getSafeInt(comment.get("commentId"));

        holder.tvUser.setText((String) comment.get("username"));
        holder.tvContent.setText((String) comment.get("content"));
        holder.tvLikes.setText("(" + getSafeLong(comment.get("likes")) + ")");
        holder.tvDislikes.setText("(" + getSafeLong(comment.get("dislikes")) + ")");

        // Hiển thị chapter tag bên cạnh tên user (giống phía User)
        Object chapterNameObj = comment.get("chapterName");
        if (chapterNameObj instanceof String && !((String) chapterNameObj).isEmpty()) {
            holder.tvChapterTag.setVisibility(View.VISIBLE);
            holder.tvChapterTag.setText("• " + chapterNameObj);
        } else {
            holder.tvChapterTag.setVisibility(View.GONE);
        }

        long reportCount = getSafeLong(comment.get("reports"));
        holder.tvReportsCount.setText(String.valueOf(reportCount));

        Glide.with(holder.itemView.getContext())
                .load((String) comment.get("avatarUrl"))
                .placeholder(android.R.drawable.sym_def_app_icon)
                .circleCrop()
                .into(holder.imgAvatar);

        int commentUserId = getSafeInt(comment.get("userId"));
        holder.imgAvatar.setOnClickListener(v -> navigateToUserDetail(v.getContext(), commentUserId));

        updateAdminLikeDislikeUI(holder, comment);

        holder.rvReplies.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext()));
        holder.rvReplies.setNestedScrollingEnabled(false);
        AdminReplyAdapter replyAdapter = new AdminReplyAdapter(commentId, listener);
        holder.rvReplies.setAdapter(replyAdapter);

        int totalReplies = 0;
        if (comment.containsKey("replyCount") && comment.get("replyCount") != null) {
            totalReplies = getSafeInt(comment.get("replyCount"));
        } else if (comment.containsKey("repliesCount") && comment.get("repliesCount") != null) {
            totalReplies = getSafeInt(comment.get("repliesCount"));
        }

        // Khởi tạo cache ban đầu CHỈ NẾU chưa từng tồn tại
        // Quan trọng: KHÔNG reset displayedCount nếu đã có (ví dụ sau prepareReloadAfterReply)
        if (!repliesCache.containsKey(commentId)) {
            repliesCache.put(commentId, new ArrayList<>());
        }
        if (!displayedCountCache.containsKey(commentId)) {
            displayedCountCache.put(commentId, 0);
        }

        List<Comment> cachedReplies = repliesCache.get(commentId);
        int currentDisplayedCount = displayedCountCache.get(commentId);

        if (currentDisplayedCount > 0 && !cachedReplies.isEmpty()) {
            // Tình huống bình thường: đang mở và có cache
            holder.rvReplies.setVisibility(View.VISIBLE);
            int endBound = Math.min(currentDisplayedCount, cachedReplies.size());
            replyAdapter.setReplies(new ArrayList<>(cachedReplies.subList(0, endBound)));
            holder.tvLoadMoreReplies.setVisibility(View.VISIBLE);
            if (cachedReplies.size() > currentDisplayedCount) {
                holder.tvLoadMoreReplies.setText("—— Xem thêm phản hồi ——");
            } else {
                holder.tvLoadMoreReplies.setText("—— Thu gọn phản hồi ——");
            }
        } else if (currentDisplayedCount > 0 && cachedReplies.isEmpty()) {
            // Tình huống sau khi gửi reply: displayedCount > 0 nhưng cache bị xóa → tự động fetch lại
            holder.rvReplies.setVisibility(View.GONE);
            holder.tvLoadMoreReplies.setVisibility(View.VISIBLE);
            holder.tvLoadMoreReplies.setText("—— Đang tải phản hồi... ——");
            int currentUserId2 = SharedPrefsManager.getUserId(holder.itemView.getContext());
            ApiClient.getApiService().getRepliesByParentId(commentId, currentUserId2 != -1 ? currentUserId2 : null)
                    .enqueue(new Callback<List<Comment>>() {
                        @Override
                        public void onResponse(Call<List<Comment>> call, Response<List<Comment>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                List<Comment> serverReplies = response.body();
                                java.util.Collections.sort(serverReplies, (c1, c2) -> Integer.compare(c1.getCommentId(), c2.getCommentId()));
                                Map<Integer, Comment> lookupMap = new java.util.HashMap<>();
                                for (Comment r : serverReplies) { lookupMap.put(r.getCommentId(), r); }
                                for (Comment r : serverReplies) {
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
                                int showCount = Math.min(currentDisplayedCount, serverReplies.size());
                                if (showCount > 0) {
                                    holder.rvReplies.setVisibility(View.VISIBLE);
                                    replyAdapter.setReplies(new ArrayList<>(serverReplies.subList(0, showCount)));
                                    holder.tvLoadMoreReplies.setVisibility(View.VISIBLE);
                                    if (serverReplies.size() > showCount) {
                                        holder.tvLoadMoreReplies.setText("—— Xem thêm phản hồi ——");
                                    } else {
                                        holder.tvLoadMoreReplies.setText("—— Thu gọn phản hồi ——");
                                    }
                                } else {
                                    holder.rvReplies.setVisibility(View.GONE);
                                    holder.tvLoadMoreReplies.setVisibility(View.GONE);
                                    displayedCountCache.put(commentId, 0);
                                }
                            }
                        }
                        @Override public void onFailure(Call<List<Comment>> call, Throwable t) {
                            holder.tvLoadMoreReplies.setText("—— Xem phản hồi ——");
                        }
                    });
        } else {
            holder.rvReplies.setVisibility(View.GONE);
            replyAdapter.setReplies(new ArrayList<>());
            if (totalReplies > 0) {
                holder.tvLoadMoreReplies.setVisibility(View.VISIBLE);
                holder.tvLoadMoreReplies.setText("—— Xem phản hồi (" + totalReplies + ") ——");
            } else {
                holder.tvLoadMoreReplies.setVisibility(View.GONE);
            }
        }

        int currentUserId = SharedPrefsManager.getUserId(holder.itemView.getContext());

        holder.layoutLike.setOnClickListener(v -> {
            if (commentUserId == currentUserId && currentUserId != -1) return;
            if (listener != null) listener.onInteract(commentId, 1, position);
        });

        holder.layoutDislike.setOnClickListener(v -> {
            if (commentUserId == currentUserId && currentUserId != -1) return;
            if (listener != null) listener.onInteract(commentId, -1, position);
        });

        holder.layoutReply.setOnClickListener(v -> {
            if (listener != null) listener.onReply(comment);
        });

        holder.layoutReport.setOnClickListener(v -> {
            if (listener != null) listener.onShowReports(commentId);
        });

        holder.layoutDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(commentId, position);
        });

        int finalTotalReplies = totalReplies;
        holder.tvLoadMoreReplies.setOnClickListener(v -> {
            List<Comment> currentList = repliesCache.get(commentId);
            if (currentList == null || currentList.isEmpty()) {
                ApiClient.getApiService().getRepliesByParentId(commentId, currentUserId != -1 ? currentUserId : null)
                        .enqueue(new Callback<List<Comment>>() {
                            @Override
                            public void onResponse(Call<List<Comment>> call, Response<List<Comment>> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    List<Comment> serverReplies = response.body();
                                    Collections.sort(serverReplies, (c1, c2) -> Integer.compare(c1.getCommentId(), c2.getCommentId()));

                                    Map<Integer, Comment> lookupMap = new HashMap<>();
                                    for (Comment r : serverReplies) { lookupMap.put(r.getCommentId(), r); }
                                    for (Comment r : serverReplies) {
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
                                    paginateAdminReplies(commentId, holder, replyAdapter, Math.max(finalTotalReplies, serverReplies.size()));
                                }
                            }
                            @Override public void onFailure(Call<List<Comment>> call, Throwable t) {}
                        });
            } else {
                paginateAdminReplies(commentId, holder, replyAdapter, Math.max(finalTotalReplies, currentList.size()));
            }
        });
    }

    private void updateAdminLikeDislikeUI(CommentViewHolder holder, Map<String, Object> comment) {
        boolean isLiked = getSafeBoolean(comment.get("isLiked")) || getSafeBoolean(comment.get("liked"));
        boolean isDisliked = getSafeBoolean(comment.get("isDisliked")) || getSafeBoolean(comment.get("disliked"));

        if (isLiked) {
            holder.imgLikeIcon.setImageResource(R.drawable.ic_thumb_up_filled);
            holder.imgLikeIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#FFB77D")));
            holder.tvLikes.setTextColor(Color.parseColor("#FFB77D"));
        } else {
            holder.imgLikeIcon.setImageResource(R.drawable.ic_thumb_up_outline);
            holder.imgLikeIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#DBC2B0")));
            holder.tvLikes.setTextColor(Color.parseColor("#DBC2B0"));
        }

        if (isDisliked) {
            holder.imgDislikeIcon.setImageResource(R.drawable.ic_thumb_down_filled);
            holder.imgDislikeIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#E91E63")));
            holder.tvDislikes.setTextColor(Color.parseColor("#E91E63"));
        } else {
            holder.imgDislikeIcon.setImageResource(R.drawable.ic_thumb_down_outline);
            holder.imgDislikeIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#DBC2B0")));
            holder.tvDislikes.setTextColor(Color.parseColor("#DBC2B0"));
        }
    }

    private void paginateAdminReplies(int commentId, CommentViewHolder holder, AdminReplyAdapter replyAdapter, int totalReplies) {
        List<Comment> fullList = repliesCache.get(commentId);
        int currentCount = displayedCountCache.get(commentId);

        if (fullList == null || fullList.isEmpty()) {
            holder.tvLoadMoreReplies.setVisibility(View.GONE);
            return;
        }

        if (currentCount >= fullList.size()) {
            displayedCountCache.put(commentId, 0);
            holder.rvReplies.setVisibility(View.GONE);
            replyAdapter.setReplies(new ArrayList<>());
            if (totalReplies > 0) {
                holder.tvLoadMoreReplies.setVisibility(View.VISIBLE);
                holder.tvLoadMoreReplies.setText("—— Xem phản hồi (" + totalReplies + ") ——");
            } else {
                holder.tvLoadMoreReplies.setVisibility(View.GONE);
            }
            return;
        }

        currentCount += 10;
        if (currentCount > fullList.size()) currentCount = fullList.size();
        displayedCountCache.put(commentId, currentCount);

        holder.rvReplies.setVisibility(View.VISIBLE);
        replyAdapter.setReplies(new ArrayList<>(fullList.subList(0, currentCount)));

        holder.tvLoadMoreReplies.setVisibility(View.VISIBLE);
        if (currentCount < fullList.size()) {
            holder.tvLoadMoreReplies.setText("—— Xem thêm phản hồi quản trị ——");
        } else {
            holder.tvLoadMoreReplies.setText("—— Thu gọn phản hồi ——");
        }
    }

    // ĐÃ THÊM: Hỗ trợ xóa bộ nhớ đệm cache câu trả lời con để làm mới thời gian thực khi đăng bình luận phản hồi thành công
    public void resetRepliesCache(Integer parentCommentId) {
        if (parentCommentId == null) return;
        repliesCache.remove(parentCommentId);
        displayedCountCache.put(parentCommentId, 0);
        for (int i = 0; i < commentList.size(); i++) {
            if (getSafeInt(commentList.get(i).get("commentId")) == parentCommentId) {
                notifyItemChanged(i);
                break;
            }
        }
    }

    /**
     * Cập nhật số like/dislike tại chỗ không cần reload toàn bộ list.
     * Dùng sau khi tương tác like/dislike để tránh mất state reply đang mở.
     */
    public void updateLikeDislikeInPlace(int commentId, Comment updatedComment) {
        for (int i = 0; i < commentList.size(); i++) {
            if (getSafeInt(commentList.get(i).get("commentId")) == commentId) {
                Map<String, Object> item = commentList.get(i);
                item.put("likes", (double) updatedComment.getLikeCount());
                item.put("dislikes", (double) updatedComment.getDislikeCount());
                item.put("liked", updatedComment.isLiked());
                item.put("disliked", updatedComment.isDisliked());
                item.put("isLiked", updatedComment.isLiked());
                item.put("isDisliked", updatedComment.isDisliked());
                notifyItemChanged(i);
                break;
            }
        }
    }

    static void navigateToUserDetail(Context context, int userId) {
        if (userId <= 0) return;
        Intent intent = new Intent(context, AdminUserDetailActivity.class);
        intent.putExtra("USER_ID", userId);
        context.startActivity(intent);
    }

    private long getSafeLong(Object obj) {
        if (obj instanceof Number) return ((Number) obj).longValue();
        return 0L;
    }

    private int getSafeInt(Object obj) {
        if (obj instanceof Number) return ((Number) obj).intValue();
        return 0;
    }

    private boolean getSafeBoolean(Object obj) {
        if (obj instanceof Boolean) return (Boolean) obj;
        if (obj instanceof String) return Boolean.parseBoolean((String) obj);
        return false;
    }

    @Override
    public int getItemCount() { return commentList.size(); }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar, imgLikeIcon, imgDislikeIcon;
        TextView tvUser, tvContent, tvChapterTag, tvLikes, tvDislikes, tvReportsCount, btnDelete, tvLoadMoreReplies;
        View layoutLike, layoutDislike, layoutReply, layoutReport, layoutDelete;
        RecyclerView rvReplies;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgAdminCommentAvatar);
            tvUser = itemView.findViewById(R.id.tvAdminCommentUser);
            tvContent = itemView.findViewById(R.id.tvAdminCommentContent);
            tvChapterTag = itemView.findViewById(R.id.tvAdminCommentChapterTag);
            rvReplies = itemView.findViewById(R.id.recyclerViewAdminReplies);
            tvLoadMoreReplies = itemView.findViewById(R.id.tvAdminLoadMoreReplies);

            layoutLike = itemView.findViewById(R.id.layoutAdminLikeComment);
            layoutDislike = itemView.findViewById(R.id.layoutAdminDislikeComment);
            layoutReply = itemView.findViewById(R.id.layoutAdminReplyComment);
            layoutReport = itemView.findViewById(R.id.layoutAdminReportComment);
            layoutDelete = itemView.findViewById(R.id.layoutAdminDeleteComment);

            tvLikes = itemView.findViewById(R.id.tvAdminCommentLikes);
            tvDislikes = itemView.findViewById(R.id.tvAdminCommentDislikes);
            btnDelete = itemView.findViewById(R.id.btnAdminCommentDelete);

            tvReportsCount = itemView.findViewById(R.id.tvAdminCommentReportsCount);

            imgLikeIcon = itemView.findViewById(R.id.imgAdminLikeIcon);
            imgDislikeIcon = itemView.findViewById(R.id.imgAdminDislikeIcon);
        }
    }
}