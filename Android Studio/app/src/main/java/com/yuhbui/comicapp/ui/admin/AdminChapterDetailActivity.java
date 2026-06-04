package com.yuhbui.comicapp.ui.admin;

import android.content.ClipData;
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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.ItemTouchHelper;
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
import java.util.ArrayList;
import java.util.Collections;
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
    private NestedScrollView nestedScrollView;
    private RecyclerView rvComments;
    private AdminCommentAdapter commentAdapter;
    private EditText edtCommentInput;
    private Button btnSendComment;

    // Mảng lưu danh sách dữ liệu trang truyện phục vụ kéo thả reorder vị trí
    private List<Map<String, Object>> pageList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_chapter_detail);

        chapterId = getIntent().getIntExtra("CHAPTER_ID", -1);
        String chTitle = getIntent().getStringExtra("CHAPTER_TITLE");

        View layoutHeader = findViewById(R.id.layoutHeaderChapterDetail);
        TextView headerLogo = layoutHeader.findViewById(R.id.headerLogo);
        ImageView headerMenu = layoutHeader.findViewById(R.id.headerMenu);
        layoutHeader.findViewById(R.id.headerSearch).setVisibility(View.GONE);
        layoutHeader.findViewById(R.id.headerNotification).setVisibility(View.GONE);
        headerLogo.setText(chTitle != null ? chTitle.toUpperCase() : "QUẢN LÝ TRANG TRUYỆN");
        headerLogo.setTextColor(Color.parseColor("#E74C3C"));
        headerMenu.setOnClickListener(v -> finish());

        // Ánh xạ thành phần trang truyện
        rvPages = findViewById(R.id.rvAdminChapterPages);
        btnUploadPage = findViewById(R.id.btnAdminUploadPage);
        nestedScrollView = findViewById(R.id.nestedScrollViewChapterDetail);

        // Ánh xạ thành phần bình luận chương từ layout XML
        rvComments = findViewById(R.id.rvAdminChapterComments);
        edtCommentInput = findViewById(R.id.edtAdminChapterCommentInput);
        btnSendComment = findViewById(R.id.btnAdminChapterSendComment);

        // Cấu hình LayoutManager cuộn dọc cho trang truyện
        rvPages.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminChapterImageAdapter((imageId, position) -> deletePageFromServer(imageId));
        rvPages.setAdapter(adapter);

        // Cấu hình RecyclerView và gán Adapter bình luận chương truyện
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        commentAdapter = new AdminCommentAdapter(this);
        rvComments.setAdapter(commentAdapter);

        // KÍCH HOẠT TÍNH NĂNG KÉO THẢ ĐỔI VỊ TRÍ ẢNH VÀ TỰ ĐỘNG DI CHUYỂN MÀN HÌNH THEO CHIỀU KÉO
        ItemTouchHelper.SimpleCallback touchHelperCallback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder src, @NonNull RecyclerView.ViewHolder target) {
                int fromPos = src.getAdapterPosition();
                int toPos = target.getAdapterPosition();
                Collections.swap(pageList, fromPos, toPos);
                adapter.notifyItemMoved(fromPos, toPos);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}

            @Override
            public void onChildDraw(@NonNull android.graphics.Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && isCurrentlyActive) {
                    View itemView = viewHolder.itemView;
                    int[] itemLocation = new int[2];
                    itemView.getLocationOnScreen(itemLocation);
                    int itemY = itemLocation[1];

                    int[] scrollLocation = new int[2];
                    nestedScrollView.getLocationOnScreen(scrollLocation);
                    int scrollViewTop = scrollLocation[1];
                    int scrollViewBottom = scrollViewTop + nestedScrollView.getHeight();

                    int threshold = 250;
                    int scrollSpeed = 25;

                    if (itemY < scrollViewTop + threshold) {
                        nestedScrollView.smoothScrollBy(0, -scrollSpeed);
                    } else if (itemY + itemView.getHeight() > scrollViewBottom - threshold) {
                        nestedScrollView.smoothScrollBy(0, scrollSpeed);
                    }
                }
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                executeReorderPagesOnServer();
            }
        };
        new ItemTouchHelper(touchHelperCallback).attachToRecyclerView(rvPages);

        // Bấm nút thêm trang mới (Cho phép lựa chọn đa tệp đồng thời)
        btnUploadPage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(Intent.createChooser(intent, "Chọn các trang truyện muốn thêm"), 202);
        });

        // Đăng ký sự kiện click gửi bình luận chương truyện của Admin
        btnSendComment.setOnClickListener(v -> sendChapterCommentToServer());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadChapterPages();
        loadChapterComments();
    }

    private void loadChapterPages() {
        ApiClient.getApiService().adminGetChapterPages(chapterId).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    pageList = response.body();
                    adapter.setData(pageList);
                }
            }
            @Override public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {}
        });
    }

    private void loadChapterComments() {
        ApiClient.getApiService().adminGetChapterComments(chapterId).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    commentAdapter.setData(response.body());
                }
            }
            @Override public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {}
        });
    }

    private void sendChapterCommentToServer() {
        String content = edtCommentInput.getText().toString().trim();
        if (content.isEmpty()) return;

        Comment adminComment = new Comment();
        adminComment.setChapterId(chapterId);
        adminComment.setUserId(SharedPrefsManager.getUserId(this));
        adminComment.setContent(content);

        ApiClient.getApiService().postComment(adminComment).enqueue(new Callback<Comment>() {
            @Override
            public void onResponse(Call<Comment> call, Response<Comment> response) {
                if (response.isSuccessful()) {
                    edtCommentInput.setText("");
                    Toast.makeText(AdminChapterDetailActivity.this, "Đã đăng bình luận chương thành công!", Toast.LENGTH_SHORT).show();
                    loadChapterComments();
                } else if (response.code() == 403) {
                    Toast.makeText(AdminChapterDetailActivity.this, "Tài khoản đang bị khóa chức năng bình luận!", Toast.LENGTH_LONG).show();
                }
            }
            @Override public void onFailure(Call<Comment> call, Throwable t) {}
        });
    }

    private void executeReorderPagesOnServer() {
        List<Integer> reorderedIds = new ArrayList<>();
        for (Map<String, Object> page : pageList) {
            reorderedIds.add(((Double) page.get("imageId")).intValue());
        }

        ApiClient.getApiService().adminReorderPages(reorderedIds).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AdminChapterDetailActivity.this, "Đã lưu vị trí ảnh mới!", Toast.LENGTH_SHORT).show();
                    loadChapterPages();
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    private void uploadMultiplePagesToServer(List<Uri> uris) {
        if (uris == null || uris.isEmpty()) return;
        Toast.makeText(this, "Đang tuần tự tải lên " + uris.size() + " trang truyện...", Toast.LENGTH_SHORT).show();
        uploadPageSequentially(uris, 0, uris.size(), 0);
    }

    private void uploadPageSequentially(List<Uri> uris, int index, int total, int successCount) {
        if (index >= total) {
            Toast.makeText(this, "Đã thêm thành công " + successCount + "/" + total + " trang truyện mới!", Toast.LENGTH_SHORT).show();
            loadChapterPages();
            return;
        }

        Uri uri = uris.get(index);
        File file = getFileFromUri(uri);
        if (file == null) {
            uploadPageSequentially(uris, index + 1, total, successCount);
            return;
        }

        RequestBody rf = RequestBody.create(MediaType.parse(getContentResolver().getType(uri)), file);
        MultipartBody.Part part = MultipartBody.Part.createFormData("file", file.getName(), rf);

        ApiClient.getApiService().adminUploadChapterPage(chapterId, part).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                if (response.isSuccessful()) {
                    uploadPageSequentially(uris, index + 1, total, successCount + 1);
                } else {
                    uploadPageSequentially(uris, index + 1, total, successCount);
                }
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                uploadPageSequentially(uris, index + 1, total, successCount);
            }
        });
    }

    private void deletePageFromServer(int imageId) {
        ApiClient.getApiService().adminDeleteChapterPage(imageId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) { loadChapterPages(); }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 202 && resultCode == RESULT_OK && data != null) {
            List<Uri> selectedUris = new ArrayList<>();

            if (data.getClipData() != null) {
                ClipData clipData = data.getClipData();
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    selectedUris.add(clipData.getItemAt(i).getUri());
                }
            } else if (data.getData() != null) {
                selectedUris.add(data.getData());
            }

            if (!selectedUris.isEmpty()) {
                uploadMultiplePagesToServer(selectedUris);
            }
        }
    }

    private File getFileFromUri(Uri uri) {
        try {
            String ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(getContentResolver().getType(uri));
            File tempFile = new File(getCacheDir(), "page_ch_" + System.currentTimeMillis() + "." + (ext != null ? ext : "jpg"));
            try (InputStream is = getContentResolver().openInputStream(uri); OutputStream os = new FileOutputStream(tempFile)) {
                byte[] buf = new byte[4096]; int len;
                while ((len = is.read(buf)) > 0) os.write(buf, 0, len);
                os.flush();
            }
            return tempFile;
        } catch (Exception e) { return null; }
    }

    @Override
    public void onReply(Map<String, Object> comment) {
        String username = (String) comment.get("username");
        if (username != null) {
            edtCommentInput.setText("@" + username + " ");
            edtCommentInput.requestFocus();
            edtCommentInput.setSelection(edtCommentInput.getText().length());
        }
    }

    @Override
    public void onShowReports(int commentId) {
        ApiClient.getApiService().adminGetCommentReports(commentId).enqueue(new Callback<List<String>>() {
            @Override
            public void onResponse(Call<List<String>> call, Response<List<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<String> reports = response.body();
                    if (reports.isEmpty()) {
                        Toast.makeText(AdminChapterDetailActivity.this, "Bình luận chương này chưa bị báo cáo!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    CharSequence[] items = reports.toArray(new CharSequence[0]);
                    new AlertDialog.Builder(AdminChapterDetailActivity.this)
                            .setTitle("Nội dung người dùng báo cáo (" + reports.size() + ")")
                            .setItems(items, null)
                            .setPositiveButton("Đóng", null).show();
                }
            }
            @Override public void onFailure(Call<List<String>> call, Throwable t) {}
        });
    }

    @Override
    public void onDelete(int commentId, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa bình luận chương")
                .setMessage("Bạn có chắc chắn muốn xóa vĩnh viễn bình luận này không?")
                .setPositiveButton("Xóa vĩnh viễn", (dialog, which) -> {
                    ApiClient.getApiService().adminDeleteComment(commentId).enqueue(new Callback<Map<String, Object>>() {
                        @Override
                        public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(AdminChapterDetailActivity.this, "Đã xóa bình luận khỏi hệ thống!", Toast.LENGTH_SHORT).show();
                                loadChapterComments();
                            }
                        }
                        @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
                    });
                })
                .setNegativeButton("Hủy", null).show();
    }

    @Override
    public void onInteract(int commentId, int type, int position) {
        int currentUserId = SharedPrefsManager.getUserId(this);
        if (currentUserId == -1) return;
        ApiClient.getApiService().interactWithComment(commentId, currentUserId, type).enqueue(new Callback<Comment>() {
            @Override
            public void onResponse(Call<Comment> call, Response<Comment> response) {
                if (response.isSuccessful()) { loadChapterComments(); }
            }
            @Override public void onFailure(Call<Comment> call, Throwable t) {}
        });
    }
}