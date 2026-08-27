package com.company.devplatform.module.home.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.devplatform.module.home.service.HomeService;
import com.company.devplatform.module.home.vo.HomeSummaryVO;
import com.company.devplatform.module.home.vo.RecentItemVO;
import com.company.devplatform.module.release.entity.FileResource;
import com.company.devplatform.module.release.entity.ReleaseRecord;
import com.company.devplatform.module.release.mapper.FileResourceMapper;
import com.company.devplatform.module.release.mapper.ReleaseRecordMapper;
import com.company.devplatform.module.task.entity.IssueTask;
import com.company.devplatform.module.task.entity.TaskType;
import com.company.devplatform.module.task.enums.TaskStatus;
import com.company.devplatform.module.task.mapper.IssueTaskMapper;
import com.company.devplatform.module.task.mapper.TaskTypeMapper;
import com.company.devplatform.module.wiki.entity.WikiDoc;
import com.company.devplatform.module.wiki.mapper.WikiDocMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Home 总览聚合实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HomeServiceImpl implements HomeService {

    private static final int RECENT_LIMIT = 5;

    private final ReleaseRecordMapper releaseRecordMapper;
    private final IssueTaskMapper issueTaskMapper;
    private final TaskTypeMapper taskTypeMapper;
    private final WikiDocMapper wikiDocMapper;
    private final FileResourceMapper fileResourceMapper;

    @Override
    public HomeSummaryVO summary() {
        HomeSummaryVO vo = new HomeSummaryVO();

        // ---- 发布统计 ----
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        vo.setReleaseTotal(releaseRecordMapper.selectCount(null));
        vo.setReleaseToday(releaseRecordMapper.selectCount(new LambdaQueryWrapper<ReleaseRecord>()
                .ge(ReleaseRecord::getPublishTime, todayStart)));
        vo.setReleaseSuccess(releaseRecordMapper.selectCount(new LambdaQueryWrapper<ReleaseRecord>()
                .eq(ReleaseRecord::getResult, 1)));
        vo.setReleaseFailed(releaseRecordMapper.selectCount(new LambdaQueryWrapper<ReleaseRecord>()
                .eq(ReleaseRecord::getResult, 2)));

        // ---- 任务追踪统计 ----
        vo.setTaskTotal(issueTaskMapper.selectCount(null));
        vo.setTaskPending(issueTaskMapper.selectCount(new LambdaQueryWrapper<IssueTask>()
                .eq(IssueTask::getStatus, TaskStatus.PENDING.getCode())));
        vo.setTaskProcessing(issueTaskMapper.selectCount(new LambdaQueryWrapper<IssueTask>()
                .eq(IssueTask::getStatus, TaskStatus.PROCESSING.getCode())));
        vo.setMyTodo(issueTaskMapper.selectCount(new LambdaQueryWrapper<IssueTask>()
                .eq(IssueTask::getAssigneeId, StpUtil.getLoginIdAsLong())
                .in(IssueTask::getStatus, TaskStatus.PENDING.getCode(), TaskStatus.PROCESSING.getCode())));

        // ---- Wiki / 文件 ----
        vo.setWikiDocCount(wikiDocMapper.selectCount(new LambdaQueryWrapper<WikiDoc>()
                .eq(WikiDoc::getParentId, 0)));
        vo.setFileCount(fileResourceMapper.selectCount(null));

        // ---- 最新动态 ----
        vo.setRecentReleases(recentReleases());
        vo.setRecentTasks(recentTasks());
        vo.setRecentWikis(recentWikis());
        vo.setRecentFiles(recentFiles());
        return vo;
    }

    private List<RecentItemVO> recentReleases() {
        List<ReleaseRecord> records = releaseRecordMapper.selectList(new LambdaQueryWrapper<ReleaseRecord>()
                .orderByDesc(ReleaseRecord::getPublishTime)
                .last("limit " + RECENT_LIMIT));
        return records.stream().map(r -> {
            RecentItemVO item = new RecentItemVO();
            item.setId(r.getId());
            String branch = StringUtils.hasText(r.getBranch()) ? r.getBranch() : "未知分支";
            String version = StringUtils.hasText(r.getVersion()) ? r.getVersion() : "";
            item.setTitle(branch + (StringUtils.hasText(version) ? " @" + version : ""));
            item.setSubtitle(r.getResult() != null && r.getResult() == 2 ? "发布失败" : "发布成功");
            item.setTime(r.getPublishTime());
            item.setPath("/release");
            return item;
        }).collect(Collectors.toList());
    }

    private List<RecentItemVO> recentTasks() {
        List<IssueTask> tasks = issueTaskMapper.selectList(new LambdaQueryWrapper<IssueTask>()
                .orderByDesc(IssueTask::getCreateTime)
                .last("limit " + RECENT_LIMIT));
        return tasks.stream().map(t -> {
            RecentItemVO item = new RecentItemVO();
            item.setId(t.getId());
            TaskType type = t.getTaskTypeId() == null ? null : taskTypeMapper.selectById(t.getTaskTypeId());
            item.setTitle((type == null ? "任务" : type.getName()) + "：" + t.getTitle());
            item.setSubtitle(t.getTaskNo());
            item.setTime(t.getCreateTime());
            item.setPath("/task");
            return item;
        }).collect(Collectors.toList());
    }

    private List<RecentItemVO> recentWikis() {
        List<WikiDoc> docs = wikiDocMapper.selectList(new LambdaQueryWrapper<WikiDoc>()
                .orderByDesc(WikiDoc::getUpdateTime)
                .last("limit " + RECENT_LIMIT));
        return docs.stream().map(d -> {
            RecentItemVO item = new RecentItemVO();
            item.setId(d.getId());
            item.setTitle(d.getTitle());
            item.setSubtitle("知识库文档");
            item.setTime(d.getUpdateTime());
            item.setPath("/wiki");
            return item;
        }).collect(Collectors.toList());
    }

    private List<RecentItemVO> recentFiles() {
        List<FileResource> files = fileResourceMapper.selectList(new LambdaQueryWrapper<FileResource>()
                .orderByDesc(FileResource::getCreateTime)
                .last("limit " + RECENT_LIMIT));
        return files.stream().map(f -> {
            RecentItemVO item = new RecentItemVO();
            item.setId(f.getId());
            item.setTitle(f.getFileName());
            item.setSubtitle(f.getFileType() != null && f.getFileType() == 1 ? "测试报告" : "文件资源");
            item.setTime(f.getCreateTime());
            item.setPath("/files");
            return item;
        }).collect(Collectors.toList());
    }
}
