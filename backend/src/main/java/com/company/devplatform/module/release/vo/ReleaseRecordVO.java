package com.company.devplatform.module.release.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 发布记录列表项/详情
 */
@Data
public class ReleaseRecordVO {

    private Long id;

    /** 代码分支(第一展示维度) */
    private String branch;

    /** 镜像版本号 */
    private String version;

    /** 发布结果:1成功 2失败 */
    private Integer result;

    private String resultDesc;

    /** 测试报告文件ID */
    private Long reportFileId;

    private String reportFileName;

    private Integer totalCount;

    private Integer passedCount;

    private Integer failedCount;

    private Integer errorCount;

    private Integer skippedCount;

    /** 通过率(%) */
    private BigDecimal passRate;

    /** 总耗时(秒) */
    private BigDecimal durationSec;

    /** 报告生成时间 */
    private LocalDateTime reportTime;

    /** 发布人 */
    private Long publisherId;

    private String publisherName;

    /** 发布时间 */
    private LocalDateTime publishTime;

    /** 来源:1人工上传 2Jenkins推送 3手动创建 */
    private Integer source;

    private String sourceDesc;

    private String remark;

    /** 关联机型 */
    private List<Long> projectIds;

    private List<String> projectNames;

    /** 关联失败任务ID（结果失败时展示入口） */
    private Long failTaskId;
}
