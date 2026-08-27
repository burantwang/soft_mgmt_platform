package com.company.devplatform.module.wiki.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Wiki 目录树节点
 */
@Data
public class WikiDocNodeVO {

    /** 文档ID */
    private Long id;

    /** 文档标题 */
    private String title;

    /** 父目录ID */
    private Long parentId;

    /** 排序(同级) */
    private Integer sort;

    /** 是否含正文内容 */
    private Boolean hasContent;

    /** 最后编辑人昵称 */
    private String editorName;

    /** 最后更新时间 */
    private LocalDateTime updateTime;

    /** 子节点 */
    private List<WikiDocNodeVO> children = new ArrayList<>();
}
