package com.company.devplatform.module.weekly.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.company.devplatform.common.Result;
import com.company.devplatform.module.weekly.dto.WeeklyGroupedQuery;
import com.company.devplatform.module.weekly.dto.WeeklyReportConfirmDTO;
import com.company.devplatform.module.weekly.service.WeeklyReportService;
import com.company.devplatform.module.weekly.vo.WeeklyGroupedVO;
import com.company.devplatform.module.weekly.vo.WeeklyRecentDayStatVO;
import com.company.devplatform.module.weekly.vo.WeeklyReportPreviewVO;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

/**
 * WeeklySanity 周度测试报告接口（独立于 DailySanity，数据不共享）
 */
@RestController
@RequestMapping("/api/weekly")
@RequiredArgsConstructor
public class WeeklyReportController {

    private final WeeklyReportService weeklyReportService;

    /** 上传多个 HTML 报告并解析预览（每个文件为一个模块） */
    @PostMapping("/report/preview")
    @SaCheckPermission("sonic:view")
    public Result<WeeklyReportPreviewVO> preview(@RequestParam("files") List<MultipartFile> files) {
        return Result.ok(weeklyReportService.previewReport(files));
    }

    /** 预览确认入库（为每个模块×机型生成独立周度报告与失败用例） */
    @PostMapping("/report/confirm")
    @SaCheckPermission("sonic:view")
    public Result<List<Long>> confirm(@Valid @RequestBody WeeklyReportConfirmDTO dto) {
        return Result.ok(weeklyReportService.confirmReport(dto));
    }

    /** 按日期+分支×机型分组查询失败用例 */
    @GetMapping("/report/grouped")
    @SaCheckPermission("sonic:view")
    public Result<List<WeeklyGroupedVO>> listGroupedCases(WeeklyGroupedQuery query) {
        if (query.getDate() == null) {
            query.setDate(LocalDate.now());
        }
        return Result.ok(weeklyReportService.listGroupedCases(query));
    }

    /** 最近7天分析完成统计 */
    @GetMapping("/report/recent-week-stats")
    @SaCheckPermission("sonic:view")
    public Result<List<WeeklyRecentDayStatVO>> recentWeekStats() {
        return Result.ok(weeklyReportService.recentWeekStats());
    }

    /** 获取原始测试报告文件内容（新窗口打开） */
    @GetMapping("/reports/{fileId}/content")
    @SaCheckPermission("sonic:view")
    public void reportContent(@PathVariable Long fileId, HttpServletResponse response) {
        weeklyReportService.outputReportContent(fileId, response);
    }
}
