package com.company.devplatform.module.wiki.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 文件重命名入参
 */
@Data
public class FileRenameDTO {

    /** 文件ID */
    @NotNull(message = "文件ID不能为空")
    private Long fileId;

    /** 新文件名(仅允许修改主名,扩展名必须与原文件一致) */
    @NotBlank(message = "新文件名不能为空")
    @Size(max = 255, message = "文件名不能超过255个字符")
    private String newName;
}
