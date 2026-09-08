package com.company.devplatform.module.auth.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.devplatform.common.Result;
import com.company.devplatform.module.auth.dto.UserCreateDTO;
import com.company.devplatform.module.auth.dto.UserUpdateDTO;
import com.company.devplatform.module.auth.service.UserService;
import com.company.devplatform.module.auth.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 用户管理接口
 */
@RestController
@RequestMapping("/api/system/users")
@RequiredArgsConstructor
@SaCheckPermission("system:manage")
public class UserController {

    private final UserService userService;

    /** 分页查询用户 */
    @GetMapping
    public Result<Page<UserVO>> page(@RequestParam(required = false) String keyword,
                                     @RequestParam(required = false) Integer status,
                                     @RequestParam(required = false) Long groupId,
                                     @RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "10") int size) {
        return Result.ok(userService.page(keyword, status, groupId, page, Math.min(Math.max(size, 1), 100)));
    }

    /** 用户详情 */
    @GetMapping("/{id}")
    public Result<UserVO> detail(@PathVariable Long id) {
        return Result.ok(userService.getById(id));
    }

    /** 新增用户 */
    @PostMapping
    public Result<Void> create(@Valid @RequestBody UserCreateDTO dto) {
        userService.create(dto);
        return Result.ok();
    }

    /** 编辑用户 */
    @PutMapping
    public Result<Void> update(@Valid @RequestBody UserUpdateDTO dto) {
        userService.update(dto);
        return Result.ok();
    }

    /** 删除用户 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.ok();
    }

    /** 重置密码 */
    @PutMapping("/{id}/password")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String password = body == null ? null : body.get("password");
        userService.resetPassword(id, password);
        return Result.ok();
    }

    /** 启用/禁用 */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        userService.updateStatus(id, body.get("status"));
        return Result.ok();
    }
}
