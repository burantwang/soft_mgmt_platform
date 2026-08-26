package com.company.devplatform.module.release.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.company.devplatform.common.Result;
import com.company.devplatform.module.release.service.DashboardService;
import com.company.devplatform.module.release.vo.DashboardSummaryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 发布统计看板
 */
@RestController
@RequestMapping("/api/release/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /** 看板统计汇总（总览 + 单日 + 按分支 + 按机型 + 近15天趋势），date 不传默认今天 */
    @GetMapping("/summary")
    @SaCheckLogin
    @SaCheckPermission("sonic:view")
    public Result<DashboardSummaryVO> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.ok(dashboardService.summary(date));
    }
}
