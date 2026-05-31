package com.yuhbui.comicapp.ui.admin;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.yuhbui.comicapp.R;
import com.yuhbui.comicapp.data.api.ApiClient;
import com.yuhbui.comicapp.data.model.Comic;
import com.yuhbui.comicapp.ui.adapters.AdminComicAdapter;

import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminManageComicsActivity extends AppCompatActivity implements AdminComicAdapter.OnComicActionListener {

    private RecyclerView rvAdminManageComics;
    private AdminComicAdapter adapter;
    private FloatingActionButton fabAddComic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_manage_comics);

        // 1. Khởi tạo Header tối giản đặc thù dành riêng cho Admin
        View layoutHeader = findViewById(R.id.layoutHeaderManageComics);
        ImageView headerMenu = layoutHeader.findViewById(R.id.headerMenu);
        TextView headerLogo = layoutHeader.findViewById(R.id.headerLogo);
        layoutHeader.findViewById(R.id.headerSearch).setVisibility(View.GONE);
        layoutHeader.findViewById(R.id.headerNotification).setVisibility(View.GONE);

        headerLogo.setText("QUẢN LÝ TRUYỆN TRANH");
        headerLogo.setTextColor(Color.parseColor("#E74C3C"));
        headerMenu.setOnClickListener(v -> finish());

        // 2. Cài đặt RecyclerView danh sách
        rvAdminManageComics = findViewById(R.id.rvAdminManageComics);
        rvAdminManageComics.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminComicAdapter(this);
        rvAdminManageComics.setAdapter(adapter);

        // 3. Sự kiện click nút Thêm truyện nổi
        fabAddComic = findViewById(R.id.fabAddComic);
        fabAddComic.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminEditComicActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAllComics();
    }

    private void loadAllComics() {
        ApiClient.getApiService().adminGetAllComics().enqueue(new Callback<List<Comic>>() {
            @Override
            public void onResponse(Call<List<Comic>> call, Response<List<Comic>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adapter.setData(response.body());
                }
            }
            @Override
            public void onFailure(Call<List<Comic>> call, Throwable t) {
                Toast.makeText(AdminManageComicsActivity.this, "Lỗi tải danh sách truyện Admin", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onEdit(Comic comic) {
        Intent intent = new Intent(this, AdminEditComicActivity.class);
        intent.putExtra("EDIT_COMIC_ID", comic.getComicId());
        intent.putExtra("TITLE", comic.getTitle());
        intent.putExtra("AUTHOR", comic.getAuthor());
        intent.putExtra("COVER_URL", comic.getCoverImageUrl());
        intent.putExtra("STATUS", comic.getStatus());
        intent.putExtra("DESC", comic.getDescription());
        startActivity(intent);
    }

    @Override
    public void onDelete(Comic comic, int position) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa vĩnh viễn bộ truyện '" + comic.getTitle() + "' không? Hành động này sẽ xóa hết các chapter liên quan!")
                .setPositiveButton("Xóa vĩnh viễn", (dialog, which) -> {
                    // ĐÃ SỬA: Chuyển đổi Callback sang okhttp3.ResponseBody
                    ApiClient.getApiService().adminDeleteComic(comic.getComicId()).enqueue(new Callback<okhttp3.ResponseBody>() {
                        @Override
                        public void onResponse(Call<okhttp3.ResponseBody> call, Response<okhttp3.ResponseBody> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(AdminManageComicsActivity.this, "Đã xóa truyện khỏi hệ thống!", Toast.LENGTH_SHORT).show();
                                loadAllComics(); // Làm mới danh sách hiển thị
                            } else {
                                Toast.makeText(AdminManageComicsActivity.this, "Xóa thất bại từ Server!", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {
                            Toast.makeText(AdminManageComicsActivity.this, "Lỗi kết nối mạng khi xóa", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}