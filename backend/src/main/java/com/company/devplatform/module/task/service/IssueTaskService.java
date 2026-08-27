package com.company.devplatform.module.task.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.company.devplatform.module.task.dto.IssueItemDTO;
import com.company.devplatform.module.task.dto.IssueTaskAssignDTO;
import com.company.devplatform.module.task.dto.IssueTaskCreateDTO;
import com.company.devplatform.module.task.dto.IssueTaskStatusDTO;
import com.company.devplatform.module.task.dto.IssueTaskUpdateDTO;
import com.company.devplatform.module.task.vo.IssueTaskDetailVO;
import com.company.devplatform.module.task.vo.IssueTaskVO;
import com.company.devplatform.module.task.vo.TaskFailGroupedVO;
import com.company.devplatform.module.task.vo.TaskTypeCountVO;

import java.util.List;

/**
 * 问题单任务服务（通用任务追踪）
 */
public interface IssueTaskService {

    /** 分页查询（按类型/状态/关键字/责任人；mine=true 时查我的待办） */
    IPage<IssueTaskVO> pageTasks(int page, int size, Long taskTypeId, Integer status,
                                 String keyword, Long assigneeId, boolean mine);

    /** 详情（含明细） */
    IssueTaskDetailVO detail(Long id);

    /** 创建（可带明细） */
    IssueTaskVO create(IssueTaskCreateDTO dto);

    /** 编辑基本信息 */
    void update(IssueTaskUpdateDTO dto);

    /** 删除（级联明细） */
    void delete(Long id);

    /** 指派责任人（待处理明细继承） */
    void assign(Long id, IssueTaskAssignDTO dto);

    /** 状态流转 */
    void changeStatus(Long id, IssueTaskStatusDTO dto);

    /** 处理明细 */
    void handleItem(IssueItemDTO dto);

    /** 各类型任务统计（列表页顶部/Home 用） */
    List<TaskTypeCountVO> overview();

    /**
     * DailySanity 失败任务追踪：按日期/分支/机型/用例名分组查询
     * 分组维度为 分支×机型，明细来自 issue_task_item
     */
    List<TaskFailGroupedVO> groupedFailCases(String date, String branch, String projectName, String caseName);
}
