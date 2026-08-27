package com.company.devplatform.module.task.vo;

import com.company.devplatform.module.release.vo.ReportFileItemVO;
import lombok.Data;

import java.util.List;

/**
 * DailySanity 失败任务追踪-按分支×机型分组的失败明细统计
 * 预留栏位说明：
 *   - passedCount / passRate：issue_task 暂无成功用例统计，暂为 0，后续补充自动解析报告后填充
 *   - reportFiles：原始报告文件关联预留，后续补充
 */
@Data
public class TaskFailGroupedVO {

    private String branch;

    private String projectName;

    /** 失败明细总数 */
    private Integer totalCount;

    /** 成功用例数（预留） */
    private Integer passedCount;

    /** 失败明细数 */
    private Integer failedCount;

    /** 通过率（预留） */
    private Integer passRate;

    private List<TaskFailCaseVO> cases;

    /** 原始测试报告文件（预留） */
    private List<ReportFileItemVO> reportFiles;
}
