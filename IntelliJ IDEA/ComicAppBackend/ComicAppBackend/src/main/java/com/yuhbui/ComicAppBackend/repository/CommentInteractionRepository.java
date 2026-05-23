package com.yuhbui.ComicAppBackend.repository;

import com.yuhbui.ComicAppBackend.entity.CommentInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CommentInteractionRepository extends JpaRepository<CommentInteraction, Integer> {
    // Tìm tương tác dựa trên cặp UserId và CommentId
    Optional<CommentInteraction> findByUserIdAndCommentId(Integer userId, Integer commentId);
}