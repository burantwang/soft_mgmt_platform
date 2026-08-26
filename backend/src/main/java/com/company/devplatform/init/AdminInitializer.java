package com.company.devplatform.init;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.company.devplatform.module.auth.entity.SysRole;
import com.company.devplatform.module.auth.entity.SysUser;
import com.company.devplatform.module.auth.entity.SysUserRole;
import com.company.devplatform.module.auth.mapper.SysRoleMapper;
import com.company.devplatform.module.auth.mapper.SysUserMapper;
import com.company.devplatform.module.auth.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 超管账号初始化器
 * <p>启动时检查 sys_user 中是否存在 admin，不存在则创建并绑定 super_admin 角色。
 * 初始密码通过环境变量 ADMIN_INIT_PASSWORD 注入，默认 Admin@123，首次登录强制改密。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private static final String SUPER_ADMIN_CODE = "super_admin";
    private static final String ADMIN_USERNAME = "admin";

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${admin.init-password:Admin@123}")
    private String initPassword;

    @Override
    public void run(ApplicationArguments args) {
        SysRole superRole = roleMapper.selectOne(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getRoleCode, SUPER_ADMIN_CODE));
        if (superRole == null) {
            log.warn("[初始化] 未找到 super_admin 角色，跳过超管账号创建");
            return;
        }
        Long count = userMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, ADMIN_USERNAME));
        if (count > 0) {
            return;
        }

        SysUser admin = new SysUser();
        admin.setUsername(ADMIN_USERNAME);
        admin.setPassword(passwordEncoder.encode(initPassword));
        admin.setNickname("超级管理员");
        admin.setStatus(1);
        admin.setMustChangePwd(1);
        userMapper.insert(admin);

        SysUserRole relation = new SysUserRole();
        relation.setUserId(admin.getId());
        relation.setRoleId(superRole.getId());
        userRoleMapper.insert(relation);

        log.warn("[初始化] 超管账号 admin 已创建，首次登录需修改初始密码");
    }
}
