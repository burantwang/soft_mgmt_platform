package com.company.devplatform.module.release.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.company.devplatform.module.release.dto.JenkinsPushDTO;
import com.company.devplatform.module.release.dto.ReleaseRecordCreateDTO;
import com.company.devplatform.module.release.dto.ReleaseRecordUpdateDTO;
import com.company.devplatform.module.release.dto.ReportConfirmDTO;
import com.company.devplatform.module.release.vo.ReleaseRecordExcelVO;
import com.company.devplatform.module.release.vo.ReleaseRecordVO;
import com.company.devplatform.module.release.vo.ReportPreviewVO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 版本发布记录服务
 */
public interface ReleaseRecordService {

    /** 分页查询 */
    IPage<ReleaseRecordVO> page(int page, int size, String branch, Integer result, Integer source,
                                Long projectId, LocalDateTime startTime, LocalDateTime endTime);

    /** 已出现过的分支列表（历史筛选下拉） */
    List<String> listBranches();

    /** 导出（Excel 数据，与分页查询相同筛选条件，全量） */
    List<ReleaseRecordExcelVO> exportRecords(String branch, Integer result, Integer source,
                                             Long projectId, LocalDateTime startTime, LocalDateTime endTime);

    /** 详情 */
    ReleaseRecordVO detail(Long id);

    /** 手动创建（source=3） */
    Long createManual(ReleaseRecordCreateDTO dto);

    /** 编辑 */
    void update(ReleaseRecordUpdateDTO dto);

    /** 删除（含报告文件、关联机型、失败任务联动） */
    void delete(Long id);

    /** 报告上传解析预览（source=1 第一步） */
    ReportPreviewVO previewReport(MultipartFile file);

    /** 报告确认入库（source=1 第二步） */
    Long confirmReport(ReportConfirmDTO dto);

    /** Jenkins 推送入库（source=2，带报告文件） */
    Long createFromJenkinsWithReport(String branch, String version, String imageUrl, List<String> projectCodes,
                                     String remark, FileStorageService.StoredFile storedFile);

    /** Jenkins 推送入库（source=2，JSON 统计数字） */
    Long createFromJenkinsJson(JenkinsPushDTO dto);

    /** 由统计数字构建记录并入库（Jenkins/JSON 共用） */
    Long createRecord(Integer source, String branch, String version, String imageUrl, List<Long> projectIds,
                      String remark, Integer result, Integer totalCount, Integer passedCount, Integer failedCount,
                      Integer errorCount, Integer skippedCount, BigDecimal durationSec,
                      LocalDateTime reportTime, Long reportFileId, Long publisherId,
                      List<ReportPreviewVO.FailCase> failCases);

    /** 输出原始测试报告文件内容（失败任务追踪查看原始 HTML 报告） */
    void outputReportContent(Long fileId, HttpServletResponse response);
}
