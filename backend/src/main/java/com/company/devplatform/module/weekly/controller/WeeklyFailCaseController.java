package com.company.devplatform.module.weekly.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.company.devplatform.common.Result;
import com.company.devplatform.module.weekly.dto.WeeklyFailCaseAssignDTO;
import com.company.devplatform.module.weekly.dto.WeeklyFailCaseUpdateDTO;
import com.company.devplatform.module.weekly.service.WeeklyFailCaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * WeeklySanity 失败用例接口
 */
@RestController
@RequestMapping("/api/weekly")
@RequiredArgsConstructor
public class WeeklyFailCaseController {

    private final WeeklyFailCaseService failCaseService;

    /** 更新失败用例处理信息 */
    @PutMapping("/fail-cases/{caseId}")
    @SaCheckPermission("sonic:view")
    public Result<Void> updateCase(@PathVariable Long caseId, @Valid @RequestBody WeeklyFailCaseUpdateDTO dto) {
        failCaseService.updateCase(caseId, dto);
        return Result.ok();
    }

    /** 快速指派用例责任人（仅更新 assigneeId，立即生效） */
    @PutMapping("/fail-cases/{caseId}/assign")
    @SaCheckPermission("sonic:view")
    public Result<Void> assignCase(@PathVariable Long caseId, @Valid @RequestBody WeeklyFailCaseAssignDTO dto) {
        failCaseService.assignCaseAssignee(caseId, dto);
        return Result.ok();
    }
}
