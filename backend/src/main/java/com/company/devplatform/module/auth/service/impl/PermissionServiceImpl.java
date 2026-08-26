package com.company.devplatform.module.auth.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.company.devplatform.module.auth.entity.SysPermission;
import com.company.devplatform.module.auth.mapper.SysPermissionMapper;
import com.company.devplatform.module.auth.service.PermissionService;
import com.company.devplatform.module.auth.vo.PermissionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 权限点管理服务实现
 */
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final SysPermissionMapper permissionMapper;

    @Override
    public List<PermissionVO> list() {
        List<SysPermission> permissions = permissionMapper.selectList(
                Wrappers.<SysPermission>lambdaQuery()
                        .orderByAsc(SysPermission::getModule)
                        .orderByAsc(SysPermission::getId));
        return permissions.stream().map(p -> {
            PermissionVO vo = new PermissionVO();
            BeanUtils.copyProperties(p, vo);
            return vo;
        }).toList();
    }
}
