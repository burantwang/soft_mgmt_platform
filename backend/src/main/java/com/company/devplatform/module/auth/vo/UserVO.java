package com.company.devplatform.module.auth.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户 VO
 */
@Data
public class UserVO {

    private Long id;

    private String username;

    private String nickname;

    private String email;

    private String phone;

    /** 状态:1正常 0禁用 */
    private Integer status;

    /** 是否强制改密:1是 0否 */
    private Integer mustChangePwd;

    private String remark;

    /** 角色ID列表(编辑回显) */
    private List<Long> roleIds;

    /** 角色名称列表(展示) */
    private List<String> roleNames;

    /** 角色编码列表(用于前端权限判断：super_admin/admin/employee) */
    private List<String> roleCodes;

    /** 所属组ID列表(编辑回显) */
    private List<Long> groupIds;

    /** 所属组名称列表(展示) */
    private List<String> groupNames;

    private LocalDateTime createTime;
}
