package com.yuhbui.comicapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.yuhbui.comicapp.R;
import com.yuhbui.comicapp.ui.adapters.NotificationAdapter;
import com.yuhbui.comicapp.data.api.ApiClient;
import com.yuhbui.comicapp.data.model.Notification;
import com.yuhbui.comicapp.utils.HeaderUtils;
import com.yuhbui.comicapp.utils.MenuUtils;
import com.yuhbui.comicapp.utils.SharedPrefsManager;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationListActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;

    private RecyclerView rvNotif;
    private TextView tvNoData;
    private NotificationAdapter adapter;
    private int userId;

    // Các thành phần của Header
    private View layoutHeader;
    private TextView headerLogo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_list);

        // Ánh xạ DrawerLayout từ XML
        drawerLayout = findViewById(R.id.drawerLayout);

        // CẤU HÌNH HEADER VÀ MENU TRƯỢT ĐỒNG BỘ
        layoutHeader = findViewById(R.id.layoutHeader);
        headerLogo = findViewById(R.id.headerLogo); // Ánh xạ trực tiếp từ layout để đảm bảo an toàn

        // Kích hoạt toàn bộ tính năng Header (gồm cả Tìm kiếm chuyển hướng toàn cục) và Menu trượt trái
        HeaderUtils.initHeader(this, layoutHeader, drawerLayout);
        MenuUtils.setupSideMenu(this, drawerLayout, findViewById(R.id.headerMenu));

        // CHỈNH SỬA TẠI ĐÂY: Giữ lại tiêu đề ứng dụng gốc và thiết lập click chuyển hướng về Trang chủ
        if (headerLogo != null) {
            headerLogo.setText("COMIC APP"); // Đặt lại tên app ban đầu
            headerLogo.setOnClickListener(v -> {
                Intent intent = new Intent(NotificationListActivity.this, MainActivity.class);
                // Dùng cờ Clear Top và Single Top để mở lại trang chủ cũ mượt mà, không tạo thêm nhiều trang mới chồng lên nhau
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish(); // Đóng màn hình thông báo lại
            });
        }

        // Cấu hình sự kiện nút Back hệ thống (Ưu tiên đóng menu trượt)
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
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

        // Giao diện hiển thị danh sách
        rvNotif = findViewById(R.id.recyclerViewNotifications);
        tvNoData = findViewById(R.id.tvNoNotifications);
        rvNotif.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter();
        rvNotif.setAdapter(adapter);

        userId = SharedPrefsManager.getUserId(this);
        loadNotifications();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (layoutHeader != null) {
            HeaderUtils.loadHeaderAvatar(this, layoutHeader.findViewById(R.id.headerAvatar));
            HeaderUtils.loadUnreadNotificationCount(this, layoutHeader.findViewById(R.id.tvNotificationBadge));
        }
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
                        if (!notif.isRead()) {
                            ApiClient.getApiService().markNotificationAsRead(notif.getNotificationId()).enqueue(new Callback<Void>() {
                                @Override public void onResponse(Call<Void> call, Response<Void> response) {
                                    loadNotifications();
                                }
                                @Override public void onFailure(Call<Void> call, Throwable t) {}
                            });
                        }

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
}