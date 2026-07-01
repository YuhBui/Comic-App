package com.yuhbui.ComicAppBackend.repository;

import com.yuhbui.ComicAppBackend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);

    Optional<User> findByDisplayName(String displayName);

    // Hàm phân trang tìm kiếm nâng cao theo Keyword và Role giống phía User
    @Query("SELECT u FROM User u WHERE 1=1 " +
            "AND (:keyword IS NULL OR LOWER(u.displayName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:role IS NULL OR u.role = :role)")
    Page<User> findAllAdminWithPagination(
            @Param("keyword") String keyword,
            @Param("role") String role,
            Pageable pageable);
}