package com.company.devplatform.module.release.vo;

import lombok.Data;

/**
 * 原始测试报告文件项（用于失败任务分组查看原始 HTML 报告）
 */
@Data
public class ReportFileItemVO {

    /** 文件资源ID */
    private Long fileId;

    /** 原始文件名 */
    private String fileName;
}
