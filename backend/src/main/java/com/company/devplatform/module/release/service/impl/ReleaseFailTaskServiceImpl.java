package com.company.devplatform.module.release.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.devplatform.common.ErrorCode;
import com.company.devplatform.common.exception.BusinessException;
import com.company.devplatform.common.vo.MyTaskCaseVO;
import com.company.devplatform.module.auth.entity.SysUser;
import com.company.devplatform.module.auth.mapper.SysUserMapper;
import com.company.devplatform.module.release.dto.FailCaseAssignDTO;
import com.company.devplatform.module.release.dto.FailCaseGroupedQuery;
import com.company.devplatform.module.release.dto.FailCaseHandleDTO;
import com.company.devplatform.module.release.dto.FailCaseUpdateDTO;
import com.company.devplatform.module.release.dto.FailTaskAssignDTO;
import com.company.devplatform.module.release.dto.FailTaskCreateDTO;
import com.company.devplatform.module.release.dto.FailTaskStatusDTO;
import com.company.devplatform.module.release.dto.FailTaskUpdateDTO;
import com.company.devplatform.module.release.entity.ReleaseFailCase;
import com.company.devplatform.module.release.entity.ReleaseFailTask;
import com.company.devplatform.module.release.entity.FileResource;
import com.company.devplatform.module.release.entity.ReleaseProject;
import com.company.devplatform.module.release.entity.ReleaseRecord;
import com.company.devplatform.module.release.entity.ReleaseRecordProject;
import com.company.devplatform.module.release.enums.FailCaseStatus;
import com.company.devplatform.module.release.mapper.FileResourceMapper;
import com.company.devplatform.module.release.enums.FailTaskStatus;
import com.company.devplatform.module.release.enums.ReleaseResult;
import com.company.devplatform.module.release.mapper.ReleaseFailCaseMapper;
import com.company.devplatform.module.release.mapper.ReleaseFailTaskMapper;
import com.company.devplatform.module.release.mapper.ReleaseProjectMapper;
import com.company.devplatform.module.release.mapper.ReleaseRecordMapper;
import com.company.devplatform.module.release.mapper.ReleaseRecordProjectMapper;
import com.company.devplatform.module.release.service.AiAnalysisService;
import com.company.devplatform.module.release.service.ReleaseFailTaskService;
import com.company.devplatform.module.release.util.HtmlReportParser;
import com.company.devplatform.module.release.vo.AiAnalysisResult;
import com.company.devplatform.module.release.vo.FailCaseGroupedVO;
import com.company.devplatform.module.release.vo.FailCaseVO;
import com.company.devplatform.module.release.vo.FailTaskDetailVO;
import com.company.devplatform.module.release.vo.FailTaskVO;
import com.company.devplatform.module.release.vo.GroupedFailCaseVO;
import com.company.devplatform.module.release.vo.RecentDayStatVO;
import com.company.devplatform.module.release.vo.ReportFileItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 失败聚合任务服务实现（阶段2自动创建 + 阶段4查询/CRUD/指派/流转/联动）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReleaseFailTaskServiceImpl implements ReleaseFailTaskService {

    private final ReleaseFailTaskMapper taskMapper;
    private final ReleaseFailCaseMapper caseMapper;
    private final ReleaseRecordMapper recordMapper;
    private final ReleaseRecordProjectMapper recordProjectMapper;
    private final ReleaseProjectMapper projectMapper;
    private final SysUserMapper userMapper;
    private final FileResourceMapper fileResourceMapper;
    private final AiAnalysisService aiAnalysisService;

    /* ==================== 阶段2：自动创建 ==================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReleaseFailTask createTaskForRecord(ReleaseRecord record, List<HtmlReportParser.FailCase> failCases) {
        ReleaseFailTask task = new ReleaseFailTask();
        task.setTaskNo(genTaskNo());
        task.setRecordId(record.getId());
        task.setStatus(FailTaskStatus.PENDING.getCode());
        task.setSummary(buildSummary(record));
        task.setFailReason("发布报告解析出 " + record.getFailedCount() + " 个失败用例、"
                + record.getErrorCount() + " 个错误用例，待定位");
        task.setCreatorId(record.getPublisherId());
        taskMapper.insert(task);

        if (failCases != null) {
            for (HtmlReportParser.FailCase fc : failCases) {
                ReleaseFailCase c = new ReleaseFailCase();
                c.setTaskId(task.getId());
                c.setCaseType("error".equals(fc.getStatus()) ? "error" : "failed");
                c.setCaseName(fc.getName());
                c.setCaseLog(fc.getLog());
                c.setStatus(FailCaseStatus.PENDING.getCode());
                caseMapper.insert(c);
            }
        }
        log.info("[失败任务] 已生成 taskNo={}, recordId={}, cases={}",
                task.getTaskNo(), record.getId(), failCases == null ? 0 : failCases.size());
        return task;
    }

    @Override
    public String genTaskNo() {
        String prefix = "FT" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        // 取物理表最大序号 +1，避免逻辑删除记录占用唯一索引导致编号冲突
        String latest = taskMapper.selectLatestTaskNo(prefix);
        long seq = 1;
        if (StringUtils.hasText(latest) && latest.length() > prefix.length()) {
            try {
                seq = Long.parseLong(latest.substring(prefix.length())) + 1;
            } catch (NumberFormatException ignored) {
                seq = 1;
            }
        }
        return prefix + String.format("%03d", seq);
    }

    @Override
    public ReleaseFailTask getByRecordId(Long recordId) {
        if (recordId == null) {
            return null;
        }
        return taskMapper.selectOne(new LambdaQueryWrapper<ReleaseFailTask>()
                .eq(ReleaseFailTask::getRecordId, recordId)
                .last("limit 1"));
    }

    @Override
    public List<ReleaseFailCase> listCases(Long taskId) {
        return caseMapper.selectList(new LambdaQueryWrapper<ReleaseFailCase>()
                .eq(ReleaseFailCase::getTaskId, taskId)
                .orderByAsc(ReleaseFailCase::getId));
    }

    /* ==================== 阶段4：查询 ==================== */

    @Override
    public IPage<FailTaskVO> pageTasks(int page, int size, Integer status, String keyword, Long assigneeId, boolean mine) {
        LambdaQueryWrapper<ReleaseFailTask> wrapper = new LambdaQueryWrapper<ReleaseFailTask>()
                .eq(status != null, ReleaseFailTask::getStatus, status)
                .eq(assigneeId != null, ReleaseFailTask::getAssigneeId, assigneeId)
                .orderByDesc(ReleaseFailTask::getCreateTime)
                .orderByDesc(ReleaseFailTask::getId);
        if (mine) {
            long current = currentUserId();
            wrapper.eq(ReleaseFailTask::getAssigneeId, current)
                    .in(ReleaseFailTask::getStatus, FailTaskStatus.PENDING.getCode(), FailTaskStatus.PROCESSING.getCode());
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            // 关联发布记录的分支/版本匹配
            List<ReleaseRecord> records = recordMapper.selectList(new LambdaQueryWrapper<ReleaseRecord>()
                    .and(w -> w.like(ReleaseRecord::getBranch, kw).or().like(ReleaseRecord::getVersion, kw)));
            List<Long> recordIds = records.stream().map(ReleaseRecord::getId).collect(Collectors.toList());
            wrapper.and(w -> w.like(ReleaseFailTask::getTaskNo, kw)
                    .or().like(ReleaseFailTask::getSummary, kw)
                    .or(CollectionUtils.isEmpty(recordIds) ? null : w2 -> w2.in(ReleaseFailTask::getRecordId, recordIds)));
        }

        IPage<ReleaseFailTask> p = taskMapper.selectPage(new Page<>(page, size), wrapper);
        List<FailTaskVO> vos = buildVOs(p.getRecords());
        IPage<FailTaskVO> result = new Page<>(p.getCurrent(), p.getSize(), p.getTotal());
        result.setRecords(vos);
        return result;
    }

    @Override
    public FailTaskDetailVO detail(Long id) {
        ReleaseFailTask task = getTaskOrThrow(id);
        // 复用列表页组装逻辑，补全发布记录/机型/用户名信息
        FailTaskVO base = buildVOs(Collections.singletonList(task)).get(0);
        FailTaskDetailVO vo = new FailTaskDetailVO();
        BeanUtils.copyProperties(base, vo);
        List<ReleaseFailCase> cases = listCases(id);
        vo.setCases(toCaseVOs(cases));
        return vo;
    }

    /* ==================== 阶段4：CRUD ==================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReleaseFailTask create(FailTaskCreateDTO dto) {
        if (dto.getRecordId() != null) {
            ReleaseRecord record = recordMapper.selectById(dto.getRecordId());
            if (record == null) {
                throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "关联的发布记录不存在");
            }
            if (record.getResult() == null || record.getResult() != 2) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "仅失败状态的发布记录可关联失败任务");
            }
            if (getByRecordId(record.getId()) != null) {
                throw new BusinessException(ErrorCode.DATA_EXIST, "该发布记录已存在失败任务");
            }
        }
        checkUserExists(dto.getAssigneeId());

        ReleaseFailTask task = new ReleaseFailTask();
        task.setTaskNo(genTaskNo());
        task.setRecordId(dto.getRecordId());
        task.setStatus(FailTaskStatus.PENDING.getCode());
        task.setSummary(dto.getSummary());
        task.setFailReason(dto.getFailReason());
        task.setFixPlan(dto.getFixPlan());
        task.setAssigneeId(dto.getAssigneeId());
        task.setCreatorId(currentUserId());
        taskMapper.insert(task);

        if (!CollectionUtils.isEmpty(dto.getCases())) {
            for (FailTaskCreateDTO.FailCaseItemDTO item : dto.getCases()) {
                ReleaseFailCase c = new ReleaseFailCase();
                c.setTaskId(task.getId());
                c.setCaseType("error".equals(item.getCaseType()) ? "error" : "failed");
                c.setCaseName(item.getCaseName());
                c.setCaseLog(item.getCaseLog());
                c.setStatus(FailCaseStatus.PENDING.getCode());
                c.setAssigneeId(dto.getAssigneeId());
                caseMapper.insert(c);
            }
        }
        log.info("[失败任务] 手动创建 taskNo={}, cases={}", task.getTaskNo(),
                dto.getCases() == null ? 0 : dto.getCases().size());
        return task;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(FailTaskUpdateDTO dto) {
        ReleaseFailTask task = getTaskOrThrow(dto.getId());
        checkOperatePermission(task);
        if (StringUtils.hasText(dto.getSummary())) {
            task.setSummary(dto.getSummary());
        }
        task.setFailReason(dto.getFailReason());
        task.setFixPlan(dto.getFixPlan());
        taskMapper.updateById(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ReleaseFailTask task = getTaskOrThrow(id);
        checkManagePermission(task);
        // 级联删除用例明细（逻辑删除）
        caseMapper.delete(new LambdaQueryWrapper<ReleaseFailCase>().eq(ReleaseFailCase::getTaskId, id));
        taskMapper.deleteById(id);
        log.info("[失败任务] 删除 taskNo={}", task.getTaskNo());
    }

    /* ==================== 阶段4：指派/流转/处理 ==================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assign(FailTaskAssignDTO dto) {
        ReleaseFailTask task = getTaskOrThrow(dto.getId());
        checkManagePermission(task);
        FailTaskStatus current = FailTaskStatus.of(task.getStatus());
        if (current == FailTaskStatus.COMPLETED || current == FailTaskStatus.CLOSED) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "任务已结束，不能重新指派");
        }
        checkUserExists(dto.getAssigneeId());
        task.setAssigneeId(dto.getAssigneeId());
        taskMapper.updateById(task);
        // 待处理用例继承任务责任人
        List<ReleaseFailCase> cases = listCases(task.getId());
        for (ReleaseFailCase c : cases) {
            if (c.getStatus() != null && c.getStatus() == FailCaseStatus.PENDING.getCode()) {
                c.setAssigneeId(dto.getAssigneeId());
                caseMapper.updateById(c);
            }
        }
        log.info("[失败任务] 指派 taskNo={} -> userId={}", task.getTaskNo(), dto.getAssigneeId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(FailTaskStatusDTO dto) {
        ReleaseFailTask task = getTaskOrThrow(dto.getId());
        checkOperatePermission(task);
        FailTaskStatus current = FailTaskStatus.of(task.getStatus());
        FailTaskStatus target = FailTaskStatus.of(dto.getStatus());
        if (current == null || target == null || current == target) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "非法的状态流转");
        }
        if (current == FailTaskStatus.COMPLETED || current == FailTaskStatus.CLOSED) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "任务已结束，不能流转");
        }
        if (target == FailTaskStatus.COMPLETED) {
            List<ReleaseFailCase> cases = listCases(task.getId());
            boolean allDone = cases.stream()
                    .allMatch(c -> c.getStatus() != null && c.getStatus() >= FailCaseStatus.FIXED.getCode());
            if (!allDone) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "存在未处理完成的用例，请先处理全部用例");
            }
        }
        task.setStatus(target.getCode());
        task.setHandleTime(target == FailTaskStatus.COMPLETED ? LocalDateTime.now() : null);
        taskMapper.updateById(task);
        log.info("[失败任务] 状态流转 taskNo={} {} -> {}", task.getTaskNo(), current.getDesc(), target.getDesc());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleCase(FailCaseHandleDTO dto) {
        ReleaseFailCase c = caseMapper.selectById(dto.getCaseId());
        if (c == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "用例明细不存在");
        }
        ReleaseFailTask task = taskMapper.selectById(c.getTaskId());
        if (task == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "所属失败任务不存在");
        }
        // handleCase 会在后续自动将未指派用例指派给当前用户，权限校验时若用例未指派则视当前用户为有效被指派人
        Long effectiveAssigneeId = c.getAssigneeId() != null ? c.getAssigneeId() : currentUserId();
        checkCasePermission(task, effectiveAssigneeId);
        FailTaskStatus taskStatus = FailTaskStatus.of(task.getStatus());
        if (taskStatus == FailTaskStatus.COMPLETED || taskStatus == FailTaskStatus.CLOSED) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "任务已结束，不能处理用例");
        }

        FailCaseStatus current = FailCaseStatus.of(c.getStatus());
        FailCaseStatus target = FailCaseStatus.of(dto.getStatus());
        if (current == null || target == null || current == target) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "非法的用例状态流转");
        }
        // 已处理完成的用例仅允许回退到处理中，不允许回到待处理
        if (current.getCode() >= FailCaseStatus.FIXED.getCode() && target == FailCaseStatus.PENDING) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "已处理完成的用例不能回退为待处理");
        }
        if (target == FailCaseStatus.FIXED || target == FailCaseStatus.NOT_DEFECT) {
            if (!StringUtils.hasText(dto.getFailReason()) && !StringUtils.hasText(dto.getFixPlan())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "请填写失败原因或修改方案");
            }
            c.setHandleTime(LocalDateTime.now());
        }
        c.setStatus(target.getCode());
        c.setFailReason(dto.getFailReason());
        c.setFixPlan(dto.getFixPlan());
        if (c.getAssigneeId() == null) {
            c.setAssigneeId(currentUserId());
        }
        caseMapper.updateById(c);

        // 联动刷新任务状态
        refreshTaskStatus(task.getId());
        log.info("[失败任务] 处理用例 taskNo={} caseId={} {} -> {}", task.getTaskNo(), c.getId(),
                current.getDesc(), target.getDesc());
    }

    @Override
    public List<FailCaseGroupedVO> listGroupedCases(FailCaseGroupedQuery query) {
        LocalDate date = query.getDate() == null ? LocalDate.now() : query.getDate();
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(java.time.LocalTime.MAX);

        LambdaQueryWrapper<ReleaseRecord> rw = new LambdaQueryWrapper<ReleaseRecord>()
                .between(ReleaseRecord::getPublishTime, start, end)
                .eq(ReleaseRecord::getResult, ReleaseResult.FAILED.getCode());
        if (StringUtils.hasText(query.getBranch())) {
            rw.like(ReleaseRecord::getBranch, query.getBranch().trim());
        }
        List<ReleaseRecord> records = recordMapper.selectList(rw);
        if (CollectionUtils.isEmpty(records)) {
            return new ArrayList<>();
        }

        Set<Long> recordIds = records.stream().map(ReleaseRecord::getId).collect(Collectors.toSet());
        Map<Long, ReleaseRecord> recordMap = records.stream()
                .collect(Collectors.toMap(ReleaseRecord::getId, Function.identity()));
        Map<Long, List<String>> recordProjectMap = buildRecordProjectMap(recordIds);

        List<ReleaseFailTask> tasks = taskMapper.selectList(
                new LambdaQueryWrapper<ReleaseFailTask>().in(ReleaseFailTask::getRecordId, recordIds));
        if (CollectionUtils.isEmpty(tasks)) {
            return new ArrayList<>();
        }
        Set<Long> taskIds = tasks.stream().map(ReleaseFailTask::getId).collect(Collectors.toSet());
        Map<Long, List<ReleaseFailCase>> casesByTask = new HashMap<>();
        LambdaQueryWrapper<ReleaseFailCase> cw = new LambdaQueryWrapper<ReleaseFailCase>()
                .in(ReleaseFailCase::getTaskId, taskIds);
        if (StringUtils.hasText(query.getCaseName())) {
            cw.like(ReleaseFailCase::getCaseName, query.getCaseName().trim());
        }
        List<ReleaseFailCase> allCases = caseMapper.selectList(cw);
        for (ReleaseFailCase c : allCases) {
            casesByTask.computeIfAbsent(c.getTaskId(), k -> new ArrayList<>()).add(c);
        }

        Set<Long> userIds = new HashSet<>();
        tasks.forEach(t -> {
            if (t.getAssigneeId() != null) {
                userIds.add(t.getAssigneeId());
            }
        });
        userIds.addAll(casesByTask.values().stream().flatMap(List::stream)
                .map(ReleaseFailCase::getAssigneeId).filter(java.util.Objects::nonNull).collect(Collectors.toSet()));
        Map<Long, String> userMap = userIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(SysUser::getId, u -> {
                            String name = u.getNickname();
                            return StringUtils.hasText(name) ? name : u.getUsername();
                        }));

        Map<String, FailCaseGroupedVO> groupMap = new LinkedHashMap<>();
        // 收集每个分组（分支×机型）涉及的原始报告文件ID
        Map<String, Set<Long>> groupFileIds = new HashMap<>();
        for (ReleaseFailTask task : tasks) {
            ReleaseRecord record = recordMap.get(task.getRecordId());
            if (record == null) {
                continue;
            }
            List<String> projectNames = recordProjectMap.getOrDefault(record.getId(), Collections.emptyList());
            if (CollectionUtils.isEmpty(projectNames)) {
                projectNames = Collections.singletonList("未知机型");
            }
            List<ReleaseFailCase> cases = casesByTask.getOrDefault(task.getId(), Collections.emptyList());
            if (CollectionUtils.isEmpty(cases)) {
                continue;
            }
            for (String projectName : projectNames) {
                if (StringUtils.hasText(query.getProjectName())
                        && !projectName.contains(query.getProjectName().trim())) {
                    continue;
                }
                String key = record.getBranch() + "#" + projectName;
                if (record.getReportFileId() != null) {
                    groupFileIds.computeIfAbsent(key, k -> new HashSet<>()).add(record.getReportFileId());
                }
                FailCaseGroupedVO group = groupMap.computeIfAbsent(key, k -> {
                    FailCaseGroupedVO g = new FailCaseGroupedVO();
                    String[] parts = k.split("#", 2);
                    g.setBranch(parts[0]);
                    g.setProjectName(parts[1]);
                    g.setTotalCount(0);
                    g.setPassedCount(0);
                    g.setFailedCount(0);
                    g.setPassRate(0);
                    g.setCases(new ArrayList<>());
                    return g;
                });
                group.setTotalCount(group.getTotalCount() + nvl(record.getTotalCount()));
                group.setPassedCount(group.getPassedCount() + nvl(record.getPassedCount()));
                group.setFailedCount(group.getFailedCount() + (nvl(record.getTotalCount()) - nvl(record.getPassedCount())));
                for (ReleaseFailCase c : cases) {
                    GroupedFailCaseVO gcv = new GroupedFailCaseVO();
                    BeanUtils.copyProperties(c, gcv);
                    gcv.setRecordId(record.getId());
                    gcv.setTaskId(task.getId());
                    FailCaseStatus st = FailCaseStatus.of(c.getStatus());
                    gcv.setStatusDesc(st == null ? null : st.getDesc());
                    gcv.setCaseTypeDesc("error".equals(c.getCaseType()) ? "错误" : "失败");
                    gcv.setAssigneeName(c.getAssigneeId() == null ? null : userMap.get(c.getAssigneeId()));
                    gcv.setPublishTime(record.getPublishTime());
                    group.getCases().add(gcv);
                }
            }
        }

        for (FailCaseGroupedVO group : groupMap.values()) {
            int total = group.getTotalCount();
            int passed = group.getPassedCount();
            group.setPassRate(total > 0 ? (int) Math.round(passed * 100.0 / total) : 0);
        }

        // 批量查询分组涉及的原始 HTML 报告文件
        Set<Long> allFileIds = groupFileIds.values().stream().flatMap(Set::stream).collect(Collectors.toSet());
        Map<Long, String> fileNameMap = allFileIds.isEmpty() ? Collections.emptyMap()
                : fileResourceMapper.selectBatchIds(allFileIds).stream()
                        .collect(Collectors.toMap(FileResource::getId, FileResource::getFileName));
        for (FailCaseGroupedVO group : groupMap.values()) {
            Set<Long> ids = groupFileIds.getOrDefault(group.getBranch() + "#" + group.getProjectName(),
                    Collections.emptySet());
            List<ReportFileItemVO> files = ids.stream()
                    .filter(fileNameMap::containsKey)
                    .map(id -> {
                        ReportFileItemVO item = new ReportFileItemVO();
                        item.setFileId(id);
                        item.setFileName(fileNameMap.get(id));
                        return item;
                    })
                    .collect(Collectors.toList());
            group.setReportFiles(files);
        }

        return groupMap.values().stream()
                .sorted(java.util.Comparator.comparing(FailCaseGroupedVO::getBranch)
                        .thenComparing(FailCaseGroupedVO::getProjectName))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCase(Long caseId, FailCaseUpdateDTO dto) {
        ReleaseFailCase c = caseMapper.selectById(caseId);
        if (c == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "用例明细不存在");
        }
        ReleaseFailTask task = taskMapper.selectById(c.getTaskId());
        if (task == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "所属失败任务不存在");
        }
        // 权限校验（用例级）：管理员（超管/普通管理员）不受限；普通用户仅当责任人是自己时可操作
        checkCasePermission(task, c.getAssigneeId());
        FailTaskStatus taskStatus = FailTaskStatus.of(task.getStatus());
        if (taskStatus == FailTaskStatus.COMPLETED || taskStatus == FailTaskStatus.CLOSED) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "任务已结束，不能处理用例");
        }

        // 前置条件校验：仅普通用户遵循，管理员（超管/普通管理员）豁免
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
        caseMapper.updateById(c);

        refreshTaskStatus(task.getId());
        log.info("[失败任务] 更新用例 taskNo={} caseId={} status={}", task.getTaskNo(), c.getId(), dto.getStatus());
    }

    @Override
    public void assignCaseAssignee(Long caseId, FailCaseAssignDTO dto) {
        ReleaseFailCase c = caseMapper.selectById(caseId);
        if (c == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "用例明细不存在");
        }
        ReleaseFailTask task = taskMapper.selectById(c.getTaskId());
        if (task == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "所属失败任务不存在");
        }
        Long assigneeId = dto.getAssigneeId();
        // 权限校验（用例级）：管理员（超管/普通管理员）不受限；普通用户仅当责任人是自己时可操作
        checkCasePermission(task, c.getAssigneeId());
        FailTaskStatus taskStatus = FailTaskStatus.of(task.getStatus());
        if (taskStatus == FailTaskStatus.COMPLETED || taskStatus == FailTaskStatus.CLOSED) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "任务已结束，不能处理用例");
        }
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
        caseMapper.updateById(c);
        refreshTaskStatus(task.getId());
        log.info("[失败任务] 快速指派责任人 taskNo={} caseId={} assigneeId={}", task.getTaskNo(), c.getId(), assigneeId);
    }

    @Override
    public List<MyTaskCaseVO> listMyCases(boolean all) {
        long current = currentUserId();
        LambdaQueryWrapper<ReleaseFailCase> cw = new LambdaQueryWrapper<ReleaseFailCase>()
                .eq(ReleaseFailCase::getAssigneeId, current);
        if (!all) {
            cw.in(ReleaseFailCase::getStatus, FailCaseStatus.PENDING.getCode(), FailCaseStatus.PROCESSING.getCode());
        }
        cw.orderByDesc(ReleaseFailCase::getId);
        List<ReleaseFailCase> cases = caseMapper.selectList(cw);
        if (CollectionUtils.isEmpty(cases)) {
            return new ArrayList<>();
        }

        // 关联失败任务与发布记录，补齐分支/版本/机型/时间等来源信息
        Set<Long> taskIds = cases.stream().map(ReleaseFailCase::getTaskId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<Long, ReleaseFailTask> taskMap = taskIds.isEmpty() ? Collections.emptyMap()
                : taskMapper.selectBatchIds(taskIds).stream()
                        .collect(Collectors.toMap(ReleaseFailTask::getId, Function.identity()));
        Set<Long> recordIds = taskMap.values().stream().map(ReleaseFailTask::getRecordId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<Long, ReleaseRecord> recordMap = recordIds.isEmpty() ? Collections.emptyMap()
                : recordMapper.selectBatchIds(recordIds).stream()
                        .collect(Collectors.toMap(ReleaseRecord::getId, Function.identity()));
        Map<Long, List<String>> projectMap = buildRecordProjectMap(recordIds);

        return cases.stream().map(c -> {
            MyTaskCaseVO vo = new MyTaskCaseVO();
            BeanUtils.copyProperties(c, vo);
            vo.setBoard("daily");
            FailCaseStatus st = FailCaseStatus.of(c.getStatus());
            vo.setStatusDesc(st == null ? null : st.getDesc());
            vo.setCaseTypeDesc("error".equals(c.getCaseType()) ? "错误" : "失败");
            ReleaseFailTask t = taskMap.get(c.getTaskId());
            if (t != null) {
                vo.setTaskNo(t.getTaskNo());
                ReleaseRecord r = recordMap.get(t.getRecordId());
                if (r != null) {
                    vo.setBranch(r.getBranch());
                    vo.setVersion(r.getVersion());
                    vo.setPublishTime(r.getPublishTime());
                    List<String> names = projectMap.getOrDefault(r.getId(), Collections.emptyList());
                    vo.setProjectName(names.isEmpty() ? null : String.join(", ", names));
                }
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public AiAnalysisResult aiAnalyze(Long caseId) {
        ReleaseFailCase c = caseMapper.selectById(caseId);
        if (c == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "用例明细不存在");
        }
        if (!StringUtils.hasText(c.getCaseLog())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该用例无执行日志，无法分析");
        }
        AiAnalysisResult result = aiAnalysisService.analyze(c.getCaseLog(), "daily_sanity");
        c.setAiRootCause(result.getRootCause());
        c.setAiEvidence(result.getEvidence());
        c.setAiSolution(result.getSolution());
        caseMapper.updateById(c);
        return result;
    }

    @Override
    public List<RecentDayStatVO> recentWeekStats() {
        LocalDate today = LocalDate.now();
        LocalDate startDay = today.minusDays(6);
        LocalDateTime start = startDay.atStartOfDay();
        LocalDateTime end = today.atTime(LocalTime.MAX);

        // 1. 查询最近7天的失败发布记录
        LambdaQueryWrapper<ReleaseRecord> rw = new LambdaQueryWrapper<ReleaseRecord>()
                .between(ReleaseRecord::getPublishTime, start, end)
                .eq(ReleaseRecord::getResult, ReleaseResult.FAILED.getCode());
        List<ReleaseRecord> records = recordMapper.selectList(rw);

        // 2. 查询关联的 fail tasks
        Set<Long> recordIds = records.stream().map(ReleaseRecord::getId).collect(Collectors.toSet());
        List<ReleaseFailTask> tasks = recordIds.isEmpty() ? Collections.emptyList()
                : taskMapper.selectList(new LambdaQueryWrapper<ReleaseFailTask>()
                        .in(ReleaseFailTask::getRecordId, recordIds));

        // 3. 查询所有 cases
        Set<Long> taskIds = tasks.stream().map(ReleaseFailTask::getId).collect(Collectors.toSet());
        List<ReleaseFailCase> allCases = taskIds.isEmpty() ? Collections.emptyList()
                : caseMapper.selectList(new LambdaQueryWrapper<ReleaseFailCase>()
                        .in(ReleaseFailCase::getTaskId, taskIds));

        // 4. 按日期分组统计
        Map<LocalDate, List<ReleaseRecord>> recordsByDate = records.stream()
                .collect(Collectors.groupingBy(r -> r.getPublishTime().toLocalDate()));

        List<RecentDayStatVO> result = new ArrayList<>();
        for (LocalDate d = startDay; !d.isAfter(today); d = d.plusDays(1)) {
            List<ReleaseRecord> dayRecords = recordsByDate.getOrDefault(d, Collections.emptyList());
            Set<Long> dayRecordIds = dayRecords.stream().map(ReleaseRecord::getId).collect(Collectors.toSet());

            List<ReleaseFailTask> dayTasks = tasks.stream()
                    .filter(t -> dayRecordIds.contains(t.getRecordId()))
                    .collect(Collectors.toList());
            Set<Long> dayTaskIds = dayTasks.stream().map(ReleaseFailTask::getId).collect(Collectors.toSet());

            List<ReleaseFailCase> dayCases = allCases.stream()
                    .filter(c -> dayTaskIds.contains(c.getTaskId()))
                    .collect(Collectors.toList());

            int total = dayCases.size();
            int analyzed = (int) dayCases.stream()
                    .filter(c -> c.getStatus() != null && c.getStatus() >= FailCaseStatus.FIXED.getCode())
                    .count();
            // 当天无执行明细时 rate 为 null，前端展示 None
            Integer rate = total > 0 ? (int) Math.round(analyzed * 100.0 / total) : null;

            RecentDayStatVO vo = new RecentDayStatVO();
            vo.setDate(d.toString());
            vo.setTotalCount(total);
            vo.setAnalyzedCount(analyzed);
            vo.setRate(rate);
            result.add(vo);
        }
        return result;
    }

    /* ==================== 私有方法：联动规则与组装 ==================== */

    /**
     * 任务状态与明细状态联动规则：
     * <ul>
     *   <li>存在任一处理中(2)明细 → 任务=处理中</li>
     *   <li>全部明细∈{已修复(3),非缺陷(4)} → 任务=已完成并记录完成时间</li>
     *   <li>其余（存在待处理且无处理中）→ 任务=待处理</li>
     *   <li>任务处于终态(已完成/已关闭)时不自动联动</li>
     * </ul>
     */
    private void refreshTaskStatus(Long taskId) {
        ReleaseFailTask task = taskMapper.selectById(taskId);
        if (task == null || task.getStatus() == null) {
            return;
        }
        FailTaskStatus taskStatus = FailTaskStatus.of(task.getStatus());
        if (taskStatus == FailTaskStatus.COMPLETED || taskStatus == FailTaskStatus.CLOSED) {
            return;
        }
        List<ReleaseFailCase> cases = listCases(taskId);
        if (cases.isEmpty()) {
            return;
        }
        boolean allDone = cases.stream()
                .allMatch(c -> c.getStatus() != null && c.getStatus() >= FailCaseStatus.FIXED.getCode());
        boolean anyProcessing = cases.stream()
                .anyMatch(c -> c.getStatus() != null && c.getStatus() == FailCaseStatus.PROCESSING.getCode());
        if (allDone) {
            task.setStatus(FailTaskStatus.COMPLETED.getCode());
            task.setHandleTime(LocalDateTime.now());
        } else if (anyProcessing) {
            task.setStatus(FailTaskStatus.PROCESSING.getCode());
            task.setHandleTime(null);
        } else {
            task.setStatus(FailTaskStatus.PENDING.getCode());
            task.setHandleTime(null);
        }
        taskMapper.updateById(task);
    }

    private List<FailTaskVO> buildVOs(List<ReleaseFailTask> tasks) {
        if (CollectionUtils.isEmpty(tasks)) {
            return new ArrayList<>();
        }
        List<Long> taskIds = tasks.stream().map(ReleaseFailTask::getId).collect(Collectors.toList());
        Set<Long> recordIds = tasks.stream().map(ReleaseFailTask::getRecordId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Set<Long> userIds = new HashSet<>();
        tasks.forEach(t -> {
            if (t.getAssigneeId() != null) {
                userIds.add(t.getAssigneeId());
            }
            if (t.getCreatorId() != null) {
                userIds.add(t.getCreatorId());
            }
        });

        Map<Long, ReleaseRecord> recordMap = recordIds.isEmpty() ? Collections.emptyMap()
                : recordMapper.selectBatchIds(recordIds).stream()
                        .collect(Collectors.toMap(ReleaseRecord::getId, Function.identity()));
        Map<Long, List<String>> recordProjectMap = buildRecordProjectMap(recordIds);
        Map<Long, String> userMap = userIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(SysUser::getId, u -> {
                            String name = u.getNickname();
                            return StringUtils.hasText(name) ? name : u.getUsername();
                        }));
        Map<Long, int[]> caseStats = buildCaseStats(taskIds);

        return tasks.stream().map(t -> toVO(t, recordMap, recordProjectMap, userMap, caseStats))
                .collect(Collectors.toList());
    }

    private FailTaskVO toVO(ReleaseFailTask t) {
        return toVO(t, Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
    }

    private FailTaskVO toVO(ReleaseFailTask t, Map<Long, ReleaseRecord> recordMap,
                            Map<Long, List<String>> recordProjectMap, Map<Long, String> userMap,
                            Map<Long, int[]> caseStats) {
        FailTaskVO vo = new FailTaskVO();
        BeanUtils.copyProperties(t, vo);
        ReleaseRecord r = recordMap.get(t.getRecordId());
        if (r != null) {
            vo.setBranch(r.getBranch());
            vo.setVersion(r.getVersion());
        }
        vo.setProjectNames(recordProjectMap.getOrDefault(t.getRecordId(), Collections.emptyList()));
        FailTaskStatus st = FailTaskStatus.of(t.getStatus());
        vo.setStatusDesc(st == null ? null : st.getDesc());
        vo.setAssigneeName(t.getAssigneeId() == null ? null : userMap.get(t.getAssigneeId()));
        vo.setCreatorName(t.getCreatorId() == null ? null : userMap.get(t.getCreatorId()));
        int[] stat = caseStats.get(t.getId());
        vo.setCaseTotal(stat == null ? 0 : stat[0]);
        vo.setCasePending(stat == null ? 0 : stat[1]);
        vo.setCaseProcessing(stat == null ? 0 : stat[2]);
        vo.setCaseDone(stat == null ? 0 : stat[3]);
        return vo;
    }

    private List<FailCaseVO> toCaseVOs(List<ReleaseFailCase> cases) {
        if (CollectionUtils.isEmpty(cases)) {
            return new ArrayList<>();
        }
        Set<Long> userIds = cases.stream().map(ReleaseFailCase::getAssigneeId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> userMap = userIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(SysUser::getId, u -> {
                            String name = u.getNickname();
                            return StringUtils.hasText(name) ? name : u.getUsername();
                        }));
        return cases.stream().map(c -> {
            FailCaseVO vo = new FailCaseVO();
            BeanUtils.copyProperties(c, vo);
            FailCaseStatus st = FailCaseStatus.of(c.getStatus());
            vo.setStatusDesc(st == null ? null : st.getDesc());
            vo.setCaseTypeDesc("error".equals(c.getCaseType()) ? "错误" : "失败");
            vo.setAssigneeName(c.getAssigneeId() == null ? null : userMap.get(c.getAssigneeId()));
            return vo;
        }).collect(Collectors.toList());
    }

    /** recordId -> [total, pending, processing, done(已修复+非缺陷)] */
    private Map<Long, int[]> buildCaseStats(List<Long> taskIds) {
        if (CollectionUtils.isEmpty(taskIds)) {
            return Collections.emptyMap();
        }
        List<ReleaseFailCase> cases = caseMapper.selectList(new LambdaQueryWrapper<ReleaseFailCase>()
                .in(ReleaseFailCase::getTaskId, taskIds));
        Map<Long, int[]> map = new HashMap<>();
        for (ReleaseFailCase c : cases) {
            int[] stat = map.computeIfAbsent(c.getTaskId(), k -> new int[4]);
            stat[0]++;
            if (c.getStatus() != null && c.getStatus() >= FailCaseStatus.FIXED.getCode()) {
                stat[3]++;
            } else if (c.getStatus() != null && c.getStatus() == FailCaseStatus.PROCESSING.getCode()) {
                stat[2]++;
            } else {
                stat[1]++;
            }
        }
        return map;
    }

    /** recordId -> List<机型名> */
    private Map<Long, List<String>> buildRecordProjectMap(Set<Long> recordIds) {
        if (CollectionUtils.isEmpty(recordIds)) {
            return Collections.emptyMap();
        }
        List<ReleaseRecordProject> links = recordProjectMapper.selectList(
                new LambdaQueryWrapper<ReleaseRecordProject>().in(ReleaseRecordProject::getRecordId, recordIds));
        if (CollectionUtils.isEmpty(links)) {
            return Collections.emptyMap();
        }
        Set<Long> projectIds = links.stream().map(ReleaseRecordProject::getProjectId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> nameMap = projectIds.isEmpty() ? Collections.emptyMap()
                : projectMapper.selectBatchIds(projectIds).stream()
                        .collect(Collectors.toMap(ReleaseProject::getId, ReleaseProject::getProjectName));
        Map<Long, List<String>> result = new HashMap<>();
        for (ReleaseRecordProject link : links) {
            String name = nameMap.get(link.getProjectId());
            if (StringUtils.hasText(name)) {
                result.computeIfAbsent(link.getRecordId(), k -> new ArrayList<>()).add(name);
            }
        }
        return result;
    }

    private ReleaseFailTask getTaskOrThrow(Long id) {
        ReleaseFailTask task = id == null ? null : taskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "失败任务不存在");
        }
        return task;
    }

    private void checkUserExists(Long userId) {
        if (userId != null && userMapper.selectById(userId) == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "指定的责任人不存在");
        }
    }

    /* ==================== 私有方法：权限校验（供测试 spy 覆盖） ==================== */

    protected long currentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    protected boolean isSuperAdmin() {
        return StpUtil.hasRole("super_admin");
    }

    /** 是否管理员：超管或普通管理员（普通员工不视为管理员） */
    protected boolean isAdmin() {
        return StpUtil.hasRole("super_admin") || StpUtil.hasRole("admin");
    }

    /**
     * 操作权限：仅任务创建人、任务被指派人、用例被指派人或超管可流转/处理
     */
    protected void checkOperatePermission(ReleaseFailTask task) {
        checkOperatePermission(task, null);
    }

    /**
     * 操作权限（含用例级被指派人校验）：DailySanity 场景下用例可单独指派，
     * 任务级 assigneeId 可能为 null，需额外校验用例级 assigneeId。
     */
    protected void checkOperatePermission(ReleaseFailTask task, Long caseAssigneeId) {
        long current = currentUserId();
        if (isSuperAdmin()) {
            return;
        }
        boolean isCreator = task.getCreatorId() != null && task.getCreatorId().equals(current);
        boolean isTaskAssignee = task.getAssigneeId() != null && task.getAssigneeId().equals(current);
        boolean isCaseAssignee = caseAssigneeId != null && caseAssigneeId.equals(current);
        if (!isCreator && !isTaskAssignee && !isCaseAssignee) {
            throw new BusinessException(ErrorCode.NO_PERMISSION, "仅任务创建人或被指派人可操作");
        }
    }

    /**
     * 用例级操作权限：管理员（超管/普通管理员）不受限；
     * 普通用户仅当用例责任人是自己时可操作（未指派 null 不在此拦截，由调用方按"认领/指派"规则进一步校验）。
     */
    protected void checkCasePermission(ReleaseFailTask task, Long caseAssigneeId) {
        long current = currentUserId();
        if (isSuperAdmin() || isAdmin()) {
            return;
        }
        if (caseAssigneeId != null && !caseAssigneeId.equals(current)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION, "仅责任人为自己的问题单可操作");
        }
    }

    /**
     * 管理权限：仅任务创建人或超管可删除/指派
     */
    protected void checkManagePermission(ReleaseFailTask task) {
        long current = currentUserId();
        if (isSuperAdmin()) {
            return;
        }
        boolean isCreator = task.getCreatorId() != null && task.getCreatorId().equals(current);
        if (!isCreator) {
            throw new BusinessException(ErrorCode.NO_PERMISSION, "仅任务创建人可执行该操作");
        }
    }

    private String buildSummary(ReleaseRecord record) {
        String branch = StringUtils.hasText(record.getBranch()) ? record.getBranch() : "未知分支";
        String version = StringUtils.hasText(record.getVersion()) ? record.getVersion() : "未知版本";
        return "发布 " + branch + (StringUtils.hasText(record.getVersion()) ? "@" + version : "")
                + " 存在 " + record.getFailedCount() + " 个失败用例";
    }

    private int nvl(Integer value) {
        return value == null ? 0 : value;
    }
}
