package com.company.devplatform.module.release.vo;

import lombok.Data;

import java.util.List;

/**
 * 按分支×机型分组的失败用例统计
 */
@Data
public class FailCaseGroupedVO {

    private String branch;

    private String projectName;

    private Integer totalCount;

    private Integer passedCount;

    private Integer failedCount;

    private Integer passRate;

    private List<GroupedFailCaseVO> cases;

    /** 该分组涉及的原始测试报告文件（分支×机型关联记录的 HTML 报告） */
    private List<ReportFileItemVO> reportFiles;
}
