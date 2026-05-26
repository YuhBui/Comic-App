package com.yuhbui.comicapp.data.api;

import com.yuhbui.comicapp.data.model.Category;
import com.yuhbui.comicapp.data.model.ChapterImage;
import com.yuhbui.comicapp.data.model.Comic;
import com.yuhbui.comicapp.data.model.Chapter;
import com.yuhbui.comicapp.data.model.ComicDetailResponse;
import com.yuhbui.comicapp.data.model.Comment;
import com.yuhbui.comicapp.data.model.LoginRequest;
import com.yuhbui.comicapp.data.model.ReadingHistory;
import com.yuhbui.comicapp.data.model.User;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path; // Import thêm Path
import retrofit2.http.Query;

public interface ApiService {
    @GET("/api/comics")
    Call<List<Comic>> getAllComics();

    @GET("/api/comics/{comicId}/chapters")
    Call<List<Chapter>> getChaptersByComicId(@Path("comicId") int comicId);

    @GET("/api/chapters/{chapterId}/images")
    Call<List<ChapterImage>> getImagesByChapterId(@Path("chapterId") int chapterId);

    @POST("/api/users/login")
    Call<User> login(@Body LoginRequest request);

    @POST("/api/users/register")
    Call<User> register(@Body User user);

    @POST("/api/history/save")
    Call<ReadingHistory> saveReadingHistory(@Body ReadingHistory history);

    @GET("/api/comments/comic/{comicId}")
    Call<List<Comment>> getCommentsByComic(@Path("comicId") int comicId);

    @POST("/api/comments/post")
    Call<Comment> postComment(@Body Comment comment);

    @POST("/api/comments/{commentId}/interact")
    Call<Comment> interactWithComment(
            @Path("commentId") int commentId,
            @Query("userId") int userId,
            @Query("type") int type
    );

    @GET("/api/comments/{parentCommentId}/replies")
    Call<List<Comment>> getRepliesByParentId(@Path("parentCommentId") int parentCommentId);

    @POST("/api/comments/{commentId}/report")
    Call<String> reportComment(
            @Path("commentId") int commentId,
            @Query("userId") int userId,
            @Query("reason") String reason
    );

    // Lấy bình luận của riêng chapter đó
    @GET("/api/comments/chapter/{chapterId}")
    Call<List<Comment>> getCommentsByChapter(@Path("chapterId") int chapterId);

    // API xóa bình luận chính chủ
    @PUT("/api/comments/{commentId}/delete")
    Call<Comment> deleteComment(
            @Path("commentId") int commentId,
            @Query("userId") int userId
    );

    // API Người dùng gửi đánh giá sao (1-5) cho truyện
    // Server Spring Boot đang trả về chuỗi text, nên ta hứng bằng Call<String>
    @POST("/api/comics/{comicId}/rate")
    Call<String> rateComic(
            @Path("comicId") int comicId,
            @Query("userId") int userId,
            @Query("score") int score
    );

    // Lấy chi tiết truyện kèm thể loại và trạng thái thích của User
    @GET("/api/comics/{comicId}")
    Call<ComicDetailResponse> getComicDetail(
            @Path("comicId") int comicId,
            @Query("userId") Integer userId
    );

    // Bấm nút Yêu thích / Bỏ yêu thích
    @POST("/api/comics/{comicId}/toggle-favorite")
    Call<Boolean> toggleFavorite(
            @Path("comicId") int comicId,
            @Query("userId") int userId
    );

    // API lấy truyện mới cập nhật kèm phân trang số
    @GET("/api/comics/home/updates")
    Call<List<Comic>> getHomeUpdates(@Query("page") int page);

    // API lấy danh sách truyện đề cử hot (top rating)
    @GET("/api/comics/home/recommended")
    Call<List<Comic>> getRecommendedComics();

    // Lấy danh sách toàn bộ thể loại để làm thanh lọc
    @GET("/api/categories")
    Call<List<Category>> getCategories();

    // Lấy danh sách truyện đã được lọc theo mã thể loại
    @GET("/api/comics/filter")
    Call<List<Comic>> getComicsByCategory(@Query("catId") int catId);

    // API lấy bảng xếp hạng top 10
    @GET("/api/comics/home/ranking")
    Call<List<Comic>> getTopRanking(@Query("type") String type);

    @GET("/api/history/user/{userId}")
    Call<List<Comic>> getReadingHistoryByUserId(@Path("userId") int userId);

    // Lấy danh sách truyện yêu thích của người dùng (kèm đầy đủ thông số)
    @GET("/api/comics/favorites/{userId}")
    Call<List<Comic>> getFavoriteComics(@Path("userId") int userId);
}