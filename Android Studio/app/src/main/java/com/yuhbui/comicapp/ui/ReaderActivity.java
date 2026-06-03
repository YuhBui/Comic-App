package com.yuhbui.comicapp.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.yuhbui.comicapp.R;
import com.yuhbui.comicapp.data.api.ApiClient;
import com.yuhbui.comicapp.data.model.Chapter;
import com.yuhbui.comicapp.data.model.ChapterImage;
import com.yuhbui.comicapp.data.model.Comment;
import com.yuhbui.comicapp.ui.adapters.CommentAdapter;
import com.yuhbui.comicapp.ui.adapters.ImageAdapter;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReaderActivity extends AppCompatActivity {

    private RecyclerView recyclerViewImages;
    private ImageAdapter imageAdapter;

    // Khai báo các biến cho phần bình luận
    private RecyclerView recyclerViewComments;
    private CommentAdapter commentAdapter;
    private EditText edtCommentInput;
    private Button btnSendComment;

    // --- CÁC BIẾN ĐIỀU HƯỚNG CHUYỂN CHƯƠNG MỚI TÍCH HỢP ---
    private Button btnPrevChapter, btnNextChapter;
    private Spinner spinnerChapters;
    private List<Chapter> allChaptersInComic = new ArrayList<>(); // Lưu danh sách chương của bộ truyện
    private int currentChapterIndex = -1; // Vị trí của chương hiện tại trong danh sách
    private boolean isSpinnerFirstInit = true; // Cờ chặn Spinner tự kích hoạt khi vừa mở màn hình

    private int currentChapterId = -1;
    private int currentComicId = -1;
    private Integer targetParentCommentId = null; // Quản lý reply lồng nhau

    // --- KHAI BÁO CÁC THÀNH PHẦN CỦA HEADER DÙNG CHUNG ---
    private View layoutHeader;
    private ImageView headerMenu, headerSearch, headerNotification, headerAvatar;
    private TextView headerLogo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reader);

        // 0. ÁNH XẠ CỤM HEADER CHUNG VÀ ĐĂNG KÝ SỰ KIỆN CLICK
        layoutHeader = findViewById(R.id.layoutHeaderReader);
        headerMenu = layoutHeader.findViewById(R.id.headerMenu);
        headerLogo = layoutHeader.findViewById(R.id.headerLogo);
        headerSearch = layoutHeader.findViewById(R.id.headerSearch);
        headerNotification = layoutHeader.findViewById(R.id.headerNotification);
        headerAvatar = layoutHeader.findViewById(R.id.headerAvatar);

        headerMenu.setOnClickListener(v -> showHeaderPopupMenu(v));
        headerLogo.setOnClickListener(v -> Toast.makeText(this, "[Reader] Quay lại trang chính", Toast.LENGTH_SHORT).show());
        headerSearch.setOnClickListener(v -> Toast.makeText(this, "[Reader] Mở tìm kiếm", Toast.LENGTH_SHORT).show());
        headerNotification.setOnClickListener(v -> Toast.makeText(this, "[Reader] Mở thông báo", Toast.LENGTH_SHORT).show());
        headerAvatar.setOnClickListener(v -> Toast.makeText(this, "[Reader] Mở hồ sơ cá nhân", Toast.LENGTH_SHORT).show());

        // 1. Ánh xạ phần đọc ảnh truyện
        recyclerViewImages = findViewById(R.id.recyclerViewImages);
        recyclerViewImages.setLayoutManager(new LinearLayoutManager(this));

        // ĐÃ SỬA: Cấu hình tối ưu bộ nhớ đệm và layout đo đạc kích cỡ để hiển thị đầy đủ loạt ảnh chương truyện
        recyclerViewImages.setHasFixedSize(true);
        recyclerViewImages.setItemViewCacheSize(30);
        recyclerViewImages.setNestedScrollingEnabled(false); // Ngăn ngừa xung đột cuộn mượt khi bọc trong ScrollView cha

        imageAdapter = new ImageAdapter();
        recyclerViewImages.setAdapter(imageAdapter);

        // 2. Ánh xạ phần bình luận
        recyclerViewComments = findViewById(R.id.recyclerViewCommentsReader);
        edtCommentInput = findViewById(R.id.edtCommentInputReader);
        btnSendComment = findViewById(R.id.btnSendCommentReader);

        recyclerViewComments.setLayoutManager(new LinearLayoutManager(this));
        commentAdapter = new CommentAdapter();
        recyclerViewComments.setAdapter(commentAdapter);

        // --- ÁNH XẠ CỤM ĐIỀU HƯỚNG CHUYỂN CHƯƠNG ---
        btnPrevChapter = findViewById(R.id.btnPrevChapter);
        btnNextChapter = findViewById(R.id.btnNextChapter);
        spinnerChapters = findViewById(R.id.spinnerChapters);

        // Lấy các ID truyền từ màn hình trước sang
        currentChapterId = getIntent().getIntExtra("CHAPTER_ID", -1);
        currentComicId = getIntent().getIntExtra("COMIC_ID", -1);

        if (currentChapterId != -1) {
            loadChapterContent(currentChapterId); // Gom việc tải ảnh và bình luận vào hàm chung
        }

        // Tải danh sách tất cả các chương phục vụ cho thanh điều hướng dropdown
        if (currentComicId != -1) {
            loadAllChaptersNavigation(currentComicId);
        }

        // Sự kiện bấm nút CHƯƠNG TRƯỚC (Giảm index đi 1)
        btnPrevChapter.setOnClickListener(v -> {
            if (currentChapterIndex < allChaptersInComic.size() - 1) {
                navigateToChapter(currentChapterIndex + 1);
            }
        });

        // Sự kiện bấm nút CHƯƠNG SAU (Vì mảng DESC nên Chương sau phải là giảm Index đi -1)
        btnNextChapter.setOnClickListener(v -> {
            if (currentChapterIndex > 0) {
                navigateToChapter(currentChapterIndex - 1);
            }
        });

        // 3. Cấu hình tính năng Phản hồi (Reply) lồng nhau cho Adapter giống hệt trang chi tiết
        commentAdapter.setOnCommentClickListener(new CommentAdapter.OnCommentClickListener() {
            @Override
            public void onReplyClick(Comment parentComment) {
                targetParentCommentId = parentComment.getCommentId();
                if (parentComment.getParentCommentId() == null && parentComment.getUserId() != 0) {
                    String tagText = "@Thành viên #" + parentComment.getUserId() + " ";
                    edtCommentInput.setText(tagText);
                    edtCommentInput.setSelection(tagText.length());
                    edtCommentInput.setHint("Đang trả lời...");
                } else {
                    edtCommentInput.setHint("Trả lời bình luận của #" + parentComment.getUserId() + ":");
                }
                edtCommentInput.requestFocus();
            }
        });

        // 4. Bắt sự kiện click nút Gửi bình luận
        btnSendComment.setOnClickListener(v -> sendCommentToServer());
    }

    // Hàm phụ trách tải toàn bộ nội dung của chương tại chỗ mà không cần mở lại Activity
    private void loadChapterContent(int chapterId) {
        loadImages(chapterId);    // Tải ảnh truyện ở trên
        loadChapterComments(chapterId); // Tải bình luận ở dưới

        // Tự động lưu lịch sử đọc
        int userId = com.yuhbui.comicapp.utils.SharedPrefsManager.getUserId(this);
        if (userId != -1 && currentComicId != -1) {
            saveHistoryToServer(userId, currentComicId, chapterId);
        }
    }

    // Hàm gọi API lấy danh sách toàn bộ chương để nạp vào Spinner điều hướng nhanh
    private void loadAllChaptersNavigation(int comicId) {
        ApiClient.getApiService().getChaptersByComicId(comicId).enqueue(new Callback<List<Chapter>>() {
            @Override
            public void onResponse(Call<List<Chapter>> call, Response<List<Chapter>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allChaptersInComic = response.body();

                    // Tạo danh sách chuỗi hiển thị tên chương lên Spinner thả xuống
                    List<String> spinnerItems = new ArrayList<>();
                    for (int i = 0; i < allChaptersInComic.size(); i++) {
                        Chapter ch = allChaptersInComic.get(i);
                        spinnerItems.add("Chương " + ch.getChapterNumber() + (ch.getTitle() != null ? ": " + ch.getTitle() : ""));

                        // Xác định xem chương hiện tại đang đọc khớp với vị trí index nào
                        if (ch.getChapterId() == currentChapterId) {
                            currentChapterIndex = i;
                        }
                    }

                    // Thiết lập Adapter kết nối dữ liệu chuỗi vào Spinner UI
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(ReaderActivity.this,
                            android.R.layout.simple_spinner_item, spinnerItems);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerChapters.setAdapter(adapter);

                    // Đồng bộ vị trí hiển thị của Spinner trùng với chương đang đọc
                    if (currentChapterIndex != -1) {
                        isSpinnerFirstInit = true; // Đánh dấu trạng thái mở màn hình ban đầu
                        spinnerChapters.setSelection(currentChapterIndex);
                        updateButtonNavigationUI(); // Cập nhật trạng thái ẩn/hiện nút Trước/Sau
                    }

                    // Lắng nghe sự kiện click chọn chương bất kỳ từ Spinner xổ xuống
                    spinnerChapters.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            // Chặn không cho gọi mạng trùng lặp ở lần đầu tiên khởi chạy ứng dụng
                            if (isSpinnerFirstInit) {
                                isSpinnerFirstInit = false;
                                return;
                            }
                            navigateToChapter(position);
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });
                }
            }

            @Override
            public void onFailure(Call<List<Chapter>> call, Throwable t) {
                Log.e("YUH_TEST", "Lỗi tải danh sách chương điều hướng: " + t.getMessage());
            }
        });
    }

    // Logic thực hiện xử lý chuyển đổi dữ liệu chương khi người dùng tương tác điều hướng
    private void navigateToChapter(int targetIndex) {
        if (targetIndex >= 0 && targetIndex < allChaptersInComic.size()) {
            Chapter targetChapter = allChaptersInComic.get(targetIndex);

            // Thay đổi ID chương hiện tại sang chương mới chọn
            currentChapterId = targetChapter.getChapterId();
            currentChapterIndex = targetIndex;

            // Làm mới giao diện bằng cách nạp data ảnh và comment của chương mới
            loadChapterContent(currentChapterId);

            // Đồng bộ lại vị trí hiển thị trên thanh Spinner thả xuống
            isSpinnerFirstInit = true;
            spinnerChapters.setSelection(currentChapterIndex);

            // Kiểm tra trạng thái giới hạn để đóng mở khóa nút bấm Trước/Sau
            updateButtonNavigationUI();
        }
    }

    // Hàm cập nhật trạng thái đóng/mở khóa (Enabled) của nút Trước và nút Sau
    private void updateButtonNavigationUI() {
        if (currentChapterIndex >= allChaptersInComic.size() - 1) {
            btnPrevChapter.setEnabled(false);
            btnPrevChapter.setAlpha(0.4f);
        } else {
            btnPrevChapter.setEnabled(true);
            btnPrevChapter.setAlpha(1.0f);
        }

        if (currentChapterIndex <= 0) {
            btnNextChapter.setEnabled(false);
            btnNextChapter.setAlpha(0.4f);
        } else {
            btnNextChapter.setEnabled(true);
            btnNextChapter.setAlpha(1.0f);
        }
    }

    private void loadImages(int chapterId) {
        // ĐÃ SỬA: Giải phóng và xóa danh sách trang ảnh cũ ngay lập tức khi đổi chương để tránh lag hình
        if (imageAdapter != null) {
            imageAdapter.setImages(new ArrayList<>());
            imageAdapter.notifyDataSetChanged();
        }

        ApiClient.getApiService().getImagesByChapterId(chapterId).enqueue(new Callback<List<ChapterImage>>() {
            @Override
            public void onResponse(Call<List<ChapterImage>> call, Response<List<ChapterImage>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    imageAdapter.setImages(response.body());
                    // ĐÃ SỬA: Ép giao diện vẽ lại toàn bộ mảng tranh truyện lên màn hình ngay khi nạp xong
                    imageAdapter.notifyDataSetChanged();
                }
            }
            @Override
            public void onFailure(Call<List<ChapterImage>> call, Throwable t) {}
        });
    }

    private void loadChapterComments(int chapterId) {
        ApiClient.getApiService().getCommentsByChapter(chapterId).enqueue(new Callback<List<Comment>>() {
            @Override
            public void onResponse(Call<List<Comment>> call, Response<List<Comment>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    commentAdapter.setComments(response.body());
                }
            }
            @Override
            public void onFailure(Call<List<Comment>> call, Throwable t) {}
        });
    }

    private void sendCommentToServer() {
        String content = edtCommentInput.getText().toString().trim();
        int userId = com.yuhbui.comicapp.utils.SharedPrefsManager.getUserId(this);

        if (content.isEmpty()) return;
        if (userId == -1) {
            Toast.makeText(this, "Vui lòng đăng nhập để bình luận!", Toast.LENGTH_SHORT).show();
            return;
        }

        Comment newComment = new Comment();
        newComment.setUserId(userId);
        newComment.setChapterId(currentChapterId);
        newComment.setComicId(currentComicId);
        newComment.setContent(content);
        newComment.setParentCommentId(targetParentCommentId);

        ApiClient.getApiService().postComment(newComment).enqueue(new Callback<Comment>() {
            @Override
            public void onResponse(Call<Comment> call, Response<Comment> response) {
                if (response.isSuccessful()) {
                    edtCommentInput.setText("");
                    edtCommentInput.setHint("Chia sẻ cảm xúc về chương này...");

                    if (targetParentCommentId != null) {
                        commentAdapter.resetRepliesCache(targetParentCommentId);
                    }
                    targetParentCommentId = null;

                    loadChapterComments(currentChapterId);
                }
            }
            @Override
            public void onFailure(Call<Comment> call, Throwable t) {}
        });
    }

    private void saveHistoryToServer(int userId, int comicId, int chapterId) {
        com.yuhbui.comicapp.data.model.ReadingHistory history = new com.yuhbui.comicapp.data.model.ReadingHistory();
        history.setUserId(userId);
        history.setComicId(comicId);
        history.setLastChapterId(chapterId);
        ApiClient.getApiService().saveReadingHistory(history).enqueue(new Callback<com.yuhbui.comicapp.data.model.ReadingHistory>() {
            @Override public void onResponse(Call<com.yuhbui.comicapp.data.model.ReadingHistory> call, Response<com.yuhbui.comicapp.data.model.ReadingHistory> response) {}
            @Override public void onFailure(Call<com.yuhbui.comicapp.data.model.ReadingHistory> call, Throwable t) {}
        });
    }

    private void showHeaderPopupMenu(View anchorView) {
        androidx.appcompat.widget.PopupMenu popupMenu = new androidx.appcompat.widget.PopupMenu(this, anchorView);
        popupMenu.getMenuInflater().inflate(R.menu.menu_header_options, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();

            if (id == R.id.menu_home) {
                Toast.makeText(this, "Chuyển hướng sang Trang chủ", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.menu_history) {
                Toast.makeText(this, "Mở Lịch sử đọc", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.menu_follow) {
                Toast.makeText(this, "Mở Truyện yêu thích", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.menu_downloads) {
                Toast.makeText(this, "Mở Truyện tải xuống", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.menu_profile) {
                Toast.makeText(this, "Mở Hồ sơ cá nhân", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.menu_logout) {
                Toast.makeText(this, "Đang đăng xuất tài khoản...", Toast.LENGTH_SHORT).show();
                return true;
            }

            return false;
        });
        popupMenu.show();
    }
}