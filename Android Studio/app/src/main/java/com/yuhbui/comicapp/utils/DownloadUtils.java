package com.yuhbui.comicapp.utils;

import android.content.Context;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadUtils {

    private static final OkHttpClient client = new OkHttpClient();

    /**
     * Tải ảnh từ URL về bộ nhớ nội bộ của máy
     * @param context Context của ứng dụng
     * @param urlString Link ảnh mạng cần tải
     * @param subFolder Thư mục con phân cấp (ví dụ: "covers" hoặc "comic_1/chapter_5")
     * @param fileName Tên file ảnh đích (ví dụ: "cover.jpg", "page_1.jpg")
     * @return Đường dẫn tuyệt đối của file local vừa lưu, trả về null nếu lỗi.
     */
    public static String downloadFile(Context context, String urlString, String subFolder, String fileName) {
        try {
            // Định vị đường dẫn lưu file: /data/user/0/package_name/files/truyen_downloads/subFolder
            File baseFolder = new File(context.getFilesDir(), "truyen_downloads/" + subFolder);
            if (!baseFolder.exists()) {
                baseFolder.mkdirs(); // Tự động tạo cây thư mục nếu chưa có
            }

            File targetFile = new File(baseFolder, fileName);

            Request request = new Request.Builder().url(urlString).build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) return null;

                try (InputStream is = response.body().byteStream();
                     FileOutputStream fos = new FileOutputStream(targetFile)) {

                    byte[] buffer = new byte[4096];
                    int read;
                    while ((read = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, read);
                    }
                    fos.flush();
                    return targetFile.getAbsolutePath(); // Trả về đường dẫn local thực tế
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}