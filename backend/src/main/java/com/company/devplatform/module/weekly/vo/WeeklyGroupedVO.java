package com.company.devplatform.module.weekly.vo;

import lombok.Data;

import java.util.List;

/**
 * WeeklySanity 按日期+分支×机型分组的失败用例统计
 * <p>组内含多个模块(HTML)统计与全部失败用例明细。</p>
 */
@Data
public class WeeklyGroupedVO {

    private String branch;

    private String projectName;

    /** 该分组涉及的模块(HTML)统计列表 */
    private List<WeeklyModuleVO> modules;

    private Integer totalCount;

    private Integer passedCount;

    private Integer failedCount;

    private Integer passRate;

    private List<WeeklyFailCaseVO> cases;

    /** 该分组涉及的原始测试报告文件 */
    private List<ReportFileItemVO> reportFiles;

    /** 原始报告文件项 */
    @Data
    public static class ReportFileItemVO {
        private Long fileId;
        private String fileName;
    }
}
