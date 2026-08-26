package com.company.devplatform.config;

import cn.dev33.satoken.stp.StpInterface;
import com.company.devplatform.module.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Sa-Token 权限数据加载实现
 * <p>登录成功后每次鉴权都会调用，从数据库实时加载角色与权限码。
 * 超管(超级管理员角色)返回通配权限码 "*"，表示拥有全部权限。</p>
 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final UserService userService;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Long userId = toLong(loginId);
        if (userId == null) {
            return List.of();
        }
        return userService.getPermCodes(userId);
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long userId = toLong(loginId);
        if (userId == null) {
            return List.of();
        }
        return userService.getRoleCodes(userId);
    }

    private Long toLong(Object loginId) {
        try {
            return Long.valueOf(loginId.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
