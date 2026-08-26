package com.company.devplatform.module.release.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 版本发布记录实体
 */
@Data
@TableName("release_record")
public class ReleaseRecord {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 代码分支(第一展示维度) */
    private String branch;

    /** 镜像版本号(Environment.Version) */
    private String version;

    /** 发布结果:1成功 2失败 */
    private Integer result;

    /** 测试报告文件ID */
    private Long reportFileId;

    /** 用例总数 */
    private Integer totalCount;

    private Integer passedCount;

    private Integer failedCount;

    private Integer errorCount;

    private Integer skippedCount;

    /** 总耗时(秒) */
    private BigDecimal durationSec;

    /** 报告生成时间 */
    private LocalDateTime reportTime;

    /** 发布人(用户ID) */
    private Long publisherId;

    /** 发布时间 */
    private LocalDateTime publishTime;

    /** 来源:1人工上传 2Jenkins推送 3手动创建 */
    private Integer source;

    /** 备注 */
    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 逻辑删除:0未删 1已删 */
    @TableLogic
    private Integer isDeleted;
}
