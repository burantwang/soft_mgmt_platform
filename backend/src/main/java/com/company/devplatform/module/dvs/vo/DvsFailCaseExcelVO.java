package com.company.devplatform.module.dvs.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DVS 失败用例 Excel 导出
 */
@Data
public class DvsFailCaseExcelVO {

    @ExcelProperty("分支")
    private String branch;

    @ExcelProperty("机型")
    private String projectName;

    @ExcelProperty("失败脚本")
    private String caseName;

    @ExcelProperty("用例类型")
    private String caseTypeDesc;

    @ExcelProperty("失败原因")
    private String failReason;

    @ExcelProperty("修复方案")
    private String fixPlan;

    @ExcelProperty("结论进展（含分类）")
    private String conclusion;

    @ExcelProperty("Bug单号")
    private String bugNo;

    @ExcelProperty("责任人")
    private String assigneeName;

    @ExcelProperty("状态")
    private String statusDesc;

    @ExcelProperty("AI根因")
    private String aiRootCause;

    @ExcelProperty("AI佐证")
    private String aiEvidence;

    @ExcelProperty("AI解决建议")
    private String aiSolution;

    @ExcelProperty("AI判断")
    private String aiAnalysisCorrect;

    @ExcelProperty("报告时间")
    private LocalDateTime publishTime;
}
