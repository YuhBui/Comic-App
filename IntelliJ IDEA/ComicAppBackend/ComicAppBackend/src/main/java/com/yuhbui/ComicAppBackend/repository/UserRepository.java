package com.yuhbui.ComicAppBackend.repository;

import com.yuhbui.ComicAppBackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    // Hàm này giúp tìm xem có user nào mang email này trong DB không
    Optional<User> findByEmail(String email);
}