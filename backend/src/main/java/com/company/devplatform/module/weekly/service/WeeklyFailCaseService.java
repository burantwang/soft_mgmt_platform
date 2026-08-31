package com.company.devplatform.module.weekly.service;

import com.company.devplatform.module.release.vo.AiAnalysisResult;
import com.company.devplatform.module.weekly.dto.WeeklyFailCaseAssignDTO;
import com.company.devplatform.module.weekly.dto.WeeklyFailCaseUpdateDTO;

/**
 * WeeklySanity 失败用例处理服务
 */
public interface WeeklyFailCaseService {

    /**
     * 更新失败用例处理信息（含状态流转）
     */
    void updateCase(Long caseId, WeeklyFailCaseUpdateDTO dto);

    /**
     * 快速指派用例责任人（仅更新 assigneeId，立即生效）
     */
    void assignCaseAssignee(Long caseId, WeeklyFailCaseAssignDTO dto);

    /**
     * 触发 AI 分析并回写结果（根因/佐证/修复建议）
     */
    AiAnalysisResult aiAnalyze(Long caseId);
}
