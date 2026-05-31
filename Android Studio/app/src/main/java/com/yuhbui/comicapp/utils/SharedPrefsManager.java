package com.yuhbui.comicapp.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.yuhbui.comicapp.data.model.User;

public class SharedPrefsManager {
    private static final String PREF_NAME = "ComicAppPrefs";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_NAME = "userName";

    private static final String KEY_USER_ROLE = "user_role";
    // Hàm lưu thông tin khi đăng nhập thành công
    public static void saveUser(Context context, User user) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_USER_ID, user.getUserId());
        editor.putString(KEY_USER_NAME, user.getDisplayName());
        editor.putString(KEY_USER_ROLE, user.getRole()); // <-- THÊM DÒNG NÀY: Tự động gom việc lưu Role vào đây
        editor.apply();
    }

    // Hàm lấy ID người dùng hiện tại (Trả về -1 nếu chưa đăng nhập)
    public static int getUserId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_USER_ID, -1);
    }

    // Hàm Đăng xuất
    public static void logout(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }

    public static String getUserName(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_USER_NAME, "Bạn đọc"); // "Bạn đọc" là giá trị mặc định nếu không tìm thấy
    }

    // BỔ SUNG 2: Hàm kiểm tra xem đã đăng nhập chưa cho code gọn gàng
    public static boolean isLoggedIn(Context context) {
        return getUserId(context) != -1;
    }

    // Hàm lưu Role
    public static void saveUserRole(Context context, String role) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        pref.edit().putString(KEY_USER_ROLE, role).apply();
    }

    // Hàm lấy Role (Mặc định nếu trống là User)
    public static String getUserRole(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return pref.getString(KEY_USER_ROLE, "User");
    }
}