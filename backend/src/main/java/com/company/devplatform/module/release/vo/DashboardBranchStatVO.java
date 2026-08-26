package com.company.devplatform.module.release.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 看板-按分支统计
 */
@Data
public class DashboardBranchStatVO {

    /** 代码分支 */
    private String branch;

    /** 发布次数 */
    private int total;

    /** 成功次数 */
    private int success;

    /** 失败次数 */
    private int failed;

    /** 成功率(%) */
    private BigDecimal successRate;
}
