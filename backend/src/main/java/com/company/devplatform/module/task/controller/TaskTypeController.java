package com.company.devplatform.module.task.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.company.devplatform.common.Result;
import com.company.devplatform.module.task.dto.TaskTypeDTO;
import com.company.devplatform.module.task.service.TaskTypeService;
import com.company.devplatform.module.task.vo.TaskTypeVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 任务类型管理
 */
@RestController
@RequestMapping("/api/task/types")
@RequiredArgsConstructor
public class TaskTypeController {

    private final TaskTypeService taskTypeService;

    /** 启用的类型列表（菜单/下拉用，需登录即可） */
    @GetMapping
    public Result<List<TaskTypeVO>> listEnabled() {
        return Result.ok(taskTypeService.listEnabled());
    }

    /** 管理分页（含停用） */
    @GetMapping("/all")
    @SaCheckPermission("task:edit")
    public Result<IPage<TaskTypeVO>> pageAll(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int size,
                                             @RequestParam(required = false) String keyword) {
        return Result.ok(taskTypeService.pageAll(page, size, keyword));
    }

    @PostMapping
    @SaCheckPermission("task:edit")
    public Result<TaskTypeVO> create(@Valid @RequestBody TaskTypeDTO dto) {
        return Result.ok(taskTypeService.create(dto));
    }

    @PutMapping
    @SaCheckPermission("task:edit")
    public Result<Void> update(@Valid @RequestBody TaskTypeDTO dto) {
        taskTypeService.update(dto);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("task:edit")
    public Result<Void> delete(@PathVariable Long id) {
        taskTypeService.delete(id);
        return Result.ok();
    }

    /** 启停用 */
    @PutMapping("/{id}/status")
    @SaCheckPermission("task:edit")
    public Result<Void> toggleEnabled(@PathVariable Long id) {
        taskTypeService.toggleEnabled(id);
        return Result.ok();
    }
}
