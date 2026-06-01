package com.yuhbui.comicapp.ui.admin;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.yuhbui.comicapp.R;
import com.yuhbui.comicapp.data.api.ApiClient;
import com.yuhbui.comicapp.data.model.Comment;
import com.yuhbui.comicapp.ui.adapters.AdminChapterImageAdapter;
import com.yuhbui.comicapp.ui.adapters.AdminCommentAdapter;
import com.yuhbui.comicapp.utils.SharedPrefsManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminChapterDetailActivity extends AppCompatActivity implements AdminCommentAdapter.OnCommentAdminActionListener {

    private int chapterId;
    private RecyclerView rvPages;
    private AdminChapterImageAdapter adapter;
    private Button btnUploadPage;
    private RecyclerView rvComments;
    private AdminCommentAdapter commentAdapter;
    private EditText edtCommentInput;
    private Button btnSendComment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_chapter_detail);

        chapterId = getIntent().getIntExtra("CHAPTER_ID", -1);
        String chTitle = getIntent().getStringExtra("CHAPTER_TITLE");

        // Khởi tạo thanh Header Admin
        View layoutHeader = findViewById(R.id.layoutHeaderChapterDetail);
        TextView headerLogo = layoutHeader.findViewById(R.id.headerLogo);
        ImageView headerMenu = layoutHeader.findViewById(R.id.headerMenu);
        layoutHeader.findViewById(R.id.headerSearch).setVisibility(View.GONE);
        layoutHeader.findViewById(R.id.headerNotification).setVisibility(View.GONE);
        headerLogo.setText(chTitle != null ? chTitle.toUpperCase() : "CHI TIẾT CHƯƠNG");
        headerLogo.setTextColor(Color.parseColor("#E74C3C"));
        headerMenu.setOnClickListener(v -> finish());

        // Ánh xạ thành phần quản lý trang truyện
        rvPages = findViewById(R.id.rvAdminChapterPages);
        btnUploadPage = findViewById(R.id.btnAdminUploadPage);

        // BỔ SUNG: Ánh xạ thành phần quản lý bình luận từ Layout XML mới
        rvComments = findViewById(R.id.rvAdminChapterComments);
        edtCommentInput = findViewById(R.id.edtAdminChapterCommentInput);
        btnSendComment = findViewById(R.id.btnAdminChapterSendComment);

        // ĐÃ SỬA: Chuyển đổi hiển thị sang dạng cuộn dọc (LinearLayoutManager) nối đuôi nhau như User style
        rvPages.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminChapterImageAdapter((imageId, position) -> deletePageFromServer(imageId));
        rvPages.setAdapter(adapter);

        // BỔ SUNG: Thiết lập RecyclerView danh sách bình luận cho Admin quản lý
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        commentAdapter = new AdminCommentAdapter(this);
        rvComments.setAdapter(commentAdapter);

        // Sự kiện chọn ảnh thêm trang truyện mới
        btnUploadPage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, 202);
        });

        // BỔ SUNG: Sự kiện gửi bình luận trực tiếp của Admin vào chương này
        btnSendComment.setOnClickListener(v -> sendChapterCommentToServer());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadChapterPages();
        loadChapterComments(); // Tải lại dữ liệu bình luận mỗi khi mở/quay lại màn hình
    }

    private void loadChapterPages() {
        ApiClient.getApiService().adminGetChapterPages(chapterId).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adapter.setData(response.body());
                }
            }
            @Override public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                Toast.makeText(AdminChapterDetailActivity.this, "Lỗi nạp danh sách trang truyện!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // BỔ SUNG: Hàm tải danh sách bình luận riêng biệt của chương này cho Admin quản lý
    private void loadChapterComments() {
        ApiClient.getApiService().adminGetComicComments(chapterId).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    commentAdapter.setData(response.body());
                }
            }
            @Override public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                Toast.makeText(AdminChapterDetailActivity.this, "Lỗi kết nối tải bình luận chương!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // BỔ SUNG: Logic xử lý đăng bình luận Admin trực tiếp vào chương truyện
    private void sendChapterCommentToServer() {
        String content = edtCommentInput.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, "Nội dung bình luận không được để trống!", Toast.LENGTH_SHORT).show();
            return;
        }

        int currentUserId = SharedPrefsManager.getUserId(this);
        if (currentUserId == -1) {
            Toast.makeText(this, "Không thể xác định thông tin tài khoản đăng nhập!", Toast.LENGTH_SHORT).show();
            return;
        }

        Comment adminComment = new Comment();
        adminComment.setChapterId(chapterId);
        adminComment.setUserId(currentUserId);
        adminComment.setContent(content);

        ApiClient.getApiService().postComment(adminComment).enqueue(new Callback<Comment>() {
            @Override
            public void onResponse(Call<Comment> call, Response<Comment> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AdminChapterDetailActivity.this, "Đăng bình luận Admin thành công!", Toast.LENGTH_SHORT).show();
                    edtCommentInput.setText(""); // Xóa trống khung nhập
                    loadChapterComments(); // Làm mới danh sách hiển thị tức thì
                }
            }
            @Override
            public void onFailure(Call<Comment> call, Throwable t) {
                Toast.makeText(AdminChapterDetailActivity.this, "Gửi bình luận thất bại!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 202 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            uploadPageToServer(imageUri);
        }
    }

    private void uploadPageToServer(Uri uri) {
        File file = getFileFromUri(uri);
        if (file == null) return;

        RequestBody requestFile = RequestBody.create(MediaType.parse(getContentResolver().getType(uri)), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

        ApiClient.getApiService().adminUploadChapterPage(chapterId, body).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AdminChapterDetailActivity.this, "Đã thêm trang truyện thành công!", Toast.LENGTH_SHORT).show();
                    loadChapterPages();
                } else {
                    Toast.makeText(AdminChapterDetailActivity.this, "Server từ chối nhận file ảnh!", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<Map<String, String>> call, Throwable t) {
                Toast.makeText(AdminChapterDetailActivity.this, "Lỗi kết nối upload trang truyện!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deletePageFromServer(int imageId) {
        ApiClient.getApiService().adminDeleteChapterPage(imageId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AdminChapterDetailActivity.this, "Đã xóa trang truyện thành công!", Toast.LENGTH_SHORT).show();
                    loadChapterPages();
                } else {
                    Toast.makeText(AdminChapterDetailActivity.this, "Xóa trang truyện thất bại!", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(AdminChapterDetailActivity.this, "Lỗi kết nối khi thực hiện xóa trang!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private File getFileFromUri(Uri uri) {
        try {
            String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(getContentResolver().getType(uri));
            if (extension == null) extension = "jpg";
            File tempFile = new File(getCacheDir(), "page_upload_" + System.currentTimeMillis() + "." + extension);
            try (InputStream inputStream = getContentResolver().openInputStream(uri);
                 OutputStream outputStream = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[4096];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
                outputStream.flush();
            }
            return tempFile;
        } catch (Exception e) {
            return null;
        }
    }

    // INTERFACE CALLBACKS: Thực thi hành động tương tác quản trị bình luận chương truyện
    @Override
    public void onReply(Map<String, Object> comment) {
        if (comment.get("commentId") != null) {
            Number idNum = (Number) comment.get("commentId");
            Toast.makeText(this, "Chức năng phản hồi bình luận chương ID: " + idNum.intValue(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDelete(int commentId, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa bình luận chương")
                .setMessage("Bạn có chắc chắn muốn xóa vĩnh viễn bình luận phạm quy này không?")
                .setPositiveButton("Xóa vĩnh viễn", (dialog, which) -> {
                    ApiClient.getApiService().adminDeleteComment(commentId).enqueue(new Callback<Map<String, Object>>() {
                        @Override
                        public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(AdminChapterDetailActivity.this, "Đã xóa bình luận khỏi hệ thống!", Toast.LENGTH_SHORT).show();
                                loadChapterComments(); // Làm mới danh sách hiển thị
                            }
                        }
                        @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
                    });
                })
                .setNegativeButton("Hủy", null).show();
    }
}