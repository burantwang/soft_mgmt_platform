package com.company.devplatform.module.release.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.company.devplatform.common.Result;
import com.company.devplatform.module.release.dto.ReleaseProjectDTO;
import com.company.devplatform.module.release.entity.ReleaseProject;
import com.company.devplatform.module.release.service.ReleaseProjectService;
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
 * 项目(机型)维护
 */
@RestController
@RequestMapping("/api/release/projects")
@RequiredArgsConstructor
public class ReleaseProjectController {

    private final ReleaseProjectService projectService;

    /** 分页查询（sonic:view） */
    @GetMapping
    @SaCheckPermission("sonic:view")
    public Result<IPage<ReleaseProject>> page(@RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) Integer status) {
        return Result.ok(projectService.page(page, size, keyword, status));
    }

    /** 启用机型列表（sonic:view） */
    @GetMapping("/enabled")
    @SaCheckPermission("sonic:view")
    public Result<List<ReleaseProject>> enabled() {
        return Result.ok(projectService.listEnabled());
    }

    /** 全部机型（sonic:view） */
    @GetMapping("/all")
    @SaCheckPermission("sonic:view")
    public Result<List<ReleaseProject>> all() {
        return Result.ok(projectService.listAll());
    }

    /** 新增（sonic:edit） */
    @PostMapping
    @SaCheckPermission("sonic:edit")
    public Result<ReleaseProject> create(@Valid @RequestBody ReleaseProjectDTO dto) {
        return Result.ok(projectService.create(dto));
    }

    /** 编辑（sonic:edit） */
    @PutMapping
    @SaCheckPermission("sonic:edit")
    public Result<ReleaseProject> update(@Valid @RequestBody ReleaseProjectDTO dto) {
        return Result.ok(projectService.update(dto));
    }

    /** 删除（sonic:edit） */
    @DeleteMapping("/{id}")
    @SaCheckPermission("sonic:edit")
    public Result<Void> delete(@PathVariable Long id) {
        projectService.delete(id);
        return Result.ok();
    }
}
