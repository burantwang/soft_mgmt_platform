package com.company.devplatform.module.wiki.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Wiki 文档详情
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WikiDocDetailVO extends WikiDocNodeVO {

    /** 正文(富文本HTML) */
    private String content;

    /** 创建人昵称 */
    private String creatorName;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 子文档数量 */
    private Integer childCount;
}
