package com.yuhbui.comicapp.ui.admin;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
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
import com.yuhbui.comicapp.utils.SharedPrefsManager;

import java.util.List;
import java.util.Map;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminComicDetailActivity extends AppCompatActivity implements AdminCommentAdapter.OnCommentAdminActionListener {

    private int comicId;
    private ImageView imgCover;
    private TextView tvTitle, tvAuthor, tvStatus, tvDesc, tvGenre, tvRelease, tvViews, tvFavorites, tvRating;
    private Button btnEdit, btnDelete, btnAdminSendComment, btnAdminAddChapter;
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

        // Khởi tạo thanh Header Admin
        View layoutHeader = findViewById(R.id.layoutHeaderAdminDetail);
        TextView headerLogo = layoutHeader.findViewById(R.id.headerLogo);
        ImageView headerMenu = layoutHeader.findViewById(R.id.headerMenu);
        layoutHeader.findViewById(R.id.headerSearch).setVisibility(View.GONE);
        layoutHeader.findViewById(R.id.headerNotification).setVisibility(View.GONE);
        headerLogo.setText("CHI TIẾT QUẢN TRỊ");
        headerLogo.setTextColor(Color.parseColor("#E74C3C"));
        headerMenu.setOnClickListener(v -> finish());

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
                intent.putExtra("CHAPTER_TITLE", "Chương " + chapter.get("chapterNumber"));
                startActivity(intent);
            }

            @Override
            public void onEdit(Map<String, Object> chapter) {
                showChapterFormDialog(chapter);
            }

            @Override
            public void onDelete(int chapterId, int position) {
                // Đã được tích hợp trực tiếp vào danh sách chương hàng ngang hoặc Dialog
            }
        });
        rvChapters.setAdapter(chapterAdapter);

        btnAdminAddChapter.setOnClickListener(v -> showChapterFormDialog(null));

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

            ApiClient.getApiService().postComment(adminComment).enqueue(new Callback<Comment>() {
                @Override
                public void onResponse(Call<Comment> call, Response<Comment> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(AdminComicDetailActivity.this, "Đăng bình luận thành công!", Toast.LENGTH_SHORT).show();
                        edtAdminCommentInput.setText("");
                        loadCommentsData();
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadComicDetailData();
        loadChaptersData();
        loadCommentsData();
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
                    currentComic = response.body().getComic();
                    tvTitle.setText(currentComic.getTitle());
                    tvAuthor.setText("Tác giả: " + currentComic.getAuthor());
                    tvStatus.setText("Tình trạng: " + currentComic.getStatus());
                    tvDesc.setText(currentComic.getDescription());

                    tvViews.setText("👁️ " + currentComic.getViewCount());
                    tvRating.setText("⭐ " + currentComic.getRating() + "/5");
                    tvFavorites.setText("❤️ " + currentComic.getFollowCount());
                    tvRelease.setText("Phát hành: " + (currentComic.getCreatedAt() != null ? currentComic.getCreatedAt() : "Đang cập nhật"));

                    if (response.body().getGenres() != null && !response.body().getGenres().isEmpty()) {
                        tvGenre.setText("Thể loại: " + response.body().getGenres());
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
        ApiClient.getApiService().adminGetComicComments(comicId).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    commentAdapter.setData(response.body());
                }
            }
            @Override public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {}
        });
    }

    // ĐÃ SỬA: Thực hiện đúng chức năng Gắn tag phản hồi khi nhấn nút Phản hồi / Trả lời
    @Override
    public void onReply(Map<String, Object> comment) {
        String username = (String) comment.get("username");
        if (username != null) {
            edtAdminCommentInput.setText("@" + username + " ");
            edtAdminCommentInput.requestFocus();
            edtAdminCommentInput.setSelection(edtAdminCommentInput.getText().length());
        }
    }

    // ĐÃ BỔ SUNG: Chức năng xử lý khi bấm vào dòng văn bản Báo cáo sẽ hiển thị danh sách lý do
    @Override
    public void onShowReports(int commentId) {
        ApiClient.getApiService().adminGetCommentReports(commentId).enqueue(new Callback<List<String>>() {
            @Override
            public void onResponse(Call<List<String>> call, Response<List<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<String> reports = response.body();
                    if (reports.isEmpty()) {
                        Toast.makeText(AdminComicDetailActivity.this, "Bình luận này chưa có lượt báo cáo chi tiết nào!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    CharSequence[] items = reports.toArray(new CharSequence[0]);
                    new AlertDialog.Builder(AdminComicDetailActivity.this)
                            .setTitle("Nội dung người dùng báo cáo (" + reports.size() + ")")
                            .setItems(items, null)
                            .setPositiveButton("Đóng", null).show();
                }
            }
            @Override public void onFailure(Call<List<String>> call, Throwable t) {}
        });
    }

    // ĐÃ BỔ SUNG: Kích hoạt chức năng tương tác nút Like/Dislike trực tiếp dành cho Admin
    @Override
    public void onInteract(int commentId, int type, int position) {
        int currentUserId = SharedPrefsManager.getUserId(this);
        if (currentUserId == -1) return;

        ApiClient.getApiService().interactWithComment(commentId, currentUserId, type).enqueue(new Callback<Comment>() {
            @Override
            public void onResponse(Call<Comment> call, Response<Comment> response) {
                if (response.isSuccessful()) {
                    loadCommentsData(); // Tải lại danh sách để cập nhật số lượt tương tác nhảy ngay lập tức
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