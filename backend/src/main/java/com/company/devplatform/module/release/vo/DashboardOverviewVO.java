package com.company.devplatform.module.release.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 看板-总览统计
 */
@Data
public class DashboardOverviewVO {

    /** 发布总次数 */
    private int totalRecords;

    /** 成功次数 */
    private int successCount;

    /** 失败次数 */
    private int failedCount;

    /** 发布成功率(%) */
    private BigDecimal successRate;

    /** 用例总数 */
    private int totalCases;

    /** 通过用例数 */
    private int passedCases;

    /** 用例总通过率(%) */
    private BigDecimal overallPassRate;

    /** 覆盖机型数 */
    private int projectCount;
}
