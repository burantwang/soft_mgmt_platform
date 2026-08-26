package com.company.devplatform.module.release.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 发布记录 Excel 导出
 */
@Data
public class ReleaseRecordExcelVO {

    /** 代码分支 */
    @ExcelProperty("代码分支")
    private String branch;

    /** 镜像版本号 */
    @ExcelProperty("镜像版本")
    private String version;

    /** 发布结果 */
    @ExcelProperty("发布结果")
    private String resultDesc;

    /** 关联机型 */
    @ExcelProperty("关联机型")
    private String projectNames;

    /** 用例总数 */
    @ExcelProperty("用例总数")
    private Integer totalCount;

    /** 通过用例 */
    @ExcelProperty("通过用例")
    private Integer passedCount;

    /** 失败用例 */
    @ExcelProperty("失败用例")
    private Integer failedCount;

    /** 错误用例 */
    @ExcelProperty("错误用例")
    private Integer errorCount;

    /** 跳过用例 */
    @ExcelProperty("跳过用例")
    private Integer skippedCount;

    /** 通过率(%) */
    @ExcelProperty("通过率(%)")
    private BigDecimal passRate;

    /** 总耗时(秒) */
    @ExcelProperty("总耗时(秒)")
    private BigDecimal durationSec;

    /** 报告生成时间 */
    @ExcelProperty("报告生成时间")
    private LocalDateTime reportTime;

    /** 发布人 */
    @ExcelProperty("发布人")
    private String publisherName;

    /** 发布时间 */
    @ExcelProperty("发布时间")
    private LocalDateTime publishTime;

    /** 来源 */
    @ExcelProperty("来源")
    private String sourceDesc;

    /** 备注 */
    @ExcelProperty("备注")
    private String remark;
}
