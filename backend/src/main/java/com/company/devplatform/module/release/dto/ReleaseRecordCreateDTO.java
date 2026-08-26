package com.company.devplatform.module.release.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 发布记录手动创建入参
 */
@Data
public class ReleaseRecordCreateDTO {

    @NotBlank(message = "代码分支不能为空")
    @Size(max = 128, message = "分支最长128字符")
    private String branch;

    @Size(max = 128, message = "版本号最长128字符")
    private String version;

    @Size(max = 500, message = "镜像地址最长500字符")
    private String imageUrl;

    /** 发布结果:1成功 2失败 */
    @NotNull(message = "发布结果不能为空")
    private Integer result;

    /** 关联机型ID列表（至少一个） */
    @NotEmpty(message = "请至少选择一个机型")
    private List<Long> projectIds;

    @Size(max = 255, message = "备注最长255字符")
    private String remark;
}
