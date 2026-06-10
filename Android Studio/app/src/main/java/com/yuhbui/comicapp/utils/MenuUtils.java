package com.yuhbui.comicapp.utils;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.Toast;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.yuhbui.comicapp.R;
import com.yuhbui.comicapp.ui.MainActivity;
import com.yuhbui.comicapp.ui.HistoryActivity;
import com.yuhbui.comicapp.ui.FollowActivity;
import com.yuhbui.comicapp.ui.ProfileActivity;
import com.yuhbui.comicapp.ui.LoginActivity;
import com.yuhbui.comicapp.ui.admin.AdminDashboardActivity;
import com.yuhbui.comicapp.ui.admin.AdminManageComicsActivity;
import com.yuhbui.comicapp.ui.admin.AdminManageNotificationActivity;
import com.yuhbui.comicapp.ui.admin.AdminManageUsersActivity;

public class MenuUtils {

    /**
     * Hàm cấu hình Menu trượt dùng chung cho mọi Activity có nhúng layout_side_menu
     */
    public static void setupSideMenu(Activity activity, DrawerLayout drawerLayout, View headerMenu) {
        if (drawerLayout == null) return;

        // 1. Sự kiện click vào nút Menu trên Header để Mở/Đóng thanh trượt
        if (headerMenu != null) {
            headerMenu.setOnClickListener(v -> {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            });
        }

        // 2. Xử lý sự kiện click cho mục "Trang chủ"
        View menuHome = activity.findViewById(R.id.menuHome);
        if (menuHome != null) {
            menuHome.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                if (!(activity instanceof MainActivity)) {
                    activity.startActivity(new Intent(activity, MainActivity.class));
                }
            });
        }

        // 3. Xử lý sự kiện click cho mục "Lịch sử đọc"
        View menuHistory = activity.findViewById(R.id.menuHistory);
        if (menuHistory != null) {
            menuHistory.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                if (!(activity instanceof HistoryActivity)) {
                    activity.startActivity(new Intent(activity, HistoryActivity.class));
                }
            });
        }

        // 4. Xử lý sự kiện click cho mục "Đang theo dõi"
        View menuFollow = activity.findViewById(R.id.menuFollow);
        if (menuFollow != null) {
            menuFollow.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                if (!(activity instanceof FollowActivity)) {
                    activity.startActivity(new Intent(activity, FollowActivity.class));
                }
            });
        }

        // 5. Xử lý sự kiện click cho mục "Hồ sơ cá nhân"
        View menuProfile = activity.findViewById(R.id.menuProfile);
        if (menuProfile != null) {
            menuProfile.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                if (!(activity instanceof ProfileActivity)) {
                    activity.startActivity(new Intent(activity, ProfileActivity.class));
                }
            });
        }

        // 6. Xử lý sự kiện click cho mục "Đăng xuất"
        View menuLogout = activity.findViewById(R.id.menuLogout);
        if (menuLogout != null) {
            menuLogout.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                Toast.makeText(activity, "Đang đăng xuất...", Toast.LENGTH_SHORT).show();
                SharedPrefsManager.logout(activity);

                Intent intent = new Intent(activity, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                activity.startActivity(intent);
                activity.finish();
            });
        }
    }

    public static void setupAdminSideMenu(Activity activity, DrawerLayout drawerLayout, View headerMenu) {
        if (drawerLayout == null) return;

        // Kéo mở/đóng Menu Admin
        if (headerMenu != null) {
            headerMenu.setOnClickListener(v -> {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            });
        }

        // 1. Click Bảng thống kê
        View menuDashboard = activity.findViewById(R.id.menuAdminDashboard);
        if (menuDashboard != null) {
            menuDashboard.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                if (!(activity instanceof AdminDashboardActivity)) {
                    activity.startActivity(new Intent(activity, AdminDashboardActivity.class));
                }
            });
        }

        // 2. Click Quản lý truyện
        View menuComics = activity.findViewById(R.id.menuAdminComics);
        if (menuComics != null) {
            menuComics.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                if (!(activity instanceof AdminManageComicsActivity)) {
                    activity.startActivity(new Intent(activity, AdminManageComicsActivity.class));
                }
            });
        }

        // 3. Click Quản lý User
        View menuUsers = activity.findViewById(R.id.menuAdminUsers);
        if (menuUsers != null) {
            menuUsers.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                if (!(activity instanceof AdminManageUsersActivity)) {
                    activity.startActivity(new Intent(activity, AdminManageUsersActivity.class));
                }
            });
        }

        // 4. Click Quản lý Thông báo
        View menuNotifications = activity.findViewById(R.id.menuAdminNotifications);
        if (menuNotifications != null) {
            menuNotifications.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                if (!(activity instanceof AdminManageNotificationActivity)) {
                    activity.startActivity(new Intent(activity, AdminManageNotificationActivity.class));
                }
            });
        }

        // 5. Đăng xuất hệ thống
        View menuLogout = activity.findViewById(R.id.menuAdminLogout);
        if (menuLogout != null) {
            menuLogout.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                SharedPrefsManager.logout(activity);
                Intent intent = new Intent(activity, com.yuhbui.comicapp.ui.LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                activity.startActivity(intent);
                activity.finish();
            });
        }
    }
}