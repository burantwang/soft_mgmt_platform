package com.company.devplatform.module.release.vo;

import lombok.Data;

/**
 * 最近某天 DailySanity 分析完成统计
 */
@Data
public class RecentDayStatVO {

    /** 日期 (yyyy-MM-dd) */
    private String date;

    /** 失败用例总数 */
    private Integer totalCount;

    /** 已分析数 (status=3已完成/4已关闭) */
    private Integer analyzedCount;

    /** 分析完成率 (%)，当天无执行明细时为 null */
    private Integer rate;
}
