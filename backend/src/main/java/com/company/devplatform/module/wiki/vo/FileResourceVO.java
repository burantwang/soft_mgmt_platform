package com.company.devplatform.module.wiki.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件资源列表项
 */
@Data
public class FileResourceVO {

    /** 文件ID */
    private Long id;

    /** 原始文件名 */
    private String fileName;

    /** 后缀(小写) */
    private String fileExt;

    /** MIME类型 */
    private String mimeType;

    /** 文件大小(字节) */
    private Long fileSize;

    /** 文件大小(可读描述,如 1.2 MB) */
    private String sizeDesc;

    /** 类型:1测试报告 2普通附件 */
    private Integer fileType;

    /** 类型描述 */
    private String fileTypeDesc;

    /** 关联Wiki文档ID */
    private Long docId;

    /** 关联文档标题 */
    private String docTitle;

    /** 上传人 */
    private Long uploaderId;

    /** 上传人昵称 */
    private String uploaderName;

    /** 下载次数 */
    private Integer downloadCount;

    /** 上传时间 */
    private LocalDateTime createTime;
}
