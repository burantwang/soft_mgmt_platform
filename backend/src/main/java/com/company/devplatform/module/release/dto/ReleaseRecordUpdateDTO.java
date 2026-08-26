package com.company.devplatform.module.release.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 发布记录编辑入参
 */
@Data
public class ReleaseRecordUpdateDTO {

    @NotNull(message = "记录ID不能为空")
    private Long id;

    @NotBlank(message = "代码分支不能为空")
    @Size(max = 128, message = "分支最长128字符")
    private String branch;

    @Size(max = 128, message = "版本号最长128字符")
    private String version;

    @NotEmpty(message = "请至少选择一个机型")
    private List<Long> projectIds;

    @Size(max = 255, message = "备注最长255字符")
    private String remark;
}
