package com.company.devplatform.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 鉴权配置
 * <p>所有业务接口统一拦截校验登录状态；登录接口与 Jenkins 开放接口放行。
 * 细粒度模块权限通过接口 {@code @SaCheckPermission} 注解实现。</p>
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> SaRouter
                        // 统一拦截 /api 下所有接口
                        .match("/api/**")
                        // 放行：登录接口、Jenkins 开放接口、错误页
                        .notMatch("/api/auth/login", "/api/open/**")
                        .check(r -> StpUtil.checkLogin())
                ))
                .addPathPatterns("/**");
    }
}
