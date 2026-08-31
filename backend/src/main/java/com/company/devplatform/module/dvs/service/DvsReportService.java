package com.company.devplatform.module.dvs.service;

import com.company.devplatform.module.dvs.dto.DvsGroupedQuery;
import com.company.devplatform.module.dvs.dto.DvsReportConfirmDTO;
import com.company.devplatform.module.dvs.vo.DvsFailCaseExcelVO;
import com.company.devplatform.module.dvs.vo.DvsGroupedVO;
import com.company.devplatform.module.dvs.vo.DvsRecentDayStatVO;
import com.company.devplatform.module.dvs.vo.DvsReportPreviewVO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * DVS 测试报告服务
 */
public interface DvsReportService {

    /**
     * 上传多个 HTML 报告并解析预览（每个文件独立解析为一个模块）
     */
    DvsReportPreviewVO previewReport(List<MultipartFile> files);

    /**
     * 预览确认入库：为每个模块×机型生成独立 DVS 报告与失败用例
     *
     * @return 生成的 DVS 报告ID列表
     */
    List<Long> confirmReport(DvsReportConfirmDTO dto);

    /**
     * 按日期+分支×机型分组查询失败用例
     */
    List<DvsGroupedVO> listGroupedCases(DvsGroupedQuery query);

    /**
     * 最近7天分析完成统计
     */
    List<DvsRecentDayStatVO> recentWeekStats();

    /**
     * 输出原始测试报告文件内容
     */
    void outputReportContent(Long fileId, HttpServletResponse response);

    /**
     * 导出某日期全部失败用例（每个项目一个 Sheet）
     *
     * @return sheet名 -> 用例行列表
     */
    Map<String, List<DvsFailCaseExcelVO>> exportByDate(LocalDate date);
}
