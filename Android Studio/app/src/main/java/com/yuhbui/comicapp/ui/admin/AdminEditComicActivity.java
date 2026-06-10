package com.yuhbui.comicapp.ui.admin;

import android.content.Intent;
import android.graphics.Color; // THÊM: Để đổi màu chữ Header
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.activity.OnBackPressedCallback; // THÊM: Để bắt sự kiện nút Back hệ thống
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;              // THÊM: Điều hướng DrawerLayout trượt trái
import androidx.drawerlayout.widget.DrawerLayout;    // THÊM: Biến DrawerLayout root

import com.bumptech.glide.Glide;
import com.yuhbui.comicapp.R;
import com.yuhbui.comicapp.data.api.ApiClient;
import com.yuhbui.comicapp.data.model.Category;
import com.yuhbui.comicapp.data.model.Comic;
import com.yuhbui.comicapp.utils.HeaderUtils;          // THÊM: Tiện ích Header dùng chung
import com.yuhbui.comicapp.utils.MenuUtils;            // THÊM: Tiện ích Menu Admin dùng chung

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

    private DrawerLayout drawerLayout; // THÊM: Thành phần quản lý Menu trượt trái đè màn hình

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

    // Các thành phần của Header
    private View layoutHeader;
    private TextView headerLogo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_edit_comic);

        // 1. Ánh xạ DrawerLayout Root mới
        drawerLayout = findViewById(R.id.drawerLayout);

        // 2. Thiết lập cấu hình tùy biến thanh Header Admin & Menu trượt tập trung
        setupAdminHeaderView();

        // 3. Ánh xạ các View nhập liệu mẫu form từ XML
        tvFormTitle = findViewById(R.id.tvFormTitle);
        edtComicTitle = findViewById(R.id.edtComicTitle);
        edtComicAuthor = findViewById(R.id.edtComicAuthor);
        edtComicDescription = findViewById(R.id.edtComicDescription);
        imgComicCoverSelect = findViewById(R.id.imgComicCoverSelect);
        btnSelectCover = findViewById(R.id.btnSelectCover);
        btnCancelComicForm = findViewById(R.id.btnCancelComicForm);
        btnSaveComicForm = findViewById(R.id.btnSaveComicForm);
        tvComicCategoriesSelect = findViewById(R.id.tvComicCategoriesSelect);
        rgStatus = findViewById(R.id.rgStatus);

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

        // 4. CẤU HÌNH: Khóa nút quay lại (Back cứng) - Ưu tiên đóng Menu trượt nếu đang mở
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
    }

    // Làm mới avatar Admin mỗi khi quay lại từ các trang quản lý khác hoặc trang cá nhân
    @Override
    protected void onResume() {
        super.onResume();
        if (layoutHeader != null && layoutHeader.findViewById(R.id.headerAvatar) != null) {
            HeaderUtils.loadHeaderAvatar(this, layoutHeader.findViewById(R.id.headerAvatar));
        }
    }

    /**
     * Hàm cấu hình thanh Header Admin chuyên sâu đặc thù, ẩn các nút chức năng dư thừa
     */
    private void setupAdminHeaderView() {
        layoutHeader = findViewById(R.id.layoutHeaderEditComic); // Đảm bảo ID này trùng với ID include trong file XML
        headerLogo = layoutHeader.findViewById(R.id.headerLogo);

        // Khởi tạo các tính năng lõi của Header
        HeaderUtils.initHeader(this, layoutHeader, drawerLayout);

        // Kích hoạt tính năng kéo/mở menu điều hướng Admin tập trung
        MenuUtils.setupAdminSideMenu(this, drawerLayout, layoutHeader.findViewById(R.id.headerMenu));

        // ĐÁP ỨNG YÊU CẦU: Ẩn triệt để hai nút không thuộc phận sự của Admin
        if (layoutHeader.findViewById(R.id.headerSearch) != null) {
            layoutHeader.findViewById(R.id.headerSearch).setVisibility(View.GONE);
        }
        if (layoutHeader.findViewById(R.id.headerNotification) != null) {
            layoutHeader.findViewById(R.id.headerNotification).setVisibility(View.GONE);
        }

        // Đồng bộ phong cách chữ tiêu đề đỏ cam nhận diện không gian làm việc của Admin
        if (headerLogo != null) {
            headerLogo.setText("COMIC APP");
            headerLogo.setTextColor(Color.parseColor("#E74C3C"));
            headerLogo.setOnClickListener(v -> {
                Intent intent = new Intent(this, AdminDashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            imgComicCoverSelect.setImageURI(selectedImageUri);
        }
    }

    private void loadCategoriesFromServer() {
        String currentGenresString = getIntent().getStringExtra("CURRENT_GENRES_STRING");

        ApiClient.getApiService().getAllCategories().enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allCategories = response.body();
                    checkedItems = new boolean[allCategories.size()];
                    selectedCategoryIds.clear();

                    StringBuilder displayText = new StringBuilder();

                    for (int i = 0; i < allCategories.size(); i++) {
                        Category category = allCategories.get(i);
                        if (currentGenresString != null && currentGenresString.contains(category.getName())) {
                            checkedItems[i] = true;
                            selectedCategoryIds.add(category.getCategoryId());

                            if (displayText.length() > 0) displayText.append(", ");
                            displayText.append(category.getName());
                        }
                    }

                    if (selectedCategoryIds.isEmpty()) {
                        tvComicCategoriesSelect.setText("Bấm vào đây để chọn thể loại...");
                    } else {
                        tvComicCategoriesSelect.setText(displayText.toString());
                    }
                }
            }
            @Override public void onFailure(Call<List<Category>> call, Throwable t) {}
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
                        Category c = new Category(name);
                        ApiClient.getApiService().createCategory(c).enqueue(new Callback<Category>() {
                            @Override
                            public void onResponse(Call<Category> call, Response<Category> response) {
                                if (response.isSuccessful()) {
                                    Toast.makeText(AdminEditComicActivity.this, "Đã tạo thể loại mới thành công!", Toast.LENGTH_SHORT).show();
                                    loadCategoriesFromServer();
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
                        saveComicTextInfo(title);
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
            saveComicTextInfo(title);
        }
    }

    private void saveComicTextInfo(String title) {
        Comic comic = new Comic();
        comic.setTitle(title);
        comic.setAuthor(edtComicAuthor.getText().toString().trim());
        comic.setDescription(edtComicDescription.getText().toString().trim());
        comic.setCoverImageUrl(uploadedCoverUrl);

        String status = "Ongoing";
        if (rgStatus.getCheckedRadioButtonId() == R.id.rbCompleted) {
            status = "Completed";
        }
        comic.setStatus(status);

        if (editComicId == -1) {
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