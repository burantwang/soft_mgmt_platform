package com.company.devplatform.module.release.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件资源实体
 */
@Data
@TableName("file_resource")
public class FileResource {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 原始文件名 */
    private String fileName;

    /** 存储相对路径(禁止存绝对路径) */
    private String storedPath;

    /** 后缀(小写) */
    private String fileExt;

    /** MIME类型 */
    private String mimeType;

    /** 文件大小(字节) */
    private Long fileSize;

    /** 类型:1测试报告 2普通附件 */
    private Integer fileType;

    /** 关联Wiki文档ID,可为空 */
    private Long docId;

    /** 上传人 */
    private Long uploaderId;

    /** 下载次数 */
    private Integer downloadCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 逻辑删除:0未删 1已删 */
    @TableLogic
    private Integer isDeleted;
}
