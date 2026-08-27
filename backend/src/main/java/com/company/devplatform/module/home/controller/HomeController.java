package com.company.devplatform.module.home.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.company.devplatform.common.Result;
import com.company.devplatform.module.home.service.HomeService;
import com.company.devplatform.module.home.vo.HomeSummaryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Home 总览
 */
@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    /** 总览聚合（各板块统计 + 最新动态） */
    @GetMapping("/summary")
    @SaCheckLogin
    public Result<HomeSummaryVO> summary() {
        return Result.ok(homeService.summary());
    }
}
