package com.company.devplatform.module.task.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.company.devplatform.common.Result;
import com.company.devplatform.module.task.dto.IssueItemDTO;
import com.company.devplatform.module.task.dto.IssueTaskAssignDTO;
import com.company.devplatform.module.task.dto.IssueTaskCreateDTO;
import com.company.devplatform.module.task.dto.IssueTaskStatusDTO;
import com.company.devplatform.module.task.dto.IssueTaskUpdateDTO;
import com.company.devplatform.module.task.service.IssueTaskService;
import com.company.devplatform.module.task.vo.IssueTaskDetailVO;
import com.company.devplatform.module.task.vo.IssueTaskVO;
import com.company.devplatform.module.task.vo.TaskFailGroupedVO;
import com.company.devplatform.module.task.vo.TaskTypeCountVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 问题单任务（通用任务追踪）
 */
@RestController
@RequestMapping("/api/task/issues")
@RequiredArgsConstructor
public class IssueTaskController {

    private final IssueTaskService issueTaskService;

    /** 分页查询 */
    @GetMapping
    @SaCheckPermission("task:view")
    public Result<IPage<IssueTaskVO>> page(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size,
                                           @RequestParam(required = false) Long taskTypeId,
                                           @RequestParam(required = false) Integer status,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) Long assigneeId) {
        return Result.ok(issueTaskService.pageTasks(page, size, taskTypeId, status, keyword, assigneeId, false));
    }

    /** 我的待办（跨类型） */
    @GetMapping("/mine")
    @SaCheckPermission("task:view")
    public Result<IPage<IssueTaskVO>> mine(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size) {
        return Result.ok(issueTaskService.pageTasks(page, size, null, null, null, null, true));
    }

    /** 各类型任务统计 */
    @GetMapping("/overview")
    @SaCheckPermission("task:view")
    public Result<List<TaskTypeCountVO>> overview() {
        return Result.ok(issueTaskService.overview());
    }

    /** DailySanity 失败任务追踪：按日期/分支/机型/用例名分组查询（统计/报告栏位预留） */
    @GetMapping("/fail-cases/grouped")
    @SaCheckPermission("task:view")
    public Result<List<TaskFailGroupedVO>> groupedFailCases(@RequestParam(required = false) String date,
                                                            @RequestParam(required = false) String branch,
                                                            @RequestParam(required = false) String projectName,
                                                            @RequestParam(required = false) String caseName) {
        return Result.ok(issueTaskService.groupedFailCases(date, branch, projectName, caseName));
    }

    /** 详情（含明细） */
    @GetMapping("/{id}")
    @SaCheckPermission("task:view")
    public Result<IssueTaskDetailVO> detail(@PathVariable Long id) {
        return Result.ok(issueTaskService.detail(id));
    }

    @PostMapping
    @SaCheckPermission("task:edit")
    public Result<IssueTaskVO> create(@Valid @RequestBody IssueTaskCreateDTO dto) {
        return Result.ok(issueTaskService.create(dto));
    }

    @PutMapping
    @SaCheckPermission("task:edit")
    public Result<Void> update(@Valid @RequestBody IssueTaskUpdateDTO dto) {
        issueTaskService.update(dto);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("task:edit")
    public Result<Void> delete(@PathVariable Long id) {
        issueTaskService.delete(id);
        return Result.ok();
    }

    /** 指派 */
    @PutMapping("/{id}/assign")
    @SaCheckPermission("task:edit")
    public Result<Void> assign(@PathVariable Long id, @Valid @RequestBody IssueTaskAssignDTO dto) {
        issueTaskService.assign(id, dto);
        return Result.ok();
    }

    /** 状态流转 */
    @PutMapping("/{id}/status")
    @SaCheckPermission("task:edit")
    public Result<Void> changeStatus(@PathVariable Long id, @Valid @RequestBody IssueTaskStatusDTO dto) {
        issueTaskService.changeStatus(id, dto);
        return Result.ok();
    }

    /** 处理明细 */
    @PutMapping("/items")
    @SaCheckPermission("task:edit")
    public Result<Void> handleItem(@Valid @RequestBody IssueItemDTO dto) {
        issueTaskService.handleItem(dto);
        return Result.ok();
    }
}
