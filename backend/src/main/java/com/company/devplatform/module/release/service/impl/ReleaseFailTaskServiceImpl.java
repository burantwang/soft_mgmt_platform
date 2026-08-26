package com.company.devplatform.module.release.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.devplatform.module.release.entity.ReleaseFailCase;
import com.company.devplatform.module.release.entity.ReleaseFailTask;
import com.company.devplatform.module.release.entity.ReleaseRecord;
import com.company.devplatform.module.release.enums.FailTaskStatus;
import com.company.devplatform.module.release.mapper.ReleaseFailCaseMapper;
import com.company.devplatform.module.release.mapper.ReleaseFailTaskMapper;
import com.company.devplatform.module.release.service.ReleaseFailTaskService;
import com.company.devplatform.module.release.util.HtmlReportParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 失败聚合任务服务实现（阶段2：自动生成；完整流转见阶段4）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReleaseFailTaskServiceImpl implements ReleaseFailTaskService {

    private final ReleaseFailTaskMapper taskMapper;
    private final ReleaseFailCaseMapper caseMapper;

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
                c.setCaseName(fc.getName());
                c.setCaseLog(fc.getLog());
                c.setStatus(FailTaskStatus.PENDING.getCode());
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

    private String buildSummary(ReleaseRecord record) {
        String branch = StringUtils.hasText(record.getBranch()) ? record.getBranch() : "未知分支";
        String version = StringUtils.hasText(record.getVersion()) ? record.getVersion() : "未知版本";
        return "发布 " + branch + (StringUtils.hasText(record.getVersion()) ? "@" + version : "")
                + " 存在 " + record.getFailedCount() + " 个失败用例";
    }
}
