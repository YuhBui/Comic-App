package com.yuhbui.comicapp.ui.admin;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView; // THÊM THƯ VIỆN
import android.widget.ArrayAdapter;
import android.widget.Button; // THÊM THƯ VIỆN
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.yuhbui.comicapp.R;
import com.yuhbui.comicapp.data.api.ApiClient;
import com.yuhbui.comicapp.data.model.Comic;
import com.yuhbui.comicapp.data.model.Notification;
import com.yuhbui.comicapp.ui.adapters.AdminNotificationAdapter;
import com.yuhbui.comicapp.utils.HeaderUtils;
import com.yuhbui.comicapp.utils.MenuUtils;
import com.yuhbui.comicapp.utils.SharedPrefsManager;

import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminManageNotificationActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private RecyclerView rvNotif;
    private CardView btnAdd;
    private EditText edtSearchNotif;
    private AdminNotificationAdapter adapter;

    private List<Comic> systemComicsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_manage_notification);

        drawerLayout = findViewById(R.id.drawerLayout);
        setupHeader();

        rvNotif = findViewById(R.id.recyclerViewAdminNotifications);
        btnAdd = findViewById(R.id.btnAdminAddNotif);
        edtSearchNotif = findViewById(R.id.edtSearchNotif);

        rvNotif.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminNotificationAdapter();
        rvNotif.setAdapter(adapter);

        btnAdd.setOnClickListener(v -> showAddNotificationDialog());

        edtSearchNotif.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadAllNotifications(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

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

        loadAllNotifications("");
        loadSystemComics();
    }

    @Override
    protected void onResume() {
        super.onResume();
        View headerView = findViewById(R.id.layoutHeaderAdmin);
        if (headerView != null && headerView.findViewById(R.id.headerAvatar) != null) {
            HeaderUtils.loadHeaderAvatar(this, headerView.findViewById(R.id.headerAvatar));
        }
    }

    private void setupHeader() {
        View headerView = findViewById(R.id.layoutHeaderAdmin);
        if (headerView != null) {
            ImageView headerMenu = headerView.findViewById(R.id.headerMenu);
            TextView headerLogo = headerView.findViewById(R.id.headerLogo);
            ImageView headerAvatar = headerView.findViewById(R.id.headerAvatar);

            HeaderUtils.initHeader(this, headerView, drawerLayout);
            MenuUtils.setupAdminSideMenu(this, drawerLayout, headerMenu);

            if (headerView.findViewById(R.id.headerSearch) != null) {
                headerView.findViewById(R.id.headerSearch).setVisibility(View.GONE);
            }
            if (headerView.findViewById(R.id.headerNotification) != null) {
                headerView.findViewById(R.id.headerNotification).setVisibility(View.GONE);
            }

            if (headerAvatar != null) {
                headerAvatar.setVisibility(View.VISIBLE);
                headerAvatar.setOnClickListener(v -> showAvatarPopupMenu(v));
            }

            if (headerLogo != null) {
                headerLogo.setText(android.text.Html.fromHtml("<font color='#D97707'>h</font><font color='#FFFFFF'>ay</font><font color='#D97707'>c</font><font color='#FFFFFF'>omic</font>", android.text.Html.FROM_HTML_MODE_COMPACT));
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

    private void loadSystemComics() {
        ApiClient.getApiService().getAllComics().enqueue(new Callback<List<Comic>>() {
            @Override
            public void onResponse(Call<List<Comic>> call, Response<List<Comic>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    systemComicsList = response.body();
                    if (adapter != null) {
                        adapter.setComicList(systemComicsList);
                    }
                }
            }
            @Override
            public void onFailure(Call<List<Comic>> call, Throwable t) {
                Log.e("API_ERROR", "Không thể lấy danh sách truyện.");
            }
        });
    }

    private void loadAllNotifications(String keyword) {
        ApiClient.getApiService().getAllNotificationsForAdmin(keyword).enqueue(new Callback<List<Notification>>() {
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
        layout.setPadding(50, 30, 50, 30);

        final EditText edtTitle = new EditText(this);
        edtTitle.setHint("Nhập tiêu đề thông báo...");
        layout.addView(edtTitle);

        final EditText edtMsg = new EditText(this);
        edtMsg.setHint("Nhập nội dung thông báo...");
        layout.addView(edtMsg);

        TextView tvGroupLabel = new TextView(this);
        tvGroupLabel.setText("\nChọn nhóm người nhận:");
        tvGroupLabel.setTextSize(14);
        layout.addView(tvGroupLabel);

        RadioGroup radioGroup = new RadioGroup(this);
        radioGroup.setOrientation(RadioGroup.HORIZONTAL);

        RadioButton rbAll = new RadioButton(this);
        rbAll.setText("Tất cả ");
        rbAll.setId(View.generateViewId());
        rbAll.setChecked(true);

        RadioButton rbComic = new RadioButton(this);
        rbComic.setText("Người theo dõi truyện");
        rbComic.setId(View.generateViewId());

        radioGroup.addView(rbAll);
        radioGroup.addView(rbComic);
        layout.addView(radioGroup);

        Spinner spinnerComics = new Spinner(this);
        List<String> comicTitles = new ArrayList<>();
        for (Comic c : systemComicsList) {
            comicTitles.add(c.getTitle());
        }
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, comicTitles);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerComics.setAdapter(spinnerAdapter);
        spinnerComics.setVisibility(View.GONE);
        layout.addView(spinnerComics);

        // ĐÃ SỬA: Chuyển sang .create() để can thiệp vòng đời cấu hình đóng/mở khóa nút Lưu
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Gửi thông báo mới")
                .setView(layout)
                .setPositiveButton("Gửi", (dialogInterface, which) -> {
                    String title = edtTitle.getText().toString().trim();
                    String message = edtMsg.getText().toString().trim();

                    if (!title.isEmpty() && !message.isEmpty()) {
                        Notification n = new Notification();
                        n.setTitle(title);
                        n.setMessage(message);

                        if (radioGroup.getCheckedRadioButtonId() == rbComic.getId()) {
                            int selectedPosition = spinnerComics.getSelectedItemPosition();
                            if (selectedPosition >= 0 && selectedPosition < systemComicsList.size()) {
                                n.setComicId(systemComicsList.get(selectedPosition).getComicId());
                            }
                        } else {
                            n.setComicId(null);
                        }

                        ApiClient.getApiService().adminCreateNotification(n).enqueue(new Callback<Void>() {
                            @Override
                            public void onResponse(Call<Void> call, Response<Void> response) {
                                if (response.isSuccessful()) {
                                    Toast.makeText(AdminManageNotificationActivity.this, "Đã gửi thông báo thành công!", Toast.LENGTH_SHORT).show();
                                    loadAllNotifications(edtSearchNotif.getText().toString());
                                } else {
                                    Toast.makeText(AdminManageNotificationActivity.this, "Gửi thất bại!", Toast.LENGTH_SHORT).show();
                                }
                            }
                            @Override
                            public void onFailure(Call<Void> call, Throwable t) {
                                Toast.makeText(AdminManageNotificationActivity.this, "Lỗi mạng!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                })
                .setNegativeButton("Hủy", null)
                .create();

        // Ẩn hiện Spinner chọn truyện
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == rbComic.getId()) {
                spinnerComics.setVisibility(View.VISIBLE);
            } else {
                spinnerComics.setVisibility(View.GONE);
            }
        });

        // ĐÃ THÊM: Bộ kiểm toán form liên tục reactive bắt buộc điền thông tin mới cho bấm Lưu
        dialog.setOnShowListener(dialogInterface -> {
            Button btnPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (btnPositive != null) {
                btnPositive.setEnabled(false);
                btnPositive.setAlpha(0.5f);

                TextWatcher watcher = new TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                        String title = edtTitle.getText().toString().trim();
                        String message = edtMsg.getText().toString().trim();
                        boolean isValid = !title.isEmpty() && !message.isEmpty();
                        btnPositive.setEnabled(isValid);
                        btnPositive.setAlpha(isValid ? 1.0f : 0.5f);
                    }
                    @Override public void afterTextChanged(Editable s) {}
                };
                edtTitle.addTextChangedListener(watcher);
                edtMsg.addTextChangedListener(watcher);
            }
        });

        dialog.show();
    }

    private void showEditNotificationDialog(Notification notification) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 30);

        final EditText edtTitle = new EditText(this);
        edtTitle.setText(notification.getTitle());
        layout.addView(edtTitle);

        final EditText edtMsg = new EditText(this);
        edtMsg.setText(notification.getMessage());
        layout.addView(edtMsg);

        TextView tvGroupLabel = new TextView(this);
        tvGroupLabel.setText("\nChọn nhóm người nhận:");
        tvGroupLabel.setTextSize(14);
        layout.addView(tvGroupLabel);

        RadioGroup radioGroup = new RadioGroup(this);
        radioGroup.setOrientation(RadioGroup.HORIZONTAL);

        RadioButton rbAll = new RadioButton(this);
        rbAll.setText("Tất cả (All) ");
        rbAll.setId(View.generateViewId());

        RadioButton rbComic = new RadioButton(this);
        rbComic.setText("Người theo dõi truyện");
        rbComic.setId(View.generateViewId());

        radioGroup.addView(rbAll);
        radioGroup.addView(rbComic);
        layout.addView(radioGroup);

        Spinner spinnerComics = new Spinner(this);
        List<String> comicTitles = new ArrayList<>();
        int initialSelectedIndex = 0;

        for (int i = 0; i < systemComicsList.size(); i++) {
            Comic c = systemComicsList.get(i);
            comicTitles.add(c.getTitle());
            if (notification.getComicId() != null && notification.getComicId().equals(c.getComicId())) {
                initialSelectedIndex = i;
            }
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, comicTitles);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerComics.setAdapter(spinnerAdapter);
        layout.addView(spinnerComics);

        if (notification.getComicId() != null && notification.getComicId() > 0) {
            rbComic.setChecked(true);
            spinnerComics.setVisibility(View.VISIBLE);
            spinnerComics.setSelection(initialSelectedIndex);
        } else {
            rbAll.setChecked(true);
            spinnerComics.setVisibility(View.GONE);
        }

        // Lưu trữ thông tin gốc phục vụ Dirty-checking chống Submit dữ liệu cũ dư thừa
        String origTitle = notification.getTitle() != null ? notification.getTitle().trim() : "";
        String origMsg = notification.getMessage() != null ? notification.getMessage().trim() : "";
        Integer origComicId = notification.getComicId();

        // ĐÃ SỬA: Chuyển sang .create()
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Sửa đổi thông báo hệ thống")
                .setView(layout)
                .setPositiveButton("Cập nhật", (dialogInterface, which) -> {
                    String title = edtTitle.getText().toString().trim();
                    String message = edtMsg.getText().toString().trim();

                    if (!title.isEmpty() && !message.isEmpty()) {
                        notification.setTitle(title);
                        notification.setMessage(message);

                        if (radioGroup.getCheckedRadioButtonId() == rbComic.getId()) {
                            int selectedPosition = spinnerComics.getSelectedItemPosition();
                            if (selectedPosition >= 0 && selectedPosition < systemComicsList.size()) {
                                notification.setComicId(systemComicsList.get(selectedPosition).getComicId());
                            }
                        } else {
                            notification.setComicId(null);
                        }

                        ApiClient.getApiService().adminUpdateNotification(notification.getNotificationId(), notification).enqueue(new Callback<Void>() {
                            @Override
                            public void onResponse(Call<Void> call, Response<Void> response) {
                                if (response.isSuccessful()) {
                                    Toast.makeText(AdminManageNotificationActivity.this, "Đã sửa thành công!", Toast.LENGTH_SHORT).show();
                                    loadAllNotifications(edtSearchNotif.getText().toString());
                                } else {
                                    Toast.makeText(AdminManageNotificationActivity.this, "Cập nhật thất bại!", Toast.LENGTH_SHORT).show();
                                }
                            }
                            @Override
                            public void onFailure(Call<Void> call, Throwable t) {
                                Toast.makeText(AdminManageNotificationActivity.this, "Lỗi kết nối server!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                })
                .setNegativeButton("Hủy", null)
                .create();

        // ĐÃ THÊM: Khối xử lý kiểm toán so sánh thay đổi sâu thời gian thực (Text + Radio + Spinner)
        Runnable checkChanges = () -> {
            Button btnPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (btnPositive == null) return;

            String currentTitle = edtTitle.getText().toString().trim();
            String currentMsg = edtMsg.getText().toString().trim();

            if (currentTitle.isEmpty() || currentMsg.isEmpty()) {
                btnPositive.setEnabled(false);
                btnPositive.setAlpha(0.5f);
                return;
            }

            boolean hasChanges = false;
            if (!currentTitle.equals(origTitle)) hasChanges = true;
            if (!currentMsg.equals(origMsg)) hasChanges = true;

            boolean origIsComic = (origComicId != null && origComicId > 0);
            boolean currentIsComic = (radioGroup.getCheckedRadioButtonId() == rbComic.getId());

            if (origIsComic != currentIsComic) {
                hasChanges = true;
            } else if (currentIsComic) {
                int selectedPosition = spinnerComics.getSelectedItemPosition();
                if (selectedPosition >= 0 && selectedPosition < systemComicsList.size()) {
                    int currentSelectedComicId = systemComicsList.get(selectedPosition).getComicId();
                    if (origComicId == null || origComicId != currentSelectedComicId) {
                        hasChanges = true;
                    }
                }
            }

            btnPositive.setEnabled(hasChanges);
            btnPositive.setAlpha(hasChanges ? 1.0f : 0.5f);
        };

        // Lắng nghe sự kiện chuyển nhóm đối tượng nhận tin
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == rbComic.getId()) {
                spinnerComics.setVisibility(View.VISIBLE);
            } else {
                spinnerComics.setVisibility(View.GONE);
            }
            checkChanges.run();
        });

        // Thiết lập đồng bộ các Listener kiểm tra dữ liệu thay đổi
        dialog.setOnShowListener(dialogInterface -> {
            checkChanges.run(); // Mặc định khóa chặt nút khi vừa mở

            TextWatcher watcher = new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    checkChanges.run();
                }
                @Override public void afterTextChanged(Editable s) {}
            };
            edtTitle.addTextChangedListener(watcher);
            edtMsg.addTextChangedListener(watcher);

            spinnerComics.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    checkChanges.run();
                }
                @Override public void onNothingSelected(AdapterView<?> parent) {}
            });
        });

        dialog.show();
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
                                loadAllNotifications(edtSearchNotif.getText().toString());
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