package com.company.devplatform.module.release.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 看板单日分支×机型维度统计
 */
@Data
public class DashboardDayStatVO {

    /** 分支 */
    private String branch;

    /** 机型名称 */
    private String projectName;

    /** 脚本总数（用例总数） */
    private Integer totalCount;

    /** 成功数 */
    private Integer passedCount;

    /** 失败数（总数 - 成功数） */
    private Integer failedCount;

    /** 通过率 */
    private BigDecimal passRate;

    /** 发布结果：1成功/可发布 2失败/不可发布 */
    private Integer result;

    /** 发布结果描述 */
    private String resultDesc;

    /** 来源：1人工上传 2Jenkins推送 3手动创建 */
    private Integer source;

    /** 来源描述 */
    private String sourceDesc;

    /** 最新一次版本号 */
    private String version;

    /** 最新一次镜像地址 */
    private String imageUrl;

    /** 最新一次发布时间 */
    private LocalDateTime publishTime;
}
