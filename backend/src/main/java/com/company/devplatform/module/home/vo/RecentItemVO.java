package com.company.devplatform.module.home.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Home 最新动态条目
 */
@Data
public class RecentItemVO {

    private Long id;

    /** 标题 */
    private String title;

    /** 副标题/说明 */
    private String subtitle;

    /** 时间 */
    private LocalDateTime time;

    /** 前端路由跳转地址（相对，如 /release） */
    private String path;
}
