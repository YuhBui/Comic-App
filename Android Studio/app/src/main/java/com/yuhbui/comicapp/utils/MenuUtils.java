package com.yuhbui.comicapp.utils;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
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
     * Hàm cấu hình Menu trượt dùng chung cho mọi Activity phía USER
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

        // 1. Ánh xạ các mục menu từ file layout_side_menu.xml
        View menuHome = drawerLayout.findViewById(R.id.menuHome);
        View menuHistory = drawerLayout.findViewById(R.id.menuHistory);
        View menuFollow = drawerLayout.findViewById(R.id.menuFavorites);
        View menuDownload = drawerLayout.findViewById(R.id.menuDownloads);
        View menuProfile = drawerLayout.findViewById(R.id.menuProfile);
        View menuLogout = drawerLayout.findViewById(R.id.menuLogout);

        // 2. Reset toàn bộ các mục về màu nền trong suốt (mặc định khi chưa chọn)
        if (menuHome != null) menuHome.setBackgroundColor(Color.TRANSPARENT);
        if (menuHistory != null) menuHistory.setBackgroundColor(Color.TRANSPARENT);
        if (menuFollow != null) menuFollow.setBackgroundColor(Color.TRANSPARENT);
        if (menuDownload != null) menuDownload.setBackgroundColor(Color.TRANSPARENT);
        if (menuProfile != null) menuProfile.setBackgroundColor(Color.TRANSPARENT);

        // 3. Định nghĩa mã màu highlight khi mục được chọn (Ví dụ: màu cam hổ phách #D97707)
        int activeColor = Color.parseColor("#D97707");

        // 4. Kiểm tra Activity (màn hình) nào đang active để nhuộm màu tương ứng
        // Khi đang ở Trang chủ, activity sẽ là MainActivity
        if (activity instanceof com.yuhbui.comicapp.ui.MainActivity && menuHome != null) {
            menuHome.setBackgroundColor(activeColor); // Nhuộm màu cho Trang chủ khi được chọn
        } else if (activity instanceof com.yuhbui.comicapp.ui.HistoryActivity && menuHistory != null) {
            menuHistory.setBackgroundColor(activeColor);
        } else if (activity instanceof com.yuhbui.comicapp.ui.FollowActivity && menuFollow != null) {
            menuFollow.setBackgroundColor(activeColor);
        } else if (activity instanceof com.yuhbui.comicapp.ui.DownloadListActivity && menuDownload != null) {
            menuDownload.setBackgroundColor(activeColor);
        } else if (activity instanceof com.yuhbui.comicapp.ui.ProfileActivity && menuProfile != null) {
            menuProfile.setBackgroundColor(activeColor);
        }

        // 2. Xử lý sự kiện click cho mục "Trang chủ"
        if (menuHome != null) {
            menuHome.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                if (!(activity instanceof MainActivity)) {
                    activity.startActivity(new Intent(activity, MainActivity.class));
                }
            });
        }

        // 3. Xử lý sự kiện click cho mục "Lịch sử đọc"
        if (menuHistory != null) {
            menuHistory.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                if (!(activity instanceof HistoryActivity)) {
                    activity.startActivity(new Intent(activity, HistoryActivity.class));
                }
            });
        }

        // 4. Xử lý sự kiện click cho mục "Đang theo dõi"
        if (menuFollow != null) {
            menuFollow.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                if (!(activity instanceof FollowActivity)) {
                    activity.startActivity(new Intent(activity, FollowActivity.class));
                }
            });
        }

        // 5. Xử lý sự kiện click cho mục "Truyện tải xuống"
        if (menuDownload != null) {
            menuDownload.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                if (!(activity instanceof com.yuhbui.comicapp.ui.DownloadListActivity)) {
                    activity.startActivity(new Intent(activity, com.yuhbui.comicapp.ui.DownloadListActivity.class));
                }
            });
        }

        // 6. Xử lý sự kiện click cho mục "Hồ sơ cá nhân"
        if (menuProfile != null) {
            menuProfile.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                if (!(activity instanceof ProfileActivity)) {
                    activity.startActivity(new Intent(activity, ProfileActivity.class));
                }
            });
        }

        // 7. Xử lý sự kiện click cho mục "Đăng xuất"
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

        // Ánh xạ tất cả các ô mục chức năng trong layout_admin_side_menu.xml
        View menuDashboard = activity.findViewById(R.id.menuAdminDashboard);
        View menuComics = activity.findViewById(R.id.menuAdminComics);
        View menuUsers = activity.findViewById(R.id.menuAdminUsers);
        View menuNotifications = activity.findViewById(R.id.menuAdminNotifications);
        View menuLogout = activity.findViewById(R.id.menuAdminLogout);

        // Đưa tất cả mục menu Admin về màu nền trong suốt mặc định
        if (menuDashboard != null) menuDashboard.setBackgroundColor(Color.TRANSPARENT);
        if (menuComics != null) menuComics.setBackgroundColor(Color.TRANSPARENT);
        if (menuUsers != null) menuUsers.setBackgroundColor(Color.TRANSPARENT);
        if (menuNotifications != null) menuNotifications.setBackgroundColor(Color.TRANSPARENT);

        // Định nghĩa mã màu highlight khi được chọn cho Admin (Đồng bộ cam #D97707)
        int adminActiveColor = Color.parseColor("#D97707");

        // Kiểm tra thực tế Activity Admin đang mở để nhuộm màu nổi bật
        if (activity instanceof AdminDashboardActivity && menuDashboard != null) {
            menuDashboard.setBackgroundColor(adminActiveColor);
        } else if (activity instanceof AdminManageComicsActivity && menuComics != null) {
            menuComics.setBackgroundColor(adminActiveColor);
        } else if (activity instanceof AdminManageUsersActivity && menuUsers != null) {
            menuUsers.setBackgroundColor(adminActiveColor);
        } else if (activity instanceof AdminManageNotificationActivity && menuNotifications != null) {
            menuNotifications.setBackgroundColor(adminActiveColor);
        }

        // 1. Click Bảng thống kê
        if (menuDashboard != null) {
            menuDashboard.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                if (!(activity instanceof AdminDashboardActivity)) {
                    activity.startActivity(new Intent(activity, AdminDashboardActivity.class));
                }
            });
        }

        // 2. Click Quản lý truyện
        if (menuComics != null) {
            menuComics.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                if (!(activity instanceof AdminManageComicsActivity)) {
                    activity.startActivity(new Intent(activity, AdminManageComicsActivity.class));
                }
            });
        }

        // 3. Click Quản lý User
        if (menuUsers != null) {
            menuUsers.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                if (!(activity instanceof AdminManageUsersActivity)) {
                    activity.startActivity(new Intent(activity, AdminManageUsersActivity.class));
                }
            });
        }

        // 4. Click Quản lý Thông báo
        if (menuNotifications != null) {
            menuNotifications.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                if (!(activity instanceof AdminManageNotificationActivity)) {
                    activity.startActivity(new Intent(activity, AdminManageNotificationActivity.class));
                }
            });
        }

        // 5. Đăng xuất hệ thống
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