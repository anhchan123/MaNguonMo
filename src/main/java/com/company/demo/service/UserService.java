package com.company.demo.service;

import com.company.demo.entity.User;
import com.company.demo.model.dto.UserDto;
import com.company.demo.model.request.ChangePasswordReq;
import com.company.demo.model.request.CreateUserReq;
import com.company.demo.model.request.UpdateProfileReq;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public interface UserService {
    User createUser(CreateUserReq req);

    void changePassword(User user, ChangePasswordReq req);

    User updateProfile(User user, UpdateProfileReq req);

    void sendVerificationEmail(User user);

    boolean verifyAccount(String token);

    // 🆕 Gửi email đặt lại mật khẩu
    void sendResetPasswordEmail(String email);

    // 🆕 Xác minh token đặt lại mật khẩu
    boolean verifyResetPasswordToken(String token);

    // 🆕 Đặt lại mật khẩu mới
    void resetPassword(String token, String newPassword);

    public List<UserDto> getAllUsers();

    public User createUser(UserDto req);

    public void updateUser(long id, UserDto req);

    public void deleteUser(long id);

}

