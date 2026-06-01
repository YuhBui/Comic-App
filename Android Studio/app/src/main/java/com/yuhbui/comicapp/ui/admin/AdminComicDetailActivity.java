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
    private TextView tvTitle, tvAuthor, tvStatus, tvDesc;
    private Button btnEdit, btnDelete;
    private RecyclerView rvComments;
    private AdminCommentAdapter commentAdapter;
    private Comic currentComic;
    private EditText edtAdminCommentInput;
    private Button btnAdminSendComment;

    // Các thành phần xử lý quản trị Chương truyện
    private RecyclerView rvChapters;
    private AdminChapterAdapter chapterAdapter;
    private Button btnAdminAddChapter;

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

        // Ánh xạ View chính
        imgCover = findViewById(R.id.imgAdminDetailCover);
        tvTitle = findViewById(R.id.tvAdminDetailTitle);
        tvAuthor = findViewById(R.id.tvAdminDetailAuthor);
        tvStatus = findViewById(R.id.tvAdminDetailStatus);
        tvDesc = findViewById(R.id.tvAdminDetailDesc);
        btnEdit = findViewById(R.id.btnAdminEditComicDetail);
        btnDelete = findViewById(R.id.btnAdminDeleteComicDetail);
        rvComments = findViewById(R.id.rvAdminDetailComments);

        // Ánh xạ phần ô nhập và nút gửi bình luận của Admin từ Layout XML
        edtAdminCommentInput = findViewById(R.id.edtAdminCommentInput);
        btnAdminSendComment = findViewById(R.id.btnAdminSendComment);

        // ĐÃ SỬA: Ánh xạ thành phần Quản lý chương truyện từ giao diện Layout XML
        rvChapters = findViewById(R.id.rvAdminDetailChapters);
        btnAdminAddChapter = findViewById(R.id.btnAdminAddChapter);

        // Thiết lập danh sách bình luận hàng dọc
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        commentAdapter = new AdminCommentAdapter(this);
        rvComments.setAdapter(commentAdapter);

        // ĐÃ SỬA: Khởi tạo RecyclerView cấu hình Adapter Chương truyện
        rvChapters.setLayoutManager(new LinearLayoutManager(this));
        chapterAdapter = new AdminChapterAdapter(new AdminChapterAdapter.OnChapterAdminActionListener() {
            @Override
            public void onClick(Map<String, Object> chapter) {
                // Click xem chi tiết -> Mở màn hình quản trị nội dung các trang ảnh truyện
                Number idNum = (Number) chapter.get("chapterId");
                int id = idNum != null ? idNum.intValue() : -1;

                Intent intent = new Intent(AdminComicDetailActivity.this, AdminChapterDetailActivity.class);
                intent.putExtra("CHAPTER_ID", id);
                intent.putExtra("CHAPTER_TITLE", "Chương " + chapter.get("chapterNumber"));
                startActivity(intent);
            }

            @Override
            public void onEdit(Map<String, Object> chapter) {
                // Giữ lâu dòng chương truyện bất kỳ để mở Form Chỉnh sửa hoặc Xóa chương
                showChapterFormDialog(chapter);
            }

            @Override
            public void onDelete(int chapterId, int position) {
                // Đã được tích hợp trực tiếp vào trong Dialog Form để giao diện sạch đẹp
            }
        });
        rvChapters.setAdapter(chapterAdapter);

        // Sự kiện khi Admin bấm nút thêm chương mới
        btnAdminAddChapter.setOnClickListener(v -> showChapterFormDialog(null));

        // Xử lý sự kiện khi Admin bấm nút gửi bình luận trực tiếp tại đây
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
                    } else {
                        Toast.makeText(AdminComicDetailActivity.this, "Lỗi đăng bình luận từ server!", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Comment> call, Throwable t) {
                    Toast.makeText(AdminComicDetailActivity.this, "Lỗi mạng, không thể gửi bình luận!", Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Sự kiện click nút Sửa truyện
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

        // Sự kiện click nút Xóa truyện
        btnDelete.setOnClickListener(v -> {
            if (currentComic == null) return;
            new AlertDialog.Builder(this)
                    .setTitle("Xác nhận xóa truyện")
                    .setMessage("Bạn có chắc chắn muốn xóa vĩnh viễn bộ truyện '" + currentComic.getTitle() + "' không? Hành động này sẽ xóa tất cả các chương và bình luận liên quan!")
                    .setPositiveButton("Xóa vĩnh viễn", (dialog, which) -> {
                        ApiClient.getApiService().adminDeleteComic(comicId).enqueue(new Callback<ResponseBody>() {
                            @Override
                            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                if (response.isSuccessful()) {
                                    Toast.makeText(AdminComicDetailActivity.this, "Đã xóa truyện khỏi hệ thống thành công!", Toast.LENGTH_SHORT).show();
                                    finish();
                                } else {
                                    Toast.makeText(AdminComicDetailActivity.this, "Xóa thất bại từ phía Server!", Toast.LENGTH_SHORT).show();
                                }
                            }
                            @Override
                            public void onFailure(Call<ResponseBody> call, Throwable t) {
                                Toast.makeText(AdminComicDetailActivity.this, "Lỗi mạng khi thực hiện xóa truyện!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    })
                    .setNegativeButton("Hủy", null).show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadComicDetailData();
        loadChaptersData(); // Tải lại danh sách chương khi màn hình hiển thị hoặc quay lại
        loadCommentsData();
    }

    // ĐA CHỨC NĂNG DIALOG: Thêm mới, Chỉnh sửa, Xóa chương truyện tích hợp chung một form nhập liệu
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

            // Nếu luồng sửa: Bổ sung thêm nút XÓA CHƯƠNG ở góc trái thanh Dialog
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
            if (numStr.isEmpty()) {
                Toast.makeText(AdminComicDetailActivity.this, "Số thứ tự chương không được rỗng!", Toast.LENGTH_SHORT).show();
                return;
            }

            double num = Double.parseDouble(numStr);
            if (!isEdit) {
                // CHẠY TIẾN TRÌNH TẠO MỚI CHƯƠNG TRUYỆN
                ApiClient.getApiService().adminCreateChapter(comicId, num, title).enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(AdminComicDetailActivity.this, "Đã thêm chương truyện mới!", Toast.LENGTH_SHORT).show();
                            loadChaptersData();
                        }
                    }
                    @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
                });
            } else {
                // CHẠY TIẾN TRÌNH CẬP NHẬT CHƯƠNG TRUYỆN
                Number idNum = (Number) chapterData.get("chapterId");
                int chId = idNum != null ? idNum.intValue() : -1;

                ApiClient.getApiService().adminUpdateChapter(chId, num, title).enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(AdminComicDetailActivity.this, "Cập nhật chương thành công!", Toast.LENGTH_SHORT).show();
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
                    tvStatus.setText(currentComic.getStatus());
                    tvDesc.setText(currentComic.getDescription());

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

    // ĐÃ BỔ SUNG: Hàm tải danh sách chương truyện từ Backend đổ vào Adapter
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

    @Override
    public void onReply(Map<String, Object> comment) {
        if (comment.get("commentId") != null) {
            Toast.makeText(this, "Chức năng trả lời bình luận ID: " + comment.get("commentId"), Toast.LENGTH_SHORT).show();
        }
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
                            } else {
                                Toast.makeText(AdminComicDetailActivity.this, "Xóa thất bại từ phía Server!", Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
                    });
                })
                .setNegativeButton("Hủy", null).show();
    }
}