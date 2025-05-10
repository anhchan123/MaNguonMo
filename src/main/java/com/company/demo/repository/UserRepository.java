package com.company.demo.repository;

import com.company.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
    
    User findByVerificationToken(String token);
    
    Optional<User> findByResetToken(String token);
}
