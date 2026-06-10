package com.yuhbui.comicapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;              // THÊM: Thư viện điều hướng Drawer
import androidx.drawerlayout.widget.DrawerLayout;    // THÊM: Thư viện DrawerLayout
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
import com.yuhbui.comicapp.data.model.User;
import com.yuhbui.comicapp.ui.adapters.ChapterAdapter;
import com.yuhbui.comicapp.ui.adapters.CommentAdapter;
import com.yuhbui.comicapp.utils.HeaderUtils;          // THÊM: Nhúng lớp tiện ích Header dùng chung
import com.yuhbui.comicapp.utils.MenuUtils;            // THÊM: Nhúng lớp tiện ích Menu dùng chung
import com.yuhbui.comicapp.utils.SharedPrefsManager;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ComicDetailActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout; // THÊM: Khai báo biến quản lý DrawerLayout màn hình chi tiết

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
    private boolean isUserBanned = false;

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
    private TextView headerLogo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comic_detail);

        // 1. Ánh xạ toàn bộ các View từ XML chi tiết truyện
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

        // --- 2. ÁNH XẠ VÀ CẤU HÌNH ĐỒNG BỘ LAYOUT HEADER & MENU TRƯỢT MỚI ---
        drawerLayout = findViewById(R.id.drawerLayout); // Hãy chắc chắn đã bọc root XML bằng DrawerLayout
        layoutHeader = findViewById(R.id.layoutHeader);
        headerLogo = layoutHeader.findViewById(R.id.headerLogo);

        // Khởi tạo các tính năng lõi (Menu, Chuông, Avatar, Ô nhập Tìm kiếm toàn cục nhảy về trang chủ)
        HeaderUtils.initHeader(this, layoutHeader, drawerLayout);

        // Thiết lập chức năng điều hướng cho các mục bấm bên trong thanh Side Menu trượt
        MenuUtils.setupSideMenu(this, drawerLayout, layoutHeader.findViewById(R.id.headerMenu));

        headerLogo.setOnClickListener(v -> {
            finish();
        });

        // Ưu tiên đóng Menu trượt nếu người dùng nhấn nút Back hệ thống
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
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

        // 3. Cài đặt cấu trúc hiển thị danh sách Chương truyện
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChapterAdapter();
        recyclerView.setAdapter(adapter);

        // 4. Cài đặt cấu trúc hiển thị danh sách Bình luận
        recyclerViewComments.setLayoutManager(new LinearLayoutManager(this));
        commentAdapter = new CommentAdapter();
        recyclerViewComments.setAdapter(commentAdapter);

        // 4.1 Lắng nghe sự kiện người dùng bấm vào nút "Phản hồi"
        commentAdapter.setOnCommentClickListener(new CommentAdapter.OnCommentClickListener() {
            @Override
            public void onReplyClick(Comment parentComment) {
                if (isUserBanned) {
                    Toast.makeText(ComicDetailActivity.this, "Bạn hiện đang bị cấm chat!", Toast.LENGTH_SHORT).show();
                    return;
                }

                targetParentCommentId = parentComment.getCommentId();

                if (parentComment.getUserDisplayName() != null) {
                    String tagText = "@" + parentComment.getUserDisplayName() + " ";
                    edtCommentInput.setText(tagText);
                    edtCommentInput.setSelection(tagText.length());
                    edtCommentInput.setHint("Đang trả lời...");
                } else {
                    edtCommentInput.setText("");
                    edtCommentInput.setHint("Viết phản hồi...");
                }
                edtCommentInput.requestFocus();
            }
        });

        // 5. "Hứng" dữ liệu thông tin truyện sơ bộ từ Intent gửi sang
        if (getIntent() != null) {
            currentComicId = getIntent().getIntExtra("COMIC_ID", -1);
            currentComicTitle = getIntent().getStringExtra("COMIC_TITLE");
        }
        tvTitle.setText(currentComicTitle);

        int currentUserId = SharedPrefsManager.getUserId(this);

        if (currentUserId != -1) {
            checkCurrentUserBanStatus(currentUserId);
        }

        // 6. Kết nối luồng dữ liệu MVVM để cập nhật Chương truyện
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

        // 7. Ra lệnh tải dữ liệu tổng thể từ máy chủ nếu mã ID hợp lệ
        if (currentComicId != -1) {
            loadComicFullDetails(currentComicId, currentUserId != -1 ? currentUserId : null);
            viewModel.loadChapters(currentComicId);
            loadComments(currentComicId);
        }

        // 7.1 Bắt sự kiện click nút YÊU THÍCH (FOLLOW)
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

        // 8. Bắt sự kiện tương tác thay đổi số SAO ĐÁNH GIÁ (RatingBar)
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

        // 9. Bắt sự kiện bấm nút BẮT ĐẦU ĐỌC TRUYỆN
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

        // 10. Bắt sự kiện bấm nút GỬI bình luận
        btnSendComment.setOnClickListener(v -> {
            if (isUserBanned) {
                Toast.makeText(this, "Bạn hiện đang bị cấm chat!", Toast.LENGTH_SHORT).show();
                return;
            }
            sendCommentToServer();
        });
    }

    // THÊM: Đồng bộ và làm tươi Avatar, Số thông báo mới mỗi khi người dùng quay lại hoặc vào màn hình này
    @Override
    protected void onResume() {
        super.onResume();
        if (layoutHeader != null) {
            HeaderUtils.loadHeaderAvatar(this, layoutHeader.findViewById(R.id.headerAvatar));
            HeaderUtils.loadUnreadNotificationCount(this, layoutHeader.findViewById(R.id.tvNotificationBadge));
        }
    }

    private void checkCurrentUserBanStatus(int userId) {
        ApiClient.getApiService().getUserProfile(userId).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body();
                    if ("Banned".equalsIgnoreCase(user.getStatus())) {
                        isUserBanned = true;

                        edtCommentInput.setEnabled(false);
                        btnSendComment.setEnabled(false);
                        edtCommentInput.setHint("Bạn hiện đang bị cấm chat");

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

                    tvFavorites.setText("❤️ " + String.valueOf(data.getFavoriteCount()));
                    tvRatingAverage.setText("⭐ " + comic.getRating() + "/5");

                    String statusStr = (comic.getStatus() != null ? comic.getStatus() : "Đang tiến hành");
                    String latestChapStr = (data.getLatestChapterNumber() != null ? data.getLatestChapterNumber() : "Chưa có");
                    tvStatus.setText("Tình trạng: " + statusStr + " (" + latestChapStr + ")");

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
}