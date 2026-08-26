package com.company.devplatform.module.release.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.devplatform.common.Result;
import com.company.devplatform.module.auth.entity.SysUser;
import com.company.devplatform.module.auth.mapper.SysUserMapper;
import com.company.devplatform.module.release.vo.UserOptionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 失败任务模块辅助接口（启用用户下拉）
 */
@RestController
@RequestMapping("/api/release")
@RequiredArgsConstructor
public class ReleaseUserController {

    private final SysUserMapper userMapper;

    /** 启用用户下拉（指派任务责任人） */
    @GetMapping("/users/enabled")
    @SaCheckPermission("sonic:edit")
    public Result<List<UserOptionVO>> enabledUsers() {
        List<UserOptionVO> list = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getStatus, 1)
                        .orderByAsc(SysUser::getId))
                .stream().map(u -> {
                    UserOptionVO vo = new UserOptionVO();
                    vo.setId(u.getId());
                    vo.setUsername(u.getUsername());
                    vo.setNickname(StringUtils.hasText(u.getNickname()) ? u.getNickname() : u.getUsername());
                    return vo;
                }).collect(Collectors.toList());
        return Result.ok(list);
    }
}
