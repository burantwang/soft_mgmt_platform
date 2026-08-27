package com.company.devplatform.module.home.vo;

import lombok.Data;

import java.util.List;

/**
 * Home 总览聚合
 */
@Data
public class HomeSummaryVO {

    /* ---- 发布板块 ---- */
    private Long releaseTotal;
    private Long releaseToday;
    private Long releaseSuccess;
    private Long releaseFailed;

    /* ---- 任务追踪板块 ---- */
    private Long taskPending;
    private Long taskProcessing;
    private Long taskTotal;
    /** 我的待办（待处理+处理中） */
    private Long myTodo;

    /* ---- Wiki / 文件 ---- */
    private Long wikiDocCount;
    private Long fileCount;

    /* ---- 最新动态 ---- */
    private List<RecentItemVO> recentReleases;
    private List<RecentItemVO> recentTasks;
    private List<RecentItemVO> recentWikis;
    private List<RecentItemVO> recentFiles;
}
