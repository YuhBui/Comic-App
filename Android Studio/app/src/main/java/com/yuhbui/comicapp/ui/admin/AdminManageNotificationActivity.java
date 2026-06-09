package com.yuhbui.comicapp.ui.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.yuhbui.comicapp.R;
import com.yuhbui.comicapp.data.api.ApiClient;
import com.yuhbui.comicapp.data.model.Notification;
import com.yuhbui.comicapp.ui.adapters.AdminNotificationAdapter;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminManageNotificationActivity extends AppCompatActivity {

    private RecyclerView rvNotif;
    private Button btnAdd;
    private AdminNotificationAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_manage_notification);

        // Khởi chạy Header tích hợp nút Back ẩn bên trong
        setupHeader();

        rvNotif = findViewById(R.id.recyclerViewAdminNotifications);
        btnAdd = findViewById(R.id.btnAdminAddNotif);

        rvNotif.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminNotificationAdapter();
        rvNotif.setAdapter(adapter);

        btnAdd.setOnClickListener(v -> showAddNotificationDialog());

        loadAllNotifications();
    }

    private void setupHeader() {
        View headerView = findViewById(R.id.layoutHeaderAdmin);
        if (headerView != null) {
            ImageView headerMenu = headerView.findViewById(R.id.headerMenu);
            TextView headerLogo = headerView.findViewById(R.id.headerLogo);

            headerView.findViewById(R.id.headerAvatar).setVisibility(View.GONE);
            headerView.findViewById(R.id.headerSearch).setVisibility(View.GONE);
            headerView.findViewById(R.id.headerNotification).setVisibility(View.GONE);

            // Cấu hình thanh tiêu đề Admin động
            headerLogo.setText("QUẢN LÝ THÔNG BÁO");
            headerMenu.setImageResource(android.R.drawable.ic_menu_revert);
            headerMenu.setOnClickListener(v -> finish()); // Trở thành nút Back duy nhất của màn hình này
        }
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
                }
            }
            @Override public void onFailure(Call<List<Notification>> call, Throwable t) {}
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
                                Toast.makeText(AdminManageNotificationActivity.this, "Đã phát sóng thành công!", Toast.LENGTH_SHORT).show();
                                loadAllNotifications();
                            }
                            @Override public void onFailure(Call<Void> call, Throwable t) {}
                        });
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
                                Toast.makeText(AdminManageNotificationActivity.this, "Đã sửa thành công!", Toast.LENGTH_SHORT).show();
                                loadAllNotifications();
                            }
                            @Override public void onFailure(Call<Void> call, Throwable t) {}
                        });
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
                            Toast.makeText(AdminManageNotificationActivity.this, "Đã xóa!", Toast.LENGTH_SHORT).show();
                            loadAllNotifications();
                        }
                        @Override public void onFailure(Call<Void> call, Throwable t) {}
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}