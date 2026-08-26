package com.company.devplatform.module.auth.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.devplatform.common.Result;
import com.company.devplatform.module.auth.dto.AssignPermissionsDTO;
import com.company.devplatform.module.auth.dto.RoleDTO;
import com.company.devplatform.module.auth.service.RoleService;
import com.company.devplatform.module.auth.vo.RoleVO;
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

import java.util.List;

/**
 * 角色管理接口
 */
@RestController
@RequestMapping("/api/system/roles")
@RequiredArgsConstructor
@SaCheckPermission("system:manage")
public class RoleController {

    private final RoleService roleService;

    /** 分页查询角色 */
    @GetMapping
    public Result<Page<RoleVO>> page(@RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "10") int size) {
        return Result.ok(roleService.page(page, Math.min(Math.max(size, 1), 100)));
    }

    /** 全部启用角色(下拉选项) */
    @GetMapping("/all")
    public Result<List<RoleVO>> all() {
        return Result.ok(roleService.all());
    }

    /** 新增角色 */
    @PostMapping
    public Result<Void> create(@Valid @RequestBody RoleDTO dto) {
        roleService.create(dto);
        return Result.ok();
    }

    /** 编辑角色 */
    @PutMapping
    public Result<Void> update(@Valid @RequestBody RoleDTO dto) {
        roleService.update(dto);
        return Result.ok();
    }

    /** 删除角色 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.ok();
    }

    /** 分配权限 */
    @PutMapping("/{id}/permissions")
    public Result<Void> assignPermissions(@PathVariable Long id, @Valid @RequestBody AssignPermissionsDTO dto) {
        roleService.assignPermissions(id, dto.getPermissionIds());
        return Result.ok();
    }

    /** 查询角色已分配权限ID */
    @GetMapping("/{id}/permissions")
    public Result<List<Long>> getPermissionIds(@PathVariable Long id) {
        return Result.ok(roleService.getPermissionIds(id));
    }
}
