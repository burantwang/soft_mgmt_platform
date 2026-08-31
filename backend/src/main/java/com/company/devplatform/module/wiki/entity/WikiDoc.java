package com.company.devplatform.module.wiki.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Wiki 文档实体
 */
@Data
@TableName("wiki_doc")
public class WikiDoc {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 文档标题 */
    private String title;

    /** 正文(富文本HTML,入库前经XSS白名单过滤) */
    private String content;

    /** 父目录ID,0为根 */
    private Long parentId;

    /** 排序(同级) */
    private Integer sort;

    /** 创建人 */
    private Long creatorId;

    /** 最后编辑人 */
    private Long editorId;

    /** 启用状态:1启用 0停用（仅 AI 技能文档使用，普通 wiki 文档恒为 1） */
    private Integer enabled;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 逻辑删除:0未删 1已删 */
    @TableLogic
    private Integer isDeleted;
}
