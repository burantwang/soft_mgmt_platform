package com.company.devplatform.module.auth.controller;

import com.company.devplatform.common.Result;
import com.company.devplatform.module.auth.dto.ChangePasswordDTO;
import com.company.devplatform.module.auth.dto.LoginDTO;
import com.company.devplatform.module.auth.service.AuthService;
import com.company.devplatform.module.auth.vo.LoginResultVO;
import com.company.devplatform.module.auth.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 登录 */
    @PostMapping("/login")
    public Result<LoginResultVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.ok(authService.login(dto));
    }

    /** 退出登录 */
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.ok();
    }

    /** 当前登录用户信息 */
    @GetMapping("/info")
    public Result<UserVO> info() {
        return Result.ok(authService.getCurrentUser());
    }

    /** 修改密码 */
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        authService.changePassword(dto);
        return Result.ok();
    }
}
