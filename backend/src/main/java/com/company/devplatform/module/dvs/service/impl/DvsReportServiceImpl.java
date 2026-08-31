package com.company.devplatform.module.dvs.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.devplatform.common.ErrorCode;
import com.company.devplatform.common.exception.BusinessException;
import com.company.devplatform.module.auth.entity.SysUser;
import com.company.devplatform.module.auth.mapper.SysUserMapper;
import com.company.devplatform.module.release.entity.FileResource;
import com.company.devplatform.module.release.entity.ReleaseProject;
import com.company.devplatform.module.release.enums.FailCaseStatus;
import com.company.devplatform.module.release.enums.FileType;
import com.company.devplatform.module.release.enums.ReleaseResult;
import com.company.devplatform.module.release.mapper.FileResourceMapper;
import com.company.devplatform.module.release.mapper.ReleaseProjectMapper;
import com.company.devplatform.module.release.service.FileStorageService;
import com.company.devplatform.module.release.util.HtmlReportParser;
import com.company.devplatform.module.dvs.util.DvsHtmlReportParser;
import com.company.devplatform.module.dvs.dto.DvsGroupedQuery;
import com.company.devplatform.module.dvs.dto.DvsReportConfirmDTO;
import com.company.devplatform.module.dvs.entity.DvsFailCase;
import com.company.devplatform.module.dvs.entity.DvsReport;
import com.company.devplatform.module.dvs.mapper.DvsFailCaseMapper;
import com.company.devplatform.module.dvs.mapper.DvsReportMapper;
import com.company.devplatform.module.dvs.service.DvsReportPreviewStore;
import com.company.devplatform.module.dvs.service.DvsReportService;
import com.company.devplatform.module.dvs.vo.DvsFailCaseExcelVO;
import com.company.devplatform.module.dvs.vo.DvsFailCaseVO;
import com.company.devplatform.module.dvs.vo.DvsGroupedVO;
import com.company.devplatform.module.dvs.vo.DvsModuleVO;
import com.company.devplatform.module.dvs.vo.DvsPreviewItemVO;
import com.company.devplatform.module.dvs.vo.DvsRecentDayStatVO;
import com.company.devplatform.module.dvs.vo.DvsReportPreviewVO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * DVS 测试报告服务实现
 * <p>独立于 DailySanity/WeeklySanity：数据不共享，按 分支×机型 分组，组内按 HTML 模块单独统计、处理、指派。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DvsReportServiceImpl implements DvsReportService {

    private final DvsReportMapper reportMapper;
    private final DvsFailCaseMapper failCaseMapper;
    private final ReleaseProjectMapper projectMapper;
    private final SysUserMapper userMapper;
    private final FileResourceMapper fileResourceMapper;
    private final FileStorageService fileStorageService;
    private final DvsReportPreviewStore previewStore;

    @Override
    public DvsReportPreviewVO previewReport(List<MultipartFile> files) {
        if (CollectionUtils.isEmpty(files)) {
            throw new BusinessException(ErrorCode.FILE_EMPTY);
        }
        List<DvsReportPreviewStore.Item> items = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                throw new BusinessException(ErrorCode.FILE_EMPTY, "存在空文件，请检查上传内容");
            }
            FileStorageService.StoredFile stored = fileStorageService.storeReportFile(file);
            byte[] content;
            try {
                content = file.getBytes();
            } catch (Exception e) {
                fileStorageService.deletePhysical(stored);
                throw new BusinessException(ErrorCode.FILE_READ_ERROR);
            }
            HtmlReportParser.ParseResult parsed;
            try {
                parsed = DvsHtmlReportParser.parse(content);
            } catch (BusinessException e) {
                fileStorageService.deletePhysical(stored);
                throw e;
            }
            DvsPreviewItemVO vo = new DvsPreviewItemVO();
            vo.setFileName(stored.getFileName());
            vo.setFileSize(stored.getFileSize());
            vo.setVersion(parsed.getVersion());
            vo.setTotalCount(parsed.getTotalCount());
            vo.setPassedCount(parsed.getPassedCount());
            vo.setFailedCount(parsed.getFailedCount());
            vo.setErrorCount(parsed.getErrorCount());
            vo.setSkippedCount(parsed.getSkippedCount());
            vo.setDurationSec(parsed.getDurationSec());
            vo.setReportTime(parsed.getReportTime());
            int result = (parsed.getFailedCount() > 0 || parsed.getErrorCount() > 0)
                    ? ReleaseResult.FAILED.getCode() : ReleaseResult.SUCCESS.getCode();
            vo.setResult(result);
            vo.setResultDesc(ReleaseResult.of(result).getDesc());
            List<DvsPreviewItemVO.FailCase> failCases = new ArrayList<>();
            if (parsed.getFailCases() != null) {
                for (HtmlReportParser.FailCase fc : parsed.getFailCases()) {
                    DvsPreviewItemVO.FailCase c = new DvsPreviewItemVO.FailCase();
                    c.setStatus(fc.getStatus());
                    c.setName(fc.getName());
                    c.setLog(fc.getLog());
                    failCases.add(c);
                }
            }
            vo.setFailCases(failCases);
            items.add(new DvsReportPreviewStore.Item(vo, stored));
        }

        DvsReportPreviewVO vo = new DvsReportPreviewVO();
        vo.setItems(items.stream().map(DvsReportPreviewStore.Item::getPreview).collect(Collectors.toList()));
        vo.setPreviewToken(previewStore.put(items));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> confirmReport(DvsReportConfirmDTO dto) {
        // 事务内仅 peek 校验，事务提交成功后再 remove，避免入库失败回滚后令牌丢失导致重试报"预览令牌无效"
        DvsReportPreviewStore.Entry entry = previewStore.peek(dto.getPreviewToken());
        if (entry == null || CollectionUtils.isEmpty(entry.getItems())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "预览令牌无效或已过期，请重新上传报告");
        }
        List<Long> projectIds = validateProjects(dto.getProjectIds());
        String version = StringUtils.hasText(dto.getVersion()) ? dto.getVersion().trim() : null;
        String branch = dto.getBranch().trim();
        Long publisherId = currentUserId();
        List<Long> createdReportIds = new ArrayList<>();

        for (DvsReportPreviewStore.Item item : entry.getItems()) {
            DvsPreviewItemVO preview = item.getPreview();
            // 每个模块(HTML)登记一个文件资源；同一模块被多个机型引用时共用文件ID
            FileResource resource = fileStorageService.register(item.getStoredFile(), publisherId, FileType.REPORT);
            for (Long projectId : projectIds) {
                DvsReport report = new DvsReport();
                report.setBranch(branch);
                report.setVersion(StringUtils.hasText(preview.getVersion()) ? preview.getVersion() : version);
                report.setProjectId(projectId);
                report.setModuleName(preview.getFileName());
                report.setReportFileId(resource.getId());
                report.setTotalCount(preview.getTotalCount());
                report.setPassedCount(preview.getPassedCount());
                report.setFailedCount(preview.getFailedCount());
                report.setErrorCount(preview.getErrorCount());
                report.setSkippedCount(preview.getSkippedCount());
                report.setDurationSec(preview.getDurationSec());
                report.setReportTime(preview.getReportTime());
                report.setPublisherId(publisherId);
                report.setPublishTime(LocalDateTime.now());
                report.setRemark(dto.getRemark());
                reportMapper.insert(report);
                createdReportIds.add(report.getId());

                // 失败模块：为该机型记录生成失败用例（可独立处理、指派）
                if (ReleaseResult.FAILED.getCode() == preview.getResult()
                        && !CollectionUtils.isEmpty(preview.getFailCases())) {
                    for (DvsPreviewItemVO.FailCase fc : preview.getFailCases()) {
                        DvsFailCase c = new DvsFailCase();
                        c.setReportId(report.getId());
                        c.setCaseType("error".equals(fc.getStatus()) ? "error" : "failed");
                        c.setCaseName(fc.getName());
                        c.setCaseLog(fc.getLog());
                        c.setStatus(FailCaseStatus.PENDING.getCode());
                        failCaseMapper.insert(c);
                    }
                }
            }
        }
        log.info("[DVS] 报告确认入库 branch={}, 模块数={}, 机型数={}, 生成报告数={}",
                branch, entry.getItems().size(), projectIds.size(), createdReportIds.size());
        // 事务提交成功后移除预览令牌，避免同令牌被重复使用；失败回滚时不消费令牌，支持重试
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                previewStore.remove(dto.getPreviewToken());
            }
        });
        return createdReportIds;
    }

    @Override
    public List<DvsGroupedVO> listGroupedCases(DvsGroupedQuery query) {
        LocalDate date = query.getDate() == null ? LocalDate.now() : query.getDate();
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);

        LambdaQueryWrapper<DvsReport> rw = new LambdaQueryWrapper<DvsReport>()
                .between(DvsReport::getPublishTime, start, end);
        if (StringUtils.hasText(query.getBranch())) {
            rw.like(DvsReport::getBranch, query.getBranch().trim());
        }
        List<DvsReport> reports = reportMapper.selectList(rw);
        if (CollectionUtils.isEmpty(reports)) {
            return new ArrayList<>();
        }

        // 机型名映射
        Set<Long> projectIds = reports.stream().map(DvsReport::getProjectId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> projectNameMap = projectIds.isEmpty() ? Collections.emptyMap()
                : projectMapper.selectBatchIds(projectIds).stream()
                        .collect(Collectors.toMap(ReleaseProject::getId, ReleaseProject::getProjectName));

        // 失败用例
        Set<Long> reportIds = reports.stream().map(DvsReport::getId).collect(Collectors.toSet());
        LambdaQueryWrapper<DvsFailCase> cw = new LambdaQueryWrapper<DvsFailCase>()
                .in(DvsFailCase::getReportId, reportIds)
                .orderByAsc(DvsFailCase::getId);
        if (StringUtils.hasText(query.getCaseName())) {
            cw.like(DvsFailCase::getCaseName, query.getCaseName().trim());
        }
        List<DvsFailCase> allCases = failCaseMapper.selectList(cw);
        Map<Long, List<DvsFailCase>> casesByReport = new HashMap<>();
        for (DvsFailCase c : allCases) {
            casesByReport.computeIfAbsent(c.getReportId(), k -> new ArrayList<>()).add(c);
        }

        // 用户名映射
        Set<Long> userIds = allCases.stream().map(DvsFailCase::getAssigneeId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        userIds.addAll(reports.stream().map(DvsReport::getPublisherId)
                .filter(Objects::nonNull).collect(Collectors.toSet()));
        Map<Long, String> userMap = userIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(SysUser::getId, u -> {
                            String name = u.getNickname();
                            return StringUtils.hasText(name) ? name : u.getUsername();
                        }));

        // 报告文件名映射
        Set<Long> fileIds = reports.stream().map(DvsReport::getReportFileId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> fileNameMap = fileIds.isEmpty() ? Collections.emptyMap()
                : fileResourceMapper.selectBatchIds(fileIds).stream()
                        .collect(Collectors.toMap(FileResource::getId, FileResource::getFileName));

        // 按 分支×机型 分组
        Map<String, DvsGroupedVO> groupMap = new LinkedHashMap<>();
        for (DvsReport report : reports) {
            String projectName = projectNameMap.getOrDefault(report.getProjectId(), "未知机型");
            if (StringUtils.hasText(query.getProjectName())
                    && !projectName.contains(query.getProjectName().trim())) {
                continue;
            }
            String key = report.getBranch() + "#" + projectName;
            DvsGroupedVO group = groupMap.computeIfAbsent(key, k -> {
                DvsGroupedVO g = new DvsGroupedVO();
                String[] parts = k.split("#", 2);
                g.setBranch(parts[0]);
                g.setProjectName(parts[1]);
                g.setModules(new ArrayList<>());
                g.setCases(new ArrayList<>());
                g.setTotalCount(0);
                g.setPassedCount(0);
                g.setFailedCount(0);
                g.setPassRate(0);
                g.setReportFiles(new ArrayList<>());
                return g;
            });
            // 模块统计
            DvsModuleVO mv = new DvsModuleVO();
            mv.setReportId(report.getId());
            mv.setModuleName(report.getModuleName());
            mv.setResult(calcResult(report));
            mv.setResultDesc(ReleaseResult.of(calcResult(report)).getDesc());
            mv.setTotalCount(nvl(report.getTotalCount()));
            mv.setPassedCount(nvl(report.getPassedCount()));
            mv.setFailedCount(nvl(report.getFailedCount()));
            mv.setErrorCount(nvl(report.getErrorCount()));
            mv.setSkippedCount(nvl(report.getSkippedCount()));
            mv.setPassRate(mv.getTotalCount() > 0
                    ? (int) Math.round(mv.getPassedCount() * 100.0 / mv.getTotalCount()) : 0);
            mv.setDurationSec(report.getDurationSec());
            mv.setReportTime(report.getReportTime());
            mv.setReportFileId(report.getReportFileId());
            mv.setReportFileName(report.getReportFileId() == null ? null
                    : fileNameMap.get(report.getReportFileId()));
            group.getModules().add(mv);

            group.setTotalCount(group.getTotalCount() + nvl(report.getTotalCount()));
            group.setPassedCount(group.getPassedCount() + nvl(report.getPassedCount()));
            group.setFailedCount(group.getFailedCount()
                    + (nvl(report.getTotalCount()) - nvl(report.getPassedCount())));

            // 用例
            List<DvsFailCase> cases = casesByReport.getOrDefault(report.getId(), Collections.emptyList());
            for (DvsFailCase c : cases) {
                DvsFailCaseVO cv = new DvsFailCaseVO();
                cv.setId(c.getId());
                cv.setReportId(report.getId());
                cv.setModuleName(report.getModuleName());
                cv.setCaseType(c.getCaseType());
                cv.setCaseTypeDesc("error".equals(c.getCaseType()) ? "错误" : "失败");
                cv.setCaseName(c.getCaseName());
                cv.setCaseLog(c.getCaseLog());
                cv.setStatus(c.getStatus());
                FailCaseStatus st = FailCaseStatus.of(c.getStatus());
                cv.setStatusDesc(st == null ? null : st.getDesc());
                cv.setAssigneeId(c.getAssigneeId());
                cv.setAssigneeName(c.getAssigneeId() == null ? null : userMap.get(c.getAssigneeId()));
                cv.setFailReason(c.getFailReason());
                cv.setFixPlan(c.getFixPlan());
                cv.setIsBug(c.getIsBug());
                cv.setProgress(c.getProgress());
                cv.setConclusion(c.getConclusion());
                cv.setAiAnalysis(c.getAiAnalysis());
                cv.setAiAnalysisCorrect(c.getAiAnalysisCorrect());
                cv.setAiRootCause(c.getAiRootCause());
                cv.setAiEvidence(c.getAiEvidence());
                cv.setAiSolution(c.getAiSolution());
                cv.setBugNo(c.getBugNo());
                cv.setIssueCategory(c.getIssueCategory());
                cv.setPublishTime(report.getPublishTime());
                group.getCases().add(cv);
            }
            // 报告文件（按文件ID去重）
            if (report.getReportFileId() != null && fileNameMap.containsKey(report.getReportFileId())) {
                boolean exists = group.getReportFiles().stream()
                        .anyMatch(f -> f.getFileId().equals(report.getReportFileId()));
                if (!exists) {
                    DvsGroupedVO.ReportFileItemVO f = new DvsGroupedVO.ReportFileItemVO();
                    f.setFileId(report.getReportFileId());
                    f.setFileName(fileNameMap.get(report.getReportFileId()));
                    group.getReportFiles().add(f);
                }
            }
        }

        for (DvsGroupedVO group : groupMap.values()) {
            int total = group.getTotalCount();
            int passed = group.getPassedCount();
            group.setPassRate(total > 0 ? (int) Math.round(passed * 100.0 / total) : 0);
        }

        return groupMap.values().stream()
                .sorted(java.util.Comparator.comparing(DvsGroupedVO::getBranch)
                        .thenComparing(DvsGroupedVO::getProjectName))
                .collect(Collectors.toList());
    }

    @Override
    public List<DvsRecentDayStatVO> recentWeekStats() {
        LocalDate today = LocalDate.now();
        LocalDate startDay = today.minusDays(6);
        LocalDateTime start = startDay.atStartOfDay();
        LocalDateTime end = today.atTime(LocalTime.MAX);

        List<DvsReport> reports = reportMapper.selectList(new LambdaQueryWrapper<DvsReport>()
                .between(DvsReport::getPublishTime, start, end));
        if (CollectionUtils.isEmpty(reports)) {
            List<DvsRecentDayStatVO> empty = new ArrayList<>();
            for (LocalDate d = startDay; !d.isAfter(today); d = d.plusDays(1)) {
                empty.add(buildStat(d, 0, 0));
            }
            return empty;
        }
        Set<Long> reportIds = reports.stream().map(DvsReport::getId).collect(Collectors.toSet());
        List<DvsFailCase> allCases = failCaseMapper.selectList(new LambdaQueryWrapper<DvsFailCase>()
                .in(DvsFailCase::getReportId, reportIds));

        Map<Long, LocalDate> reportDateMap = reports.stream()
                .collect(Collectors.toMap(DvsReport::getId,
                        r -> r.getPublishTime() == null ? today : r.getPublishTime().toLocalDate()));
        Map<LocalDate, List<DvsFailCase>> casesByDate = allCases.stream()
                .collect(Collectors.groupingBy(c -> reportDateMap.getOrDefault(c.getReportId(), today)));

        List<DvsRecentDayStatVO> result = new ArrayList<>();
        for (LocalDate d = startDay; !d.isAfter(today); d = d.plusDays(1)) {
            List<DvsFailCase> dayCases = casesByDate.getOrDefault(d, Collections.emptyList());
            int total = dayCases.size();
            int analyzed = (int) dayCases.stream()
                    .filter(c -> c.getStatus() != null && c.getStatus() >= FailCaseStatus.FIXED.getCode())
                    .count();
            result.add(buildStat(d, total, analyzed));
        }
        return result;
    }

    private DvsRecentDayStatVO buildStat(LocalDate date, int total, int analyzed) {
        DvsRecentDayStatVO vo = new DvsRecentDayStatVO();
        vo.setDate(date.toString());
        vo.setTotalCount(total);
        vo.setAnalyzedCount(analyzed);
        vo.setRate(total > 0 ? (int) Math.round(analyzed * 100.0 / total) : null);
        return vo;
    }

    @Override
    public void outputReportContent(Long fileId, HttpServletResponse response) {
        if (fileId == null) {
            throw new BusinessException(ErrorCode.FILE_READ_ERROR, "报告文件不存在");
        }
        FileResource file = fileResourceMapper.selectById(fileId);
        if (file == null) {
            throw new BusinessException(ErrorCode.FILE_READ_ERROR, "报告文件不存在");
        }
        Path absolute = fileStorageService.resolveAbsolutePath(file.getStoredPath());
        if (!Files.exists(absolute)) {
            throw new BusinessException(ErrorCode.FILE_READ_ERROR, "报告文件不存在或已被清理");
        }
        try {
            String mime = StringUtils.hasText(file.getMimeType()) ? file.getMimeType() : "text/html";
            String ext = StringUtils.hasText(file.getFileExt()) ? file.getFileExt() : "html";
            response.setContentType(mime + "; charset=UTF-8");
            response.setHeader("Content-Disposition",
                    "inline; filename=\"dvs_report_" + fileId + "." + ext + "\"");
            Files.copy(absolute, response.getOutputStream());
            response.flushBuffer();
        } catch (IOException e) {
            log.error("[DVS] 输出原始报告失败 fileId={}", fileId, e);
            throw new BusinessException(ErrorCode.FILE_READ_ERROR, "报告文件读取失败");
        }
    }

    @Override
    public Map<String, List<DvsFailCaseExcelVO>> exportByDate(LocalDate date) {
        DvsGroupedQuery query = new DvsGroupedQuery();
        query.setDate(date);
        List<DvsGroupedVO> groups = listGroupedCases(query);
        Map<String, List<DvsFailCaseExcelVO>> result = new LinkedHashMap<>();
        for (DvsGroupedVO g : groups) {
            String sheet = g.getBranch() + "-" + g.getProjectName();
            List<DvsFailCaseExcelVO> rows = new ArrayList<>();
            for (DvsFailCaseVO c : g.getCases()) {
                rows.add(toExcel(g, c));
            }
            result.put(sheet, rows);
        }
        return result;
    }

    private DvsFailCaseExcelVO toExcel(DvsGroupedVO g, DvsFailCaseVO c) {
        DvsFailCaseExcelVO vo = new DvsFailCaseExcelVO();
        vo.setBranch(g.getBranch());
        vo.setProjectName(g.getProjectName());
        vo.setCaseName(c.getCaseName());
        vo.setCaseTypeDesc(c.getCaseTypeDesc());
        vo.setFailReason(c.getFailReason());
        vo.setFixPlan(c.getFixPlan());
        vo.setConclusion(join(" | ", c.getProgress(), c.getIssueCategory()));
        vo.setBugNo(c.getBugNo());
        vo.setAssigneeName(c.getAssigneeName());
        vo.setStatusDesc(c.getStatusDesc());
        vo.setAiRootCause(c.getAiRootCause());
        vo.setAiEvidence(c.getAiEvidence());
        vo.setAiSolution(c.getAiSolution());
        vo.setAiAnalysisCorrect(c.getAiAnalysisCorrect() == null
                ? null : (c.getAiAnalysisCorrect() == 1 ? "Y" : "N"));
        vo.setPublishTime(c.getPublishTime());
        return vo;
    }

    private String join(String sep, String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (StringUtils.hasText(p)) {
                if (sb.length() > 0) {
                    sb.append(sep);
                }
                sb.append(p);
            }
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    /* ==================== 私有辅助 ==================== */

    private int calcResult(DvsReport report) {
        return (nvl(report.getFailedCount()) > 0 || nvl(report.getErrorCount()) > 0)
                ? ReleaseResult.FAILED.getCode() : ReleaseResult.SUCCESS.getCode();
    }

    /** 校验机型存在性，返回去重后的 id 列表 */
    private List<Long> validateProjects(List<Long> projectIds) {
        if (CollectionUtils.isEmpty(projectIds)) {
            throw new BusinessException(ErrorCode.PROJECT_REQUIRED);
        }
        List<Long> distinct = projectIds.stream().distinct().collect(Collectors.toList());
        List<ReleaseProject> projects = projectMapper.selectBatchIds(distinct);
        if (projects.size() != distinct.size()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "包含不存在的机型");
        }
        return distinct;
    }

    private int nvl(Integer value) {
        return value == null ? 0 : value;
    }

    private Long currentUserId() {
        return StpUtil.getLoginIdAsLong();
    }
}
