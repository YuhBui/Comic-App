package com.yuhbui.comicapp.ui.admin;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.yuhbui.comicapp.R;
import com.yuhbui.comicapp.data.api.ApiClient;
import com.yuhbui.comicapp.data.model.Category;
import com.yuhbui.comicapp.data.model.Comic;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminEditComicActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 101;

    private TextView tvFormTitle, tvComicCategoriesSelect;
    private EditText edtComicTitle, edtComicAuthor, edtComicDescription;
    private ImageView imgComicCoverSelect;
    private Button btnSelectCover, btnCancelComicForm, btnSaveComicForm;

    // Đã sửa: Thay đổi Spinner trạng thái cũ sang RadioGroup
    private RadioGroup rgStatus;

    private int editComicId = -1;
    private Uri selectedImageUri = null;
    private String uploadedCoverUrl = "";

    // Quản lý đa lựa chọn thể loại truyện
    private List<Category> allCategories = new ArrayList<>();
    private List<Integer> selectedCategoryIds = new ArrayList<>();
    private boolean[] checkedItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_edit_comic);

        // Ánh xạ View từ XML
        tvFormTitle = findViewById(R.id.tvFormTitle);
        edtComicTitle = findViewById(R.id.edtComicTitle);
        edtComicAuthor = findViewById(R.id.edtComicAuthor);
        edtComicDescription = findViewById(R.id.edtComicDescription);
        imgComicCoverSelect = findViewById(R.id.imgComicCoverSelect);
        btnSelectCover = findViewById(R.id.btnSelectCover);
        btnCancelComicForm = findViewById(R.id.btnCancelComicForm);
        btnSaveComicForm = findViewById(R.id.btnSaveComicForm);
        tvComicCategoriesSelect = findViewById(R.id.tvComicCategoriesSelect);
        rgStatus = findViewById(R.id.rgStatus); // Ánh xạ RadioGroup trạng thái

        // Tải danh mục thể loại nền từ hệ thống Server
        loadCategoriesFromServer();

        // Đăng ký sự kiện mở danh sách chọn nhiều thể loại truyện
        tvComicCategoriesSelect.setOnClickListener(v -> showMultiSelectCategoryDialog());

        // Kiểm tra xử lý nạp dữ liệu nếu là luồng SỬA BỘ TRUYỆN HỆ THỐNG
        if (getIntent() != null && getIntent().hasExtra("EDIT_COMIC_ID")) {
            editComicId = getIntent().getIntExtra("EDIT_COMIC_ID", -1);
            tvFormTitle.setText("CHỈNH SỬA THÔNG TIN TRUYỆN");
            edtComicTitle.setText(getIntent().getStringExtra("TITLE"));
            edtComicAuthor.setText(getIntent().getStringExtra("AUTHOR"));
            edtComicDescription.setText(getIntent().getStringExtra("DESC"));
            uploadedCoverUrl = getIntent().getStringExtra("COVER_URL");

            // Tải trạng thái và tích chọn tương ứng lên giao diện RadioButton
            String currentStatus = getIntent().getStringExtra("STATUS");
            if ("Completed".equalsIgnoreCase(currentStatus)) {
                rgStatus.check(R.id.rbCompleted);
            } else {
                rgStatus.check(R.id.rbOngoing);
            }

            if (uploadedCoverUrl != null && !uploadedCoverUrl.isEmpty()) {
                Glide.with(this).load(uploadedCoverUrl).into(imgComicCoverSelect);
            }
        }

        // Sự kiện click nút Chọn ảnh từ thư viện máy
        btnSelectCover.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        });

        // Nút hủy bỏ đóng màn hình quay lại vùng quản lý truyện an toàn
        btnCancelComicForm.setOnClickListener(v -> finish());

        // Nút lưu thông tin dữ liệu truyện
        btnSaveComicForm.setOnClickListener(v -> executeSavingProcess());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            imgComicCoverSelect.setImageURI(selectedImageUri); // Đổ ảnh xem trước
        }
    }

    private void loadCategoriesFromServer() {
        ApiClient.getApiService().getAllCategories().enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allCategories = response.body();
                    checkedItems = new boolean[allCategories.size()];
                } else {
                    Toast.makeText(AdminEditComicActivity.this, "Không thể tải danh sách thể loại từ máy chủ", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {
                Toast.makeText(AdminEditComicActivity.this, "Lỗi kết nối danh mục thể loại", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showMultiSelectCategoryDialog() {
        if (allCategories.isEmpty()) {
            Toast.makeText(this, "Dữ liệu thể loại đang được tải, vui lòng thử lại sau giây lát!", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] catNames = new String[allCategories.size()];
        for (int i = 0; i < allCategories.size(); i++) {
            catNames[i] = allCategories.get(i).getName();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chọn các thể loại cho truyện");
        builder.setMultiChoiceItems(catNames, checkedItems, (dialog, which, isChecked) -> checkedItems[which] = isChecked);

        builder.setPositiveButton("Xác nhận", (dialog, which) -> {
            selectedCategoryIds.clear();
            StringBuilder displayText = new StringBuilder();

            for (int i = 0; i < checkedItems.length; i++) {
                if (checkedItems[i]) {
                    selectedCategoryIds.add(allCategories.get(i).getCategoryId());
                    if (displayText.length() > 0) displayText.append(", ");
                    displayText.append(allCategories.get(i).getName());
                }
            }

            if (selectedCategoryIds.isEmpty()) {
                tvComicCategoriesSelect.setText("Bấm vào đây để chọn thể loại...");
            } else {
                tvComicCategoriesSelect.setText(displayText.toString());
            }
        });

        builder.setNegativeButton("Hủy bỏ", null);
        builder.setNeutralButton("+ Tạo thể loại mới", (dialog, which) -> showAddNewCategoryDialog());
        builder.show();
    }

    private void showAddNewCategoryDialog() {
        EditText edtNewCat = new EditText(this);
        edtNewCat.setHint("Nhập tên thể loại truyện mới");
        new AlertDialog.Builder(this)
                .setTitle("Tạo thể loại mới")
                .setView(edtNewCat)
                .setPositiveButton("Tạo", (dialog, which) -> {
                    String name = edtNewCat.getText().toString().trim();
                    if (!name.isEmpty()) {
                        Category c = new Category(name); // Không còn bị lỗi gạch đỏ nữa
                        ApiClient.getApiService().createCategory(c).enqueue(new Callback<Category>() {
                            @Override
                            public void onResponse(Call<Category> call, Response<Category> response) {
                                if (response.isSuccessful()) {
                                    Toast.makeText(AdminEditComicActivity.this, "Đã tạo thể loại mới thành công!", Toast.LENGTH_SHORT).show();
                                    loadCategoriesFromServer(); // Làm mới danh mục nền
                                } else {
                                    Toast.makeText(AdminEditComicActivity.this, "Server từ chối tạo thể loại!", Toast.LENGTH_SHORT).show();
                                }
                            }
                            @Override
                            public void onFailure(Call<Category> call, Throwable t) {
                                Toast.makeText(AdminEditComicActivity.this, "Lỗi kết nối tạo thể loại mới", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void executeSavingProcess() {
        String title = edtComicTitle.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(this, "Tên bộ truyện tranh không được bỏ trống!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedImageUri != null) {
            File file = getFileFromUri(selectedImageUri);
            if (file == null) {
                Toast.makeText(this, "Không thể đọc dữ liệu file ảnh bìa này!", Toast.LENGTH_SHORT).show();
                return;
            }

            RequestBody requestFile = RequestBody.create(MediaType.parse(getContentResolver().getType(selectedImageUri)), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

            ApiClient.getApiService().adminUploadCover(body).enqueue(new Callback<Map<String, String>>() {
                @Override
                public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        uploadedCoverUrl = response.body().get("coverUrl");
                        saveComicTextInfo(title); // Gửi tiếp phần thông tin văn bản truyện
                    } else {
                        Toast.makeText(AdminEditComicActivity.this, "Server từ chối nhận file ảnh! Mã lỗi: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(Call<Map<String, String>> call, Throwable t) {
                    Toast.makeText(AdminEditComicActivity.this, "Lỗi mạng khi upload tệp ảnh bìa: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Không chọn ảnh mới -> Lưu luôn thông tin text (Giữ nguyên link ảnh cũ)
            saveComicTextInfo(title);
        }
    }

    private void saveComicTextInfo(String title) {
        Comic comic = new Comic();
        comic.setTitle(title);
        comic.setAuthor(edtComicAuthor.getText().toString().trim());
        comic.setDescription(edtComicDescription.getText().toString().trim());
        comic.setCoverImageUrl(uploadedCoverUrl);

        // Đã sửa: Bóc tách lấy text từ RadioButton trạng thái được chọn
        String status = "Ongoing";
        if (rgStatus.getCheckedRadioButtonId() == R.id.rbCompleted) {
            status = "Completed";
        }
        comic.setStatus(status);

        if (editComicId == -1) {
            // TIẾN TRÌNH THÊM MỚI TRUYỆN TRANH
            ApiClient.getApiService().adminCreateComic(comic, selectedCategoryIds).enqueue(new Callback<Comic>() {
                @Override
                public void onResponse(Call<Comic> call, Response<Comic> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(AdminEditComicActivity.this, "Thêm truyện mới thành công!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(AdminEditComicActivity.this, "Lỗi tạo truyện từ server. Mã: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(Call<Comic> call, Throwable t) {
                    Toast.makeText(AdminEditComicActivity.this, "Thất bại kết nối mạng thêm truyện", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // TIẾN TRÌNH CẬP NHẬT (EDIT) TRUYỆN TRANH
            ApiClient.getApiService().adminUpdateComic(editComicId, comic, selectedCategoryIds).enqueue(new Callback<Comic>() {
                @Override
                public void onResponse(Call<Comic> call, Response<Comic> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(AdminEditComicActivity.this, "Cập nhật dữ liệu thành công!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(AdminEditComicActivity.this, "Lỗi sửa truyện từ server. Mã: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(Call<Comic> call, Throwable t) {
                    Toast.makeText(AdminEditComicActivity.this, "Thất bại kết nối mạng sửa truyện", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private File getFileFromUri(Uri uri) {
        try {
            String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(getContentResolver().getType(uri));
            if (extension == null) extension = "jpg";
            File tempFile = new File(getCacheDir(), "cover_upload_" + System.currentTimeMillis() + "." + extension);
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
}