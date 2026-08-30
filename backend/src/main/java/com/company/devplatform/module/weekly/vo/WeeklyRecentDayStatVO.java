package com.company.devplatform.module.weekly.vo;

import lombok.Data;

/**
 * WeeklySanity 最近某天分析完成统计
 */
@Data
public class WeeklyRecentDayStatVO {

    private String date;

    private Integer totalCount;

    private Integer analyzedCount;

    /** 分析完成率(%)，当天无执行明细时为 null */
    private Integer rate;
}
