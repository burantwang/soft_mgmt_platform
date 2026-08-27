package com.company.devplatform.module.wiki.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Wiki 文档创建/更新入参
 */
@Data
public class WikiDocDTO {

    /** 文档标题 */
    @NotBlank(message = "文档标题不能为空")
    @Size(max = 255, message = "文档标题不能超过255个字符")
    private String title;

    /** 正文(富文本HTML,入库前 XSS 净化) */
    private String content;

    /** 父目录ID,0为根 */
    private Long parentId;

    /** 排序(同级) */
    private Integer sort;
}
