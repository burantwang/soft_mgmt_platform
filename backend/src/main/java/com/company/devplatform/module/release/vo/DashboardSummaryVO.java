package com.company.devplatform.module.release.vo;

import lombok.Data;

import java.util.List;

/**
 * 发布统计看板汇总
 */
@Data
public class DashboardSummaryVO {

    /** 总览（累计） */
    private DashboardOverviewVO overview;

    /** 单日统计日期（查询日，默认今天） */
    private java.time.LocalDate dayDate;

    /** 单日总览 */
    private DashboardOverviewVO dayOverview;

    /** 单日发布明细 */
    private List<ReleaseRecordVO> dayRecords;

    /** 单日分支×机型聚合统计 */
    private List<DashboardDayStatVO> dayStats;

    /** 按分支统计 */
    private List<DashboardBranchStatVO> branchStats;

    /** 按机型统计 */
    private List<DashboardProjectStatVO> projectStats;

    /** 近15天趋势 */
    private List<DashboardTrendPointVO> trend;
}
