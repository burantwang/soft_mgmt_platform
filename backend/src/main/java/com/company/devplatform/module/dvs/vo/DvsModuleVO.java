package com.company.devplatform.module.dvs.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DVS 分组内模块(HTML)统计
 * <p>一个机型版本的结果可能包含 A.html、B.html 等多个模块，每个模块单独记录统计。</p>
 */
@Data
public class DvsModuleVO {

    /** DVS报告ID */
    private Long reportId;

    /** 模块名(HTML文件名) */
    private String moduleName;

    /** 发布结果:1成功 2失败 */
    private Integer result;

    private String resultDesc;

    private Integer totalCount;

    private Integer passedCount;

    private Integer failedCount;

    private Integer errorCount;

    private Integer skippedCount;

    /** 通过率(%) */
    private Integer passRate;

    /** 总耗时(秒) */
    private BigDecimal durationSec;

    /** 报告生成时间 */
    private LocalDateTime reportTime;

    /** 报告文件ID */
    private Long reportFileId;

    /** 报告文件名 */
    private String reportFileName;
}
