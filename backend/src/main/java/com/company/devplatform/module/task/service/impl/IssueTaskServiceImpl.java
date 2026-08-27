package com.company.devplatform.module.task.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.devplatform.common.ErrorCode;
import com.company.devplatform.common.exception.BusinessException;
import com.company.devplatform.module.auth.entity.SysUser;
import com.company.devplatform.module.auth.mapper.SysUserMapper;
import com.company.devplatform.module.task.dto.IssueItemDTO;
import com.company.devplatform.module.task.dto.IssueTaskAssignDTO;
import com.company.devplatform.module.task.dto.IssueTaskCreateDTO;
import com.company.devplatform.module.task.dto.IssueTaskStatusDTO;
import com.company.devplatform.module.task.dto.IssueTaskUpdateDTO;
import com.company.devplatform.module.task.entity.IssueTask;
import com.company.devplatform.module.task.entity.IssueTaskItem;
import com.company.devplatform.module.task.entity.TaskType;
import com.company.devplatform.module.task.enums.TaskItemStatus;
import com.company.devplatform.module.task.enums.TaskPriority;
import com.company.devplatform.module.task.enums.TaskStatus;
import com.company.devplatform.module.task.mapper.IssueTaskItemMapper;
import com.company.devplatform.module.task.mapper.IssueTaskMapper;
import com.company.devplatform.module.task.mapper.TaskTypeMapper;
import com.company.devplatform.module.task.service.IssueTaskService;
import com.company.devplatform.module.task.vo.IssueItemVO;
import com.company.devplatform.module.task.vo.IssueTaskDetailVO;
import com.company.devplatform.module.task.vo.IssueTaskVO;
import com.company.devplatform.module.task.vo.TaskFailCaseVO;
import com.company.devplatform.module.task.vo.TaskFailGroupedVO;
import com.company.devplatform.module.task.vo.TaskTypeCountVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
 * 问题单任务服务实现（通用任务追踪：ATDD/DVS/Pylint/静安/自定义类型）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IssueTaskServiceImpl implements IssueTaskService {

    private final IssueTaskMapper taskMapper;
    private final IssueTaskItemMapper itemMapper;
    private final TaskTypeMapper typeMapper;
    private final SysUserMapper userMapper;

    /* ==================== 查询 ==================== */

    @Override
    public IPage<IssueTaskVO> pageTasks(int page, int size, Long taskTypeId, Integer status,
                                        String keyword, Long assigneeId, boolean mine) {
        LambdaQueryWrapper<IssueTask> wrapper = new LambdaQueryWrapper<IssueTask>()
                .eq(taskTypeId != null, IssueTask::getTaskTypeId, taskTypeId)
                .eq(status != null, IssueTask::getStatus, status)
                .eq(assigneeId != null, IssueTask::getAssigneeId, assigneeId)
                .orderByDesc(IssueTask::getCreateTime)
                .orderByDesc(IssueTask::getId);
        if (mine) {
            long current = currentUserId();
            wrapper.eq(IssueTask::getAssigneeId, current)
                    .in(IssueTask::getStatus, TaskStatus.PENDING.getCode(), TaskStatus.PROCESSING.getCode());
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(IssueTask::getTaskNo, kw)
                    .or().like(IssueTask::getTitle, kw)
                    .or().like(IssueTask::getBranch, kw)
                    .or().like(IssueTask::getVersion, kw));
        }

        IPage<IssueTask> p = taskMapper.selectPage(new Page<>(page, size), wrapper);
        List<IssueTaskVO> vos = buildVOs(p.getRecords());
        IPage<IssueTaskVO> result = new Page<>(p.getCurrent(), p.getSize(), p.getTotal());
        result.setRecords(vos);
        return result;
    }

    @Override
    public IssueTaskDetailVO detail(Long id) {
        IssueTask task = getTaskOrThrow(id);
        IssueTaskVO base = buildVOs(Collections.singletonList(task)).get(0);
        IssueTaskDetailVO vo = new IssueTaskDetailVO();
        BeanUtils.copyProperties(base, vo);
        vo.setSummary(task.getSummary());
        vo.setReason(task.getReason());
        vo.setFixPlan(task.getFixPlan());
        vo.setItems(toItemVOs(listItems(id)));
        return vo;
    }

    /* ==================== CRUD ==================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public IssueTaskVO create(IssueTaskCreateDTO dto) {
        TaskType type = getTypeOrThrow(dto.getTaskTypeId());
        if (type.getEnabled() == null || type.getEnabled() != 1) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该任务类型已停用，无法创建任务");
        }
        checkUserExists(dto.getAssigneeId());

        IssueTask task = new IssueTask();
        task.setTaskNo(genTaskNo(type.getCode()));
        task.setTaskTypeId(dto.getTaskTypeId());
        task.setTitle(dto.getTitle().trim());
        task.setStatus(TaskStatus.PENDING.getCode());
        task.setPriority(dto.getPriority() == null ? TaskPriority.MEDIUM.getCode() : dto.getPriority());
        task.setBranch(dto.getBranch());
        task.setVersion(dto.getVersion());
        task.setProjectName(dto.getProjectName());
        task.setAssigneeId(dto.getAssigneeId());
        task.setCreatorId(currentUserId());
        task.setSummary(dto.getSummary());
        task.setReason(dto.getReason());
        task.setFixPlan(dto.getFixPlan());
        taskMapper.insert(task);

        if (!CollectionUtils.isEmpty(dto.getItems())) {
            for (IssueItemDTO item : dto.getItems()) {
                insertItem(task.getId(), task, item);
            }
        }
        log.info("[任务追踪] 创建 taskNo={} type={} items={}", task.getTaskNo(), type.getCode(),
                dto.getItems() == null ? 0 : dto.getItems().size());
        return buildVOs(Collections.singletonList(task)).get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(IssueTaskUpdateDTO dto) {
        IssueTask task = getTaskOrThrow(dto.getId());
        checkOperatePermission(task);
        if (isFinished(task.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "任务已结束，不能编辑");
        }
        task.setTitle(dto.getTitle().trim());
        task.setPriority(dto.getPriority() == null ? TaskPriority.MEDIUM.getCode() : dto.getPriority());
        task.setBranch(dto.getBranch());
        task.setVersion(dto.getVersion());
        task.setProjectName(dto.getProjectName());
        task.setSummary(dto.getSummary());
        task.setReason(dto.getReason());
        task.setFixPlan(dto.getFixPlan());
        taskMapper.updateById(task);
        log.info("[任务追踪] 编辑 taskNo={}", task.getTaskNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        IssueTask task = getTaskOrThrow(id);
        checkManagePermission(task);
        itemMapper.delete(new LambdaQueryWrapper<IssueTaskItem>().eq(IssueTaskItem::getTaskId, id));
        taskMapper.deleteById(id);
        log.info("[任务追踪] 删除 taskNo={}", task.getTaskNo());
    }

    /* ==================== 指派/流转/处理 ==================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assign(Long id, IssueTaskAssignDTO dto) {
        IssueTask task = getTaskOrThrow(id);
        checkManagePermission(task);
        if (isFinished(task.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "任务已结束，不能重新指派");
        }
        checkUserExists(dto.getAssigneeId());
        task.setAssigneeId(dto.getAssigneeId());
        taskMapper.updateById(task);
        // 待处理明细继承任务责任人
        List<IssueTaskItem> items = listItems(id);
        for (IssueTaskItem item : items) {
            if (item.getStatus() != null && item.getStatus() == TaskItemStatus.PENDING.getCode()) {
                item.setAssigneeId(dto.getAssigneeId());
                itemMapper.updateById(item);
            }
        }
        log.info("[任务追踪] 指派 taskNo={} -> userId={}", task.getTaskNo(), dto.getAssigneeId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long id, IssueTaskStatusDTO dto) {
        IssueTask task = getTaskOrThrow(id);
        checkOperatePermission(task);
        TaskStatus current = TaskStatus.of(task.getStatus());
        TaskStatus target = TaskStatus.of(dto.getStatus());
        if (current == null || target == null || current == target) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "非法的状态流转");
        }
        if (isFinished(task.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "任务已结束，不能流转");
        }
        if (target == TaskStatus.COMPLETED) {
            List<IssueTaskItem> items = listItems(id);
            boolean allDone = items.stream()
                    .allMatch(i -> i.getStatus() != null && i.getStatus() >= TaskItemStatus.FIXED.getCode());
            if (!allDone) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "存在未处理完成的明细项，请先处理全部明细");
            }
        }
        task.setStatus(target.getCode());
        task.setHandleTime(target == TaskStatus.COMPLETED ? LocalDateTime.now() : null);
        taskMapper.updateById(task);
        log.info("[任务追踪] 流转 taskNo={} {} -> {}", task.getTaskNo(), current.getDesc(), target.getDesc());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleItem(IssueItemDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "明细ID不能为空");
        }
        IssueTaskItem item = itemMapper.selectById(dto.getId());
        if (item == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "明细项不存在");
        }
        IssueTask task = taskMapper.selectById(item.getTaskId());
        if (task == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "所属问题单不存在");
        }
        checkOperatePermission(task);
        if (isFinished(task.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "任务已结束，不能处理明细");
        }

        // 已完结明细仅允许回退到处理中，不允许回到待处理
        TaskItemStatus current = TaskItemStatus.of(item.getStatus());
        TaskItemStatus target = TaskItemStatus.of(dto.getStatus());
        if (target == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "非法的明细状态");
        }
        if (current != null && current.getCode() >= TaskItemStatus.FIXED.getCode() && target == TaskItemStatus.PENDING) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "已处理完成的明细不能回退为待处理");
        }
        if (current != null && current == target) {
            // 同状态仅更新信息
            item.setFailReason(dto.getFailReason());
            item.setFixPlan(dto.getFixPlan());
            item.setProgress(dto.getProgress());
            item.setConclusion(dto.getConclusion());
            item.setIsBug(dto.getIsBug() == null ? item.getIsBug() : dto.getIsBug());
            if (dto.getAssigneeId() != null) {
                item.setAssigneeId(dto.getAssigneeId());
            }
            itemMapper.updateById(item);
            return;
        }
        if (target == TaskItemStatus.FIXED || target == TaskItemStatus.NOT_DEFECT) {
            item.setHandleTime(LocalDateTime.now());
        }
        item.setStatus(target.getCode());
        item.setFailReason(dto.getFailReason());
        item.setFixPlan(dto.getFixPlan());
        item.setProgress(dto.getProgress());
        item.setConclusion(dto.getConclusion());
        item.setIsBug(dto.getIsBug() == null ? item.getIsBug() : dto.getIsBug());
        if (item.getAssigneeId() == null) {
            item.setAssigneeId(currentUserId());
        } else if (dto.getAssigneeId() != null) {
            item.setAssigneeId(dto.getAssigneeId());
        }
        itemMapper.updateById(item);

        // 联动刷新任务状态
        refreshTaskStatus(task.getId());
        log.info("[任务追踪] 处理明细 taskNo={} itemId={} {} -> {}", task.getTaskNo(), item.getId(),
                current == null ? "未知" : current.getDesc(), target.getDesc());
    }

    /* ==================== 统计 ==================== */

    @Override
    public List<TaskTypeCountVO> overview() {
        List<TaskType> types = typeMapper.selectList(new LambdaQueryWrapper<TaskType>()
                .eq(TaskType::getEnabled, 1)
                .orderByAsc(TaskType::getSort));
        if (CollectionUtils.isEmpty(types)) {
            return new ArrayList<>();
        }
        List<Long> typeIds = types.stream().map(TaskType::getId).collect(Collectors.toList());
        List<IssueTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<IssueTask>()
                .in(IssueTask::getTaskTypeId, typeIds));
        Map<Long, TaskTypeCountVO> map = new HashMap<>();
        for (TaskType type : types) {
            TaskTypeCountVO vo = new TaskTypeCountVO();
            vo.setTaskTypeId(type.getId());
            vo.setTaskTypeName(type.getName());
            vo.setTaskTypeCode(type.getCode());
            vo.setTotal(0L);
            vo.setPending(0L);
            vo.setProcessing(0L);
            vo.setCompleted(0L);
            vo.setClosed(0L);
            map.put(type.getId(), vo);
        }
        for (IssueTask t : tasks) {
            TaskTypeCountVO vo = map.get(t.getTaskTypeId());
            if (vo == null) {
                continue;
            }
            vo.setTotal(vo.getTotal() + 1);
            TaskStatus st = TaskStatus.of(t.getStatus());
            if (st == null) {
                continue;
            }
            switch (st) {
                case PENDING -> vo.setPending(vo.getPending() + 1);
                case PROCESSING -> vo.setProcessing(vo.getProcessing() + 1);
                case COMPLETED -> vo.setCompleted(vo.getCompleted() + 1);
                case CLOSED -> vo.setClosed(vo.getClosed() + 1);
            }
        }
        return types.stream().map(t -> map.get(t.getId())).collect(Collectors.toList());
    }

    /* ==================== 私有方法 ==================== */

    private String genTaskNo(String typeCode) {
        String prefix = (StringUtils.hasText(typeCode) ? typeCode.trim().toUpperCase() : "TSK")
                + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
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

    private void insertItem(Long taskId, IssueTask task, IssueItemDTO dto) {
        IssueTaskItem item = new IssueTaskItem();
        item.setTaskId(taskId);
        item.setItemType(StringUtils.hasText(dto.getItemType()) ? dto.getItemType() : "failed");
        item.setItemName(dto.getItemName());
        item.setItemLog(dto.getItemLog());
        item.setStatus(dto.getStatus() == null ? TaskItemStatus.PENDING.getCode() : dto.getStatus());
        item.setAssigneeId(dto.getAssigneeId() != null ? dto.getAssigneeId() : task.getAssigneeId());
        item.setFailReason(dto.getFailReason());
        item.setFixPlan(dto.getFixPlan());
        item.setIsBug(dto.getIsBug() == null ? 0 : dto.getIsBug());
        item.setProgress(dto.getProgress());
        item.setConclusion(dto.getConclusion());
        item.setAiAnalysis(dto.getAiAnalysis());
        itemMapper.insert(item);
    }

    /**
     * 任务状态与明细状态联动规则（与失败任务一致）：
     * <ul>
     *   <li>存在任一处理中(2)明细 → 任务=处理中</li>
     *   <li>全部明细∈{已修复(3),非缺陷(4)} → 任务=已完成并记录完成时间</li>
     *   <li>其余（存在待处理且无处理中）→ 任务=待处理</li>
     *   <li>任务处于终态(已完成/已关闭)时不自动联动</li>
     * </ul>
     */
    private void refreshTaskStatus(Long taskId) {
        IssueTask task = taskMapper.selectById(taskId);
        if (task == null || task.getStatus() == null) {
            return;
        }
        if (isFinished(task.getStatus())) {
            return;
        }
        List<IssueTaskItem> items = listItems(taskId);
        if (items.isEmpty()) {
            return;
        }
        boolean allDone = items.stream()
                .allMatch(i -> i.getStatus() != null && i.getStatus() >= TaskItemStatus.FIXED.getCode());
        boolean anyProcessing = items.stream()
                .anyMatch(i -> i.getStatus() != null && i.getStatus() == TaskItemStatus.PROCESSING.getCode());
        if (allDone) {
            task.setStatus(TaskStatus.COMPLETED.getCode());
            task.setHandleTime(LocalDateTime.now());
        } else if (anyProcessing) {
            task.setStatus(TaskStatus.PROCESSING.getCode());
            task.setHandleTime(null);
        } else {
            task.setStatus(TaskStatus.PENDING.getCode());
            task.setHandleTime(null);
        }
        taskMapper.updateById(task);
    }

    private List<IssueTaskVO> buildVOs(List<IssueTask> tasks) {
        if (CollectionUtils.isEmpty(tasks)) {
            return new ArrayList<>();
        }
        List<Long> taskIds = tasks.stream().map(IssueTask::getId).collect(Collectors.toList());
        Set<Long> typeIds = tasks.stream().map(IssueTask::getTaskTypeId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> userIds = new HashSet<>();
        tasks.forEach(t -> {
            if (t.getAssigneeId() != null) {
                userIds.add(t.getAssigneeId());
            }
            if (t.getCreatorId() != null) {
                userIds.add(t.getCreatorId());
            }
        });

        Map<Long, TaskType> typeMap = typeIds.isEmpty() ? Collections.emptyMap()
                : typeMapper.selectBatchIds(typeIds).stream()
                        .collect(Collectors.toMap(TaskType::getId, Function.identity()));
        Map<Long, String> userMap = userIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(SysUser::getId, u -> {
                            String name = u.getNickname();
                            return StringUtils.hasText(name) ? name : u.getUsername();
                        }));
        Map<Long, long[]> itemStats = buildItemStats(taskIds);

        return tasks.stream().map(t -> {
            IssueTaskVO vo = new IssueTaskVO();
            BeanUtils.copyProperties(t, vo);
            TaskType type = typeMap.get(t.getTaskTypeId());
            if (type != null) {
                vo.setTaskTypeName(type.getName());
            }
            TaskStatus st = TaskStatus.of(t.getStatus());
            vo.setStatusDesc(st == null ? null : st.getDesc());
            TaskPriority pr = TaskPriority.of(t.getPriority());
            vo.setPriorityDesc(pr == null ? null : pr.getDesc());
            vo.setAssigneeName(t.getAssigneeId() == null ? null : userMap.get(t.getAssigneeId()));
            vo.setCreatorName(t.getCreatorId() == null ? null : userMap.get(t.getCreatorId()));
            long[] stat = itemStats.get(t.getId());
            vo.setItemTotal(stat == null ? 0L : stat[0]);
            vo.setItemPending(stat == null ? 0L : stat[1]);
            vo.setItemProcessing(stat == null ? 0L : stat[2]);
            vo.setItemDone(stat == null ? 0L : stat[3]);
            return vo;
        }).collect(Collectors.toList());
    }

    /** taskId -> [total, pending, processing, done(已修复+非缺陷)] */
    private Map<Long, long[]> buildItemStats(List<Long> taskIds) {
        if (CollectionUtils.isEmpty(taskIds)) {
            return Collections.emptyMap();
        }
        List<IssueTaskItem> items = itemMapper.selectList(new LambdaQueryWrapper<IssueTaskItem>()
                .in(IssueTaskItem::getTaskId, taskIds));
        Map<Long, long[]> map = new HashMap<>();
        for (IssueTaskItem i : items) {
            long[] stat = map.computeIfAbsent(i.getTaskId(), k -> new long[4]);
            stat[0]++;
            if (i.getStatus() != null && i.getStatus() >= TaskItemStatus.FIXED.getCode()) {
                stat[3]++;
            } else if (i.getStatus() != null && i.getStatus() == TaskItemStatus.PROCESSING.getCode()) {
                stat[2]++;
            } else {
                stat[1]++;
            }
        }
        return map;
    }

    private List<IssueItemVO> toItemVOs(List<IssueTaskItem> items) {
        if (CollectionUtils.isEmpty(items)) {
            return new ArrayList<>();
        }
        Set<Long> userIds = items.stream().map(IssueTaskItem::getAssigneeId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> userMap = userIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(SysUser::getId, u -> {
                            String name = u.getNickname();
                            return StringUtils.hasText(name) ? name : u.getUsername();
                        }));
        return items.stream().map(i -> {
            IssueItemVO vo = new IssueItemVO();
            BeanUtils.copyProperties(i, vo);
            TaskItemStatus st = TaskItemStatus.of(i.getStatus());
            vo.setStatusDesc(st == null ? null : st.getDesc());
            vo.setItemTypeDesc(itemTypeDesc(i.getItemType()));
            vo.setAssigneeName(i.getAssigneeId() == null ? null : userMap.get(i.getAssigneeId()));
            return vo;
        }).collect(Collectors.toList());
    }

    private List<IssueTaskItem> listItems(Long taskId) {
        return itemMapper.selectList(new LambdaQueryWrapper<IssueTaskItem>()
                .eq(IssueTaskItem::getTaskId, taskId)
                .orderByAsc(IssueTaskItem::getId));
    }

    private TaskType getTypeOrThrow(Long id) {
        TaskType type = id == null ? null : typeMapper.selectById(id);
        if (type == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "任务类型不存在");
        }
        return type;
    }

    private IssueTask getTaskOrThrow(Long id) {
        IssueTask task = id == null ? null : taskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "问题单不存在");
        }
        return task;
    }

    private void checkUserExists(Long userId) {
        if (userId != null && userMapper.selectById(userId) == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "指定的责任人不存在");
        }
    }

    private boolean isFinished(Integer status) {
        return status != null
                && (status == TaskStatus.COMPLETED.getCode() || status == TaskStatus.CLOSED.getCode());
    }

    private String itemTypeDesc(String itemType) {
        if ("error".equals(itemType)) {
            return "错误";
        }
        if ("warning".equals(itemType)) {
            return "警告";
        }
        if ("note".equals(itemType)) {
            return "备注";
        }
        return "失败";
    }

    /* ==================== DailySanity 失败任务追踪 ==================== */

    @Override
    public List<TaskFailGroupedVO> groupedFailCases(String date, String branch, String projectName, String caseName) {
        LocalDate day = StringUtils.hasText(date) ? LocalDate.parse(date) : LocalDate.now();
        LocalDateTime start = day.atStartOfDay();
        LocalDateTime end = day.atTime(23, 59, 59);

        LambdaQueryWrapper<IssueTask> tq = new LambdaQueryWrapper<IssueTask>()
                .ge(IssueTask::getCreateTime, start)
                .le(IssueTask::getCreateTime, end);
        if (StringUtils.hasText(branch)) {
            tq.like(IssueTask::getBranch, branch.trim());
        }
        if (StringUtils.hasText(projectName)) {
            tq.like(IssueTask::getProjectName, projectName.trim());
        }
        tq.orderByAsc(IssueTask::getBranch).orderByAsc(IssueTask::getProjectName);
        List<IssueTask> tasks = taskMapper.selectList(tq);
        if (CollectionUtils.isEmpty(tasks)) {
            return new ArrayList<>();
        }

        // 按 分支×机型 分组（保持查询顺序）
        Map<String, List<IssueTask>> groupMap = new LinkedHashMap<>();
        for (IssueTask task : tasks) {
            String key = (task.getBranch() == null ? "" : task.getBranch())
                    + "@@" + (task.getProjectName() == null ? "" : task.getProjectName());
            groupMap.computeIfAbsent(key, k -> new ArrayList<>()).add(task);
        }

        // 查询明细（用例名过滤）
        Set<Long> taskIds = tasks.stream().map(IssueTask::getId).collect(Collectors.toSet());
        Set<Long> userIds = new HashSet<>();
        tasks.forEach(t -> {
            if (t.getAssigneeId() != null) {
                userIds.add(t.getAssigneeId());
            }
        });
        LambdaQueryWrapper<IssueTaskItem> iq = new LambdaQueryWrapper<IssueTaskItem>()
                .in(IssueTaskItem::getTaskId, taskIds);
        if (StringUtils.hasText(caseName)) {
            iq.like(IssueTaskItem::getItemName, caseName.trim());
        }
        iq.orderByAsc(IssueTaskItem::getCreateTime);
        List<IssueTaskItem> items = itemMapper.selectList(iq);
        if (CollectionUtils.isEmpty(items)) {
            return new ArrayList<>();
        }
        Map<Long, List<IssueTaskItem>> itemsByTask = new HashMap<>();
        for (IssueTaskItem item : items) {
            itemsByTask.computeIfAbsent(item.getTaskId(), k -> new ArrayList<>()).add(item);
        }
        userIds.addAll(items.stream().map(IssueTaskItem::getAssigneeId)
                .filter(Objects::nonNull).collect(Collectors.toSet()));

        Map<Long, String> userMap = userIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(SysUser::getId, u -> {
                            String name = u.getNickname();
                            return StringUtils.hasText(name) ? name : u.getUsername();
                        }));

        List<TaskFailGroupedVO> result = new ArrayList<>();
        for (Map.Entry<String, List<IssueTask>> entry : groupMap.entrySet()) {
            List<IssueTask> groupTasks = entry.getValue();
            IssueTask first = groupTasks.get(0);
            List<TaskFailCaseVO> caseVOs = new ArrayList<>();
            for (IssueTask task : groupTasks) {
                List<IssueTaskItem> taskItems = itemsByTask.get(task.getId());
                if (CollectionUtils.isEmpty(taskItems)) {
                    continue;
                }
                for (IssueTaskItem item : taskItems) {
                    TaskFailCaseVO vo = new TaskFailCaseVO();
                    vo.setId(item.getId());
                    vo.setTaskId(item.getTaskId());
                    vo.setCaseType(item.getItemType());
                    vo.setCaseTypeDesc(itemTypeDesc(item.getItemType()));
                    vo.setCaseName(item.getItemName());
                    vo.setCaseLog(item.getItemLog());
                    vo.setStatus(item.getStatus());
                    TaskItemStatus st = TaskItemStatus.of(item.getStatus());
                    vo.setStatusDesc(st == null ? null : st.getDesc());
                    vo.setAssigneeId(item.getAssigneeId());
                    vo.setAssigneeName(item.getAssigneeId() == null ? null : userMap.get(item.getAssigneeId()));
                    vo.setFailReason(item.getFailReason());
                    vo.setFixPlan(item.getFixPlan());
                    vo.setIsBug(item.getIsBug());
                    vo.setProgress(item.getProgress());
                    vo.setConclusion(item.getConclusion());
                    vo.setAiAnalysis(item.getAiAnalysis());
                    vo.setPublishTime(item.getCreateTime());
                    caseVOs.add(vo);
                }
            }
            if (caseVOs.isEmpty()) {
                continue;
            }
            TaskFailGroupedVO groupVO = new TaskFailGroupedVO();
            groupVO.setBranch(first.getBranch());
            groupVO.setProjectName(first.getProjectName());
            groupVO.setCases(caseVOs);
            groupVO.setTotalCount(caseVOs.size());
            groupVO.setFailedCount(caseVOs.size());
            // 预留栏位：issue_task 暂无成功用例统计与原始报告关联，后续补充自动解析功能后填充
            groupVO.setPassedCount(0);
            groupVO.setPassRate(0);
            groupVO.setReportFiles(Collections.emptyList());
            result.add(groupVO);
        }
        return result;
    }

    /* ==================== 权限校验 ==================== */

    protected long currentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    protected boolean isSuperAdmin() {
        return StpUtil.hasRole("super_admin");
    }

    /**
     * 操作权限：仅任务创建人、被指派人或超管可流转/处理/编辑
     */
    protected void checkOperatePermission(IssueTask task) {
        long current = currentUserId();
        if (isSuperAdmin()) {
            return;
        }
        boolean isCreator = task.getCreatorId() != null && task.getCreatorId().equals(current);
        boolean isAssignee = task.getAssigneeId() != null && task.getAssigneeId().equals(current);
        if (!isCreator && !isAssignee) {
            throw new BusinessException(ErrorCode.NO_PERMISSION, "仅任务创建人或被指派人可操作");
        }
    }

    /**
     * 管理权限：仅任务创建人或超管可删除/指派
     */
    protected void checkManagePermission(IssueTask task) {
        long current = currentUserId();
        if (isSuperAdmin()) {
            return;
        }
        boolean isCreator = task.getCreatorId() != null && task.getCreatorId().equals(current);
        if (!isCreator) {
            throw new BusinessException(ErrorCode.NO_PERMISSION, "仅任务创建人可执行该操作");
        }
    }
}
