package com.company.devplatform.module.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.company.devplatform.common.ErrorCode;
import com.company.devplatform.common.exception.BusinessException;
import com.company.devplatform.module.auth.dto.ChangePasswordDTO;
import com.company.devplatform.module.auth.dto.LoginDTO;
import com.company.devplatform.module.auth.entity.SysUser;
import com.company.devplatform.module.auth.mapper.SysUserMapper;
import com.company.devplatform.module.auth.service.AuthService;
import com.company.devplatform.module.auth.service.UserService;
import com.company.devplatform.module.auth.vo.LoginResultVO;
import com.company.devplatform.module.auth.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 认证服务实现
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper userMapper;
    private final UserService userService;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public LoginResultVO login(LoginDTO dto) {
        SysUser user = userMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, dto.getUsername()));
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }

        StpUtil.login(user.getId());
        LoginResultVO result = new LoginResultVO();
        result.setToken(StpUtil.getTokenValue());
        result.setUserInfo(userService.getById(user.getId()));
        result.setRoles(userService.getRoleCodes(user.getId()));
        result.setPermissions(userService.getPermCodes(user.getId()));
        return result;
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    @Override
    public UserVO getCurrentUser() {
        Long userId = StpUtil.getLoginIdAsLong();
        return userService.getById(userId);
    }

    @Override
    public void changePassword(ChangePasswordDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = userMapper.selectById(userId);
        if (user == null || !passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "原密码错误");
        }
        String newPassword = dto.getNewPassword();
        if (newPassword == null || newPassword.length() < 6) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "新密码长度不能少于6位");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePwd(0);
        userMapper.updateById(user);
    }
}
