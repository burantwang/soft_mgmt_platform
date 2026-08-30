package com.company.devplatform.module.weekly.service;

import com.company.devplatform.module.weekly.dto.WeeklyGroupedQuery;
import com.company.devplatform.module.weekly.dto.WeeklyReportConfirmDTO;
import com.company.devplatform.module.weekly.vo.WeeklyGroupedVO;
import com.company.devplatform.module.weekly.vo.WeeklyRecentDayStatVO;
import com.company.devplatform.module.weekly.vo.WeeklyReportPreviewVO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * WeeklySanity 周度测试报告服务
 */
public interface WeeklyReportService {

    /**
     * 上传多个 HTML 报告并解析预览（每个文件独立解析为一个模块）
     */
    WeeklyReportPreviewVO previewReport(List<MultipartFile> files);

    /**
     * 预览确认入库：为每个模块×机型生成独立周度报告与失败用例
     *
     * @return 生成的周度报告ID列表
     */
    List<Long> confirmReport(WeeklyReportConfirmDTO dto);

    /**
     * 按日期+分支×机型分组查询失败用例
     */
    List<WeeklyGroupedVO> listGroupedCases(WeeklyGroupedQuery query);

    /**
     * 最近7天分析完成统计
     */
    List<WeeklyRecentDayStatVO> recentWeekStats();

    /**
     * 输出原始测试报告文件内容
     */
    void outputReportContent(Long fileId, HttpServletResponse response);
}
