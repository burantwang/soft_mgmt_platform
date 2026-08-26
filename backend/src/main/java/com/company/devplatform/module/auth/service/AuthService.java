package com.company.devplatform.module.auth.service;

import com.company.devplatform.module.auth.dto.ChangePasswordDTO;
import com.company.devplatform.module.auth.dto.LoginDTO;
import com.company.devplatform.module.auth.vo.LoginResultVO;
import com.company.devplatform.module.auth.vo.UserVO;

/**
 * 认证服务：登录/登出/当前用户/修改密码
 */
public interface AuthService {

    LoginResultVO login(LoginDTO dto);

    void logout();

    UserVO getCurrentUser();

    void changePassword(ChangePasswordDTO dto);
}
