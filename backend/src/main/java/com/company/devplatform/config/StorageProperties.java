package com.company.devplatform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文件存储配置
 * <p>存储根路径从环境变量 {@code FILE_STORAGE_ROOT} 读取，禁止硬编码；数据库仅存相对路径。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "file.storage")
public class StorageProperties {

    /** 文件存储根路径（绝对路径，运行环境注入） */
    private String root;

    /** 测试报告子目录 */
    private String reportDir = "report";

    /** 普通附件子目录 */
    private String attachmentDir = "attachment";
}
