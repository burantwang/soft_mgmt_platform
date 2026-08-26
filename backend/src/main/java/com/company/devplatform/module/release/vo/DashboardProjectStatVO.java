package com.company.devplatform.module.release.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 看板-按机型统计
 */
@Data
public class DashboardProjectStatVO {

    /** 机型ID */
    private Long projectId;

    /** 机型名称 */
    private String projectName;

    /** 发布次数 */
    private int total;

    /** 成功次数 */
    private int success;

    /** 失败次数 */
    private int failed;

    /** 成功率(%) */
    private BigDecimal successRate;
}
