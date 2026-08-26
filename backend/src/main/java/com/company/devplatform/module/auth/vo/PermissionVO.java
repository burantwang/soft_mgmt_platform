package com.company.devplatform.module.auth.vo;

import lombok.Data;

/**
 * 权限点 VO
 */
@Data
public class PermissionVO {

    private Long id;

    private String permCode;

    private String permName;

    /** 所属模块(sonic/wiki/system) */
    private String module;

    /** 模块名称 */
    private String moduleName;
}
