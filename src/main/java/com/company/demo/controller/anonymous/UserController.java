package com.company.demo.controller.anonymous;

import com.company.demo.entity.User;
import com.company.demo.exception.BadRequestException;
import com.company.demo.model.dto.OrderDetailDto;
import com.company.demo.model.dto.OrderInfoDto;
import com.company.demo.model.mapper.UserMapper;
import com.company.demo.model.request.ChangePasswordReq;
import com.company.demo.model.request.CreateUserReq;
import com.company.demo.model.request.LoginReq;
import com.company.demo.model.request.UpdateProfileReq;
import com.company.demo.security.CustomUserDetails;
import com.company.demo.security.JwtTokenUtil;
import com.company.demo.service.OrderService;
import com.company.demo.service.UserService;
import lombok.Getter;
import org.dom4j.rule.Mode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import java.security.Principal;
import java.util.List;

import static com.company.demo.config.Constant.*;

@Controller
public class UserController {
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private UserService userService;

    @Autowired
    private OrderService orderService;

    @PostMapping("/api/register")
    public ResponseEntity<?> register(@Valid @RequestBody CreateUserReq req,
                                      HttpServletResponse response) {
        // Create user
        User result = userService.createUser(req);

        // Gen token
        UserDetails principal = new CustomUserDetails(result);
        String token = jwtTokenUtil.generateToken(principal);

        // Add token to cookie to login
        Cookie cookie = new Cookie("JWT_TOKEN", token);
        cookie.setMaxAge(MAX_AGE_COOKIE);
        cookie.setPath("/");
        response.addCookie(cookie);

        return ResponseEntity.ok(UserMapper.toUserDto(result));
    }

    @PostMapping("/api/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginReq req, HttpServletResponse response) {
        // Authenticate
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            req.getEmail(),
                            req.getPassword()));

            // Gen token
            String token = jwtTokenUtil.generateToken((CustomUserDetails) authentication.getPrincipal());

            // Add token to cookie to login
            Cookie cookie = new Cookie("JWT_TOKEN", token);
            cookie.setMaxAge(MAX_AGE_COOKIE);
            cookie.setPath("/");
            response.addCookie(cookie);

            return ResponseEntity
                    .ok(UserMapper.toUserDto(((CustomUserDetails) authentication.getPrincipal()).getUser()));
        } catch (Exception ex) {
            throw new BadRequestException("Email hoặc mật khẩu không chính xác");
        }
    }

    @GetMapping("/tai-khoan")
    public String getProfilePage(@RequestParam(name = "token", required = false) String token,
                                 Model model) {
        if (token != null) {
            boolean isVerified = userService.verifyAccount(token);
            if (isVerified) {
                model.addAttribute("message", "✅ Tài khoản đã được xác thực thành công. Vui lòng tải lại trang!");
            } else {
                model.addAttribute("message", "⚠️ Mã xác thực không hợp lệ hoặc đã được sử dụng.");
            }
        }
        return "account/account"; // Trả về trang tài khoản
    }

    @GetMapping("/quen-mat-khau")
    public String forgotPasswordPage() {
        return "account/forgot_password";
    }

    // Xử lý yêu cầu quên mật khẩu
    @PostMapping("/quen-mat-khau")
    public String forgotPassword(@RequestParam("email") String email, Model model) {
        try {
            userService.sendResetPasswordEmail(email);
            model.addAttribute("message", "✅ Email đặt lại mật khẩu đã được gửi!");
        } catch (Exception e) {
            model.addAttribute("error", "⚠️ Không tìm thấy tài khoản với email này.");
        }
        return "account/forgot_password";
    }

    @GetMapping("/dat-lai-mat-khau")
    public String resetPasswordPage(@RequestParam("token") String token, Model model) {
        boolean isValid = userService.verifyResetPasswordToken(token);
        if (!isValid) {
            model.addAttribute("error", "⚠️ Token không hợp lệ hoặc đã hết hạn.");
            return "account/forgot_password";
        }
        model.addAttribute("token", token);
        return "account/reset_password";
    }

    // Xử lý đặt lại mật khẩu
    @PostMapping("/dat-lai-mat-khau")
    public String resetPassword(@RequestParam("token") String token,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            Model model) {
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "⚠️ Mật khẩu xác nhận không khớp.");
            model.addAttribute("token", token);
            return "account/reset_password";
        }

        try {
            userService.resetPassword(token, newPassword);
            model.addAttribute("message", "✅ Đặt lại mật khẩu thành công! Vui lòng đăng nhập.");
            return "redirect:/";
        } catch (BadRequestException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("token", token);
            return "account/reset_password";
        }
    }

    @GetMapping("/tai-khoan/lich-su-giao-dich")
    public String getOrderHistoryPage(Model model) {
        // Get list order pending
        User user = ((CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .getUser();
        List<OrderInfoDto> orders = orderService.getListOrderOfPersonByStatus(ORDER_STATUS, user.getId());
        model.addAttribute("orders", orders);

        return "account/order_history";
    }

    @GetMapping("/api/get-order-list")
    public ResponseEntity<?> getListOrderByStatus(@RequestParam int status) {
        // Validate status
        if (!LIST_ORDER_STATUS.contains(status)) {
            throw new BadRequestException("Trạng thái đơn hàng không hợp lệ");
        }

        User user = ((CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .getUser();
        List<OrderInfoDto> orders = orderService.getListOrderOfPersonByStatus(status, user.getId());

        return ResponseEntity.ok(orders);
    }

    @GetMapping("/tai-khoan/lich-su-giao-dich/{id}")
    public String getDetailOrderPage(Model model, @PathVariable int id) {
        User user = ((CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .getUser();

        OrderDetailDto order = orderService.userGetDetailById(id, user.getId());
        if (order == null) {
            return "error/404";
        }
        model.addAttribute("order", order);

        if (order.getStatus() == ORDER_STATUS) {
            model.addAttribute("canCancel", true);
        } else {
            model.addAttribute("canCancel", false);
        }

        return "account/order_detail";
    }

    @PostMapping("/api/cancel-order/{id}")
    public ResponseEntity<?> cancelOrder(@PathVariable long id) {
        User user = ((CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .getUser();

        orderService.userCancelOrder(id, user.getId());

        return ResponseEntity.ok("Hủy đơn hàng thành công");
    }

    @PostMapping("/api/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordReq req) {
        User user = ((CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .getUser();

        userService.changePassword(user, req);
        return ResponseEntity.ok("Đổi mật khẩu thành công");
    }

    @PostMapping("/api/update-profile")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody UpdateProfileReq req) {
        User user = ((CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .getUser();

        user = userService.updateProfile(user, req);
        UserDetails principal = new CustomUserDetails(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null,
                principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        return ResponseEntity.ok("Cập nhật profile thành công");
    }

    @PostMapping("/verify-account")
    public ResponseEntity<?> verifyAccount(Principal principal) {
        User user = ((CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .getUser();

        if (user.isVerify()) {
            return ResponseEntity.badRequest().body("Tài khoản đã được xác thực.");
        }

        userService.sendVerificationEmail(user);
        return ResponseEntity.ok("OK");
    }

    // ✅ API xác nhận tài khoản qua email
    @GetMapping("/confirm-account")
    public String confirmAccount(@RequestParam("token") String token) {
        boolean isVerified = userService.verifyAccount(token);

        if (isVerified) {
            return "redirect:/tai-khoan?success=true"; // Chuyển hướng về trang tài khoản
        } else {
            return "redirect:/tai-khoan?error=invalid_token";
        }
    }
}
