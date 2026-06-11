package com.yuhbui.comicapp.ui.adapters;

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
import java.util.ArrayList;
import java.util.Collections; // Duy trì sắp xếp cũ đến mới
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminCommentAdapter extends RecyclerView.Adapter<AdminCommentAdapter.CommentViewHolder> {

    private List<Map<String, Object>> commentList = new ArrayList<>();
    private final OnCommentAdminActionListener listener;

    // Cache duy trì dữ liệu phản hồi con lồng thụt lề cho giao diện quản trị Admin
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
        int commentId = getSafeInt(comment.get("commentId"));

        holder.tvUser.setText((String) comment.get("username"));
        holder.tvContent.setText((String) comment.get("content"));

        holder.tvLikes.setText("👍 " + getSafeLong(comment.get("likes")));
        holder.tvDislikes.setText("👎 " + getSafeLong(comment.get("dislikes")));

        long reportCount = getSafeLong(comment.get("reports"));
        holder.tvReports.setText("⚠️ Báo cáo (" + reportCount + ")");

        Glide.with(holder.itemView.getContext())
                .load((String) comment.get("avatarUrl"))
                .placeholder(android.R.drawable.sym_def_app_icon)
                .circleCrop()
                .into(holder.imgAvatar);

        // ========== ĐÃ SỬA: CẤU HÌNH CHO RECYCLERVIEW BÌNH LUẬN CON PHÍA ADMIN ==========
        holder.rvReplies.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext()));
        holder.rvReplies.setNestedScrollingEnabled(false); // CHỐT CHẶN: Ngăn lỗi crash layout, ép hiển thị lồng ngay bên dưới cha

        ReplyAdapter replyAdapter = new ReplyAdapter();
        holder.rvReplies.setAdapter(replyAdapter);

        // Lấy số lượng phản hồi từ dữ liệu Map (hỗ trợ cả 2 khóa replyCount / repliesCount)
        int totalReplies = 0;
        if (comment.containsKey("replyCount") && comment.get("replyCount") != null) {
            totalReplies = getSafeInt(comment.get("replyCount"));
        } else if (comment.containsKey("repliesCount") && comment.get("repliesCount") != null) {
            totalReplies = getSafeInt(comment.get("repliesCount"));
        }

        if (!repliesCache.containsKey(commentId)) {
            repliesCache.put(commentId, new ArrayList<>());
            displayedCountCache.put(commentId, 0);
        }

        List<Comment> cachedReplies = repliesCache.get(commentId);
        int currentDisplayedCount = displayedCountCache.get(commentId);

        // Logic ẩn hiện dòng chữ "Xem phản hồi" dựa theo trạng thái đóng mở và số lượng reply thực tế
        if (currentDisplayedCount > 0 && !cachedReplies.isEmpty()) {
            holder.rvReplies.setVisibility(View.VISIBLE);
            int endBound = Math.min(currentDisplayedCount, cachedReplies.size());
            replyAdapter.setReplies(new ArrayList<>(cachedReplies.subList(0, endBound)));

            holder.tvLoadMoreReplies.setVisibility(View.VISIBLE);
            if (cachedReplies.size() > currentDisplayedCount) {
                holder.tvLoadMoreReplies.setText("—— Xem thêm phản hồi ——");
            } else {
                holder.tvLoadMoreReplies.setText("—— Thu gọn phản hồi ——");
            }
        } else {
            holder.rvReplies.setVisibility(View.GONE);
            replyAdapter.setReplies(new ArrayList<>());

            // Chỉ những bình luận có câu trả lời thực tế mới hiển thị dòng chữ
            if (totalReplies > 0) {
                holder.tvLoadMoreReplies.setVisibility(View.VISIBLE);
                holder.tvLoadMoreReplies.setText("—— Xem phản hồi (" + totalReplies + ") ——");
            } else {
                holder.tvLoadMoreReplies.setVisibility(View.GONE); // Không có phản hồi con -> Ẩn hoàn toàn nút
            }
        }

        holder.btnReply.setOnClickListener(v -> listener.onReply(comment));
        holder.tvLikes.setOnClickListener(v -> listener.onInteract(commentId, 1, position));
        holder.tvDislikes.setOnClickListener(v -> listener.onInteract(commentId, -1, position));
        holder.tvReports.setOnClickListener(v -> listener.onShowReports(commentId));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(commentId, position));

        // Click tải dữ liệu phân trang phản hồi thụt đầu dòng cho màn hình Admin
        int finalTotalReplies = totalReplies;
        holder.tvLoadMoreReplies.setOnClickListener(v -> {
            List<Comment> currentList = repliesCache.get(commentId);
            if (currentList == null || currentList.isEmpty()) {
                ApiClient.getApiService().getRepliesByParentId(commentId)
                        .enqueue(new Callback<List<Comment>>() {
                            @Override
                            public void onResponse(Call<List<Comment>> call, Response<List<Comment>> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    List<Comment> serverReplies = response.body();

                                    // Sắp xếp theo ID tăng dần để các phản hồi mới hơn nối tiếp từ trên xuống dưới (Cũ ở trên, mới ở dưới)
                                    Collections.sort(serverReplies, (c1, c2) -> Integer.compare(c1.getCommentId(), c2.getCommentId()));

                                    repliesCache.put(commentId, serverReplies);
                                    paginateAdminReplies(commentId, holder, replyAdapter, finalTotalReplies);
                                }
                            }
                            @Override public void onFailure(Call<List<Comment>> call, Throwable t) {}
                        });
            } else {
                paginateAdminReplies(commentId, holder, replyAdapter, finalTotalReplies);
            }
        });
    }

    private void paginateAdminReplies(int commentId, CommentViewHolder holder, ReplyAdapter replyAdapter, int totalReplies) {
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
        if (currentCount > fullList.size()) {
            currentCount = fullList.size();
        }
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
        TextView tvUser, tvContent, tvLikes, tvDislikes, tvReports, btnReply, btnDelete, tvLoadMoreReplies;
        RecyclerView rvReplies;

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
            rvReplies = itemView.findViewById(R.id.recyclerViewAdminReplies);
            tvLoadMoreReplies = itemView.findViewById(R.id.tvAdminLoadMoreReplies);
        }
    }
}