package com.company.devplatform.module.dvs.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.company.devplatform.common.Result;
import com.company.devplatform.common.vo.MyTaskCaseVO;
import com.company.devplatform.module.release.vo.AiAnalysisResult;
import com.company.devplatform.module.dvs.dto.DvsFailCaseAssignDTO;
import com.company.devplatform.module.dvs.dto.DvsFailCaseUpdateDTO;
import com.company.devplatform.module.dvs.service.DvsFailCaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
 * DVS 失败用例接口
 */
@RestController
@RequestMapping("/api/dvs")
@RequiredArgsConstructor
public class DvsFailCaseController {

    private final DvsFailCaseService failCaseService;

    /** 更新失败用例处理信息 */
    @PutMapping("/fail-cases/{caseId}")
    @SaCheckPermission("sonic:view")
    public Result<Void> updateCase(@PathVariable Long caseId, @Valid @RequestBody DvsFailCaseUpdateDTO dto) {
        failCaseService.updateCase(caseId, dto);
        return Result.ok();
    }

    /** 快速指派用例责任人（仅更新 assigneeId，立即生效） */
    @PutMapping("/fail-cases/{caseId}/assign")
    @SaCheckPermission("sonic:view")
    public Result<Void> assignCase(@PathVariable Long caseId, @Valid @RequestBody DvsFailCaseAssignDTO dto) {
        failCaseService.assignCaseAssignee(caseId, dto);
        return Result.ok();
    }

    /** 触发 AI 分析失败用例（同步等待结果，回写根因/佐证/修复建议） */
    @PostMapping("/fail-cases/{caseId}/ai-analyze")
    @SaCheckPermission("sonic:view")
    public Result<AiAnalysisResult> aiAnalyze(@PathVariable Long caseId) {
        return Result.ok(failCaseService.aiAnalyze(caseId));
    }

    /** 个人任务：当前用户被指派的 DVS 失败用例（scope=active 仅未完成，all 含全部） */
    @GetMapping("/fail-cases/mine")
    @SaCheckPermission("sonic:view")
    public Result<List<MyTaskCaseVO>> myCases(@RequestParam(defaultValue = "active") String scope) {
        return Result.ok(failCaseService.listMyCases("all".equals(scope)));
    }
}
