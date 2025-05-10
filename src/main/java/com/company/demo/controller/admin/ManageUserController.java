package com.company.demo.controller.admin;

import com.company.demo.entity.User;
import com.company.demo.model.dto.UserDto;
import com.company.demo.security.CustomUserDetails;
import com.company.demo.service.ImageService;
import com.company.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Controller
public class ManageUserController {

    @Autowired
    private UserService userService;

    @Autowired
    private ImageService imageService;

    // Trang quản lý user
    @GetMapping("/admin/users")
    public String getUserManagePage(Model model) {
        // Lấy ảnh user hiện tại (admin đang login)
        User currentUser = ((CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUser();
        List<String> images = imageService.getListImageOfUser(currentUser.getId());
        model.addAttribute("images", images);

        // Lấy danh sách người dùng
        List<UserDto> users = userService.getAllUsers();
        model.addAttribute("users", users);

        return "admin/user/list";
    }

    // API tạo user
    @PostMapping("/api/admin/users")
    public ResponseEntity<?> createUser(@Valid @RequestBody UserDto req) {
        User user = userService.createUser(req);
        return ResponseEntity.ok(user);
    }

    // API cập nhật user
    @PutMapping("/api/admin/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable long id, @Valid @RequestBody UserDto req) {
        userService.updateUser(id, req);
        return ResponseEntity.ok("Cập nhật thành công");
    }

    // API xóa user
    @DeleteMapping("/api/admin/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok("Xóa thành công");
    }
}
