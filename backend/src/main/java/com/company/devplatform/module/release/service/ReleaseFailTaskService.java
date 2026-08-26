package com.company.devplatform.module.release.service;

import com.company.devplatform.module.release.entity.ReleaseFailCase;
import com.company.devplatform.module.release.entity.ReleaseFailTask;
import com.company.devplatform.module.release.entity.ReleaseRecord;
import com.company.devplatform.module.release.util.HtmlReportParser;

import java.util.List;

/**
 * 失败聚合任务服务
 */
public interface ReleaseFailTaskService {

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
}
