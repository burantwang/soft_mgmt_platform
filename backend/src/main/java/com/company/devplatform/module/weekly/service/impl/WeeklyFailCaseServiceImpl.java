package com.company.devplatform.module.weekly.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.devplatform.common.ErrorCode;
import com.company.devplatform.common.exception.BusinessException;
import com.company.devplatform.common.vo.MyTaskCaseVO;
import com.company.devplatform.module.release.entity.ReleaseProject;
import com.company.devplatform.module.release.enums.FailCaseStatus;
import com.company.devplatform.module.release.mapper.ReleaseProjectMapper;
import com.company.devplatform.module.release.service.AiAnalysisService;
import com.company.devplatform.module.release.vo.AiAnalysisResult;
import com.company.devplatform.module.weekly.dto.WeeklyFailCaseAssignDTO;
import com.company.devplatform.module.weekly.dto.WeeklyFailCaseUpdateDTO;
import com.company.devplatform.module.weekly.entity.WeeklyFailCase;
import com.company.devplatform.module.weekly.entity.WeeklyReport;
import com.company.devplatform.module.weekly.mapper.WeeklyFailCaseMapper;
import com.company.devplatform.module.weekly.mapper.WeeklyReportMapper;
import com.company.devplatform.module.weekly.service.WeeklyFailCaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * WeeklySanity 失败用例处理服务实现
 * <p>权限规则与 DailySanity 一致：管理员（超管/普通管理员）豁免，普通用户仅可操作责任人为自己的用例。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeeklyFailCaseServiceImpl implements WeeklyFailCaseService {

    private final WeeklyFailCaseMapper failCaseMapper;
    private final WeeklyReportMapper reportMapper;
    private final ReleaseProjectMapper projectMapper;
    private final AiAnalysisService aiAnalysisService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCase(Long caseId, WeeklyFailCaseUpdateDTO dto) {
        WeeklyFailCase c = failCaseMapper.selectById(caseId);
        if (c == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "用例明细不存在");
        }
        WeeklyReport report = reportMapper.selectById(c.getReportId());
        if (report == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "所属周度报告不存在");
        }

        if (!isAdmin()) {
            // 关闭用例仅管理员可操作：普通用户既不能设为关闭，也不能修改已关闭的用例
            if ((dto.getStatus() != null && dto.getStatus() == FailCaseStatus.CLOSED.getCode())
                    || (c.getStatus() != null && c.getStatus() == FailCaseStatus.CLOSED.getCode())) {
                throw new BusinessException(ErrorCode.NO_PERMISSION, "已关闭用例仅管理员可操作");
            }
            long current = currentUserId();
            if (c.getAssigneeId() == null) {
                // 未指派责任人：仅允许指派给自己（认领），其余栏位禁止修改
                if (dto.getAssigneeId() == null || !dto.getAssigneeId().equals(current)) {
                    throw new BusinessException(ErrorCode.BUSINESS_ERROR, "请先将责任人指派给自己后再编辑");
                }
                boolean touchOther = StringUtils.hasText(dto.getFailReason())
                        || StringUtils.hasText(dto.getFixPlan())
                        || StringUtils.hasText(dto.getProgress())
                        || StringUtils.hasText(dto.getConclusion())
                        || dto.getIsBug() != null
                        || StringUtils.hasText(dto.getAiAnalysis())
                        || dto.getAiAnalysisCorrect() != null
                        || (dto.getStatus() != null && !dto.getStatus().equals(c.getStatus()));
                if (touchOther) {
                    throw new BusinessException(ErrorCode.BUSINESS_ERROR, "请先指派责任人后再填写其他信息");
                }
            } else if (!c.getAssigneeId().equals(current)) {
                // 责任人是他人：禁止操作
                throw new BusinessException(ErrorCode.NO_PERMISSION, "仅责任人为自己的问题单可操作");
            } else {
                // 责任人是自己：状态变更需失败原因、结论进展、AI分析判断均已填写
                Integer targetStatus = dto.getStatus();
                if (targetStatus != null && !targetStatus.equals(c.getStatus())) {
                    String reason = StringUtils.hasText(dto.getFailReason()) ? dto.getFailReason() : c.getFailReason();
                    String progress = StringUtils.hasText(dto.getProgress()) ? dto.getProgress() : c.getProgress();
                    Integer aiCorrect = dto.getAiAnalysisCorrect() != null ? dto.getAiAnalysisCorrect() : c.getAiAnalysisCorrect();
                    if (!StringUtils.hasText(reason) || !StringUtils.hasText(progress) || aiCorrect == null) {
                        throw new BusinessException(ErrorCode.BUSINESS_ERROR, "请填写失败原因、结论进展、AI分析判断后再更新状态");
                    }
                }
            }
        }

        FailCaseStatus current = FailCaseStatus.of(c.getStatus());
        FailCaseStatus target = FailCaseStatus.of(dto.getStatus());
        if (current == null || target == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "非法的用例状态");
        }
        if (current != target) {
            if ((current == FailCaseStatus.FIXED || current == FailCaseStatus.NOT_DEFECT) && target == FailCaseStatus.PENDING) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "已处理完成的用例不能回退为待处理");
            }
            if (target == FailCaseStatus.FIXED || target == FailCaseStatus.NOT_DEFECT) {
                if (c.getHandleTime() == null) {
                    c.setHandleTime(LocalDateTime.now());
                }
            }
            c.setStatus(target.getCode());
        }
        c.setAssigneeId(dto.getAssigneeId());
        c.setFailReason(dto.getFailReason());
        c.setFixPlan(dto.getFixPlan());
        if (dto.getIsBug() != null) {
            c.setIsBug(dto.getIsBug());
        }
        c.setProgress(dto.getProgress());
        c.setConclusion(dto.getConclusion());
        if (dto.getAiAnalysis() != null) {
            c.setAiAnalysis(dto.getAiAnalysis());
        }
        if (dto.getAiAnalysisCorrect() != null) {
            c.setAiAnalysisCorrect(dto.getAiAnalysisCorrect());
        }
        c.setBugNo(dto.getBugNo());
        c.setIssueCategory(dto.getIssueCategory());
        failCaseMapper.updateById(c);
        log.info("[WeeklySanity] 更新用例 caseId={} status={}", c.getId(), dto.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignCaseAssignee(Long caseId, WeeklyFailCaseAssignDTO dto) {
        WeeklyFailCase c = failCaseMapper.selectById(caseId);
        if (c == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "用例明细不存在");
        }
        WeeklyReport report = reportMapper.selectById(c.getReportId());
        if (report == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "所属周度报告不存在");
        }
        Long assigneeId = dto.getAssigneeId();
        // 普通用户指派规则：未指派仅可认领给自己；已指派仅当前被指派人可转派给他人，不能取消；管理员（超管/普通管理员）豁免
        if (!isAdmin()) {
            // 已关闭用例普通用户仅可查看，不能改责任人
            if (c.getStatus() != null && c.getStatus() == FailCaseStatus.CLOSED.getCode()) {
                throw new BusinessException(ErrorCode.NO_PERMISSION, "已关闭用例仅管理员可操作");
            }
            long current = currentUserId();
            if (c.getAssigneeId() == null) {
                // 未指派：仅允许认领给自己
                if (assigneeId == null || !assigneeId.equals(current)) {
                    throw new BusinessException(ErrorCode.NO_PERMISSION, "未指派用例仅可认领给自己");
                }
            } else {
                // 已指派：仅当前被指派人可转派，且不能取消指派
                if (!c.getAssigneeId().equals(current)) {
                    throw new BusinessException(ErrorCode.NO_PERMISSION, "仅责任人为自己的问题单可操作");
                }
                if (assigneeId == null) {
                    throw new BusinessException(ErrorCode.NO_PERMISSION, "无权取消指派，请转派给其他责任人");
                }
            }
        }
        c.setAssigneeId(assigneeId);
        failCaseMapper.updateById(c);
        log.info("[WeeklySanity] 快速指派责任人 caseId={} assigneeId={}", c.getId(), assigneeId);
    }

    @Override
    public AiAnalysisResult aiAnalyze(Long caseId) {
        WeeklyFailCase c = failCaseMapper.selectById(caseId);
        if (c == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "用例明细不存在");
        }
        if (!StringUtils.hasText(c.getCaseLog())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该用例无执行日志，无法分析");
        }
        AiAnalysisResult result = aiAnalysisService.analyze(c.getCaseLog(), "weekly_sanity");
        c.setAiRootCause(result.getRootCause());
        c.setAiEvidence(result.getEvidence());
        c.setAiSolution(result.getSolution());
        failCaseMapper.updateById(c);
        return result;
    }

    @Override
    public List<MyTaskCaseVO> listMyCases(boolean all) {
        long current = currentUserId();
        LambdaQueryWrapper<WeeklyFailCase> cw = new LambdaQueryWrapper<WeeklyFailCase>()
                .eq(WeeklyFailCase::getAssigneeId, current);
        if (!all) {
            cw.in(WeeklyFailCase::getStatus, FailCaseStatus.PENDING.getCode(), FailCaseStatus.PROCESSING.getCode());
        }
        cw.orderByDesc(WeeklyFailCase::getId);
        List<WeeklyFailCase> cases = failCaseMapper.selectList(cw);
        if (CollectionUtils.isEmpty(cases)) {
            return new ArrayList<>();
        }

        // 关联周度报告与机型，补齐分支/版本/模块/机型/时间等来源信息
        Set<Long> reportIds = cases.stream().map(WeeklyFailCase::getReportId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<Long, WeeklyReport> reportMap = reportIds.isEmpty() ? Collections.emptyMap()
                : reportMapper.selectBatchIds(reportIds).stream()
                        .collect(Collectors.toMap(WeeklyReport::getId, Function.identity()));
        Set<Long> projectIds = reportMap.values().stream().map(WeeklyReport::getProjectId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> projectNameMap = projectIds.isEmpty() ? Collections.emptyMap()
                : projectMapper.selectBatchIds(projectIds).stream()
                        .collect(Collectors.toMap(ReleaseProject::getId, ReleaseProject::getProjectName));

        return cases.stream().map(c -> {
            MyTaskCaseVO vo = new MyTaskCaseVO();
            BeanUtils.copyProperties(c, vo);
            vo.setBoard("weekly");
            FailCaseStatus st = FailCaseStatus.of(c.getStatus());
            vo.setStatusDesc(st == null ? null : st.getDesc());
            vo.setCaseTypeDesc("error".equals(c.getCaseType()) ? "错误" : "失败");
            WeeklyReport r = reportMap.get(c.getReportId());
            if (r != null) {
                vo.setBranch(r.getBranch());
                vo.setVersion(r.getVersion());
                vo.setModuleName(r.getModuleName());
                vo.setProjectName(projectNameMap.get(r.getProjectId()));
                vo.setPublishTime(r.getPublishTime());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    /* ==================== 私有方法：权限校验 ==================== */

    private long currentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    private boolean isAdmin() {
        return StpUtil.hasRole("super_admin") || StpUtil.hasRole("admin");
    }
}
