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
import com.yuhbui.comicapp.data.model.ComicDetailResponse; // 👉 Import Model DTO mới nhận từ server
import com.yuhbui.comicapp.data.model.Comment;
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
    private TextView btnToggleFavorite; // 👉 Thêm biến nút Yêu thích toàn cục

    private boolean isCurrentlyFavorite = false; // Lưu trạng thái yêu thích cục bộ

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
        btnToggleFavorite = findViewById(R.id.btnToggleFavorite); // 👉 Ánh xạ nút yêu thích từ XML

        recyclerView = findViewById(R.id.recyclerViewChapters);
        recyclerViewComments = findViewById(R.id.recyclerViewComments);
        edtCommentInput = findViewById(R.id.edtCommentInput);
        btnSendComment = findViewById(R.id.btnSendComment);

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

        // 4. "Hứng" dữ liệu thông tin truyện sơ bộ từ Intent gửi sang
        if (getIntent() != null) {
            currentComicId = getIntent().getIntExtra("COMIC_ID", -1);
            currentComicTitle = getIntent().getStringExtra("COMIC_TITLE");
        }
        tvTitle.setText(currentComicTitle);

        int currentUserId = SharedPrefsManager.getUserId(this);

        // 5. Kết nối luồng dữ liệu MVVM để cập nhật Chương truyện
        viewModel = new ViewModelProvider(this).get(ComicDetailViewModel.class);
        viewModel.getChapters().observe(this, new Observer<List<Chapter>>() {
            @Override
            public void onChanged(List<Chapter> chapters) {
                if (chapters != null) {
                    globalChapterList = chapters;
                    adapter.setChapters(chapters);
                }
            }
        });

        // 6. Ra lệnh tải dữ liệu tổng thể từ máy chủ nếu mã ID hợp lệ
        if (currentComicId != -1) {
            // 👉 SỬA HÀM GỌI CHI TIẾT: Truyền thêm cả UserId hiện tại (nếu có) để check trạng thái Thích thương thích
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
                        // Tải lại chi tiết để số lượng đếm ❤️ cập nhật tự động chuẩn từ Database
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
                // Đổi thành index = 0 nếu danh sách của bạn hiển thị Chương 1 lên trước
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
        btnSendComment.setOnClickListener(v -> sendCommentToServer());
    }

    // Hàm đổi giao diện nút Yêu thích
    private void updateFavoriteButtonUI(boolean isFav) {
        if (isFav) {
            btnToggleFavorite.setText("❤️ Đã yêu thích");
            btnToggleFavorite.setAlpha(0.6f);
        } else {
            btnToggleFavorite.setText("🤍 Yêu thích");
            btnToggleFavorite.setAlpha(1.0f);
        }
    }

    // --- SỬA LẠI HOÀN TOÀN HÀM NÀY: Gọi API DTO để lấy thông tin thật kết nối bảng ---
    private void loadComicFullDetails(int comicId, Integer userId) {
        ApiClient.getApiService().getComicDetail(comicId, userId).enqueue(new Callback<ComicDetailResponse>() {
            @Override
            public void onResponse(Call<ComicDetailResponse> call, Response<ComicDetailResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ComicDetailResponse data = response.body();
                    Comic comic = data.getComic();

                    // Đổ dữ liệu text thật từ MySQL sang giao diện
                    tvTitle.setText(comic.getTitle());
                    tvAuthor.setText("Tác giả: " + (comic.getAuthor() != null ? comic.getAuthor() : "Đang cập nhật"));
                    tvStatus.setText("Tình trạng: " + (comic.getStatus() != null ? comic.getStatus() : "Đang tiến hành"));
                    tvViews.setText("👁️ " + comic.getViewCount());
                    tvDescription.setText(comic.getDescription());

                    // LẤY DỮ LIỆU ĐÃ KẾT NỐI BẢNG THẬT TRÊN MYSQL
                    tvGenre.setText("Thể loại: " + data.getGenres()); // Lấy chuỗi thể loại từ bảng Comic_Categories
                    tvFavorites.setText("❤️ " + data.getFavoriteCount()); // Đếm tổng số dòng thật trong bảng Follows
                    tvRatingAverage.setText("⭐ " + comic.getRating() + "/5"); // Điểm Rating FLOAT từ bảng Comics

                    // Ép định dạng năm phát hành từ CreatedAt của MySQL
                    tvRelease.setText("Phát hành: " + (comic.getCreatedAt() != null && comic.getCreatedAt().length() >= 4 ? comic.getCreatedAt().substring(0, 4) : "2026"));

                    // Cập nhật trạng thái nút Trái tim yêu thích
                    isCurrentlyFavorite = data.isFavorite();
                    updateFavoriteButtonUI(isCurrentlyFavorite);

                    // Load ảnh bìa truyện thật từ DB
                    Glide.with(ComicDetailActivity.this)
                            .load(comic.getCoverImageUrl()) // Dùng CoverImageUrl đã đồng bộ
                            .placeholder(R.drawable.ic_launcher_background)
                            .into(imgComicCover);
                }
            }

            @Override
            public void onFailure(Call<ComicDetailResponse> call, Throwable t) {
                Log.e("YUH_TEST", "Lỗi load kết nối database chi tiết truyện: " + t.getMessage());
            }
        });
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