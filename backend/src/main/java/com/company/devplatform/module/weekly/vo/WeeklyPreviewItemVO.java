package com.company.devplatform.module.weekly.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * WeeklySanity 单模块(HTML文件)解析预览结果
 */
@Data
public class WeeklyPreviewItemVO {

    /** 原始文件名（作为模块名） */
    private String fileName;

    /** 文件大小(字节) */
    private Long fileSize;

    /** 镜像版本号(报告内 Environment.Version，可为空) */
    private String version;

    /** 发布结果:1成功 2失败 */
    private Integer result;

    private String resultDesc;

    private Integer totalCount;

    private Integer passedCount;

    private Integer failedCount;

    private Integer errorCount;

    private Integer skippedCount;

    /** 总耗时(秒) */
    private BigDecimal durationSec;

    /** 报告生成时间 */
    private LocalDateTime reportTime;

    /** 失败/错误用例明细 */
    private List<FailCase> failCases;

    /** 失败用例明细 */
    @Data
    public static class FailCase {
        /** 用例状态：failed / error */
        private String status;
        private String name;
        private String log;
    }
}
