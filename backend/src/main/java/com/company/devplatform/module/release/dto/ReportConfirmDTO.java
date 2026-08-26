package com.company.devplatform.module.release.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 报告确认入库入参
 */
@Data
public class ReportConfirmDTO {

    /** 预览令牌 */
    @NotBlank(message = "预览令牌不能为空")
    private String previewToken;

    /** 代码分支（报告内无此信息，人工补录） */
    @NotBlank(message = "代码分支不能为空")
    @Size(max = 128, message = "分支最长128字符")
    private String branch;

    /** 镜像版本号（缺省时取报告 Environment.Version） */
    @Size(max = 128, message = "版本号最长128字符")
    private String version;

    /** 关联机型ID列表（至少一个） */
    @NotEmpty(message = "请至少选择一个机型")
    private List<Long> projectIds;

    @Size(max = 255, message = "备注最长255字符")
    private String remark;
}
