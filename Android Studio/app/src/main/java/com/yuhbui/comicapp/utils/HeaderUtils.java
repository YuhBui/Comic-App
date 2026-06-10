package com.yuhbui.comicapp.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.ObjectKey;
import com.yuhbui.comicapp.R;
import com.yuhbui.comicapp.data.api.ApiClient;
import com.yuhbui.comicapp.data.model.User;
import com.yuhbui.comicapp.ui.LoginActivity;
import com.yuhbui.comicapp.ui.MainActivity;
import com.yuhbui.comicapp.ui.NotificationListActivity;
import com.yuhbui.comicapp.ui.ProfileActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HeaderUtils {

    /**
     * Khởi tạo các tính năng cốt lõi của Header dùng chung (Menu trượt, Thông báo, Avatar, Tìm kiếm toàn cục)
     *
     * @param activity     Activity hiện tại đang gọi hàm
     * @param layoutHeader View của Header layout được nhúng vào Activity (ví dụ: findViewById(R.id.layoutHeaderHistory))
     * @param drawerLayout DrawerLayout của Activity đó để điều khiển đóng/mở thanh trượt trái
     */
    public static void initHeader(Activity activity, View layoutHeader, DrawerLayout drawerLayout) {
        if (layoutHeader == null) return;

        // Ánh xạ các thành phần từ layout_header.xml
        ImageView headerMenu = layoutHeader.findViewById(R.id.headerMenu);
        ImageView headerNotification = layoutHeader.findViewById(R.id.headerNotification);
        ImageView headerAvatar = layoutHeader.findViewById(R.id.headerAvatar);
        TextView tvNotificationBadge = layoutHeader.findViewById(R.id.tvNotificationBadge);

        ImageView headerSearch = layoutHeader.findViewById(R.id.headerSearch);
        EditText edtHeaderSearch = layoutHeader.findViewById(R.id.edtHeaderSearch);
        TextView headerLogo = layoutHeader.findViewById(R.id.headerLogo);

        // 1. Sự kiện nhấn nút Menu để đóng/mở DrawerLayout trượt trái
        if (headerMenu != null && drawerLayout != null) {
            headerMenu.setOnClickListener(v -> {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            });
        }

        // 2. Sự kiện nhấn nút quả chuông để chuyển tiếp qua màn hình danh sách thông báo
        // --- SỬA TRONG LỚP HeaderUtils.java ---
        if (headerNotification != null) {
            headerNotification.setOnClickListener(v -> {
                // KIỂM TRA: Nếu đang ở sẵn trang thông báo thì không bấm chuyển trang nữa để tránh xung đột vòng lặp
                if (!(activity instanceof NotificationListActivity)) {
                    Intent intent = new Intent(activity, NotificationListActivity.class);
                    activity.startActivity(intent);
                }
            });
        }

        // 3. Cấu hình tính năng TÌM KIẾM TOÀN CỤC (Chỉ áp dụng cho các trang con, trừ MainActivity)
        if (headerSearch != null && edtHeaderSearch != null && !(activity instanceof MainActivity)) {

            // Xử lý ẩn hiện thanh nhập chữ khi click biểu tượng kính lúp
            headerSearch.setOnClickListener(v -> {
                if (edtHeaderSearch.getVisibility() == View.GONE) {
                    if (headerLogo != null) headerLogo.setVisibility(View.GONE);
                    edtHeaderSearch.setVisibility(View.VISIBLE);
                    edtHeaderSearch.requestFocus();

                    // Hiển thị bàn phím ảo
                    InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) imm.showSoftInput(edtHeaderSearch, InputMethodManager.SHOW_IMPLICIT);
                    headerSearch.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
                } else {
                    String keyword = edtHeaderSearch.getText().toString().trim();
                    if (!keyword.isEmpty()) {
                        // Nếu có từ khóa -> Chuyển hướng dữ liệu quay về Trang chủ để tìm kiếm tập trung
                        Intent intent = new Intent(activity, MainActivity.class);
                        intent.putExtra("GLOBAL_SEARCH_KEYWORD", keyword);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        activity.startActivity(intent);
                    } else {
                        // Nếu ô trống -> Đóng thanh search lại, hiện lại Logo
                        edtHeaderSearch.setText("");
                        edtHeaderSearch.setVisibility(View.GONE);
                        if (headerLogo != null) headerLogo.setVisibility(View.VISIBLE);
                        headerSearch.setImageResource(android.R.drawable.ic_menu_search);

                        // Ẩn bàn phím ảo
                        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (imm != null) imm.hideSoftInputFromWindow(edtHeaderSearch.getWindowToken(), 0);
                    }
                }
            });

            // Xử lý khi người dùng gõ từ khóa và nhấn nút "Tìm kiếm/Kính lúp" trên bàn phím ảo
            edtHeaderSearch.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String keyword = edtHeaderSearch.getText().toString().trim();
                    if (!keyword.isEmpty()) {
                        Intent intent = new Intent(activity, MainActivity.class);
                        intent.putExtra("GLOBAL_SEARCH_KEYWORD", keyword);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        activity.startActivity(intent);
                    }
                    return true;
                }
                return false;
            });
        }

        // 4. Tự động tải dữ liệu bất đồng bộ ban đầu cho Avatar và số lượng thông báo chưa đọc
        if (headerAvatar != null) {
            // Tải ảnh đại diện lên trước
            loadHeaderAvatar(activity, headerAvatar);

            // Cài đặt sự kiện hiển thị Menu Hồ sơ / Đăng xuất khi click vào Avatar cho TẤT CẢ các Activity
            headerAvatar.setOnClickListener(v -> {
                androidx.appcompat.widget.PopupMenu popupMenu = new androidx.appcompat.widget.PopupMenu(activity, v);
                popupMenu.getMenu().add(0, 1, 0, "Hồ sơ cá nhân");
                popupMenu.getMenu().add(0, 2, 1, "Đăng xuất");

                popupMenu.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == 1) {
                        // Nếu chưa ở trang Profile thì mới chuyển hướng
                        if (!(activity instanceof ProfileActivity)) {
                            Intent intent = new Intent(activity, ProfileActivity.class);
                            activity.startActivity(intent);
                        }
                        return true;
                    } else if (item.getItemId() == 2) {
                        // Xử lý Đăng xuất tập trung nhanh gọn
                        android.widget.Toast.makeText(activity, "Đang đăng xuất...", android.widget.Toast.LENGTH_SHORT).show();
                        SharedPrefsManager.logout(activity);

                        Intent intent = new Intent(activity, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        activity.startActivity(intent);
                        activity.finish();
                        return true;
                    }
                    return false;
                });
                popupMenu.show();
            });
        }
    }

    /**
     * Tải ảnh đại diện của người dùng hiện tại lên ImageView Avatar trên Header
     */
    public static void loadHeaderAvatar(Context context, ImageView headerAvatar) {
        if (headerAvatar == null) return;

        int userId = SharedPrefsManager.getUserId(context);
        if (userId == -1) {
            headerAvatar.setImageResource(android.R.drawable.sym_def_app_icon);
            return;
        }

        ApiClient.getApiService().getUserProfile(userId).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String avatarUrl = response.body().getAvatarUrl();
                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        Glide.with(context.getApplicationContext())
                                .load(avatarUrl)
                                .signature(new ObjectKey(String.valueOf(System.currentTimeMillis())))
                                .circleCrop()
                                .placeholder(android.R.drawable.sym_def_app_icon)
                                .into(headerAvatar);
                    } else {
                        headerAvatar.setImageResource(android.R.drawable.sym_def_app_icon);
                    }
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                headerAvatar.setImageResource(android.R.drawable.sym_def_app_icon);
            }
        });
    }

    /**
     * Tải và cập nhật số lượng thông báo chưa đọc lên vòng tròn Badge màu đỏ
     */
    public static void loadUnreadNotificationCount(Context context, TextView tvNotificationBadge) {
        if (tvNotificationBadge == null) return;

        int userId = SharedPrefsManager.getUserId(context);
        if (userId == -1) {
            tvNotificationBadge.setVisibility(View.GONE);
            return;
        }

        ApiClient.getApiService().getUnreadNotificationCount(userId).enqueue(new Callback<Long>() {
            @Override
            public void onResponse(Call<Long> call, Response<Long> response) {
                if (response.isSuccessful() && response.body() != null) {
                    long unreadCount = response.body();
                    if (unreadCount > 0) {
                        tvNotificationBadge.setText(String.valueOf(unreadCount));
                        tvNotificationBadge.setVisibility(View.VISIBLE);
                    } else {
                        tvNotificationBadge.setVisibility(View.GONE);
                    }
                } else {
                    tvNotificationBadge.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(Call<Long> call, Throwable t) {
                tvNotificationBadge.setVisibility(View.GONE);
            }
        });
    }
}