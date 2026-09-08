package com.company.devplatform.module.auth.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.devplatform.module.auth.dto.UserCreateDTO;
import com.company.devplatform.module.auth.dto.UserUpdateDTO;
import com.company.devplatform.module.auth.vo.UserVO;

import java.util.List;

/**
 * 用户管理服务
 */
public interface UserService {

    Page<UserVO> page(String keyword, Integer status, Long groupId, int page, int size);

    UserVO getById(Long id);

    void create(UserCreateDTO dto);

    void update(UserUpdateDTO dto);

    void delete(Long id);

    void resetPassword(Long id, String password);

    void updateStatus(Long id, Integer status);

    /** 获取用户角色编码列表 */
    List<String> getRoleCodes(Long userId);

    /** 获取用户权限码列表(超管返回全部权限码 + "*") */
    List<String> getPermCodes(Long userId);
}
