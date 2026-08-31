package com.company.devplatform.module.dvs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * DVS 报告确认入库入参
 * <p>预览解析多个 HTML 后，确认时补录分支/机型，为每个模块（HTML）生成独立 DVS 报告。</p>
 */
@Data
public class DvsReportConfirmDTO {

    /** 预览令牌（多文件共用一个令牌） */
    @NotBlank(message = "预览令牌不能为空")
    private String previewToken;

    /** 代码分支 */
    @NotBlank(message = "代码分支不能为空")
    private String branch;

    /** 镜像版本号（留空使用报告内 Environment.Version） */
    private String version;

    /** 关联机型ID列表（每个机型×每个模块各生成一条 DVS 报告） */
    @NotEmpty(message = "请至少选择一个机型")
    private List<Long> projectIds;

    /** 备注 */
    private String remark;
}
