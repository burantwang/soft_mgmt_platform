package com.company.devplatform.module.release.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.devplatform.common.ErrorCode;
import com.company.devplatform.common.exception.BusinessException;
import com.company.devplatform.module.auth.entity.SysUser;
import com.company.devplatform.module.auth.mapper.SysUserMapper;
import com.company.devplatform.module.release.dto.JenkinsPushDTO;
import com.company.devplatform.module.release.dto.ReleaseRecordCreateDTO;
import com.company.devplatform.module.release.dto.ReleaseRecordUpdateDTO;
import com.company.devplatform.module.release.dto.ReportConfirmDTO;
import com.company.devplatform.module.release.entity.FileResource;
import com.company.devplatform.module.release.entity.ReleaseFailTask;
import com.company.devplatform.module.release.entity.ReleaseProject;
import com.company.devplatform.module.release.entity.ReleaseRecord;
import com.company.devplatform.module.release.entity.ReleaseRecordProject;
import com.company.devplatform.module.release.enums.FileType;
import com.company.devplatform.module.release.enums.ReleaseResult;
import com.company.devplatform.module.release.enums.ReleaseSource;
import com.company.devplatform.module.release.mapper.FileResourceMapper;
import com.company.devplatform.module.release.mapper.ReleaseFailTaskMapper;
import com.company.devplatform.module.release.mapper.ReleaseProjectMapper;
import com.company.devplatform.module.release.mapper.ReleaseRecordMapper;
import com.company.devplatform.module.release.mapper.ReleaseRecordProjectMapper;
import com.company.devplatform.module.release.service.FileStorageService;
import com.company.devplatform.module.release.service.ReleaseFailTaskService;
import com.company.devplatform.module.release.service.ReleaseRecordService;
import com.company.devplatform.module.release.service.ReportPreviewStore;
import com.company.devplatform.module.release.util.HtmlReportParser;
import com.company.devplatform.module.release.vo.ReleaseRecordVO;
import com.company.devplatform.module.release.vo.ReportPreviewVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 版本发布记录服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReleaseRecordServiceImpl implements ReleaseRecordService {

    private final ReleaseRecordMapper recordMapper;
    private final ReleaseRecordProjectMapper recordProjectMapper;
    private final ReleaseProjectMapper projectMapper;
    private final FileResourceMapper fileResourceMapper;
    private final ReleaseFailTaskMapper failTaskMapper;
    private final SysUserMapper sysUserMapper;
    private final FileStorageService fileStorageService;
    private final ReportPreviewStore previewStore;
    private final ReleaseFailTaskService failTaskService;

    @Override
    public IPage<ReleaseRecordVO> page(int page, int size, String branch, Integer result, Integer source) {
        LambdaQueryWrapper<ReleaseRecord> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(branch)) {
            wrapper.like(ReleaseRecord::getBranch, branch);
        }
        if (result != null) {
            wrapper.eq(ReleaseRecord::getResult, result);
        }
        if (source != null) {
            wrapper.eq(ReleaseRecord::getSource, source);
        }
        wrapper.orderByDesc(ReleaseRecord::getPublishTime);

        IPage<ReleaseRecord> recordPage = recordMapper.selectPage(new Page<>(page, size), wrapper);
        return recordPage.convert(this::toVO);
    }

    @Override
    public ReleaseRecordVO detail(Long id) {
        ReleaseRecord record = recordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND);
        }
        return toVO(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createManual(ReleaseRecordCreateDTO dto) {
        List<Long> projectIds = validateProjects(dto.getProjectIds());
        ReleaseRecord record = new ReleaseRecord();
        record.setBranch(dto.getBranch().trim());
        record.setVersion(dto.getVersion() == null ? null : dto.getVersion().trim());
        record.setResult(dto.getResult());
        record.setTotalCount(0);
        record.setPassedCount(0);
        record.setFailedCount(0);
        record.setErrorCount(0);
        record.setSkippedCount(0);
        record.setPublisherId(currentUserId());
        record.setPublishTime(LocalDateTime.now());
        record.setSource(ReleaseSource.MANUAL_CREATE.getCode());
        record.setRemark(dto.getRemark());
        recordMapper.insert(record);
        saveRecordProjects(record.getId(), projectIds);
        log.info("[发布记录] 手动创建 id={}, branch={}", record.getId(), record.getBranch());
        return record.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(ReleaseRecordUpdateDTO dto) {
        ReleaseRecord record = recordMapper.selectById(dto.getId());
        if (record == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND);
        }
        List<Long> projectIds = validateProjects(dto.getProjectIds());
        record.setBranch(dto.getBranch().trim());
        record.setVersion(dto.getVersion() == null ? null : dto.getVersion().trim());
        record.setRemark(dto.getRemark());
        recordMapper.updateById(record);
        // 重写机型关联
        recordProjectMapper.delete(new LambdaQueryWrapper<ReleaseRecordProject>()
                .eq(ReleaseRecordProject::getRecordId, record.getId()));
        saveRecordProjects(record.getId(), projectIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ReleaseRecord record = recordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND);
        }
        // 删除机型关联
        recordProjectMapper.delete(new LambdaQueryWrapper<ReleaseRecordProject>()
                .eq(ReleaseRecordProject::getRecordId, id));
        // 删除失败任务及其明细
        ReleaseFailTask task = failTaskService.getByRecordId(id);
        if (task != null) {
            failTaskMapper.deleteById(task.getId());
        }
        // 删除报告文件（物理 + 逻辑联动）
        if (record.getReportFileId() != null) {
            fileStorageService.delete(record.getReportFileId());
        }
        recordMapper.deleteById(id);
        log.info("[发布记录] 删除 id={}, branch={}", id, record.getBranch());
    }

    @Override
    public ReportPreviewVO previewReport(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_EMPTY);
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

        ReportPreviewVO vo = new ReportPreviewVO();
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
        List<ReportPreviewVO.FailCase> failCases = new ArrayList<>();
        if (parsed.getFailCases() != null) {
            for (HtmlReportParser.FailCase fc : parsed.getFailCases()) {
                ReportPreviewVO.FailCase c = new ReportPreviewVO.FailCase();
                c.setName(fc.getName());
                c.setLog(fc.getLog());
                failCases.add(c);
            }
        }
        vo.setFailCases(failCases);
        String token = previewStore.put(vo, stored);
        vo.setPreviewToken(token);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long confirmReport(ReportConfirmDTO dto) {
        ReportPreviewStore.Entry entry = previewStore.take(dto.getPreviewToken());
        if (entry == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "预览令牌无效或已过期，请重新上传报告");
        }
        ReportPreviewVO preview = entry.getPreview();
        FileStorageService.StoredFile stored = entry.getStoredFile();
        List<Long> projectIds = validateProjects(dto.getProjectIds());
        String version = StringUtils.hasText(dto.getVersion()) ? dto.getVersion().trim() : preview.getVersion();

        // 登记文件资源
        FileResource resource = fileStorageService.register(stored, currentUserId(), FileType.REPORT);

        ReleaseRecord record = new ReleaseRecord();
        record.setBranch(dto.getBranch().trim());
        record.setVersion(version);
        record.setResult(preview.getResult());
        record.setReportFileId(resource.getId());
        record.setTotalCount(preview.getTotalCount());
        record.setPassedCount(preview.getPassedCount());
        record.setFailedCount(preview.getFailedCount());
        record.setErrorCount(preview.getErrorCount());
        record.setSkippedCount(preview.getSkippedCount());
        record.setDurationSec(preview.getDurationSec());
        record.setReportTime(preview.getReportTime());
        record.setPublisherId(currentUserId());
        record.setPublishTime(LocalDateTime.now());
        record.setSource(ReleaseSource.MANUAL_UPLOAD.getCode());
        record.setRemark(dto.getRemark());
        recordMapper.insert(record);
        saveRecordProjects(record.getId(), projectIds);

        // 失败报告自动生成失败聚合任务
        if (ReleaseResult.FAILED.getCode() == preview.getResult()
                && preview.getFailCases() != null && !preview.getFailCases().isEmpty()) {
            List<HtmlReportParser.FailCase> failCases = preview.getFailCases().stream()
                    .map(fc -> {
                        HtmlReportParser.FailCase c = new HtmlReportParser.FailCase();
                        c.setName(fc.getName());
                        c.setLog(fc.getLog());
                        return c;
                    })
                    .collect(Collectors.toList());
            failTaskService.createTaskForRecord(record, failCases);
        }
        log.info("[发布记录] 报告确认入库 id={}, branch={}, result={}", record.getId(), record.getBranch(), preview.getResult());
        return record.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createFromJenkinsWithReport(String branch, String version, List<String> projectCodes,
                                            String remark, FileStorageService.StoredFile storedFile) {
        List<Long> projectIds = validateProjectCodes(projectCodes);
        byte[] content = null;
        if (storedFile != null) {
            try {
                content = java.nio.file.Files.readAllBytes(
                        fileStorageService.resolveAbsolutePath(storedFile.getStoredPath()));
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.FILE_READ_ERROR);
            }
        }
        HtmlReportParser.ParseResult parsed = content == null ? new HtmlReportParser.ParseResult()
                : HtmlReportParser.parse(content);
        Long reportFileId = storedFile == null ? null
                : fileStorageService.register(storedFile, null, FileType.REPORT).getId();

        return createRecord(ReleaseSource.JENKINS_PUSH.getCode(), branch, version, projectIds, remark,
                parsed.getFailedCount() > 0 || parsed.getErrorCount() > 0
                        ? ReleaseResult.FAILED.getCode() : ReleaseResult.SUCCESS.getCode(),
                parsed.getTotalCount(), parsed.getPassedCount(), parsed.getFailedCount(),
                parsed.getErrorCount(), parsed.getSkippedCount(), parsed.getDurationSec(),
                parsed.getReportTime(), reportFileId, null, toPreviewFailCases(parsed.getFailCases()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createFromJenkinsJson(JenkinsPushDTO dto) {
        List<Long> projectIds = validateProjectCodes(dto.getProjectCodes());
        int failed = nvl(dto.getFailedCount());
        int error = nvl(dto.getErrorCount());
        int result = (failed > 0 || error > 0) ? ReleaseResult.FAILED.getCode() : ReleaseResult.SUCCESS.getCode();
        return createRecord(ReleaseSource.JENKINS_PUSH.getCode(), dto.getBranch(), dto.getVersion(), projectIds,
                dto.getRemark(), result, nvl(dto.getTotalCount()), nvl(dto.getPassedCount()), failed,
                error, nvl(dto.getSkippedCount()), dto.getDurationSec(), dto.getReportTime(),
                null, null, Collections.emptyList());
    }

    /**
     * 通用入库（手动创建/上传确认/Jenkins 共用）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRecord(Integer source, String branch, String version, List<Long> projectIds, String remark,
                             Integer result, Integer totalCount, Integer passedCount, Integer failedCount,
                             Integer errorCount, Integer skippedCount, BigDecimal durationSec,
                             LocalDateTime reportTime, Long reportFileId, Long publisherId,
                             List<ReportPreviewVO.FailCase> failCases) {
        ReleaseRecord record = new ReleaseRecord();
        record.setBranch(branch.trim());
        record.setVersion(version == null ? null : version.trim());
        record.setResult(result);
        record.setReportFileId(reportFileId);
        record.setTotalCount(totalCount == null ? 0 : totalCount);
        record.setPassedCount(passedCount == null ? 0 : passedCount);
        record.setFailedCount(failedCount == null ? 0 : failedCount);
        record.setErrorCount(errorCount == null ? 0 : errorCount);
        record.setSkippedCount(skippedCount == null ? 0 : skippedCount);
        record.setDurationSec(durationSec);
        record.setReportTime(reportTime);
        record.setPublisherId(publisherId == null ? (safeCurrentUserId()) : publisherId);
        record.setPublishTime(LocalDateTime.now());
        record.setSource(source);
        record.setRemark(remark);
        recordMapper.insert(record);
        saveRecordProjects(record.getId(), projectIds);

        if (ReleaseResult.FAILED.getCode() == result && failCases != null && !failCases.isEmpty()) {
            List<HtmlReportParser.FailCase> cases = failCases.stream().map(fc -> {
                HtmlReportParser.FailCase c = new HtmlReportParser.FailCase();
                c.setName(fc.getName());
                c.setLog(fc.getLog());
                return c;
            }).collect(Collectors.toList());
            failTaskService.createTaskForRecord(record, cases);
        }
        return record.getId();
    }

    /* ==================== 私有辅助 ==================== */

    private ReleaseRecordVO toVO(ReleaseRecord record) {
        ReleaseRecordVO vo = new ReleaseRecordVO();
        vo.setId(record.getId());
        vo.setBranch(record.getBranch());
        vo.setVersion(record.getVersion());
        vo.setResult(record.getResult());
        ReleaseResult rr = ReleaseResult.of(record.getResult());
        vo.setResultDesc(rr == null ? "-" : rr.getDesc());
        vo.setReportFileId(record.getReportFileId());
        vo.setTotalCount(record.getTotalCount());
        vo.setPassedCount(record.getPassedCount());
        vo.setFailedCount(record.getFailedCount());
        vo.setErrorCount(record.getErrorCount());
        vo.setSkippedCount(record.getSkippedCount());
        vo.setPassRate(calcPassRate(record));
        vo.setDurationSec(record.getDurationSec());
        vo.setReportTime(record.getReportTime());
        vo.setPublisherId(record.getPublisherId());
        vo.setPublishTime(record.getPublishTime());
        vo.setSource(record.getSource());
        ReleaseSource rs = ReleaseSource.of(record.getSource());
        vo.setSourceDesc(rs == null ? "-" : rs.getDesc());
        vo.setRemark(record.getRemark());

        // 机型
        List<ReleaseRecordProject> links = recordProjectMapper.selectList(
                new LambdaQueryWrapper<ReleaseRecordProject>()
                        .eq(ReleaseRecordProject::getRecordId, record.getId()));
        if (!links.isEmpty()) {
            List<Long> projectIds = links.stream().map(ReleaseRecordProject::getProjectId).collect(Collectors.toList());
            List<ReleaseProject> projects = projectMapper.selectBatchIds(projectIds);
            vo.setProjectIds(projectIds);
            vo.setProjectNames(projects.stream().map(ReleaseProject::getProjectName).collect(Collectors.toList()));
        }
        // 发布人
        if (record.getPublisherId() != null) {
            SysUser user = sysUserMapper.selectById(record.getPublisherId());
            if (user != null) {
                vo.setPublisherName(user.getNickname());
            }
        }
        // 报告文件名
        if (record.getReportFileId() != null) {
            FileResource file = fileResourceMapper.selectById(record.getReportFileId());
            if (file != null) {
                vo.setReportFileName(file.getFileName());
            }
        }
        // 失败任务入口
        if (ReleaseResult.FAILED.getCode() == record.getResult()) {
            ReleaseFailTask task = failTaskService.getByRecordId(record.getId());
            if (task != null) {
                vo.setFailTaskId(task.getId());
            }
        }
        return vo;
    }

    private BigDecimal calcPassRate(ReleaseRecord record) {
        if (record.getTotalCount() == null || record.getTotalCount() <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(record.getPassedCount() == null ? 0 : record.getPassedCount())
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(record.getTotalCount()), 2, RoundingMode.HALF_UP);
    }

    private void saveRecordProjects(Long recordId, List<Long> projectIds) {
        boolean first = true;
        for (Long pid : projectIds) {
            ReleaseRecordProject link = new ReleaseRecordProject();
            link.setRecordId(recordId);
            link.setProjectId(pid);
            link.setIsPrimary(first ? 1 : 0);
            recordProjectMapper.insert(link);
            first = false;
        }
    }

    /** 校验机型存在性，返回去重后的 id 列表 */
    private List<Long> validateProjects(List<Long> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) {
            throw new BusinessException(ErrorCode.PROJECT_REQUIRED);
        }
        List<Long> distinct = projectIds.stream().distinct().collect(Collectors.toList());
        List<ReleaseProject> projects = projectMapper.selectBatchIds(distinct);
        if (projects.size() != distinct.size()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "包含不存在的机型");
        }
        return distinct;
    }

    /** 按机型编码校验并返回 id 列表 */
    private List<Long> validateProjectCodes(List<String> projectCodes) {
        if (projectCodes == null || projectCodes.isEmpty()) {
            throw new BusinessException(ErrorCode.PROJECT_REQUIRED);
        }
        List<ReleaseProject> projects = projectMapper.selectList(
                new LambdaQueryWrapper<ReleaseProject>().in(ReleaseProject::getProjectCode, projectCodes));
        if (projects.size() != projectCodes.stream().distinct().count()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "包含不存在的机型编码");
        }
        return projects.stream().map(ReleaseProject::getId).collect(Collectors.toList());
    }

    private List<ReportPreviewVO.FailCase> toPreviewFailCases(List<HtmlReportParser.FailCase> source) {
        if (source == null) {
            return Collections.emptyList();
        }
        return source.stream().map(fc -> {
            ReportPreviewVO.FailCase c = new ReportPreviewVO.FailCase();
            c.setName(fc.getName());
            c.setLog(fc.getLog());
            return c;
        }).collect(Collectors.toList());
    }

    private int nvl(Integer v) {
        return v == null ? 0 : v;
    }

    /** 当前登录用户ID（未登录返回 null） */
    private Long safeCurrentUserId() {
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            return null;
        }
    }

    private Long currentUserId() {
        return StpUtil.getLoginIdAsLong();
    }
}
