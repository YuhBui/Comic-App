package com.yuhbui.comicapp.ui.admin;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.activity.OnBackPressedCallback;         // THÊM: Quản lý phím Back cứng hệ thống
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;              // THÊM: Hỗ trợ điều hướng trượt trái
import androidx.drawerlayout.widget.DrawerLayout;    // THÊM: Khai báo thành phần DrawerLayout root
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
import com.yuhbui.comicapp.utils.HeaderUtils;          // THÊM: Khởi tạo Header tập trung
import com.yuhbui.comicapp.utils.MenuUtils;            // THÊM: Gọi Menu trượt Admin dùng chung
import com.yuhbui.comicapp.utils.SharedPrefsManager;

import java.util.List;
import java.util.Map;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminComicDetailActivity extends AppCompatActivity implements AdminCommentAdapter.OnCommentAdminActionListener {

    private DrawerLayout drawerLayout; // THÊM: Khai báo DrawerLayout toàn cục

    private int comicId;
    private Integer targetParentCommentId = null; // ĐÃ THÊM: Quản lý ID comment cha đang được phản hồi giống phía User
    private ImageView imgCover;
    private TextView tvTitle, tvAuthor, tvStatus, tvDesc, tvGenre, tvRelease, tvViews, tvFavorites, tvRating;
    private Button btnEdit, btnDelete, btnAdminAddChapter;
    private ImageButton btnAdminSendComment;
    private EditText edtAdminCommentInput;
    private RecyclerView rvComments, rvChapters;
    private AdminCommentAdapter commentAdapter;
    private AdminChapterAdapter chapterAdapter;
    private Comic currentComic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_comic_detail);

        comicId = getIntent().getIntExtra("COMIC_ID", -1);

        // 1. Ánh xạ thành phần DrawerLayout mới bao bọc ngoài cùng
        drawerLayout = findViewById(R.id.drawerLayout);

        // 2. Khởi tạo và cấu hình thanh Header Admin chuyên biệt
        View layoutHeader = findViewById(R.id.layoutHeaderAdminDetail);
        TextView headerLogo = layoutHeader.findViewById(R.id.headerLogo);
        ImageView headerMenu = layoutHeader.findViewById(R.id.headerMenu);

        // Khởi tạo lõi các tính năng của Header
        HeaderUtils.initHeader(this, layoutHeader, drawerLayout);

        // Gán sự kiện click nút Menu góc trái để đóng/mở Menu trượt Admin
        MenuUtils.setupAdminSideMenu(this, drawerLayout, headerMenu);

        // YÊU CẦU: Khóa ẩn triệt độ nút Tìm kiếm và Thông báo trên Header dành riêng cho Admin
        if (layoutHeader.findViewById(R.id.headerSearch) != null) {
            layoutHeader.findViewById(R.id.headerSearch).setVisibility(View.GONE);
        }
        if (layoutHeader.findViewById(R.id.headerNotification) != null) {
            layoutHeader.findViewById(R.id.headerNotification).setVisibility(View.GONE);
        }

        headerLogo.setText("COMIC APP");
        headerLogo.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminDashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        // 3. CẤU HÌNH: Khóa nút Quay lại hệ thống - Ưu tiên đóng Menu trượt nếu đang mở
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

        // Ánh xạ toàn bộ View bao gồm khối thông tin nâng cấp mở rộng
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
        btnAdminSendComment = findViewById(R.id.btnAdminSendComment);
        rvChapters = findViewById(R.id.rvAdminDetailChapters);
        btnAdminAddChapter = findViewById(R.id.btnAdminAddChapter);

        // Thiết lập danh sách bình luận hàng dọc
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        commentAdapter = new AdminCommentAdapter(this);
        rvComments.setAdapter(commentAdapter);

        // Khởi tạo RecyclerView cấu hình Adapter Chương truyện
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
            adminComment.setParentCommentId(targetParentCommentId); // ĐÃ SỬA: Gửi kèm parent id khi gửi để phân luồng reply con lồng nhau chuẩn

            ApiClient.getApiService().postComment(adminComment).enqueue(new Callback<Comment>() {
                @Override
                public void onResponse(Call<Comment> call, Response<Comment> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(AdminComicDetailActivity.this, "Đăng bình luận thành công!", Toast.LENGTH_SHORT).show();
                        edtAdminCommentInput.setText("");

                        // ĐÃ SỬA: Reset cache và yêu cầu cập nhật lại các nhánh trả lời con thời gian thực giống bên User
                        if (targetParentCommentId != null) {
                            commentAdapter.resetRepliesCache(targetParentCommentId);
                        }
                        targetParentCommentId = null;

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

        // Đồng bộ cập nhật lại ảnh đại diện Admin lên Header nếu có
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

        ApiClient.getApiService().adminGetComicComments(comicId, apiUserId).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    commentAdapter.setData(response.body());
                }
            }
            @Override public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {}
        });
    }

    @Override
    public void onReply(Map<String, Object> comment) {
        // ĐÃ SỬA: Lưu lại commentId được nhận diện từ map phản hồi để gán làm ID gốc cho comment con
        Number idNum = (Number) comment.get("commentId");
        targetParentCommentId = idNum != null ? idNum.intValue() : null;

        String username = (String) comment.get("username");
        if (username != null) {
            edtAdminCommentInput.setText("@" + username + " ");
            edtAdminCommentInput.requestFocus();
            edtAdminCommentInput.setSelection(edtAdminCommentInput.getText().length());
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

            // Đường phân cách mỏng
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
                if (response.isSuccessful()) {
                    loadCommentsData();
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