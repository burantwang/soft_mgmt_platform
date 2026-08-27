package com.company.devplatform.module.task.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.company.devplatform.module.task.dto.TaskTypeDTO;
import com.company.devplatform.module.task.vo.TaskTypeVO;

import java.util.List;

/**
 * 任务类型服务
 */
public interface TaskTypeService {

    /** 启用的类型列表（菜单/下拉用） */
    List<TaskTypeVO> listEnabled();

    /** 管理分页（含停用） */
    IPage<TaskTypeVO> pageAll(int page, int size, String keyword);

    TaskTypeVO create(TaskTypeDTO dto);

    void update(TaskTypeDTO dto);

    void delete(Long id);

    /** 启停用 */
    void toggleEnabled(Long id);
}
