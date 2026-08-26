package com.company.devplatform.module.release.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 报告解析预览结果
 */
@Data
public class ReportPreviewVO {

    /** 预览令牌（确认入库时回传） */
    private String previewToken;

    /** 原始文件名 */
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
        private String name;
        private String log;
    }
}
