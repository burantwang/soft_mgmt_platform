package com.company.devplatform.module.weekly.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * WeeklySanity 周度测试报告实体
 * <p>每个 HTML 模块一条记录；一次上传（分支×机型）可包含多个 HTML 模块。</p>
 */
@Data
@TableName("weekly_report")
public class WeeklyReport {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 代码分支 */
    private String branch;

    /** 镜像版本号 */
    private String version;

    /** 机型ID(project) */
    private Long projectId;

    /** 模块名(HTML文件名) */
    private String moduleName;

    /** 测试报告文件ID(file_resource) */
    private Long reportFileId;

    /** 用例总数 */
    private Integer totalCount;

    /** 通过数 */
    private Integer passedCount;

    /** 失败数 */
    private Integer failedCount;

    /** 错误数 */
    private Integer errorCount;

    /** 跳过数 */
    private Integer skippedCount;

    /** 总耗时(秒) */
    private BigDecimal durationSec;

    /** 报告生成时间 */
    private LocalDateTime reportTime;

    /** 上传人(用户ID) */
    private Long publisherId;

    /** 上传时间 */
    private LocalDateTime publishTime;

    /** 备注 */
    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 逻辑删除:0未删 1已删 */
    @TableLogic
    private Integer isDeleted;
}
