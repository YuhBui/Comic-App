package com.yuhbui.comicapp.ui.admin;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest; // THÊM
import androidx.activity.result.contract.ActivityResultContracts; // THÊM
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.yuhbui.comicapp.R;
import com.yuhbui.comicapp.data.api.ApiClient;
import com.yuhbui.comicapp.utils.HeaderUtils;
import com.yuhbui.comicapp.utils.MenuUtils;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminAddChapterActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private int comicId;
    private EditText edtChapterNumber, edtChapterTitle;
    private TextView tvPagesCountLabel;
    private Button btnSelectImages, btnSave, btnCancel;
    private RecyclerView rvImagesPreview;

    private List<Uri> selectedImageUris = new ArrayList<>();
    private SelectedImagesAdapter previewAdapter;

    // ĐÃ SỬA: Nâng cấp lên Photo Picker chuyên dụng hỗ trợ đa chọn mượt mà
    private ActivityResultLauncher<PickVisualMediaRequest> pickMultipleMediaLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_add_chapter);

        comicId = getIntent().getIntExtra("COMIC_ID", -1);

        drawerLayout = findViewById(R.id.drawerLayout);
        edtChapterNumber = findViewById(R.id.edtAddChapterNumber);
        edtChapterTitle = findViewById(R.id.edtAddChapterTitle);
        tvPagesCountLabel = findViewById(R.id.tvPagesCountLabel);
        btnSelectImages = findViewById(R.id.btnSelectImages);
        btnSave = findViewById(R.id.btnSaveChapterWithImages);
        rvImagesPreview = findViewById(R.id.rvSelectedImagesPreview);

        View layoutHeader = findViewById(R.id.layoutHeaderAddChapter);
        HeaderUtils.initHeader(this, layoutHeader, drawerLayout);
        MenuUtils.setupAdminSideMenu(this, drawerLayout, layoutHeader.findViewById(R.id.headerMenu));

        if (layoutHeader.findViewById(R.id.headerSearch) != null) {
            layoutHeader.findViewById(R.id.headerSearch).setVisibility(View.GONE);
        }
        if (layoutHeader.findViewById(R.id.headerNotification) != null) {
            layoutHeader.findViewById(R.id.headerNotification).setVisibility(View.GONE);
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

        // CẤU HÌNH: Thiết lập hiển thị hàng dọc chuẩn như chi tiết ảnh đọc truyện
        rvImagesPreview.setLayoutManager(new LinearLayoutManager(this));
        rvImagesPreview.setNestedScrollingEnabled(false);
        previewAdapter = new SelectedImagesAdapter();
        rvImagesPreview.setAdapter(previewAdapter);

        // ĐÃ SỬA: Đăng ký nhận diện dữ liệu từ trình chọn ảnh hệ thống mới
        pickMultipleMediaLauncher = registerForActivityResult(
                new ActivityResultContracts.PickMultipleVisualMedia(100), // Cho phép chọn tối đa 100 ảnh cùng lúc
                uris -> {
                    if (uris != null && !uris.isEmpty()) {
                        selectedImageUris.addAll(uris); // Thêm nối tiếp các ảnh mới chọn vào danh sách
                        updateImagePreview();
                    }
                }
        );

        btnSelectImages.setOnClickListener(v -> {
            // Khởi chạy trình chọn ảnh đa nhiệm an toàn
            pickMultipleMediaLauncher.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        btnSave.setOnClickListener(v -> performUploadChapterWithImages());

        // THÊM ĐOẠN CODE NÀY VÀO TRONG HÀM onCreate():
        btnCancel = findViewById(R.id.btnCancelAddChapter);

        btnCancel.setOnClickListener(v -> {
            // Hiển thị Dialog xác nhận để tránh việc Admin vô tình chạm nhầm nút làm mất danh sách ảnh đã chọn
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Xác nhận hủy dữ liệu")
                    .setMessage("Bạn có chắc chắn muốn xóa toàn bộ thông tin chương và danh sách ảnh đã chọn không?")
                    .setPositiveButton("Xóa hết", (dialog, which) -> {
                        // 1. Xóa sạch chữ trong 2 ô nhập liệu
                        edtChapterNumber.setText("");
                        edtChapterTitle.setText("");

                        // 2. Làm rỗng mảng lưu Uri ảnh và cập nhật lại giao diện hiển thị dọc công khai
                        selectedImageUris.clear();
                        updateImagePreview();
                        checkFormValidity();

                        Toast.makeText(this, "Đã xóa toàn bộ dữ liệu đang nhập thành công!", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Giữ lại", null)
                    .show();
        });

        // THÊM ĐOẠN NÀY VÀO TRONG HÀM onCreate() CỦA AdminAddChapterActivity
        btnSave.setEnabled(false);
        btnSave.setAlpha(0.5f); // Mờ nút Lưu lúc ban đầu

        edtChapterNumber.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkFormValidity();
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void updateImagePreview() {
        previewAdapter.notifyDataSetChanged();
        tvPagesCountLabel.setText("Danh sách trang truyện (" + selectedImageUris.size() + " trang):");
        checkFormValidity();
    }

    private void performUploadChapterWithImages() {
        String numStr = edtChapterNumber.getText().toString().trim();
        String titleStr = edtChapterTitle.getText().toString().trim();

        if (numStr.isEmpty()) {
            Toast.makeText(this, "Số thứ tự chương không được bỏ trống!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedImageUris.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ít nhất 1 trang truyện ảnh!", Toast.LENGTH_SHORT).show();
            return;
        }

        RequestBody chapterNumberBody = RequestBody.create(MediaType.parse("text/plain"), numStr);
        RequestBody requestBodyTitle = RequestBody.create(MediaType.parse("text/plain"), titleStr);

        List<MultipartBody.Part> multipartFilesList = new ArrayList<>();
        for (int i = 0; i < selectedImageUris.size(); i++) {
            try {
                Uri imageUri = selectedImageUris.get(i);
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                byte[] bytes = getBytes(inputStream);

                RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), bytes);
                MultipartBody.Part bodyPart = MultipartBody.Part.createFormData("files", "page_" + i + ".jpg", requestFile);
                multipartFilesList.add(bodyPart);
            } catch (Exception e) {
                Toast.makeText(this, "Lỗi đọc tệp ảnh thứ " + (i + 1), Toast.LENGTH_SHORT).show();
                return;
            }
        }

        btnSave.setEnabled(false);
        btnSave.setText("⏳ ĐANG TẢI LÊN... VUI LÒNG CHỜ");

        ApiClient.getApiService().adminCreateChapterWithImages(comicId, chapterNumberBody, requestBodyTitle, multipartFilesList)
                .enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(AdminAddChapterActivity.this, "Đã tạo chương mới thành công!", Toast.LENGTH_LONG).show();
                            finish();
                        } else {
                            btnSave.setEnabled(true);
                            btnSave.setText("💾 LƯU CHƯƠNG VÀ TẢI LÊN ẢNH");
                            Toast.makeText(AdminAddChapterActivity.this, "Server báo lỗi, không thể lưu dữ liệu!", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                        btnSave.setEnabled(true);
                        btnSave.setText("💾 LƯU CHƯƠNG VÀ TẢI LÊN ẢNH");
                        Toast.makeText(AdminAddChapterActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private byte[] getBytes(InputStream inputStream) throws Exception {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    // === ADAPTER HIỂN THỊ ẢNH XẾP DỌC THEO KHUNG HÌNH THỰC TẾ ===
    private class SelectedImagesAdapter extends RecyclerView.Adapter<SelectedImagesAdapter.PreviewViewHolder> {
        @NonNull
        @Override
        public PreviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Giữ nguyên liên kết layout item gốc của bạn để đảm bảo không làm thay đổi thiết kế
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_chapter_image, parent, false);
            return new PreviewViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull PreviewViewHolder holder, int position) {
            Uri uri = selectedImageUris.get(position);

            Glide.with(AdminAddChapterActivity.this)
                    .load(uri)
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(holder.imgPage);

            holder.tvPageNum.setText("Trang " + (position + 1));

            holder.btnDelete.setOnClickListener(v -> {
                int currentPos = holder.getAdapterPosition();
                if (currentPos != RecyclerView.NO_POSITION) {
                    selectedImageUris.remove(currentPos);
                    updateImagePreview();
                }
            });
        }

        @Override
        public int getItemCount() {
            return selectedImageUris.size();
        }

        class PreviewViewHolder extends RecyclerView.ViewHolder {
            ImageView imgPage;
            View btnDelete;
            TextView tvPageNum;

            public PreviewViewHolder(@NonNull View itemView) {
                super(itemView);
                imgPage = itemView.findViewById(R.id.imgAdminPagePreview);
                tvPageNum = itemView.findViewById(R.id.tvAdminPageNumber);
                btnDelete = itemView.findViewById(R.id.btnAdminDeletePage);
            }
        }
    }

    // THÊM VÀO TRONG CLASS ADMINADDCHAPTERACTIVITY
    private void checkFormValidity() {
        String num = edtChapterNumber.getText().toString().trim();
        boolean isValid = !num.isEmpty() && !selectedImageUris.isEmpty();
        btnSave.setEnabled(isValid);
        btnSave.setAlpha(isValid ? 1.0f : 0.5f);
    }
}