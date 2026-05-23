package com.yuhbui.comicapp.ui.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
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
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private List<Comment> commentList = new ArrayList<>();
    private OnCommentClickListener replyListener;

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

        // --- 1. NÂNG CẤP: ĐỔ TÊN THẬT, TAG CHAPTER VÀ LOAD AVATAR BẰNG GLIDE ---
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
                .placeholder(R.drawable.ic_launcher_background) // Ảnh mặc định khi chờ load
                .circleCrop() // Bo tròn avatar chuẩn UX chuyên nghiệp
                .into(holder.imgUserAvatar);

        holder.tvCommentContent.setText(comment.getContent());

        // Hiển thị số lượng đếm Like, Dislike, và Phản hồi từ database
        holder.btnLike.setText("👍 Thích (" + comment.getLikeCount() + ")");
        holder.btnDislike.setText("👎 Ghét (" + comment.getDislikeCount() + ")");
        holder.btnReply.setText("💬 Phản hồi (" + comment.getReplyCount() + ")");

        // Khởi tạo RecyclerView con cho dòng này
        holder.rvReplies.setLayoutManager(new LinearLayoutManager(context));
        ReplyAdapter replyAdapter = new ReplyAdapter();
        holder.rvReplies.setAdapter(replyAdapter);

        // Reset lại bộ nhớ đệm cục bộ khi ViewHolder này được tái sử dụng để tránh lỗi cuộn lặp data
        holder.fullRepliesList.clear();
        holder.currentDisplayedCount = 0;

        replyAdapter.setOnReplyToReplyClickListener(new ReplyAdapter.OnReplyToReplyClickListener() {
            @Override
            public void onReplyToReplyClick(Comment childComment) {
                if (replyListener != null) {
                    Comment ghostComment = new Comment();
                    ghostComment.setCommentId(comment.getCommentId()); // Gắn ID của cha lớn
                    ghostComment.setUserId(childComment.getUserId());  // Gắn ID của đứa con vừa được chọn để tag
                    replyListener.onReplyClick(ghostComment);
                }
            }
        });

        // Định cấu hình chữ cho nút Xem thêm ban đầu
        int totalRepliesCount = comment.getReplyCount();
        if (totalRepliesCount > 0) {
            holder.tvLoadMoreReplies.setVisibility(View.VISIBLE);
            int showAmount = Math.min(10, totalRepliesCount);
            holder.tvLoadMoreReplies.setText("—— Xem thêm " + showAmount + " phản hồi ——");
        } else {
            holder.tvLoadMoreReplies.setVisibility(View.GONE);
            holder.rvReplies.setVisibility(View.GONE);
        }

        // Lấy ID của người dùng hiện tại đang đăng nhập máy từ SharedPreferences
        int currentUserId = SharedPrefsManager.getUserId(context);

        // Sự kiện khi bấm nút LIKE (gửi type = 1 lên server)
        holder.btnLike.setOnClickListener(v -> {
            if (currentUserId == -1) {
                Toast.makeText(context, "Vui lòng đăng nhập để tương tác!", Toast.LENGTH_SHORT).show();
                return;
            }
            executeInteraction(holder, comment.getCommentId(), currentUserId, 1);
        });

        // Sự kiện khi bấm nút DISLIKE (gửi type = -1 lên server)
        holder.btnDislike.setOnClickListener(v -> {
            if (currentUserId == -1) {
                Toast.makeText(context, "Vui lòng đăng nhập để tương tác!", Toast.LENGTH_SHORT).show();
                return;
            }
            executeInteraction(holder, comment.getCommentId(), currentUserId, -1);
        });

        // XỬ LÝ SỰ KIỆN BẤM NÚT PHẢN HỒI (REPLY)
        holder.btnReply.setOnClickListener(v -> {
            if (replyListener != null) {
                replyListener.onReplyClick(comment);
            }
        });

        // ĐỔI NÚT BÁO CÁO THÀNH NÚT XÓA NẾU LÀ CHÍNH CHỦ
        if (comment.getUserId() == currentUserId && currentUserId != -1) {
            holder.btnReport.setText("🗑️ Xóa");
            holder.btnReport.setTextColor(Color.parseColor("#F44336")); // Màu đỏ trực quan

            holder.btnReport.setOnClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setTitle("Xóa bình luận")
                        .setMessage("Bạn có chắc chắn muốn xóa bình luận này không?")
                        .setPositiveButton("Xóa", (dialog, which) -> {
                            executeDeleteComment(comment.getCommentId(), currentUserId, context, holder.getAdapterPosition());
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            });
        } else {
            // Nếu không phải chính chủ -> Giữ nguyên giao diện và logic Báo cáo (Report) cũ
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

        // XỬ LÝ SỰ KIỆN CLICK XEM THÊM 10 PHẢN HỒI HOẶC THU GỌN
        holder.tvLoadMoreReplies.setOnClickListener(v -> {
            if (holder.fullRepliesList.isEmpty()) {
                ApiClient.getApiService().getRepliesByParentId(comment.getCommentId())
                        .enqueue(new Callback<List<Comment>>() {
                            @Override
                            public void onResponse(Call<List<Comment>> call, Response<List<Comment>> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    holder.fullRepliesList = response.body();
                                    holder.currentDisplayedCount = 0;
                                    paginateReplies(holder, replyAdapter, totalRepliesCount);
                                }
                            }

                            @Override
                            public void onFailure(Call<List<Comment>> call, Throwable t) {
                                Log.e("YUH_TEST", "Không thể lấy danh sách phản hồi con: " + t.getMessage());
                            }
                        });
            } else {
                paginateReplies(holder, replyAdapter, totalRepliesCount);
            }
        });
    }

    // HÀM LỌC PHÂN TRANG VÀ QUAY VÒNG LẶP BAN ĐẦU
    private void paginateReplies(CommentViewHolder holder, ReplyAdapter replyAdapter, int totalCount) {
        if (holder.currentDisplayedCount >= holder.fullRepliesList.size()) {
            holder.currentDisplayedCount = 0;
            holder.rvReplies.setVisibility(View.GONE);
            replyAdapter.setReplies(new ArrayList<>());

            int nextAmount = Math.min(10, totalCount);
            holder.tvLoadMoreReplies.setText("—— Xem thêm " + nextAmount + " phản hồi ——");
            return;
        }

        holder.currentDisplayedCount += 10;
        if (holder.currentDisplayedCount > holder.fullRepliesList.size()) {
            holder.currentDisplayedCount = holder.fullRepliesList.size();
        }

        List<Comment> subList = holder.fullRepliesList.subList(0, holder.currentDisplayedCount);
        holder.rvReplies.setVisibility(View.VISIBLE);
        replyAdapter.setReplies(subList);

        int remaining = totalCount - holder.currentDisplayedCount;
        if (remaining > 0) {
            int nextAmount = Math.min(10, remaining);
            holder.tvLoadMoreReplies.setText("—— Xem thêm " + nextAmount + " phản hồi ——");
        } else {
            holder.tvLoadMoreReplies.setText("—— Thu gọn phản hồi ——");
        }
    }

    // Hàm thực hiện gọi API Like/Dislike
    private void executeInteraction(CommentViewHolder holder, int commentId, int userId, int type) {
        ApiClient.getApiService().interactWithComment(commentId, userId, type)
                .enqueue(new Callback<Comment>() {
                    @Override
                    public void onResponse(Call<Comment> call, Response<Comment> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Comment updatedComment = response.body();
                            holder.btnLike.setText("👍 Thích (" + updatedComment.getLikeCount() + ")");
                            holder.btnDislike.setText("👎 Ghét (" + updatedComment.getDislikeCount() + ")");
                        } else {
                            Log.e("YUH_TEST", "Tương tác thất bại. Mã lỗi: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<Comment> call, Throwable t) {
                        Log.e("YUH_TEST", "Lỗi mạng khi tương tác: " + t.getMessage());
                    }
                });
    }

    // HÀM THỰC HIỆN GỌI API XÓA BÌNH LUẬN CHÍNH CHỦ
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
                        } else {
                            Toast.makeText(context, "Không thể xóa bình luận lúc này.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Comment> call, Throwable t) {
                        Log.e("YUH_TEST", "Lỗi kết nối mạng khi xóa bình luận: " + t.getMessage());
                    }
                });
    }

    // Hàm thực hiện gửi báo cáo lên server
    private void executeReport(int commentId, int userId, String reason, Context context) {
        ApiClient.getApiService().reportComment(commentId, userId, reason)
                .enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(context, "Cảm ơn bạn đã báo cáo!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, "Bạn đã báo cáo bình luận này rồi.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<String> call, Throwable t) {
                        Log.e("YUH_TEST", "Lỗi kết nối mạng khi báo cáo: " + t.getMessage());
                    }
                });
    }

    @Override
    public int getItemCount() {
        return commentList != null ? commentList.size() : 0;
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserComment, tvCommentContent;
        TextView btnLike, btnDislike;
        TextView btnReply, btnReport;

        // --- NÂNG CẤP: THÊM CÁC TRƯỜNG THÔNG TIN MỚI VÀO VIEW HOLDER ---
        ImageView imgUserAvatar;
        TextView tvCommentChapterTag;

        RecyclerView rvReplies;
        TextView tvLoadMoreReplies;
        List<Comment> fullRepliesList = new ArrayList<>();
        int currentDisplayedCount = 0;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserComment = itemView.findViewById(R.id.tvUserComment);
            tvCommentContent = itemView.findViewById(R.id.tvCommentContent);
            btnLike = itemView.findViewById(R.id.btnLikeComment);
            btnDislike = itemView.findViewById(R.id.btnDislikeComment);
            btnReply = itemView.findViewById(R.id.btnReplyComment);
            btnReport = itemView.findViewById(R.id.btnReportComment);

            // Ánh xạ thành phần giao diện mới nâng cấp
            imgUserAvatar = itemView.findViewById(R.id.imgUserAvatar);
            tvCommentChapterTag = itemView.findViewById(R.id.tvCommentChapterTag);

            rvReplies = itemView.findViewById(R.id.recyclerViewReplies);
            tvLoadMoreReplies = itemView.findViewById(R.id.tvLoadMoreReplies);
        }
    }

    public void resetRepliesCache(int parentCommentId) {
        for (int i = 0; i < commentList.size(); i++) {
            if (commentList.get(i).getCommentId() == parentCommentId) {
                notifyItemChanged(i);
                break;
            }
        }
    }
}