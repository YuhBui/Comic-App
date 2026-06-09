package com.yuhbui.comicapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.yuhbui.comicapp.R;
import com.yuhbui.comicapp.data.api.ApiClient;
import com.yuhbui.comicapp.data.model.Chapter;
import com.yuhbui.comicapp.data.model.Comic;
import com.yuhbui.comicapp.data.model.ComicDetailResponse;
import com.yuhbui.comicapp.data.model.Comment;
import com.yuhbui.comicapp.data.model.User; // Đã có sẵn model User phục vụ kiểm tra trạng thái
import com.yuhbui.comicapp.ui.adapters.ChapterAdapter;
import com.yuhbui.comicapp.ui.adapters.CommentAdapter;
import com.yuhbui.comicapp.utils.SharedPrefsManager;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ComicDetailActivity extends AppCompatActivity {

    private int currentComicId;
    private String currentComicTitle;
    private Integer targetParentCommentId = null;

    // --- BIẾN ĐIỀU KHIỂN CHI TIẾT TRUYỆN ---
    private ImageView imgComicCover;
    private TextView tvAuthor, tvGenre, tvRelease, tvStatus;
    private TextView tvViews, tvFavorites, tvRatingAverage, tvDescription;
    private RatingBar ratingBarUser;
    private Button btnStartReading;
    private TextView btnToggleFavorite;

    private boolean isCurrentlyFavorite = false;
    private boolean isUserBanned = false; // BỔ SUNG: Cờ kiểm tra trạng thái cấm chat của tài khoản

    // Thành phần xử lý danh sách chương truyện (MVVM)
    private ComicDetailViewModel viewModel;
    private ChapterAdapter adapter;
    private RecyclerView recyclerView;
    private TextView tvTitle;
    private List<Chapter> globalChapterList = new ArrayList<>();

    // Thành phần xử lý Bình luận
    private RecyclerView recyclerViewComments;
    private CommentAdapter commentAdapter;
    private EditText edtCommentInput;
    private Button btnSendComment;

    // --- KHAI BÁO CÁC THÀNH PHẦN CỦA HEADER DÙNG CHUNG ---
    private View layoutHeader;
    private ImageView headerMenu, headerSearch, headerNotification, headerAvatar;
    private TextView headerLogo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comic_detail);

        // 1. Ánh xạ toàn bộ các View từ XML
        tvTitle = findViewById(R.id.tvComicTitle);
        imgComicCover = findViewById(R.id.imgComicCover);
        tvAuthor = findViewById(R.id.tvComicAuthor);
        tvGenre = findViewById(R.id.tvComicGenre);
        tvRelease = findViewById(R.id.tvComicRelease);
        tvStatus = findViewById(R.id.tvComicStatus);
        tvViews = findViewById(R.id.tvComicViews);
        tvFavorites = findViewById(R.id.tvComicFavorites);
        tvRatingAverage = findViewById(R.id.tvComicRatingAverage);
        tvDescription = findViewById(R.id.tvComicDescription);
        ratingBarUser = findViewById(R.id.ratingBarUser);
        btnStartReading = findViewById(R.id.btnStartReading);
        btnToggleFavorite = findViewById(R.id.btnToggleFavorite);

        recyclerView = findViewById(R.id.recyclerViewChapters);
        recyclerViewComments = findViewById(R.id.recyclerViewComments);
        edtCommentInput = findViewById(R.id.edtCommentInput);
        btnSendComment = findViewById(R.id.btnSendComment);

        // --- ÁNH XẠ VÀ THIẾT LẬP HEADER CHUNG ---
        layoutHeader = findViewById(R.id.layoutHeader);
        headerMenu = layoutHeader.findViewById(R.id.headerMenu);
        headerLogo = layoutHeader.findViewById(R.id.headerLogo);
        headerSearch = layoutHeader.findViewById(R.id.headerSearch);
        headerNotification = layoutHeader.findViewById(R.id.headerNotification);
        headerAvatar = layoutHeader.findViewById(R.id.headerAvatar);

        // Đăng ký sự kiện Click xử lý chức năng cho Header trên màn hình Chi tiết
        headerMenu.setOnClickListener(v -> showHeaderPopupMenu(v));

        headerLogo.setOnClickListener(v -> {
            finish();
        });

        headerSearch.setOnClickListener(v -> {
            Intent intent = new Intent(ComicDetailActivity.this, MainActivity.class);
            intent.putExtra("OPEN_SEARCH", true);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        headerNotification.setOnClickListener(v -> Toast.makeText(this, "Mở thông báo", Toast.LENGTH_SHORT).show());
        headerAvatar.setOnClickListener(v -> Toast.makeText(this, "Mở thông tin tài khoản người dùng", Toast.LENGTH_SHORT).show());

        // 2. Cài đặt cấu trúc hiển thị danh sách Chương truyện
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChapterAdapter();
        recyclerView.setAdapter(adapter);

        // 3. Cài đặt cấu trúc hiển thị danh sách Bình luận
        recyclerViewComments.setLayoutManager(new LinearLayoutManager(this));
        commentAdapter = new CommentAdapter();
        recyclerViewComments.setAdapter(commentAdapter);

        // 3.1 Lắng nghe sự kiện người dùng bấm vào nút "Phản hồi"
        commentAdapter.setOnCommentClickListener(new CommentAdapter.OnCommentClickListener() {
            @Override
            public void onReplyClick(Comment parentComment) {
                if (isUserBanned) {
                    Toast.makeText(ComicDetailActivity.this, "Bạn hiện đang bị cấm chat!", Toast.LENGTH_SHORT).show();
                    return;
                }

                targetParentCommentId = parentComment.getCommentId();

                // Kiểm tra xem đây có phải phản hồi lồng cấp được gửi từ danh sách reply hay không
                if (parentComment.getUserDisplayName() != null) {
                    String tagText = "@" + parentComment.getUserDisplayName() + " ";
                    edtCommentInput.setText(tagText);
                    edtCommentInput.setSelection(tagText.length());
                    edtCommentInput.setHint("Đang trả lời...");
                } else {
                    // Phản hồi trực tiếp bình luận gốc lớn nhất
                    edtCommentInput.setText("");
                    edtCommentInput.setHint("Viết phản hồi...");
                }
                edtCommentInput.requestFocus();
            }
        });

        // 4. "Hứng" dữ liệu thông tin truyện sơ bộ từ Intent gửi sang
        if (getIntent() != null) {
            currentComicId = getIntent().getIntExtra("COMIC_ID", -1);
            currentComicTitle = getIntent().getStringExtra("COMIC_TITLE");
        }
        tvTitle.setText(currentComicTitle);

        int currentUserId = SharedPrefsManager.getUserId(this);

        // BỔ SUNG: Gọi hàm kiểm tra trạng thái tài khoản Banned từ server ngay khi vào trang
        if (currentUserId != -1) {
            checkCurrentUserBanStatus(currentUserId);
        }

        // 5. Kết nối luồng dữ liệu MVVM để cập nhật Chương truyện
        viewModel = new ViewModelProvider(this).get(ComicDetailViewModel.class);
        viewModel.getChapters().observe(this, new Observer<List<Chapter>>() {
            @Override
            public void onChanged(List<Chapter> chapters) {
                if (chapters != null) {
                    globalChapterList = chapters;
                    adapter.setChapters(chapters);
                    adapter.notifyDataSetChanged();
                }
            }
        });

        // 6. Ra lệnh tải dữ liệu tổng thể từ máy chủ nếu mã ID hợp lệ
        if (currentComicId != -1) {
            loadComicFullDetails(currentComicId, currentUserId != -1 ? currentUserId : null);
            viewModel.loadChapters(currentComicId);
            loadComments(currentComicId);
        }

        // 6.1 Bắt sự kiện click nút YÊU THÍCH (FOLLOW)
        btnToggleFavorite.setOnClickListener(v -> {
            if (currentUserId == -1) {
                Toast.makeText(this, "Vui lòng đăng nhập để thêm vào danh sách yêu thích!", Toast.LENGTH_SHORT).show();
                return;
            }
            ApiClient.getApiService().toggleFavorite(currentComicId, currentUserId).enqueue(new Callback<Boolean>() {
                @Override
                public void onResponse(Call<Boolean> call, Response<Boolean> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        isCurrentlyFavorite = response.body();
                        updateFavoriteButtonUI(isCurrentlyFavorite);
                        loadComicFullDetails(currentComicId, currentUserId);
                    }
                }
                @Override
                public void onFailure(Call<Boolean> call, Throwable t) {}
            });
        });

        // 7. Bắt sự kiện tương tác thay đổi số SAO ĐÁNH GIÁ (RatingBar)
        ratingBarUser.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                if (fromUser) {
                    if (currentUserId == -1) {
                        Toast.makeText(ComicDetailActivity.this, "Vui lòng đăng nhập để đánh giá!", Toast.LENGTH_SHORT).show();
                        ratingBarUser.setRating(0);
                        return;
                    }
                    sendRatingToServer(currentComicId, currentUserId, (int) rating);
                }
            }
        });

        // 8. Bắt sự kiện bấm nút BẮT ĐẦU ĐỌC TRUYỆN
        btnStartReading.setOnClickListener(v -> {
            if (globalChapterList != null && !globalChapterList.isEmpty()) {
                Chapter firstChapter = globalChapterList.get(globalChapterList.size() - 1);

                Intent intent = new Intent(ComicDetailActivity.this, ReaderActivity.class);
                intent.putExtra("CHAPTER_ID", firstChapter.getChapterId());
                intent.putExtra("COMIC_ID", currentComicId);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Truyện hiện chưa cập nhật chương nội dung nào!", Toast.LENGTH_SHORT).show();
            }
        });

        // 9. Bắt sự kiện bấm nút GỬI bình luận
        btnSendComment.setOnClickListener(v -> {
            // ĐÃ SỬA: Chặn cứng hành vi bấm gửi nếu tài khoản nằm trong danh sách đen
            if (isUserBanned) {
                Toast.makeText(this, "Bạn hiện đang bị cấm chat!", Toast.LENGTH_SHORT).show();
                return;
            }
            sendCommentToServer();
        });
    }

    // BỔ SUNG: Kiểm tra thời gian thực trạng thái Ban của User từ Database để khóa UI thích ứng
    private void checkCurrentUserBanStatus(int userId) {
        ApiClient.getApiService().getUserProfile(userId).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body();
                    if ("Banned".equalsIgnoreCase(user.getStatus())) {
                        isUserBanned = true;

                        // ĐÃ SỬA: Khóa cứng hộp text chat, đổi Hint cảnh báo và vô hiệu hóa nút gửi
                        edtCommentInput.setEnabled(false);
                        btnSendComment.setEnabled(false);
                        edtCommentInput.setHint("Bạn hiện đang bị cấm chat");

                        // Đăng ký thêm sự kiện click trực tiếp vào ô để nhắc nhở người dùng bằng Toast
                        edtCommentInput.setOnClickListener(v ->
                                Toast.makeText(ComicDetailActivity.this, "Tài khoản của bạn hiện đang bị cấm chat!", Toast.LENGTH_SHORT).show()
                        );
                    }
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {}
        });
    }

    private void updateFavoriteButtonUI(boolean isFav) {
        if (isFav) {
            btnToggleFavorite.setText("❤️ Đã yêu thích");
            btnToggleFavorite.setAlpha(0.6f);
        } else {
            btnToggleFavorite.setText("🤍 Yêu thích");
            btnToggleFavorite.setAlpha(1.0f);
        }
    }

    private void loadComicFullDetails(int comicId, Integer userId) {
        ApiClient.getApiService().getComicDetail(comicId, userId).enqueue(new Callback<ComicDetailResponse>() {
            @Override
            public void onResponse(Call<ComicDetailResponse> call, Response<ComicDetailResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ComicDetailResponse data = response.body();
                    Comic comic = data.getComic();

                    tvTitle.setText(comic.getTitle());
                    tvAuthor.setText("Tác giả: " + (comic.getAuthor() != null ? comic.getAuthor() : "Đang cập nhật"));
                    tvViews.setText("👁️ " + comic.getViewCount());
                    tvDescription.setText(comic.getDescription());
                    tvGenre.setText("Thể loại: " + data.getGenres());

                    // 1. Hiển thị số lượt yêu thích thực tế từ data gốc
                    tvFavorites.setText("❤️ " + String.valueOf(data.getFavoriteCount()));
                    tvRatingAverage.setText("⭐ " + comic.getRating() + "/5");

                    // 2. ĐÃ CẬP NHẬT: Ghép trạng thái đi kèm thông tin Chương mới nhất
                    String statusStr = (comic.getStatus() != null ? comic.getStatus() : "Đang tiến hành");
                    String latestChapStr = (data.getLatestChapterNumber() != null ? data.getLatestChapterNumber() : "Chưa có");
                    tvStatus.setText("Tình trạng: " + statusStr + " (" + latestChapStr + ")");

                    // ĐÃ SỬA: Thay thế vùng hiển thị năm phát hành thành Thời gian cập nhật chương mới (chỉ lấy Ngày/Tháng/Năm)
                    if (data.getTimeUpdated() != null && !data.getTimeUpdated().isEmpty()) {
                        tvRelease.setText("Cập nhật: " + formatToDateOnly(data.getTimeUpdated()));
                    } else {
                        tvRelease.setText("Cập nhật: Đang cập nhật");
                    }

                    isCurrentlyFavorite = data.isFavorite();
                    updateFavoriteButtonUI(isCurrentlyFavorite);

                    Glide.with(ComicDetailActivity.this)
                            .load(comic.getCoverImageUrl())
                            .placeholder(R.drawable.ic_launcher_background)
                            .into(imgComicCover);
                }
            }

            @Override
            public void onFailure(Call<ComicDetailResponse> call, Throwable t) {
                Log.e("YUH_TEST", "Lỗi tải chi tiết truyện: " + t.getMessage());
            }
        });
    }

    private String formatToDateOnly(String rawDateTime) {
        if (rawDateTime == null || rawDateTime.trim().isEmpty()) {
            return "Đang cập nhật";
        }
        try {
            String datePart = rawDateTime.contains("T") ? rawDateTime.split("T")[0] : rawDateTime.split(" ")[0];
            String[] parts = datePart.split("-");
            if (parts.length == 3) {
                return parts[2] + "/" + parts[1] + "/" + parts[0];
            }
            return datePart;
        } catch (Exception e) {
            return rawDateTime;
        }
    }

    private void sendRatingToServer(int comicId, int userId, int score) {
        ApiClient.getApiService().rateComic(comicId, userId, score).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ComicDetailActivity.this, "Cảm ơn bạn đã đánh giá " + score + " sao!", Toast.LENGTH_SHORT).show();
                    loadComicFullDetails(comicId, userId);
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Toast.makeText(ComicDetailActivity.this, "Đã ghi nhận đánh giá!", Toast.LENGTH_SHORT).show();
                loadComicFullDetails(comicId, userId);
            }
        });
    }

    private void loadComments(int comicId) {
        ApiClient.getApiService().getCommentsByComic(comicId).enqueue(new Callback<List<Comment>>() {
            @Override
            public void onResponse(Call<List<Comment>> call, Response<List<Comment>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    commentAdapter.setComments(response.body());
                    commentAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<List<Comment>> call, Throwable t) {
                Toast.makeText(ComicDetailActivity.this, "Không thể tải bình luận", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendCommentToServer() {
        String content = edtCommentInput.getText().toString().trim();
        int userId = SharedPrefsManager.getUserId(this);

        if (content.isEmpty()) {
            Toast.makeText(this, "Nội dung bình luận không được để trống!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (userId == -1) {
            Toast.makeText(this, "Vui lòng đăng nhập để bình luận!", Toast.LENGTH_SHORT).show();
            return;
        }

        Comment newComment = new Comment();
        newComment.setUserId(userId);
        newComment.setComicId(currentComicId);
        newComment.setContent(content);
        newComment.setParentCommentId(targetParentCommentId);

        ApiClient.getApiService().postComment(newComment).enqueue(new Callback<Comment>() {
            @Override
            public void onResponse(Call<Comment> call, Response<Comment> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ComicDetailActivity.this, "Đăng bình luận thành công!", Toast.LENGTH_SHORT).show();
                    edtCommentInput.setText("");
                    edtCommentInput.setHint("Viết bình luận của bạn...");

                    if (targetParentCommentId != null) {
                        commentAdapter.resetRepliesCache(targetParentCommentId);
                    }
                    targetParentCommentId = null;
                    loadComments(currentComicId);
                }
            }

            @Override
            public void onFailure(Call<Comment> call, Throwable t) {
                Toast.makeText(ComicDetailActivity.this, "Lỗi kết nối mạng!", Toast.LENGTH_SHORT).show();
            }
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