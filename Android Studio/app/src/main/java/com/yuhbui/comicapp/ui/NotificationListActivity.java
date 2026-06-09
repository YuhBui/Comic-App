package com.yuhbui.comicapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.yuhbui.comicapp.R;
import com.yuhbui.comicapp.ui.adapters.NotificationAdapter;
import com.yuhbui.comicapp.data.api.ApiClient;
import com.yuhbui.comicapp.data.model.Notification;
import com.yuhbui.comicapp.utils.SharedPrefsManager;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationListActivity extends AppCompatActivity {

    private RecyclerView rvNotif;
    private TextView tvNoData;
    private NotificationAdapter adapter;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_list);

        setupHeader();

        rvNotif = findViewById(R.id.recyclerViewNotifications);
        tvNoData = findViewById(R.id.tvNoNotifications);
        rvNotif.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter();
        rvNotif.setAdapter(adapter);

        userId = SharedPrefsManager.getUserId(this);
        loadNotifications();
    }

    private void loadNotifications() {
        if (userId == -1) return;

        ApiClient.getApiService().getUserNotifications(userId).enqueue(new Callback<List<Notification>>() {
            @Override
            public void onResponse(Call<List<Notification>> call, Response<List<Notification>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    tvNoData.setVisibility(View.GONE);
                    rvNotif.setVisibility(View.VISIBLE);

                    adapter.setData(response.body(), notif -> {
                        // Xử lý khi click vào dòng thông báo
                        if (!notif.isRead()) {
                            ApiClient.getApiService().markNotificationAsRead(notif.getNotificationId()).enqueue(new Callback<Void>() {
                                @Override public void onResponse(Call<Void> call, Response<Void> response) {
                                    loadNotifications(); // Reload làm tươi lại giao diện mất chấm đỏ
                                }
                                @Override public void onFailure(Call<Void> call, Throwable t) {}
                            });
                        }

                        // Nếu thông báo có liên kết truyện (Ví dụ: truyện ra mắt chap mới), chuyển hướng nhanh đến truyện đó
                        if (notif.getComicId() != null && notif.getComicId() > 0) {
                            Intent intent = new Intent(NotificationListActivity.this, ComicDetailActivity.class);
                            intent.putExtra("COMIC_ID", notif.getComicId());
                            startActivity(intent);
                        }
                    });
                } else {
                    tvNoData.setVisibility(View.VISIBLE);
                    rvNotif.setVisibility(View.GONE);
                }
            }
            @Override public void onFailure(Call<List<Notification>> call, Throwable t) {}
        });
    }

    private void setupHeader() {
        View headerView = findViewById(R.id.layoutHeaderAdmin); // ID layout bọc của file layout_header.xml
        if (headerView != null) {
            ImageView headerMenu = headerView.findViewById(R.id.headerMenu);
            TextView headerLogo = headerView.findViewById(R.id.headerLogo);

            // Ẩn các tính năng Tìm kiếm, Avatar, Quả chuông không dùng tới ở trang con này đi
            headerView.findViewById(R.id.headerAvatar).setVisibility(View.GONE);
            headerView.findViewById(R.id.headerSearch).setVisibility(View.GONE);
            headerView.findViewById(R.id.headerNotification).setVisibility(View.GONE);

            // Đổi tiêu đề trung tâm
            headerLogo.setText("THÔNG BÁO CỦA TÔI");

            // Ép icon Menu bên trái đổi vai trò biến thành nút Quay lại (Back Button)
            headerMenu.setImageResource(android.R.drawable.ic_menu_revert); // Sử dụng icon quay lại mặc định hệ thống
            headerMenu.setOnClickListener(v -> finish()); // Nhấn vào sẽ thoát giao diện hiện tại
        }
    }
}