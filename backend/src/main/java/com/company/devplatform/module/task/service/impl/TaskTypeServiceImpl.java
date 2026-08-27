package com.company.devplatform.module.task.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.devplatform.common.ErrorCode;
import com.company.devplatform.common.exception.BusinessException;
import com.company.devplatform.module.task.dto.TaskTypeDTO;
import com.company.devplatform.module.task.entity.IssueTask;
import com.company.devplatform.module.task.entity.TaskType;
import com.company.devplatform.module.task.mapper.IssueTaskMapper;
import com.company.devplatform.module.task.mapper.TaskTypeMapper;
import com.company.devplatform.module.task.service.TaskTypeService;
import com.company.devplatform.module.task.vo.TaskTypeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 任务类型服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskTypeServiceImpl implements TaskTypeService {

    private final TaskTypeMapper taskTypeMapper;
    private final IssueTaskMapper issueTaskMapper;

    @Override
    public List<TaskTypeVO> listEnabled() {
        return taskTypeMapper.selectList(new LambdaQueryWrapper<TaskType>()
                        .eq(TaskType::getEnabled, 1)
                        .orderByAsc(TaskType::getSort)
                        .orderByAsc(TaskType::getId))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public IPage<TaskTypeVO> pageAll(int page, int size, String keyword) {
        LambdaQueryWrapper<TaskType> wrapper = new LambdaQueryWrapper<TaskType>()
                .like(StringUtils.hasText(keyword), TaskType::getName, keyword)
                .orderByAsc(TaskType::getSort)
                .orderByAsc(TaskType::getId);
        IPage<TaskType> p = taskTypeMapper.selectPage(new Page<>(page, size), wrapper);
        IPage<TaskTypeVO> result = new Page<>(p.getCurrent(), p.getSize(), p.getTotal());
        result.setRecords(p.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return result;
    }

    @Override
    public TaskTypeVO create(TaskTypeDTO dto) {
        checkCodeUnique(dto.getCode(), null);
        TaskType type = new TaskType();
        BeanUtils.copyProperties(dto, type);
        taskTypeMapper.insert(type);
        log.info("[任务类型] 新增 id={} code={} name={}", type.getId(), type.getCode(), type.getName());
        return toVO(type);
    }

    @Override
    public void update(TaskTypeDTO dto) {
        TaskType type = getOrThrow(dto.getId());
        checkCodeUnique(dto.getCode(), type.getId());
        type.setCode(dto.getCode());
        type.setName(dto.getName());
        type.setIcon(dto.getIcon());
        type.setSort(dto.getSort() == null ? 0 : dto.getSort());
        type.setEnabled(dto.getEnabled());
        type.setRemark(dto.getRemark());
        taskTypeMapper.updateById(type);
        log.info("[任务类型] 更新 id={} code={}", type.getId(), type.getCode());
    }

    @Override
    public void delete(Long id) {
        TaskType type = getOrThrow(id);
        Long taskCount = issueTaskMapper.selectCount(new LambdaQueryWrapper<IssueTask>()
                .eq(IssueTask::getTaskTypeId, id));
        if (taskCount != null && taskCount > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该类型下已有任务，不能删除，可改为停用");
        }
        taskTypeMapper.deleteById(id);
        log.info("[任务类型] 删除 id={} code={}", type.getId(), type.getCode());
    }

    @Override
    public void toggleEnabled(Long id) {
        TaskType type = getOrThrow(id);
        type.setEnabled(type.getEnabled() != null && type.getEnabled() == 1 ? 0 : 1);
        taskTypeMapper.updateById(type);
        log.info("[任务类型] 启停用 id={} enabled={}", type.getId(), type.getEnabled());
    }

    private void checkCodeUnique(String code, Long excludeId) {
        if (!StringUtils.hasText(code)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "类型编码不能为空");
        }
        List<TaskType> list = taskTypeMapper.selectList(new LambdaQueryWrapper<TaskType>()
                .eq(TaskType::getCode, code.trim()));
        boolean exists = list.stream().anyMatch(t -> !t.getId().equals(excludeId));
        if (exists) {
            throw new BusinessException(ErrorCode.DATA_EXIST, "类型编码已存在");
        }
    }

    private TaskType getOrThrow(Long id) {
        TaskType type = id == null ? null : taskTypeMapper.selectById(id);
        if (type == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "任务类型不存在");
        }
        return type;
    }

    private TaskTypeVO toVO(TaskType type) {
        TaskTypeVO vo = new TaskTypeVO();
        BeanUtils.copyProperties(type, vo);
        return vo;
    }
}
