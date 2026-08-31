package com.company.devplatform.module.dvs.service;

import com.company.devplatform.common.vo.MyTaskCaseVO;
import com.company.devplatform.module.release.vo.AiAnalysisResult;
import com.company.devplatform.module.dvs.dto.DvsFailCaseAssignDTO;
import com.company.devplatform.module.dvs.dto.DvsFailCaseUpdateDTO;

import java.util.List;

/**
 * DVS 失败用例处理服务
 */
public interface DvsFailCaseService {

    /**
     * 更新失败用例处理信息（含状态流转）
     */
    void updateCase(Long caseId, DvsFailCaseUpdateDTO dto);

    /**
     * 快速指派用例责任人（仅更新 assigneeId，立即生效）
     */
    void assignCaseAssignee(Long caseId, DvsFailCaseAssignDTO dto);

    /**
     * 触发 AI 分析并回写结果（根因/佐证/修复建议）
     */
    AiAnalysisResult aiAnalyze(Long caseId);

    /**
     * 查询当前用户被指派的 DVS 失败用例（个人任务）
     *
     * @param all true=含已完成/已关闭全部；false=仅待处理/处理中
     */
    List<MyTaskCaseVO> listMyCases(boolean all);
}
