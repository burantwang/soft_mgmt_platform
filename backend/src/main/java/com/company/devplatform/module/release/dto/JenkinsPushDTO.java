package com.company.devplatform.module.release.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Jenkins 开放接口推送入参（JSON 模式）
 * <p>multipart 模式使用同名表单字段 + 报告文件。</p>
 */
@Data
public class JenkinsPushDTO {

    /** 代码分支 */
    @NotBlank(message = "代码分支不能为空")
    @Size(max = 128, message = "分支最长128字符")
    private String branch;

    /** 镜像版本号 */
    @Size(max = 128, message = "版本号最长128字符")
    private String version;

    /** 镜像地址 */
    @Size(max = 500, message = "镜像地址最长500字符")
    private String imageUrl;

    /** 关联机型编码列表 */
    @NotEmpty(message = "请至少推送一个机型编码")
    private List<String> projectCodes;

    @Size(max = 255, message = "备注最长255字符")
    private String remark;

    /* ---------- 用例统计（JSON 模式直接推送统计数字） ---------- */
    private Integer totalCount;
    private Integer passedCount;
    private Integer failedCount;
    private Integer errorCount;
    private Integer skippedCount;

    /** 总耗时(秒) */
    private BigDecimal durationSec;

    /** 报告生成时间(yyyy-MM-dd HH:mm:ss) */
    private LocalDateTime reportTime;
}
