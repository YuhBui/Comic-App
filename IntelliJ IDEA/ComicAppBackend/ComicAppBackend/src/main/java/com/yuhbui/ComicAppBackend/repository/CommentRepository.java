package com.yuhbui.ComicAppBackend.repository;

import com.yuhbui.ComicAppBackend.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Integer> {

    // Lấy danh sách bình luận gốc của một bộ truyện
    List<Comment> findByComicIdAndParentCommentIdIsNullOrderByCreatedAtDesc(Integer comicId);

    // Lấy danh sách bình luận gốc của một chương truyện cụ thể
    List<Comment> findByChapterIdAndParentCommentIdIsNullOrderByCreatedAtDesc(Integer chapterId);

    // Lấy các câu trả lời (replies) của một bình luận cha
    List<Comment> findByParentCommentIdOrderByCreatedAtAsc(Integer parentCommentId);
}