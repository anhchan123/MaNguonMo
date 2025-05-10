package com.company.demo.service.impl;

import com.company.demo.entity.User;
import com.company.demo.exception.BadRequestException;
import com.company.demo.exception.DuplicateRecordException;
import com.company.demo.model.dto.UserDto;
import com.company.demo.model.mapper.UserMapper;
import com.company.demo.model.request.ChangePasswordReq;
import com.company.demo.model.request.CreateUserReq;
import com.company.demo.model.request.UpdateProfileReq;
import com.company.demo.repository.UserRepository;
import com.company.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JavaMailSender mailSender; // ✅ Thêm JavaMailSender để gửi email

    @Override
    public User createUser(CreateUserReq req) {
        // Check email exist
        User user = userRepository.findByEmail(req.getEmail());
        if (user != null) {
            throw new DuplicateRecordException("Email đã tồn tại trong hệ thống. Vui lòng sử dụng email khác.");
        }

        user = UserMapper.toUser(req);
        user.setVerify(false); // ✅ Mặc định là chưa xác thực
        user.setVerificationToken(UUID.randomUUID().toString()); // ✅ Tạo token xác thực
        userRepository.save(user);

        sendVerificationEmail(user); // ✅ Gửi email xác thực khi đăng ký

        return user;
    }

    @Override
    public void changePassword(User user, ChangePasswordReq req) {
        // Validate password
        if (!BCrypt.checkpw(req.getOldPassword(), user.getPassword())) {
            throw new BadRequestException("Mật khẩu cũ không chính xác");
        }

        String hash = BCrypt.hashpw(req.getNewPassword(), BCrypt.gensalt(12));
        user.setPassword(hash);
        userRepository.save(user);
    }

    @Override
    public User updateProfile(User user, UpdateProfileReq req) {
        user.setAddress(req.getAddress());
        user.setPhone(req.getPhone());
        user.setFullName(req.getFullName());

        return userRepository.save(user);
    }

    @Override
    public void sendVerificationEmail(User user) {
        String token = UUID.randomUUID().toString();
        user.setVerificationToken(token);
        userRepository.save(user);

        // ✅ Link xác thực tài khoản
        String verifyLink = "http://localhost:8080/tai-khoan?token=" + token;

        // ✅ Tiêu đề email
        String subject = "Xác thực tài khoản của bạn";

        // ✅ Nội dung email với HTML
        String content = "<!DOCTYPE html>" +
                "<html lang='vi'>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<title>Xác thực tài khoản</title>" +
                "<style>" +
                "   body { font-family: Arial, sans-serif; background-color: #f4f4f4; text-align: center; padding: 20px; }"
                +
                "   .email-container { background-color: #ffffff; padding: 20px; border-radius: 8px; max-width: 500px; margin: auto; box-shadow: 0px 0px 10px rgba(0, 0, 0, 0.1); }"
                +
                "   h2 { color: #333; }" +
                "   p { color: #666; font-size: 16px; line-height: 1.5; }" +
                "   .btn { display: inline-block; background-color: #28a745; color: white; padding: 12px 20px; text-decoration: none; border-radius: 5px; font-size: 16px; font-weight: bold; margin-top: 20px; }"
                +
                "   .btn:hover { background-color: #218838; }" +
                "   .footer { margin-top: 20px; font-size: 14px; color: #888; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='email-container'>" +
                "   <h2>Chào " + user.getFullName() + ",</h2>" +
                "   <p>Bạn vừa đăng ký tài khoản trên hệ thống của chúng tôi. Để hoàn tất quá trình đăng ký, vui lòng nhấp vào nút bên dưới để xác thực tài khoản:</p>"
                +
                "   <a href='" + verifyLink + "' class='btn'>Xác thực tài khoản</a>" +
                "   <p>Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email này.</p>" +
                "   <div class='footer'>Cảm ơn bạn đã đăng ký!<br> Đội ngũ hỗ trợ</div>" +
                "</div>" +
                "</body>" +
                "</html>";

        sendEmail(user.getEmail(), subject, content);
    }

    @Override
    public boolean verifyAccount(String token) {
        User user = userRepository.findByVerificationToken(token);
        if (user == null) {
            return false;
        }
        user.setVerify(true);
        user.setVerificationToken(null); // ✅ Xóa token sau khi xác thực thành công
        userRepository.save(user);
        return true;
    }

    @Override
    public void sendResetPasswordEmail(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new BadRequestException("Không tìm thấy tài khoản với email này.");
        }
        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        userRepository.save(user);
        String resetLink = "http://localhost:8080/dat-lai-mat-khau?token=" + token;
        String subject = "Đặt lại mật khẩu";
        String content = "<html><body><h2>Chào " + user.getFullName() + ",</h2>"
                + "<p>Nhấp vào nút bên dưới để đặt lại mật khẩu:</p>"
                + "<a href='" + resetLink
                + "' style='background-color:#dc3545;color:white;padding:10px 15px;text-decoration:none;border-radius:5px;'>Đặt lại mật khẩu</a>"
                + "<p>Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.</p></body></html>";
        sendEmail(user.getEmail(), subject, content);
    }

    @Override
    public boolean verifyResetPasswordToken(String token) {
        return userRepository.findByResetToken(token).isPresent();
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new BadRequestException("Token không hợp lệ hoặc đã hết hạn."));
        user.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt(12)));
        user.setResetToken(null);
        userRepository.save(user);
    }

    private void sendEmail(String to, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true); // ✅ Bật HTML

            mailSender.send(message);
            System.out.println("Email xác thực đã được gửi thành công đến: " + to);
        } catch (MessagingException e) {
            throw new RuntimeException("Lỗi khi gửi email: " + e.getMessage());
        }
    }

    @Override
    public List<UserDto> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserMapper::toUserDto)
                .collect(Collectors.toList());
    }

    @Override
    public User createUser(UserDto req) {
        if (userRepository.findByEmail(req.getEmail()) != null) {
            throw new DuplicateRecordException("Email đã tồn tại.");
        }

        User user = new User();
        user.setFullName(req.getFullName());
        user.setEmail(req.getEmail());
        user.setPassword(BCrypt.hashpw("helloworld123", BCrypt.gensalt())); // Đặt mật khẩu mặc định
        user.setPhone(req.getPhone());
        user.setAddress(req.getAddress());
        user.setRoles(req.getRoles());
        user.setStatus(true);
        user.setVerify(true);
        user.setCreatedAt(new java.sql.Timestamp(System.currentTimeMillis()));

        return userRepository.save(user);
    }

    @Override
    public void updateUser(long id, UserDto req) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy người dùng"));

        user.setFullName(req.getFullName());
        user.setPhone(req.getPhone());
        user.setAddress(req.getAddress());
        user.setRoles(req.getRoles());

        userRepository.save(user);
    }

    @Override
    public void deleteUser(long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy người dùng"));
        userRepository.delete(user);
    }
}
