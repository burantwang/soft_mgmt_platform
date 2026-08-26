package com.company.devplatform.module.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.devplatform.common.ErrorCode;
import com.company.devplatform.common.exception.BusinessException;
import com.company.devplatform.module.auth.dto.UserCreateDTO;
import com.company.devplatform.module.auth.dto.UserUpdateDTO;
import com.company.devplatform.module.auth.entity.SysPermission;
import com.company.devplatform.module.auth.entity.SysRole;
import com.company.devplatform.module.auth.entity.SysRolePermission;
import com.company.devplatform.module.auth.entity.SysUser;
import com.company.devplatform.module.auth.entity.SysUserRole;
import com.company.devplatform.module.auth.mapper.SysPermissionMapper;
import com.company.devplatform.module.auth.mapper.SysRoleMapper;
import com.company.devplatform.module.auth.mapper.SysRolePermissionMapper;
import com.company.devplatform.module.auth.mapper.SysUserMapper;
import com.company.devplatform.module.auth.mapper.SysUserRoleMapper;
import com.company.devplatform.module.auth.service.UserService;
import com.company.devplatform.module.auth.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * 用户管理服务实现
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    /** 系统超管角色编码 */
    public static final String SUPER_ADMIN_CODE = "super_admin";
    /** 默认初始密码 */
    private static final String DEFAULT_PASSWORD = "123456";

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysPermissionMapper permissionMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public Page<UserVO> page(String keyword, Integer status, int page, int size) {
        LambdaQueryWrapper<SysUser> qw = Wrappers.lambdaQuery();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            qw.and(w -> w.like(SysUser::getUsername, kw).or().like(SysUser::getNickname, kw));
        }
        qw.eq(status != null, SysUser::getStatus, status);
        qw.orderByDesc(SysUser::getCreateTime);

        Page<SysUser> p = userMapper.selectPage(new Page<>(page, size), qw);
        Page<UserVO> result = new Page<>(p.getCurrent(), p.getSize(), p.getTotal());
        result.setRecords(p.getRecords().stream().map(this::toUserVO).toList());
        return result;
    }

    @Override
    public UserVO getById(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "用户不存在");
        }
        return toUserVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(UserCreateDTO dto) {
        long cnt = userMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, dto.getUsername()));
        if (cnt > 0) {
            throw new BusinessException(ErrorCode.DATA_EXIST, "登录账号已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        String pwd = StringUtils.hasText(dto.getPassword()) ? dto.getPassword() : DEFAULT_PASSWORD;
        user.setPassword(passwordEncoder.encode(pwd));
        user.setNickname(dto.getNickname());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setRemark(dto.getRemark());
        user.setStatus(1);
        user.setMustChangePwd(1);
        userMapper.insert(user);

        bindRoles(user.getId(), dto.getRoleIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(UserUpdateDTO dto) {
        SysUser user = userMapper.selectById(dto.getId());
        if (user == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "用户不存在");
        }
        user.setNickname(dto.getNickname());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setRemark(dto.getRemark());
        if (dto.getStatus() != null) {
            user.setStatus(dto.getStatus());
        }
        userMapper.updateById(user);

        if (dto.getRoleIds() != null) {
            bindRoles(user.getId(), dto.getRoleIds());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Long currentId = StpUtil.getLoginIdAsLong();
        if (currentId.equals(id)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "不能删除当前登录账号");
        }
        if (userMapper.selectById(id) == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "用户不存在");
        }
        userMapper.deleteById(id);
        userRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, id));
    }

    @Override
    public void resetPassword(Long id, String password) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "用户不存在");
        }
        String pwd = StringUtils.hasText(password) ? password : DEFAULT_PASSWORD;
        user.setPassword(passwordEncoder.encode(pwd));
        user.setMustChangePwd(1);
        userMapper.updateById(user);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        if (status != null && status == 0) {
            Long currentId = StpUtil.getLoginIdAsLong();
            if (currentId.equals(id)) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "不能禁用当前登录账号");
            }
        }
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "用户不存在");
        }
        user.setStatus(status);
        userMapper.updateById(user);
    }

    @Override
    public List<String> getRoleCodes(Long userId) {
        List<SysUserRole> relations = userRoleMapper.selectList(Wrappers.<SysUserRole>lambdaQuery()
                .eq(SysUserRole::getUserId, userId));
        if (relations.isEmpty()) {
            return List.of();
        }
        List<Long> roleIds = relations.stream().map(SysUserRole::getRoleId).distinct().toList();
        List<SysRole> roles = roleMapper.selectBatchIds(roleIds);
        return roles.stream().map(SysRole::getRoleCode).filter(Objects::nonNull).distinct().toList();
    }

    @Override
    public List<String> getPermCodes(Long userId) {
        List<String> roleCodes = getRoleCodes(userId);
        if (roleCodes.isEmpty()) {
            return List.of();
        }
        // 超管:返回全部权限码 + "*"(Sa-Token 通配)
        if (roleCodes.contains(SUPER_ADMIN_CODE)) {
            List<String> all = permissionMapper.selectList(null).stream()
                    .map(SysPermission::getPermCode)
                    .filter(Objects::nonNull)
                    .toList();
            List<String> result = new ArrayList<>(all);
            result.add("*");
            return result;
        }
        List<SysUserRole> relations = userRoleMapper.selectList(Wrappers.<SysUserRole>lambdaQuery()
                .eq(SysUserRole::getUserId, userId));
        List<Long> roleIds = relations.stream().map(SysUserRole::getRoleId).distinct().toList();
        if (roleIds.isEmpty()) {
            return List.of();
        }
        List<SysRolePermission> rps = rolePermissionMapper.selectList(Wrappers.<SysRolePermission>lambdaQuery()
                .in(SysRolePermission::getRoleId, roleIds));
        if (rps.isEmpty()) {
            return List.of();
        }
        List<Long> permIds = rps.stream().map(SysRolePermission::getPermissionId).distinct().toList();
        return permissionMapper.selectBatchIds(permIds).stream()
                .map(SysPermission::getPermCode)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    /** 替换用户角色绑定 */
    @Transactional(rollbackFor = Exception.class)
    public void bindRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, userId));
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        List<Long> distinctIds = roleIds.stream().distinct().toList();
        if (roleMapper.selectBatchIds(distinctIds).size() != distinctIds.size()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "存在无效的角色");
        }
        for (Long roleId : distinctIds) {
            SysUserRole relation = new SysUserRole();
            relation.setUserId(userId);
            relation.setRoleId(roleId);
            userRoleMapper.insert(relation);
        }
    }

    private UserVO toUserVO(SysUser user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        List<SysUserRole> relations = userRoleMapper.selectList(Wrappers.<SysUserRole>lambdaQuery()
                .eq(SysUserRole::getUserId, user.getId()));
        if (!relations.isEmpty()) {
            List<Long> roleIds = relations.stream().map(SysUserRole::getRoleId).toList();
            List<SysRole> roles = roleMapper.selectBatchIds(roleIds);
            vo.setRoleIds(roleIds);
            vo.setRoleNames(roles.stream().map(SysRole::getRoleName).filter(Objects::nonNull).toList());
        } else {
            vo.setRoleIds(List.of());
            vo.setRoleNames(List.of());
        }
        return vo;
    }
}
