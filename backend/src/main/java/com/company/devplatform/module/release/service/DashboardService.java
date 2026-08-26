package com.company.devplatform.module.release.service;

import com.company.devplatform.module.release.vo.DashboardSummaryVO;

/**
 * 发布统计看板服务
 */
public interface DashboardService {

    /** 统计汇总（发布总览 + 指定日期单日总览/明细 + 按分支 + 按机型 + 近15天趋势） */
    DashboardSummaryVO summary(java.time.LocalDate date);
}
