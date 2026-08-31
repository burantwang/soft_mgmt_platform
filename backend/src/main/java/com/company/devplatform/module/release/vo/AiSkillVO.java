package com.company.devplatform.module.release.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 技能（skill）文档
 */
@Data
public class AiSkillVO {

    private Long id;

    /** 文件名（如 skill1.md） */
    private String title;

    /** Markdown 内容 */
    private String content;

    /** 启用:1启用 0停用 */
    private Integer enabled;

    private LocalDateTime updateTime;
}
