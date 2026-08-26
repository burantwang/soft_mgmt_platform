package com.company.devplatform.module.auth.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.company.devplatform.common.Result;
import com.company.devplatform.module.auth.service.PermissionService;
import com.company.devplatform.module.auth.vo.PermissionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 权限点管理接口
 */
@RestController
@RequestMapping("/api/system/permissions")
@RequiredArgsConstructor
@SaCheckPermission("system:manage")
public class PermissionController {

    private final PermissionService permissionService;

    /** 权限点列表(按模块分组) */
    @GetMapping
    public Result<List<PermissionVO>> list() {
        return Result.ok(permissionService.list());
    }
}
