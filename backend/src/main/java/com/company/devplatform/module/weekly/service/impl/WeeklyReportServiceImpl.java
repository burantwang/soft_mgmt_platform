package com.company.devplatform.module.weekly.service.impl;

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
import com.company.devplatform.module.weekly.dto.WeeklyGroupedQuery;
import com.company.devplatform.module.weekly.dto.WeeklyReportConfirmDTO;
import com.company.devplatform.module.weekly.entity.WeeklyFailCase;
import com.company.devplatform.module.weekly.entity.WeeklyReport;
import com.company.devplatform.module.weekly.mapper.WeeklyFailCaseMapper;
import com.company.devplatform.module.weekly.mapper.WeeklyReportMapper;
import com.company.devplatform.module.weekly.service.WeeklyReportPreviewStore;
import com.company.devplatform.module.weekly.service.WeeklyReportService;
import com.company.devplatform.module.weekly.vo.WeeklyFailCaseVO;
import com.company.devplatform.module.weekly.vo.WeeklyGroupedVO;
import com.company.devplatform.module.weekly.vo.WeeklyModuleVO;
import com.company.devplatform.module.weekly.vo.WeeklyPreviewItemVO;
import com.company.devplatform.module.weekly.vo.WeeklyRecentDayStatVO;
import com.company.devplatform.module.weekly.vo.WeeklyReportPreviewVO;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * WeeklySanity 周度测试报告服务实现
 * <p>独立于 DailySanity：数据不共享，按 分支×机型 分组，组内按 HTML 模块单独统计、处理、指派。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeeklyReportServiceImpl implements WeeklyReportService {

    private final WeeklyReportMapper reportMapper;
    private final WeeklyFailCaseMapper failCaseMapper;
    private final ReleaseProjectMapper projectMapper;
    private final SysUserMapper userMapper;
    private final FileResourceMapper fileResourceMapper;
    private final FileStorageService fileStorageService;
    private final WeeklyReportPreviewStore previewStore;

    @Override
    public WeeklyReportPreviewVO previewReport(List<MultipartFile> files) {
        if (CollectionUtils.isEmpty(files)) {
            throw new BusinessException(ErrorCode.FILE_EMPTY);
        }
        List<WeeklyReportPreviewStore.Item> items = new ArrayList<>();
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
                parsed = HtmlReportParser.parse(content);
            } catch (BusinessException e) {
                fileStorageService.deletePhysical(stored);
                throw e;
            }
            WeeklyPreviewItemVO vo = new WeeklyPreviewItemVO();
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
            List<WeeklyPreviewItemVO.FailCase> failCases = new ArrayList<>();
            if (parsed.getFailCases() != null) {
                for (HtmlReportParser.FailCase fc : parsed.getFailCases()) {
                    WeeklyPreviewItemVO.FailCase c = new WeeklyPreviewItemVO.FailCase();
                    c.setStatus(fc.getStatus());
                    c.setName(fc.getName());
                    c.setLog(fc.getLog());
                    failCases.add(c);
                }
            }
            vo.setFailCases(failCases);
            items.add(new WeeklyReportPreviewStore.Item(vo, stored));
        }

        WeeklyReportPreviewVO vo = new WeeklyReportPreviewVO();
        vo.setItems(items.stream().map(WeeklyReportPreviewStore.Item::getPreview).collect(Collectors.toList()));
        vo.setPreviewToken(previewStore.put(items));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> confirmReport(WeeklyReportConfirmDTO dto) {
        // 事务内仅 peek 校验，事务提交成功后再 remove，避免入库失败回滚后令牌丢失导致重试报"预览令牌无效"
        WeeklyReportPreviewStore.Entry entry = previewStore.peek(dto.getPreviewToken());
        if (entry == null || CollectionUtils.isEmpty(entry.getItems())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "预览令牌无效或已过期，请重新上传报告");
        }
        List<Long> projectIds = validateProjects(dto.getProjectIds());
        String version = StringUtils.hasText(dto.getVersion()) ? dto.getVersion().trim() : null;
        String branch = dto.getBranch().trim();
        Long publisherId = currentUserId();
        List<Long> createdReportIds = new ArrayList<>();

        for (WeeklyReportPreviewStore.Item item : entry.getItems()) {
            WeeklyPreviewItemVO preview = item.getPreview();
            // 每个模块(HTML)登记一个文件资源；同一模块被多个机型引用时共用文件ID
            FileResource resource = fileStorageService.register(item.getStoredFile(), publisherId, FileType.REPORT);
            for (Long projectId : projectIds) {
                WeeklyReport report = new WeeklyReport();
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
                    for (WeeklyPreviewItemVO.FailCase fc : preview.getFailCases()) {
                        WeeklyFailCase c = new WeeklyFailCase();
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
        log.info("[WeeklySanity] 报告确认入库 branch={}, 模块数={}, 机型数={}, 生成报告数={}",
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
    public List<WeeklyGroupedVO> listGroupedCases(WeeklyGroupedQuery query) {
        LocalDate date = query.getDate() == null ? LocalDate.now() : query.getDate();
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);

        LambdaQueryWrapper<WeeklyReport> rw = new LambdaQueryWrapper<WeeklyReport>()
                .between(WeeklyReport::getPublishTime, start, end);
        if (StringUtils.hasText(query.getBranch())) {
            rw.like(WeeklyReport::getBranch, query.getBranch().trim());
        }
        List<WeeklyReport> reports = reportMapper.selectList(rw);
        if (CollectionUtils.isEmpty(reports)) {
            return new ArrayList<>();
        }

        // 机型名映射
        Set<Long> projectIds = reports.stream().map(WeeklyReport::getProjectId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> projectNameMap = projectIds.isEmpty() ? Collections.emptyMap()
                : projectMapper.selectBatchIds(projectIds).stream()
                        .collect(Collectors.toMap(ReleaseProject::getId, ReleaseProject::getProjectName));

        // 失败用例
        Set<Long> reportIds = reports.stream().map(WeeklyReport::getId).collect(Collectors.toSet());
        LambdaQueryWrapper<WeeklyFailCase> cw = new LambdaQueryWrapper<WeeklyFailCase>()
                .in(WeeklyFailCase::getReportId, reportIds)
                .orderByAsc(WeeklyFailCase::getId);
        if (StringUtils.hasText(query.getCaseName())) {
            cw.like(WeeklyFailCase::getCaseName, query.getCaseName().trim());
        }
        List<WeeklyFailCase> allCases = failCaseMapper.selectList(cw);
        Map<Long, List<WeeklyFailCase>> casesByReport = new HashMap<>();
        for (WeeklyFailCase c : allCases) {
            casesByReport.computeIfAbsent(c.getReportId(), k -> new ArrayList<>()).add(c);
        }

        // 用户名映射
        Set<Long> userIds = allCases.stream().map(WeeklyFailCase::getAssigneeId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        userIds.addAll(reports.stream().map(WeeklyReport::getPublisherId)
                .filter(Objects::nonNull).collect(Collectors.toSet()));
        Map<Long, String> userMap = userIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(SysUser::getId, u -> {
                            String name = u.getNickname();
                            return StringUtils.hasText(name) ? name : u.getUsername();
                        }));

        // 报告文件名映射
        Set<Long> fileIds = reports.stream().map(WeeklyReport::getReportFileId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> fileNameMap = fileIds.isEmpty() ? Collections.emptyMap()
                : fileResourceMapper.selectBatchIds(fileIds).stream()
                        .collect(Collectors.toMap(FileResource::getId, FileResource::getFileName));

        // 按 分支×机型 分组
        Map<String, WeeklyGroupedVO> groupMap = new LinkedHashMap<>();
        for (WeeklyReport report : reports) {
            String projectName = projectNameMap.getOrDefault(report.getProjectId(), "未知机型");
            if (StringUtils.hasText(query.getProjectName())
                    && !projectName.contains(query.getProjectName().trim())) {
                continue;
            }
            String key = report.getBranch() + "#" + projectName;
            WeeklyGroupedVO group = groupMap.computeIfAbsent(key, k -> {
                WeeklyGroupedVO g = new WeeklyGroupedVO();
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
            WeeklyModuleVO mv = new WeeklyModuleVO();
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
            List<WeeklyFailCase> cases = casesByReport.getOrDefault(report.getId(), Collections.emptyList());
            for (WeeklyFailCase c : cases) {
                WeeklyFailCaseVO cv = new WeeklyFailCaseVO();
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
                cv.setPublishTime(report.getPublishTime());
                group.getCases().add(cv);
            }
            // 报告文件（按文件ID去重）
            if (report.getReportFileId() != null && fileNameMap.containsKey(report.getReportFileId())) {
                boolean exists = group.getReportFiles().stream()
                        .anyMatch(f -> f.getFileId().equals(report.getReportFileId()));
                if (!exists) {
                    WeeklyGroupedVO.ReportFileItemVO f = new WeeklyGroupedVO.ReportFileItemVO();
                    f.setFileId(report.getReportFileId());
                    f.setFileName(fileNameMap.get(report.getReportFileId()));
                    group.getReportFiles().add(f);
                }
            }
        }

        for (WeeklyGroupedVO group : groupMap.values()) {
            int total = group.getTotalCount();
            int passed = group.getPassedCount();
            group.setPassRate(total > 0 ? (int) Math.round(passed * 100.0 / total) : 0);
        }

        return groupMap.values().stream()
                .sorted(java.util.Comparator.comparing(WeeklyGroupedVO::getBranch)
                        .thenComparing(WeeklyGroupedVO::getProjectName))
                .collect(Collectors.toList());
    }

    @Override
    public List<WeeklyRecentDayStatVO> recentWeekStats() {
        LocalDate today = LocalDate.now();
        LocalDate startDay = today.minusDays(6);
        LocalDateTime start = startDay.atStartOfDay();
        LocalDateTime end = today.atTime(LocalTime.MAX);

        List<WeeklyReport> reports = reportMapper.selectList(new LambdaQueryWrapper<WeeklyReport>()
                .between(WeeklyReport::getPublishTime, start, end));
        if (CollectionUtils.isEmpty(reports)) {
            List<WeeklyRecentDayStatVO> empty = new ArrayList<>();
            for (LocalDate d = startDay; !d.isAfter(today); d = d.plusDays(1)) {
                empty.add(buildStat(d, 0, 0));
            }
            return empty;
        }
        Set<Long> reportIds = reports.stream().map(WeeklyReport::getId).collect(Collectors.toSet());
        List<WeeklyFailCase> allCases = failCaseMapper.selectList(new LambdaQueryWrapper<WeeklyFailCase>()
                .in(WeeklyFailCase::getReportId, reportIds));

        Map<Long, LocalDate> reportDateMap = reports.stream()
                .collect(Collectors.toMap(WeeklyReport::getId,
                        r -> r.getPublishTime() == null ? today : r.getPublishTime().toLocalDate()));
        Map<LocalDate, List<WeeklyFailCase>> casesByDate = allCases.stream()
                .collect(Collectors.groupingBy(c -> reportDateMap.getOrDefault(c.getReportId(), today)));

        List<WeeklyRecentDayStatVO> result = new ArrayList<>();
        for (LocalDate d = startDay; !d.isAfter(today); d = d.plusDays(1)) {
            List<WeeklyFailCase> dayCases = casesByDate.getOrDefault(d, Collections.emptyList());
            int total = dayCases.size();
            int analyzed = (int) dayCases.stream()
                    .filter(c -> c.getStatus() != null && c.getStatus() >= FailCaseStatus.FIXED.getCode())
                    .count();
            result.add(buildStat(d, total, analyzed));
        }
        return result;
    }

    private WeeklyRecentDayStatVO buildStat(LocalDate date, int total, int analyzed) {
        WeeklyRecentDayStatVO vo = new WeeklyRecentDayStatVO();
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
                    "inline; filename=\"weekly_report_" + fileId + "." + ext + "\"");
            Files.copy(absolute, response.getOutputStream());
            response.flushBuffer();
        } catch (IOException e) {
            log.error("[WeeklySanity] 输出原始报告失败 fileId={}", fileId, e);
            throw new BusinessException(ErrorCode.FILE_READ_ERROR, "报告文件读取失败");
        }
    }

    /* ==================== 私有辅助 ==================== */

    private int calcResult(WeeklyReport report) {
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
