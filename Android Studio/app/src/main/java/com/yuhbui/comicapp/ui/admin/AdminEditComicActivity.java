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

    private TextView tvFormTitle;
    private EditText edtComicTitle, edtComicAuthor, edtComicStatus, edtComicDescription;
    private ImageView imgComicCoverSelect;
    private Button btnSelectCover, btnCancelComicForm, btnSaveComicForm;
    private Spinner spinnerCategory;

    private int editComicId = -1;
    private Uri selectedImageUri = null;
    private String uploadedCoverUrl = "";

    private List<Category> categoryList = new ArrayList<>();
    private List<String> spinnerItems = new ArrayList<>();
    private ArrayAdapter<String> spinnerAdapter;
    private int selectedCategoryId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_edit_comic);

        // Ánh xạ View
        tvFormTitle = findViewById(R.id.tvFormTitle);
        edtComicTitle = findViewById(R.id.edtComicTitle);
        edtComicAuthor = findViewById(R.id.edtComicAuthor);
        edtComicStatus = findViewById(R.id.edtComicStatus);
        edtComicDescription = findViewById(R.id.edtComicDescription);
        imgComicCoverSelect = findViewById(R.id.imgComicCoverSelect);
        btnSelectCover = findViewById(R.id.btnSelectCover);
        btnCancelComicForm = findViewById(R.id.btnCancelComicForm);
        btnSaveComicForm = findViewById(R.id.btnSaveComicForm);
        spinnerCategory = findViewById(R.id.spinnerCategory);

        // Thiết lập Spinner thể loại
        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, spinnerItems);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(spinnerAdapter);

        loadCategoriesFromServer();

        // Xử lý nạp dữ liệu nếu là lệnh SỬA TRUYỆN
        if (getIntent() != null && getIntent().hasExtra("EDIT_COMIC_ID")) {
            editComicId = getIntent().getIntExtra("EDIT_COMIC_ID", -1);
            tvFormTitle.setText("CHỈNH SỬA THÔNG TIN TRUYỆN");
            edtComicTitle.setText(getIntent().getStringExtra("TITLE"));
            edtComicAuthor.setText(getIntent().getStringExtra("AUTHOR"));
            edtComicStatus.setText(getIntent().getStringExtra("STATUS"));
            edtComicDescription.setText(getIntent().getStringExtra("DESC"));
            uploadedCoverUrl = getIntent().getStringExtra("COVER_URL");

            if (uploadedCoverUrl != null && !uploadedCoverUrl.isEmpty()) {
                Glide.with(this).load(uploadedCoverUrl).into(imgComicCoverSelect);
            }
        }

        // Sự kiện nút chọn ảnh bìa
        btnSelectCover.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        });

        // NÚT HỦY BỎ QUAY LẠI HỢP LỆ
        btnCancelComicForm.setOnClickListener(v -> finish());

        // Nút lưu thông tin
        btnSaveComicForm.setOnClickListener(v -> executeSavingProcess());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            imgComicCoverSelect.setImageURI(selectedImageUri); // Hiển thị preview lên màn hình
        }
    }

    private void loadCategoriesFromServer() {
        ApiClient.getApiService().getAllCategories().enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    categoryList = response.body();
                    setupSpinnerData();
                }
            }
            @Override public void onFailure(Call<List<Category>> call, Throwable t) {}
        });
    }

    private void setupSpinnerData() {
        spinnerItems.clear();
        for (Category cat : categoryList) {
            spinnerItems.add(cat.getName()); // Lấy tên thể loại truyện
        }
        spinnerItems.add("+ Thêm thể loại mới..."); // Dòng lựa chọn mở rộng cuối Dropdown
        spinnerAdapter.notifyDataSetChanged();

        if (!categoryList.isEmpty()) {
            selectedCategoryId = categoryList.get(0).getCategoryId();
        }

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == spinnerItems.size() - 1) {
                    // KÍCH HOẠT DIALOG THÊM THỂ LOẠI MỚI KHI CLICK DÒNG CUỐI
                    showAddNewCategoryDialog();
                } else {
                    selectedCategoryId = categoryList.get(position).getCategoryId();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void showAddNewCategoryDialog() {
        EditText edtNewCat = new EditText(this);
        edtNewCat.setHint("Nhập tên thể loại truyện mới");

        new AlertDialog.Builder(this)
                .setTitle("Thêm thể loại")
                .setView(edtNewCat)
                .setPositiveButton("Thêm", (dialog, which) -> {
                    String newCatName = edtNewCat.getText().toString().trim();
                    if (!newCatName.isEmpty()) {
                        Category newCat = new Category();
                        newCat.setName(newCatName);

                        ApiClient.getApiService().createCategory(newCat).enqueue(new Callback<Category>() {
                            @Override
                            public void onResponse(Call<Category> call, Response<Category> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    Toast.makeText(AdminEditComicActivity.this, "Đã thêm thể loại mới!", Toast.LENGTH_SHORT).show();
                                    loadCategoriesFromServer(); // Reload lại dropdown
                                }
                            }
                            @Override public void onFailure(Call<Category> call, Throwable t) {}
                        });
                    }
                })
                .setNegativeButton("Hủy", (dialog, which) -> spinnerCategory.setSelection(0))
                .show();
    }

    // Tiến hành tuần tự: Nếu có ảnh mới chọn -> Upload ảnh trước lấy link -> Gửi thông tin truyện sau
    private void executeSavingProcess() {
        String title = edtComicTitle.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(this, "Tên truyện không được để trống!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedImageUri != null) {
            // Có ảnh mới, cần upload lên server trước
            File file = getFileFromUri(selectedImageUri);
            if (file == null) return;

            RequestBody requestFile = RequestBody.create(MediaType.parse(getContentResolver().getType(selectedImageUri)), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

            ApiClient.getApiService().adminUploadCover(body).enqueue(new Callback<Map<String, String>>() {
                @Override
                public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        uploadedCoverUrl = response.body().get("coverUrl");
                        saveComicTextInfo(title); // Gửi text truyện lên
                    }
                }
                @Override
                public void onFailure(Call<Map<String, String>> call, Throwable t) {
                    Toast.makeText(AdminEditComicActivity.this, "Lỗi khi upload ảnh bìa!", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Không chọn ảnh mới (Giữ nguyên ảnh cũ hoặc để trống khi thêm mới)
            saveComicTextInfo(title);
        }
    }

    private void saveComicTextInfo(String title) {
        Comic comic = new Comic();
        comic.setTitle(title);
        comic.setAuthor(edtComicAuthor.getText().toString().trim());
        comic.setStatus(edtComicStatus.getText().toString().trim());
        comic.setDescription(edtComicDescription.getText().toString().trim());
        comic.setCoverImageUrl(uploadedCoverUrl);

        if (editComicId == -1) {
            ApiClient.getApiService().adminCreateComic(comic, selectedCategoryId).enqueue(new Callback<Comic>() {
                @Override
                public void onResponse(Call<Comic> call, Response<Comic> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(AdminEditComicActivity.this, "Thêm truyện thành công!", Toast.LENGTH_SHORT).show();
                        finish(); // Quay về an toàn
                    }
                }
                @Override public void onFailure(Call<Comic> call, Throwable t) {}
            });
        } else {
            ApiClient.getApiService().adminUpdateComic(editComicId, comic, selectedCategoryId).enqueue(new Callback<Comic>() {
                @Override
                public void onResponse(Call<Comic> call, Response<Comic> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(AdminEditComicActivity.this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }
                @Override public void onFailure(Call<Comic> call, Throwable t) {}
            });
        }
    }

    // Hàm đọc File tạm tương thích Scoped Storage của Pixel 7a
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