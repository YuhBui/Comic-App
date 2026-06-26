package com.yuhbui.comicapp.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.yuhbui.comicapp.data.model.User;

public class SharedPrefsManager {
    private static final String PREF_NAME = "ComicAppPrefs";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_IS_REMEMBERED = "isRemembered";
    public static void saveUser(Context context, User user, boolean isRemembered) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_USER_ID, user.getUserId());
        editor.putString(KEY_USER_NAME, user.getDisplayName());
        editor.putString(KEY_USER_ROLE, user.getRole());
        editor.putBoolean(KEY_IS_REMEMBERED, isRemembered);
        editor.apply();
    }

    // Hàm cũ (Overload): Phục vụ cho các chức năng khác hoặc Đăng nhập Google (Mặc định mạng xã hội là luôn ghi nhớ)
    public static void saveUser(Context context, User user) {
        saveUser(context, user, true);
    }

    // HÀM MỚI: Kiểm tra xem tài khoản này ở lần đăng nhập trước có tích "Ghi nhớ" không
    public static boolean isRemembered(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_IS_REMEMBERED, false);
    }

    // Hàm lấy ID người dùng hiện tại (Trả về -1 nếu chưa đăng nhập)
    public static int getUserId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_USER_ID, -1);
    }

    // Hàm Đăng xuất (Xóa sạch mọi cấu hình)
    public static void logout(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }

    public static String getUserName(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_USER_NAME, "Bạn đọc");
    }

    // Hàm kiểm tra xem có dữ liệu người dùng đang hiện hữu hay không
    public static boolean isLoggedIn(Context context) {
        return getUserId(context) != -1;
    }

    // Hàm lưu Role độc lập
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