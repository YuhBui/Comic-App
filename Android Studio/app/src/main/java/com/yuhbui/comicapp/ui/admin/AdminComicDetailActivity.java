package com.yuhbui.comicapp.ui.admin;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html; // THÊM
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.yuhbui.comicapp.R;
import com.yuhbui.comicapp.data.api.ApiClient;
import com.yuhbui.comicapp.data.model.Comic;
import com.yuhbui.comicapp.data.model.ComicDetailResponse;
import com.yuhbui.comicapp.data.model.Comment;
import com.yuhbui.comicapp.ui.adapters.AdminChapterAdapter;
import com.yuhbui.comicapp.ui.adapters.AdminCommentAdapter;
import com.yuhbui.comicapp.ui.admin.AdminAddChapterActivity;
import com.yuhbui.comicapp.utils.HeaderUtils;
import com.yuhbui.comicapp.utils.MenuUtils;
import com.yuhbui.comicapp.utils.SharedPrefsManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminComicDetailActivity extends AppCompatActivity implements AdminCommentAdapter.OnCommentAdminActionListener {

    private DrawerLayout drawerLayout;

    private int comicId;
    private Integer targetParentCommentId = null;
    private ImageView imgCover;
    private TextView tvTitle, tvAuthor, tvStatus, tvDesc, tvGenre, tvRelease, tvViews, tvFavorites, tvRating;
    private Button btnEdit, btnDelete, btnAdminAddChapter;
    private ImageButton btnAdminSendComment;
    private EditText edtAdminCommentInput;
    private TextView tvAdminReplyingTo;
    private RecyclerView rvComments, rvChapters;
    private AdminCommentAdapter commentAdapter;
    private AdminChapterAdapter chapterAdapter;
    private Comic currentComic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_comic_detail);

        comicId = getIntent().getIntExtra("COMIC_ID", -1);

        drawerLayout = findViewById(R.id.drawerLayout);

        // 2. Khởi tạo và cấu hình thanh Header Admin chuyên biệt
        View layoutHeader = findViewById(R.id.layoutHeaderAdminDetail);
        TextView headerLogo = layoutHeader.findViewById(R.id.headerLogo);
        ImageView headerMenu = layoutHeader.findViewById(R.id.headerMenu);

        HeaderUtils.initHeader(this, layoutHeader, drawerLayout);
        MenuUtils.setupAdminSideMenu(this, drawerLayout, headerMenu);

        if (layoutHeader.findViewById(R.id.headerSearch) != null) {
            layoutHeader.findViewById(R.id.headerSearch).setVisibility(View.GONE);
        }
        if (layoutHeader.findViewById(R.id.headerNotification) != null) {
            layoutHeader.findViewById(R.id.headerNotification).setVisibility(View.GONE);
        }

        // ĐÃ SỬA: Thay đổi sang định dạng chữ HTML phân tách màu chữ giống AdminDashboardActivity
        if (headerLogo != null) {
            headerLogo.setText(Html.fromHtml("<font color='#D97707'>h</font><font color='#FFFFFF'>ay</font><font color='#D97707'>c</font><font color='#FFFFFF'>omic</font>", Html.FROM_HTML_MODE_COMPACT));
            headerLogo.setOnClickListener(v -> {
                Intent intent = new Intent(this, AdminDashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            });
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                    setEnabled(true);
                }
            }
        });

        imgCover = findViewById(R.id.imgAdminDetailCover);
        tvTitle = findViewById(R.id.tvAdminDetailTitle);
        tvAuthor = findViewById(R.id.tvAdminDetailAuthor);
        tvStatus = findViewById(R.id.tvAdminDetailStatus);
        tvGenre = findViewById(R.id.tvAdminDetailGenre);
        tvRelease = findViewById(R.id.tvAdminDetailRelease);
        tvViews = findViewById(R.id.tvAdminDetailViews);
        tvFavorites = findViewById(R.id.tvAdminDetailFavorites);
        tvRating = findViewById(R.id.tvAdminDetailRating);
        tvDesc = findViewById(R.id.tvAdminDetailDesc);

        btnEdit = findViewById(R.id.btnAdminEditComicDetail);
        btnDelete = findViewById(R.id.btnAdminDeleteComicDetail);
        rvComments = findViewById(R.id.rvAdminDetailComments);
        edtAdminCommentInput = findViewById(R.id.edtAdminCommentInput);
        tvAdminReplyingTo = findViewById(R.id.tvAdminReplyingTo);
        btnAdminSendComment = findViewById(R.id.btnAdminSendComment);
        rvChapters = findViewById(R.id.rvAdminDetailChapters);
        btnAdminAddChapter = findViewById(R.id.btnAdminAddChapter);

        rvComments.setLayoutManager(new LinearLayoutManager(this));
        commentAdapter = new AdminCommentAdapter(this);
        rvComments.setAdapter(commentAdapter);

        rvChapters.setLayoutManager(new LinearLayoutManager(this));

        chapterAdapter = new AdminChapterAdapter(new AdminChapterAdapter.OnChapterAdminActionListener() {
            @Override
            public void onClick(Map<String, Object> chapter) {
                Number idNum = (Number) chapter.get("chapterId");
                int id = idNum != null ? idNum.intValue() : -1;

                Intent intent = new Intent(AdminComicDetailActivity.this, AdminChapterDetailActivity.class);
                intent.putExtra("CHAPTER_ID", id);
                intent.putExtra("COMIC_ID", comicId);
                intent.putExtra("CHAPTER_TITLE", "Chương " + chapter.get("chapterNumber"));
                startActivity(intent);
            }

            @Override
            public void onEdit(Map<String, Object> chapter) {
                showChapterFormDialog(chapter);
            }

            @Override
            public void onDelete(int chapterId, int position) {
                new AlertDialog.Builder(AdminComicDetailActivity.this)
                        .setTitle("Xác nhận xóa chương")
                        .setMessage("Bạn có chắc chắn muốn xóa vĩnh viễn chương truyện này cùng toàn bộ các trang ảnh đính kèm không?")
                        .setPositiveButton("Xóa vĩnh viễn", (dialog, which) -> {
                            ApiClient.getApiService().adminDeleteChapter(chapterId).enqueue(new Callback<Map<String, Object>>() {
                                @Override
                                public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                                    if (response.isSuccessful()) {
                                        Toast.makeText(AdminComicDetailActivity.this, "Đã xóa chương truyện thành công!", Toast.LENGTH_SHORT).show();
                                        loadChaptersData();
                                    } else {
                                        Toast.makeText(AdminComicDetailActivity.this, "Server từ chối yêu cầu xóa chương!", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                                    Toast.makeText(AdminComicDetailActivity.this, "Lỗi kết nối máy chủ khi xóa!", Toast.LENGTH_SHORT).show();
                                }
                            });
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            }
        });
        rvChapters.setAdapter(chapterAdapter);

        btnAdminAddChapter.setOnClickListener(v -> {
            Intent intent = new Intent(AdminComicDetailActivity.this, AdminAddChapterActivity.class);
            intent.putExtra("COMIC_ID", comicId);
            startActivity(intent);
        });

        btnAdminSendComment.setOnClickListener(v -> {
            String content = edtAdminCommentInput.getText().toString().trim();
            if (content.isEmpty()) {
                Toast.makeText(this, "Nội dung bình luận không được rỗng!", Toast.LENGTH_SHORT).show();
                return;
            }

            int currentUserId = SharedPrefsManager.getUserId(this);
            if (currentUserId == -1) {
                Toast.makeText(this, "Không thể xác định tài khoản Admin đăng nhập!", Toast.LENGTH_SHORT).show();
                return;
            }

            Comment adminComment = new Comment();
            adminComment.setComicId(comicId);
            adminComment.setUserId(currentUserId);
            adminComment.setContent(content);
            adminComment.setParentCommentId(targetParentCommentId);

            ApiClient.getApiService().postComment(adminComment).enqueue(new Callback<Comment>() {
                @Override
                public void onResponse(Call<Comment> call, Response<Comment> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(AdminComicDetailActivity.this, "Đăng bình luận thành công!", Toast.LENGTH_SHORT).show();
                        edtAdminCommentInput.setText("");
                        edtAdminCommentInput.setHint("Viết bình luận Admin...");
                        tvAdminReplyingTo.setVisibility(android.view.View.GONE);

                        android.view.inputmethod.InputMethodManager imm =
                                (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                        if (imm != null && getCurrentFocus() != null) {
                            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                        }

                        Integer savedParentId = targetParentCommentId;
                        targetParentCommentId = null;

                        if (savedParentId != null) {
                            commentAdapter.prepareReloadAfterReply(savedParentId);
                        }
                        loadCommentsData();
                    } else if (response.code() == 403) {
                        Toast.makeText(AdminComicDetailActivity.this, "Tài khoản đang bị khóa chức năng bình luận!", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(AdminComicDetailActivity.this, "Lỗi đăng bình luận từ server!", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override public void onFailure(Call<Comment> call, Throwable t) {}
            });
        });

        btnEdit.setOnClickListener(v -> {
            if (currentComic == null) return;
            Intent intent = new Intent(this, AdminEditComicActivity.class);
            intent.putExtra("EDIT_COMIC_ID", currentComic.getComicId());
            intent.putExtra("TITLE", currentComic.getTitle());
            intent.putExtra("AUTHOR", currentComic.getAuthor());
            intent.putExtra("COVER_URL", currentComic.getCoverImageUrl());
            intent.putExtra("STATUS", currentComic.getStatus());
            intent.putExtra("DESC", currentComic.getDescription());
            startActivity(intent);
        });

        btnDelete.setOnClickListener(v -> {
            if (currentComic == null) return;
            new AlertDialog.Builder(this)
                    .setTitle("Xác nhận xóa truyện")
                    .setMessage("Bạn có chắc chắn muốn xóa vĩnh viễn bộ truyện '" + currentComic.getTitle() + "' không?")
                    .setPositiveButton("Xóa vĩnh viễn", (dialog, which) -> {
                        ApiClient.getApiService().adminDeleteComic(comicId).enqueue(new Callback<ResponseBody>() {
                            @Override
                            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                if (response.isSuccessful()) {
                                    Toast.makeText(AdminComicDetailActivity.this, "Đã xóa truyện khỏi hệ thống thành công!", Toast.LENGTH_SHORT).show();
                                    finish();
                                }
                            }
                            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
                        });
                    })
                    .setNegativeButton("Hủy", null).show();
        });

        ImageView btnBackComicDetail = findViewById(R.id.btnBackComicDetail);
        if (btnBackComicDetail != null) {
            btnBackComicDetail.setOnClickListener(v -> finish());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadComicDetailData();
        loadChaptersData();
        loadCommentsData();

        View layoutHeader = findViewById(R.id.layoutHeaderAdminDetail);
        if (layoutHeader != null && layoutHeader.findViewById(R.id.headerAvatar) != null) {
            HeaderUtils.loadHeaderAvatar(this, layoutHeader.findViewById(R.id.headerAvatar));
        }
    }

    private void showChapterFormDialog(@Nullable Map<String, Object> chapterData) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 30);

        final EditText edtNum = new EditText(this);
        edtNum.setHint("Nhập số thứ tự chương (Ví dụ: 1 hoặc 2.5)");
        edtNum.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(edtNum);

        final EditText edtTitle = new EditText(this);
        edtTitle.setHint("Nhập tên tiêu đề chương (Không bắt buộc)");
        layout.addView(edtTitle);

        boolean isEdit = chapterData != null;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(isEdit ? "Cập nhật chương truyện" : "Thêm chương truyện mới").setView(layout);

        if (isEdit) {
            edtNum.setText(String.valueOf(chapterData.get("chapterNumber")));
            edtTitle.setText((String) chapterData.get("title"));

            builder.setNeutralButton("XÓA CHƯƠNG", (dialog, which) -> {
                Number idNum = (Number) chapterData.get("chapterId");
                int chId = idNum != null ? idNum.intValue() : -1;

                ApiClient.getApiService().adminDeleteChapter(chId).enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(AdminComicDetailActivity.this, "Đã xóa chương truyện thành công!", Toast.LENGTH_SHORT).show();
                            loadChaptersData();
                        }
                    }
                    @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
                });
            });
        }

        builder.setPositiveButton("LƯU THÔNG TIN", (dialog, which) -> {
            String numStr = edtNum.getText().toString().trim();
            String title = edtTitle.getText().toString().trim();
            if (numStr.isEmpty()) return;

            double num = Double.parseDouble(numStr);
            if (!isEdit) {
                ApiClient.getApiService().adminCreateChapter(comicId, num, title).enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                        if (response.isSuccessful()) {
                            loadChaptersData();
                        }
                    }
                    @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
                });
            } else {
                Number idNum = (Number) chapterData.get("chapterId");
                int chId = idNum != null ? idNum.intValue() : -1;

                ApiClient.getApiService().adminUpdateChapter(chId, num, title).enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                        if (response.isSuccessful()) {
                            loadChaptersData();
                        }
                    }
                    @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
                });
            }
        });
        builder.setNegativeButton("HỦY", null);
        builder.show();
    }

    private void loadComicDetailData() {
        ApiClient.getApiService().getComicDetail(comicId, null).enqueue(new Callback<ComicDetailResponse>() {
            @Override
            public void onResponse(Call<ComicDetailResponse> call, Response<ComicDetailResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ComicDetailResponse detailResponse = response.body();
                    currentComic = detailResponse.getComic();

                    tvTitle.setText(currentComic.getTitle());
                    tvAuthor.setText("Tác giả: " + currentComic.getAuthor());
                    tvStatus.setText("Tình trạng: " + currentComic.getStatus());
                    tvDesc.setText(currentComic.getDescription());

                    tvViews.setText("Lượt xem: " + currentComic.getViewCount());
                    tvRating.setText("Đánh giá: " + currentComic.getRating() + "/5");
                    tvFavorites.setText("Yêu thích: " + detailResponse.getFavoriteCount());

                    tvRelease.setText("Phát hành: " + (currentComic.getCreatedAt() != null ? formatToDateOnly(currentComic.getCreatedAt()) : "Đang cập nhật"));

                    if (detailResponse.getGenres() != null && !detailResponse.getGenres().isEmpty()) {
                        tvGenre.setText("Thể loại: " + detailResponse.getGenres());
                    } else {
                        tvGenre.setText("Thể loại: Đang cập nhật");
                    }

                    if (!AdminComicDetailActivity.this.isDestroyed()) {
                        Glide.with(AdminComicDetailActivity.this)
                                .load(currentComic.getCoverImageUrl())
                                .placeholder(R.drawable.ic_launcher_background)
                                .into(imgCover);
                    }
                }
            }
            @Override public void onFailure(Call<ComicDetailResponse> call, Throwable t) {}
        });
    }

    private String formatToDateOnly(String rawDateTime) {
        if (rawDateTime == null || rawDateTime.trim().isEmpty()) {
            return "Đang cập nhật";
        }
        try {
            String datePart = rawDateTime.contains("T") ? rawDateTime.split("T")[0] : rawDateTime.split(" ")[0];
            String[] parts = datePart.split("-");
            if (parts.length == 3) {
                return parts[2] + "/" + parts[1] + "/" + parts[0];
            }
            return datePart;
        } catch (Exception e) {
            return rawDateTime;
        }
    }

    private void loadChaptersData() {
        ApiClient.getApiService().adminGetChapters(comicId).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    chapterAdapter.setData(response.body());
                }
            }
            @Override public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {}
        });
    }

    private void loadCommentsData() {
        int currentUserId = SharedPrefsManager.getUserId(this);
        Integer apiUserId = (currentUserId != -1) ? currentUserId : null;

        ApiClient.getApiService().getCommentsByComic(comicId, apiUserId).enqueue(new Callback<List<Comment>>() {
            @Override
            public void onResponse(Call<List<Comment>> call, Response<List<Comment>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Map<String, Object>> result = new ArrayList<>();
                    for (Comment c : response.body()) {
                        result.add(commentToAdminMap(c));
                    }
                    commentAdapter.setData(result);
                }
            }
            @Override public void onFailure(Call<List<Comment>> call, Throwable t) {}
        });
    }

    private Map<String, Object> commentToAdminMap(Comment c) {
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("commentId", c.getCommentId());
        map.put("username", c.getUserDisplayName() != null ? c.getUserDisplayName() : "User #" + c.getUserId());
        map.put("content", c.getContent());
        map.put("likes", (double) c.getLikeCount());
        map.put("dislikes", (double) c.getDislikeCount());
        map.put("liked", c.isLiked());
        map.put("disliked", c.isDisliked());
        map.put("isLiked", c.isLiked());
        map.put("isDisliked", c.isDisliked());
        map.put("userId", c.getUserId());
        map.put("avatarUrl", c.getUserAvatarUrl());
        map.put("replyCount", c.getReplyCount());
        map.put("parentCommentId", c.getParentCommentId());
        map.put("chapterName", c.getChapterName());
        map.put("reports", 0.0);
        return map;
    }

    @Override
    public void onReply(Map<String, Object> comment) {
        Number idNum = (Number) comment.get("commentId");
        targetParentCommentId = idNum != null ? idNum.intValue() : null;

        String username = (String) comment.get("username");
        if (username != null && !username.isEmpty()) {
            tvAdminReplyingTo.setVisibility(android.view.View.VISIBLE);
            tvAdminReplyingTo.setText("↳ Đang trả lời @" + username);
            String tagText = "@" + username + " ";
            edtAdminCommentInput.setText(tagText);
            edtAdminCommentInput.setSelection(tagText.length());
            edtAdminCommentInput.setHint("");
        } else {
            tvAdminReplyingTo.setVisibility(android.view.View.GONE);
            edtAdminCommentInput.setText("");
            edtAdminCommentInput.setHint("Viết phản hồi...");
        }

        edtAdminCommentInput.requestFocus();
        android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(edtAdminCommentInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
        }
    }

    @Override
    public void onShowReports(int commentId) {
        ApiClient.getApiService().adminGetCommentReports(commentId).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Map<String, Object>> reports = response.body();
                    if (reports.isEmpty()) {
                        Toast.makeText(AdminComicDetailActivity.this, "Bình luận này chưa có lượt báo cáo nào!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    showReportsDialog(reports);
                }
            }
            @Override public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {}
        });
    }

    private void showReportsDialog(List<Map<String, Object>> reports) {
        android.widget.LinearLayout container = new android.widget.LinearLayout(this);
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        container.setBackgroundColor(android.graphics.Color.parseColor("#1E1E1E"));

        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        scrollView.addView(container);

        for (Map<String, Object> report : reports) {
            android.view.View row = android.view.LayoutInflater.from(this)
                    .inflate(R.layout.item_report_entry, container, false);

            android.widget.ImageView imgAvatar = row.findViewById(R.id.imgReporterAvatar);
            android.widget.TextView tvName = row.findViewById(R.id.tvReporterName);
            android.widget.TextView tvReason = row.findViewById(R.id.tvReportReason);
            android.widget.TextView tvTime = row.findViewById(R.id.tvReportTime);

            String avatarUrl = report.get("avatarUrl") != null ? (String) report.get("avatarUrl") : null;
            String name = report.get("displayName") != null ? (String) report.get("displayName") : "Ẩn danh";
            String reason = report.get("reason") != null ? (String) report.get("reason") : "";
            String createdAt = report.get("createdAt") != null ? (String) report.get("createdAt") : "";

            com.bumptech.glide.Glide.with(this)
                    .load(avatarUrl)
                    .placeholder(R.drawable.ic_launcher_background)
                    .circleCrop()
                    .into(imgAvatar);

            tvName.setText(name);
            tvReason.setText(reason);
            if (!createdAt.isEmpty()) {
                tvTime.setText(createdAt.length() > 16 ? createdAt.substring(0, 16) : createdAt);
                tvTime.setVisibility(android.view.View.VISIBLE);
            } else {
                tvTime.setVisibility(android.view.View.GONE);
            }

            android.view.View divider = new android.view.View(this);
            android.widget.LinearLayout.LayoutParams dp = new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 1);
            dp.setMargins(12, 0, 12, 0);
            divider.setLayoutParams(dp);
            divider.setBackgroundColor(android.graphics.Color.parseColor("#33FFFFFF"));

            container.addView(row);
            container.addView(divider);
        }

        new AlertDialog.Builder(this)
                .setTitle("Báo cáo bình luận (" + reports.size() + ")")
                .setView(scrollView)
                .setPositiveButton("Đóng", null)
                .show();
    }

    @Override
    public void onInteract(int commentId, int type, int position) {
        int currentUserId = SharedPrefsManager.getUserId(this);
        if (currentUserId == -1) return;

        ApiClient.getApiService().interactWithComment(commentId, currentUserId, type).enqueue(new Callback<Comment>() {
            @Override
            public void onResponse(Call<Comment> call, Response<Comment> response) {
                if (response.isSuccessful() && response.body() != null) {
                    commentAdapter.updateLikeDislikeInPlace(commentId, response.body());
                }
            }
            @Override public void onFailure(Call<Comment> call, Throwable t) {}
        });
    }

    @Override
    public void onDelete(int commentId, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa bình luận")
                .setMessage("Bạn có chắc muốn xóa vĩnh viễn bình luận này của người dùng không?")
                .setPositiveButton("Xóa vĩnh viễn", (dialog, which) -> {
                    ApiClient.getApiService().adminDeleteComment(commentId).enqueue(new Callback<Map<String, Object>>() {
                        @Override
                        public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(AdminComicDetailActivity.this, "Đã xóa bình luận khỏi hệ thống!", Toast.LENGTH_SHORT).show();
                                loadCommentsData();
                            }
                        }
                        @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
                    });
                })
                .setNegativeButton("Hủy", null).show();
    }
}