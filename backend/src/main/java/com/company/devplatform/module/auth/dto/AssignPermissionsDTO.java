package com.company.devplatform.module.auth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AssignPermissionsDTO {

    @NotNull(message = "权限点ID列表不能为空")
    private List<Long> permissionIds;
}
