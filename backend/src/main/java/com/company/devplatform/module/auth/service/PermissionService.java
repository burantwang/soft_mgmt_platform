package com.company.devplatform.module.auth.service;

import com.company.devplatform.module.auth.vo.PermissionVO;

import java.util.List;

/**
 * 权限点管理服务
 */
public interface PermissionService {

    List<PermissionVO> list();
}
