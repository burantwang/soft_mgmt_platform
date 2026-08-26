package com.company.devplatform.module.auth.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.devplatform.module.auth.dto.RoleDTO;
import com.company.devplatform.module.auth.vo.RoleVO;

import java.util.List;

/**
 * 角色管理服务
 */
public interface RoleService {

    Page<RoleVO> page(int page, int size);

    /** 全部启用角色(下拉选项) */
    List<RoleVO> all();

    void create(RoleDTO dto);

    void update(RoleDTO dto);

    void delete(Long id);

    void assignPermissions(Long roleId, List<Long> permissionIds);

    /** 获取角色已分配权限点ID列表 */
    List<Long> getPermissionIds(Long roleId);
}
