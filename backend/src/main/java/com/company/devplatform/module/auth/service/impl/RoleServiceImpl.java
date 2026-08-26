package com.company.devplatform.module.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.devplatform.common.ErrorCode;
import com.company.devplatform.common.exception.BusinessException;
import com.company.devplatform.module.auth.dto.RoleDTO;
import com.company.devplatform.module.auth.entity.SysPermission;
import com.company.devplatform.module.auth.entity.SysRole;
import com.company.devplatform.module.auth.entity.SysRolePermission;
import com.company.devplatform.module.auth.entity.SysUserRole;
import com.company.devplatform.module.auth.mapper.SysPermissionMapper;
import com.company.devplatform.module.auth.mapper.SysRoleMapper;
import com.company.devplatform.module.auth.mapper.SysRolePermissionMapper;
import com.company.devplatform.module.auth.mapper.SysUserRoleMapper;
import com.company.devplatform.module.auth.service.RoleService;
import com.company.devplatform.module.auth.vo.RoleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

/**
 * 角色管理服务实现
 */
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final SysRoleMapper roleMapper;
    private final SysPermissionMapper permissionMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysUserRoleMapper userRoleMapper;

    @Override
    public Page<RoleVO> page(int page, int size) {
        Page<SysRole> p = roleMapper.selectPage(new Page<>(page, size),
                Wrappers.<SysRole>lambdaQuery().orderByDesc(SysRole::getCreateTime));
        Page<RoleVO> result = new Page<>(p.getCurrent(), p.getSize(), p.getTotal());
        result.setRecords(p.getRecords().stream().map(this::toRoleVO).toList());
        return result;
    }

    @Override
    public List<RoleVO> all() {
        List<SysRole> roles = roleMapper.selectList(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getStatus, 1)
                .orderByAsc(SysRole::getId));
        return roles.stream().map(r -> {
            RoleVO vo = new RoleVO();
            BeanUtils.copyProperties(r, vo);
            return vo;
        }).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(RoleDTO dto) {
        long cnt = roleMapper.selectCount(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getRoleCode, dto.getRoleCode()));
        if (cnt > 0) {
            throw new BusinessException(ErrorCode.DATA_EXIST, "角色编码已存在");
        }
        SysRole role = new SysRole();
        role.setRoleCode(dto.getRoleCode());
        role.setRoleName(dto.getRoleName());
        role.setRemark(dto.getRemark());
        role.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        roleMapper.insert(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(RoleDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "角色ID不能为空");
        }
        SysRole role = roleMapper.selectById(dto.getId());
        if (role == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "角色不存在");
        }
        long cnt = roleMapper.selectCount(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getRoleCode, dto.getRoleCode())
                .ne(SysRole::getId, dto.getId()));
        if (cnt > 0) {
            throw new BusinessException(ErrorCode.DATA_EXIST, "角色编码已存在");
        }
        role.setRoleCode(dto.getRoleCode());
        role.setRoleName(dto.getRoleName());
        role.setRemark(dto.getRemark());
        if (dto.getStatus() != null) {
            role.setStatus(dto.getStatus());
        }
        roleMapper.updateById(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "角色不存在");
        }
        if (UserServiceImpl.SUPER_ADMIN_CODE.equals(role.getRoleCode())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "系统内置角色不允许删除");
        }
        long userCnt = userRoleMapper.selectCount(Wrappers.<SysUserRole>lambdaQuery()
                .eq(SysUserRole::getRoleId, id));
        if (userCnt > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该角色已分配用户，无法删除");
        }
        roleMapper.deleteById(id);
        rolePermissionMapper.delete(Wrappers.<SysRolePermission>lambdaQuery()
                .eq(SysRolePermission::getRoleId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        if (roleMapper.selectById(roleId) == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "角色不存在");
        }
        if (permissionIds != null && !permissionIds.isEmpty()) {
            List<Long> distinctIds = permissionIds.stream().distinct().toList();
            if (permissionMapper.selectBatchIds(distinctIds).size() != distinctIds.size()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "存在无效的权限点");
            }
        }
        rolePermissionMapper.delete(Wrappers.<SysRolePermission>lambdaQuery()
                .eq(SysRolePermission::getRoleId, roleId));
        if (permissionIds == null || permissionIds.isEmpty()) {
            return;
        }
        for (Long permissionId : permissionIds.stream().distinct().toList()) {
            SysRolePermission relation = new SysRolePermission();
            relation.setRoleId(roleId);
            relation.setPermissionId(permissionId);
            rolePermissionMapper.insert(relation);
        }
    }

    @Override
    public List<Long> getPermissionIds(Long roleId) {
        List<SysRolePermission> rps = rolePermissionMapper.selectList(Wrappers.<SysRolePermission>lambdaQuery()
                .eq(SysRolePermission::getRoleId, roleId));
        return rps.stream().map(SysRolePermission::getPermissionId).toList();
    }

    private RoleVO toRoleVO(SysRole role) {
        RoleVO vo = new RoleVO();
        BeanUtils.copyProperties(role, vo);
        vo.setPermissionIds(getPermissionIds(role.getId()));
        vo.setUserCount(userRoleMapper.selectCount(Wrappers.<SysUserRole>lambdaQuery()
                .eq(SysUserRole::getRoleId, role.getId())));
        return vo;
    }
}
