package com.company.devplatform.module.release.vo;

import lombok.Data;

/**
 * 看板-每日趋势点
 */
@Data
public class DashboardTrendPointVO {

    /** 日期 yyyy-MM-dd */
    private String date;

    /** 发布次数 */
    private int total;

    /** 成功次数 */
    private int success;

    /** 失败次数 */
    private int failed;
}
