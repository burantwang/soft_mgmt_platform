package com.company.devplatform.module.release.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.company.devplatform.common.Result;
import com.company.devplatform.module.release.dto.FailCaseGroupedQuery;
import com.company.devplatform.module.release.dto.FailCaseHandleDTO;
import com.company.devplatform.module.release.dto.FailCaseUpdateDTO;
import com.company.devplatform.module.release.dto.FailTaskAssignDTO;
import com.company.devplatform.module.release.dto.FailTaskCreateDTO;
import com.company.devplatform.module.release.dto.FailTaskStatusDTO;
import com.company.devplatform.module.release.dto.FailTaskUpdateDTO;
import com.company.devplatform.module.release.entity.ReleaseFailTask;
import com.company.devplatform.module.release.service.ReleaseFailTaskService;
import com.company.devplatform.module.release.vo.FailCaseGroupedVO;
import com.company.devplatform.module.release.vo.FailTaskDetailVO;
import com.company.devplatform.module.release.vo.FailTaskVO;
import com.company.devplatform.module.release.vo.RecentDayStatVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;

import java.time.LocalDate;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 失败聚合任务接口（阶段4）
 */
@RestController
@RequestMapping("/api/release")
@RequiredArgsConstructor
public class ReleaseFailTaskController {

    private final ReleaseFailTaskService failTaskService;

    /** 任务分页查询 */
    @GetMapping("/fail-tasks")
    @SaCheckPermission("sonic:view")
    public Result<IPage<FailTaskVO>> page(@RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "10") int size,
                                          @RequestParam(required = false) Integer status,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) Long assigneeId) {
        return Result.ok(failTaskService.pageTasks(page, size, status, keyword, assigneeId, false));
    }

    /** 我的待办（当前用户被指派且未完成） */
    @GetMapping("/fail-tasks/mine")
    @SaCheckPermission("sonic:view")
    public Result<IPage<FailTaskVO>> mine(@RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "10") int size) {
        return Result.ok(failTaskService.pageTasks(page, size, null, null, null, true));
    }

    /** 任务详情（含失败用例明细） */
    @GetMapping("/fail-tasks/{id}")
    @SaCheckPermission("sonic:view")
    public Result<FailTaskDetailVO> detail(@PathVariable Long id) {
        return Result.ok(failTaskService.detail(id));
    }

    /** 手动创建任务 */
    @PostMapping("/fail-tasks")
    @SaCheckPermission("sonic:edit")
    public Result<Long> create(@Valid @RequestBody FailTaskCreateDTO dto) {
        ReleaseFailTask task = failTaskService.create(dto);
        return Result.ok(task.getId());
    }

    /** 编辑任务（概述/原因/方案） */
    @PutMapping("/fail-tasks")
    @SaCheckPermission("sonic:edit")
    public Result<Void> update(@Valid @RequestBody FailTaskUpdateDTO dto) {
        failTaskService.update(dto);
        return Result.ok();
    }

    /** 删除任务（级联删除用例明细） */
    @DeleteMapping("/fail-tasks/{id}")
    @SaCheckPermission("sonic:edit")
    public Result<Void> delete(@PathVariable Long id) {
        failTaskService.delete(id);
        return Result.ok();
    }

    /** 指派任务责任人 */
    @PutMapping("/fail-tasks/{id}/assign")
    @SaCheckPermission("sonic:edit")
    public Result<Void> assign(@PathVariable Long id, @Valid @RequestBody FailTaskAssignDTO dto) {
        dto.setId(id);
        failTaskService.assign(dto);
        return Result.ok();
    }

    /** 任务状态流转（数据级校验：仅创建人/被指派人/超管） */
    @PutMapping("/fail-tasks/{id}/status")
    @SaCheckPermission("sonic:view")
    public Result<Void> changeStatus(@PathVariable Long id, @Valid @RequestBody FailTaskStatusDTO dto) {
        dto.setId(id);
        failTaskService.changeStatus(dto);
        return Result.ok();
    }

    /** 处理失败用例明细（联动刷新任务状态） */
    @PutMapping("/fail-cases")
    @SaCheckPermission("sonic:view")
    public Result<Void> handleCase(@Valid @RequestBody FailCaseHandleDTO dto) {
        failTaskService.handleCase(dto);
        return Result.ok();
    }

    /** 按日期+分支×机型分组查询失败用例 */
    @GetMapping("/fail-cases/grouped")
    @SaCheckPermission("sonic:view")
    public Result<List<FailCaseGroupedVO>> listGroupedCases(FailCaseGroupedQuery query) {
        if (query.getDate() == null) {
            query.setDate(LocalDate.now());
        }
        return Result.ok(failTaskService.listGroupedCases(query));
    }

    /** 更新失败用例处理信息 */
    @PutMapping("/fail-cases/{caseId}")
    @SaCheckPermission("sonic:view")
    public Result<Void> updateCase(@PathVariable Long caseId, @Valid @RequestBody FailCaseUpdateDTO dto) {
        failTaskService.updateCase(caseId, dto);
        return Result.ok();
    }

    /** 最近7天 DailySanity 分析完成统计 */
    @GetMapping("/fail-cases/recent-week-stats")
    @SaCheckPermission("sonic:view")
    public Result<List<RecentDayStatVO>> recentWeekStats() {
        return Result.ok(failTaskService.recentWeekStats());
    }
}
