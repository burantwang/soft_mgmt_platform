package com.company.devplatform.module.release.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 发布记录-机型关联实体
 */
@Data
@TableName("release_record_project")
public class ReleaseRecordProject {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 发布记录ID */
    private Long recordId;

    /** 机型ID */
    private Long projectId;

    /** 是否主机型:1是 0否 */
    private Integer isPrimary;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 逻辑删除:0未删 1已删 */
    @TableLogic
    private Integer isDeleted;
}
