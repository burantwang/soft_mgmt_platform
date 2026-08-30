package com.company.devplatform.module.release.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.company.devplatform.module.release.dto.FailCaseGroupedQuery;
import com.company.devplatform.module.release.dto.FailCaseHandleDTO;
import com.company.devplatform.module.release.dto.FailCaseUpdateDTO;
import com.company.devplatform.module.release.dto.FailTaskAssignDTO;
import com.company.devplatform.module.release.dto.FailTaskCreateDTO;
import com.company.devplatform.module.release.dto.FailTaskStatusDTO;
import com.company.devplatform.module.release.dto.FailTaskUpdateDTO;
import com.company.devplatform.module.release.entity.ReleaseFailCase;
import com.company.devplatform.module.release.entity.ReleaseFailTask;
import com.company.devplatform.module.release.entity.ReleaseRecord;
import com.company.devplatform.module.release.util.HtmlReportParser;
import com.company.devplatform.module.release.vo.FailCaseGroupedVO;
import com.company.devplatform.module.release.vo.FailTaskDetailVO;
import com.company.devplatform.module.release.vo.FailTaskVO;

import java.util.List;

/**
 * 失败聚合任务服务
 */
public interface ReleaseFailTaskService {

    /* ==================== 阶段2：自动创建 ==================== */

    /**
     * 为失败发布记录自动生成聚合任务与失败用例明细
     *
     * @param record    发布记录（result=失败）
     * @param failCases 报告解析出的失败/错误用例
     * @return 聚合任务
     */
    ReleaseFailTask createTaskForRecord(ReleaseRecord record, List<HtmlReportParser.FailCase> failCases);

    /**
     * 任务编号生成规则：FT + yyyyMMdd + 3位流水
     */
    String genTaskNo();

    /** 查询发布记录关联的失败任务 */
    ReleaseFailTask getByRecordId(Long recordId);

    /** 查询失败用例明细 */
    List<ReleaseFailCase> listCases(Long taskId);

    /* ==================== 阶段4：查询/CRUD/流转 ==================== */

    /**
     * 任务分页查询
     *
     * @param mine      是否仅查询当前用户的待办（assigneeId=当前用户且状态待处理/处理中）
     */
    IPage<FailTaskVO> pageTasks(int page, int size, Integer status, String keyword, Long assigneeId, boolean mine);

    /** 任务详情（含失败用例明细） */
    FailTaskDetailVO detail(Long id);

    /** 手动创建任务 */
    ReleaseFailTask create(FailTaskCreateDTO dto);

    /** 编辑任务（概述/原因/方案） */
    void update(FailTaskUpdateDTO dto);

    /** 删除任务（级联删除用例明细） */
    void delete(Long id);

    /** 指派任务责任人 */
    void assign(FailTaskAssignDTO dto);

    /** 任务状态流转（联动规则见实现） */
    void changeStatus(FailTaskStatusDTO dto);

    /** 处理失败用例明细（联动刷新任务状态） */
    void handleCase(FailCaseHandleDTO dto);

    /**
     * 按日期+分支×机型分组查询失败用例
     */
    List<FailCaseGroupedVO> listGroupedCases(FailCaseGroupedQuery query);

    /**
     * 最近7天 DailySanity 分析完成统计（含今天，共7天）
     */
    List<com.company.devplatform.module.release.vo.RecentDayStatVO> recentWeekStats();

    /**
     * 更新失败用例处理信息
     */
    void updateCase(Long caseId, FailCaseUpdateDTO dto);
}
