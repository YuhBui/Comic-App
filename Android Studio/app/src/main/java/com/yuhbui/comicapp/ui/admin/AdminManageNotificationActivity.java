package com.yuhbui.comicapp.ui.admin;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log; // THÊM: Để kiểm soát log lỗi mạng hệ thống
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog; // SỬA: Dùng AlertDialog của androidx để đồng bộ UI
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.yuhbui.comicapp.R;
import com.yuhbui.comicapp.data.api.ApiClient;
import com.yuhbui.comicapp.data.model.Notification;
import com.yuhbui.comicapp.ui.adapters.AdminNotificationAdapter;
import com.yuhbui.comicapp.utils.HeaderUtils;
import com.yuhbui.comicapp.utils.MenuUtils;
import com.yuhbui.comicapp.utils.SharedPrefsManager;

import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminManageNotificationActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;

    private RecyclerView rvNotif;
    private Button btnAdd;
    private AdminNotificationAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_manage_notification);

        // 1. Ánh xạ DrawerLayout đè nền từ file XML
        drawerLayout = findViewById(R.id.drawerLayout);

        // 2. Cấu hình thanh Header Admin chuyên dụng & Menu trượt trái tập trung
        setupHeader();

        rvNotif = findViewById(R.id.recyclerViewAdminNotifications);
        btnAdd = findViewById(R.id.btnAdminAddNotif);

        rvNotif.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminNotificationAdapter();
        rvNotif.setAdapter(adapter);

        btnAdd.setOnClickListener(v -> showAddNotificationDialog());

        // 3. Quản lý nút Quay lại (Back cứng) của máy - Ưu tiên đóng Menu trượt nếu đang mở
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

        loadAllNotifications();
    }

    // Làm mới avatar Admin mỗi khi quay lại từ màn hình quản trị khác hoặc trang cá nhân
    @Override
    protected void onResume() {
        super.onResume();
        View headerView = findViewById(R.id.layoutHeaderAdmin);
        if (headerView != null && headerView.findViewById(R.id.headerAvatar) != null) {
            HeaderUtils.loadHeaderAvatar(this, headerView.findViewById(R.id.headerAvatar));
        }
    }

    /**
     * Cấu hình Header Admin ẩn tìm kiếm/thông báo, đồng bộ tên app cố định
     */
    private void setupHeader() {
        View headerView = findViewById(R.id.layoutHeaderAdmin);
        if (headerView != null) {
            ImageView headerMenu = headerView.findViewById(R.id.headerMenu);
            TextView headerLogo = headerView.findViewById(R.id.headerLogo);
            ImageView headerAvatar = headerView.findViewById(R.id.headerAvatar);

            // Khởi tạo các tính năng cốt lõi của Header
            HeaderUtils.initHeader(this, headerView, drawerLayout);

            // Đăng ký điều hướng tập trung cho các mục quản lý trong Menu trượt Admin
            MenuUtils.setupAdminSideMenu(this, drawerLayout, headerMenu);

            // YÊU CẦU: Ẩn triệt để hai nút Tìm kiếm và Thông báo đối với không gian Admin
            if (headerView.findViewById(R.id.headerSearch) != null) {
                headerView.findViewById(R.id.headerSearch).setVisibility(View.GONE);
            }
            if (headerView.findViewById(R.id.headerNotification) != null) {
                headerView.findViewById(R.id.headerNotification).setVisibility(View.GONE);
            }

            // Đồng bộ: Giữ hiển thị ảnh đại diện và mở popup cài đặt/đăng xuất
            if (headerAvatar != null) {
                headerAvatar.setVisibility(View.VISIBLE); // Hiện ảnh đại diện lên
                headerAvatar.setOnClickListener(v -> showAvatarPopupMenu(v));
            }

            // ĐỒNG BỘ: Hiện tên App và bắt sự kiện click quay về thẳng giao diện Dashboard Admin
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
    }

    private void showAvatarPopupMenu(View anchorView) {
        androidx.appcompat.widget.PopupMenu popupMenu = new androidx.appcompat.widget.PopupMenu(this, anchorView);
        popupMenu.getMenu().add(0, 1, 1, "👤 Hồ sơ cá nhân");
        popupMenu.getMenu().add(0, 2, 2, "🚪 Đăng xuất hệ thống");

        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == 1) {
                startActivity(new Intent(this, com.yuhbui.comicapp.ui.ProfileActivity.class));
                return true;
            } else if (id == 2) {
                SharedPrefsManager.logout(this);
                Intent intent = new Intent(this, com.yuhbui.comicapp.ui.LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                return true;
            }
            return false;
        });
        popupMenu.show();
    }

    private void loadAllNotifications() {
        ApiClient.getApiService().getAllNotificationsForAdmin().enqueue(new Callback<List<Notification>>() {
            @Override
            public void onResponse(Call<List<Notification>> call, Response<List<Notification>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adapter.setData(response.body(), new AdminNotificationAdapter.OnAdminNotifActionListener() {
                        @Override
                        public void onEdit(Notification notification) {
                            showEditNotificationDialog(notification);
                        }

                        @Override
                        public void onDelete(Notification notification, int position) {
                            showDeleteConfirmationDialog(notification, position);
                        }
                    });
                } else {
                    Toast.makeText(AdminManageNotificationActivity.this, "Không thể lấy danh sách thông báo!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Notification>> call, Throwable t) {
                Log.e("API_ERROR", "Lỗi tải thông báo: " + t.getMessage());
                Toast.makeText(AdminManageNotificationActivity.this, "Lỗi kết nối server!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddNotificationDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(44, 24, 44, 24);

        final EditText edtTitle = new EditText(this);
        edtTitle.setHint("Nhập tiêu đề phát sóng...");
        layout.addView(edtTitle);

        final EditText edtMsg = new EditText(this);
        edtMsg.setHint("Nhập nội dung thông báo gửi toàn app...");
        layout.addView(edtMsg);

        new AlertDialog.Builder(this)
                .setTitle("Tạo thông báo hệ thống mới")
                .setView(layout)
                .setPositiveButton("Phát sóng", (dialog, which) -> {
                    String title = edtTitle.getText().toString().trim();
                    String message = edtMsg.getText().toString().trim();
                    if (!title.isEmpty() && !message.isEmpty()) {
                        Notification n = new Notification();
                        n.setTitle(title);
                        n.setMessage(message);
                        ApiClient.getApiService().adminCreateNotification(n).enqueue(new Callback<Void>() {
                            @Override
                            public void onResponse(Call<Void> call, Response<Void> response) {
                                if (response.isSuccessful()) {
                                    Toast.makeText(AdminManageNotificationActivity.this, "Đã phát sóng thành công!", Toast.LENGTH_SHORT).show();
                                    loadAllNotifications();
                                } else {
                                    Toast.makeText(AdminManageNotificationActivity.this, "Server từ chối tạo thông báo!", Toast.LENGTH_SHORT).show();
                                }
                            }
                            @Override
                            public void onFailure(Call<Void> call, Throwable t) {
                                Toast.makeText(AdminManageNotificationActivity.this, "Lỗi mạng, phát sóng thất bại!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        Toast.makeText(this, "Vui lòng không để trống thông tin!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showEditNotificationDialog(Notification notification) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(44, 24, 44, 24);

        final EditText edtTitle = new EditText(this);
        edtTitle.setText(notification.getTitle());
        layout.addView(edtTitle);

        final EditText edtMsg = new EditText(this);
        edtMsg.setText(notification.getMessage());
        layout.addView(edtMsg);

        new AlertDialog.Builder(this)
                .setTitle("Sửa đổi thông báo")
                .setView(layout)
                .setPositiveButton("Cập nhật", (dialog, which) -> {
                    String title = edtTitle.getText().toString().trim();
                    String message = edtMsg.getText().toString().trim();
                    if (!title.isEmpty() && !message.isEmpty()) {
                        notification.setTitle(title);
                        notification.setMessage(message);
                        ApiClient.getApiService().adminUpdateNotification(notification.getNotificationId(), notification).enqueue(new Callback<Void>() {
                            @Override
                            public void onResponse(Call<Void> call, Response<Void> response) {
                                if (response.isSuccessful()) {
                                    Toast.makeText(AdminManageNotificationActivity.this, "Đã sửa thành công!", Toast.LENGTH_SHORT).show();
                                    loadAllNotifications();
                                } else {
                                    Toast.makeText(AdminManageNotificationActivity.this, "Cập nhật thất bại!", Toast.LENGTH_SHORT).show();
                                }
                            }
                            @Override
                            public void onFailure(Call<Void> call, Throwable t) {
                                Toast.makeText(AdminManageNotificationActivity.this, "Lỗi kết nối khi sửa!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        Toast.makeText(this, "Nội dung sửa không được để trống!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showDeleteConfirmationDialog(Notification notification, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc muốn xóa vĩnh viễn tin thông báo này không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    ApiClient.getApiService().adminDeleteNotification(notification.getNotificationId()).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(AdminManageNotificationActivity.this, "Đã xóa!", Toast.LENGTH_SHORT).show();
                                loadAllNotifications();
                            } else {
                                Toast.makeText(AdminManageNotificationActivity.this, "Xóa thất bại!", Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            Toast.makeText(AdminManageNotificationActivity.this, "Lỗi kết nối khi xóa!", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}