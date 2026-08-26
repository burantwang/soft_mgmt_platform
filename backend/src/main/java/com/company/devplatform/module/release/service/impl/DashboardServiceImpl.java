package com.company.devplatform.module.release.service.impl;

import com.company.devplatform.module.release.entity.ReleaseProject;
import com.company.devplatform.module.release.entity.ReleaseRecord;
import com.company.devplatform.module.release.entity.ReleaseRecordProject;
import com.company.devplatform.module.release.enums.ReleaseResult;
import com.company.devplatform.module.release.enums.ReleaseSource;
import com.company.devplatform.module.release.mapper.ReleaseProjectMapper;
import com.company.devplatform.module.release.mapper.ReleaseRecordMapper;
import com.company.devplatform.module.release.mapper.ReleaseRecordProjectMapper;
import com.company.devplatform.module.release.service.DashboardService;
import com.company.devplatform.module.release.vo.DashboardBranchStatVO;
import com.company.devplatform.module.release.vo.DashboardDayStatVO;
import com.company.devplatform.module.release.vo.DashboardOverviewVO;
import com.company.devplatform.module.release.vo.DashboardProjectStatVO;
import com.company.devplatform.module.release.vo.DashboardSummaryVO;
import com.company.devplatform.module.release.vo.DashboardTrendPointVO;
import com.company.devplatform.module.release.vo.ReleaseRecordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 发布统计看板服务实现（数据量小，内存聚合）
 */
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ReleaseRecordMapper recordMapper;
    private final ReleaseRecordProjectMapper recordProjectMapper;
    private final ReleaseProjectMapper projectMapper;

    @Override
    public DashboardSummaryVO summary(LocalDate date) {
        LocalDate targetDate = date == null ? LocalDate.now() : date;
        List<ReleaseRecord> records = recordMapper.selectList(null);
        List<ReleaseRecordProject> links = recordProjectMapper.selectList(null);
        List<ReleaseProject> projects = projectMapper.selectList(null);

        DashboardSummaryVO vo = new DashboardSummaryVO();
        vo.setOverview(buildOverview(records, links, projects));
        vo.setDayDate(targetDate);
        List<ReleaseRecord> dayRecords = records.stream()
                .filter(r -> r.getPublishTime() != null
                        && r.getPublishTime().toLocalDate().isEqual(targetDate))
                .sorted(Comparator.comparing(ReleaseRecord::getPublishTime).reversed())
                .collect(Collectors.toList());
        vo.setDayOverview(buildOverview(dayRecords, links, projects));
        vo.setDayRecords(buildDayRecords(dayRecords, links, projects));
        vo.setDayStats(buildDayStats(dayRecords, links, projects));
        vo.setBranchStats(buildBranchStats(records));
        vo.setProjectStats(buildProjectStats(records, links, projects));
        vo.setTrend(buildTrend(records));
        return vo;
    }

    /* ==================== 私有 ==================== */

    private DashboardOverviewVO buildOverview(List<ReleaseRecord> records,
                                              List<ReleaseRecordProject> links,
                                              List<ReleaseProject> projects) {
        DashboardOverviewVO vo = new DashboardOverviewVO();
        int total = records.size();
        int success = countSuccess(records);
        int failed = total - success;
        vo.setTotalRecords(total);
        vo.setSuccessCount(success);
        vo.setFailedCount(failed);
        vo.setSuccessRate(rate(success, total));

        int totalCases = records.stream().mapToInt(r -> nvl(r.getTotalCount())).sum();
        int passedCases = records.stream().mapToInt(r -> nvl(r.getPassedCount())).sum();
        vo.setTotalCases(totalCases);
        vo.setPassedCases(passedCases);
        vo.setOverallPassRate(rate(passedCases, totalCases));

        Set<Long> recordIds = records.stream().map(ReleaseRecord::getId).collect(Collectors.toSet());
        long distinctProjects = links.stream()
                .filter(l -> recordIds.contains(l.getRecordId()))
                .map(ReleaseRecordProject::getProjectId).distinct().count();
        vo.setProjectCount((int) distinctProjects);
        return vo;
    }

    private List<ReleaseRecordVO> buildDayRecords(List<ReleaseRecord> dayRecords,
                                                  List<ReleaseRecordProject> links,
                                                  List<ReleaseProject> projects) {
        Map<Long, ReleaseProject> projectMap = projects.stream()
                .collect(Collectors.toMap(ReleaseProject::getId, p -> p));
        Map<Long, List<ReleaseProject>> projectsByRecord = new LinkedHashMap<>();
        for (ReleaseRecordProject link : links) {
            ReleaseProject project = projectMap.get(link.getProjectId());
            if (project == null) {
                continue;
            }
            projectsByRecord.computeIfAbsent(link.getRecordId(), k -> new ArrayList<>()).add(project);
        }

        List<ReleaseRecordVO> result = new ArrayList<>();
        for (ReleaseRecord r : dayRecords) {
            ReleaseRecordVO vo = new ReleaseRecordVO();
            vo.setId(r.getId());
            vo.setBranch(r.getBranch());
            vo.setVersion(r.getVersion());
            vo.setResult(r.getResult());
            ReleaseResult rr = ReleaseResult.of(r.getResult());
            vo.setResultDesc(rr == null ? "-" : rr.getDesc());
            vo.setSource(r.getSource());
            ReleaseSource rs = ReleaseSource.of(r.getSource());
            vo.setSourceDesc(rs == null ? "-" : rs.getDesc());
            vo.setTotalCount(nvl(r.getTotalCount()));
            vo.setPassedCount(nvl(r.getPassedCount()));
            vo.setFailedCount(nvl(r.getFailedCount()));
            vo.setErrorCount(nvl(r.getErrorCount()));
            vo.setSkippedCount(nvl(r.getSkippedCount()));
            vo.setPassRate(rate(vo.getPassedCount(), vo.getTotalCount()));
            vo.setPublishTime(r.getPublishTime());
            List<ReleaseProject> ps = projectsByRecord.get(r.getId());
            if (ps != null) {
                vo.setProjectIds(ps.stream().map(ReleaseProject::getId).collect(Collectors.toList()));
                vo.setProjectNames(ps.stream().map(ReleaseProject::getProjectName).collect(Collectors.toList()));
            }
            result.add(vo);
        }
        return result;
    }

    private List<DashboardDayStatVO> buildDayStats(List<ReleaseRecord> dayRecords,
                                                   List<ReleaseRecordProject> links,
                                                   List<ReleaseProject> projects) {
        Map<Long, ReleaseProject> projectMap = projects.stream()
                .collect(Collectors.toMap(ReleaseProject::getId, p -> p));
        Map<Long, List<ReleaseProject>> projectsByRecord = new LinkedHashMap<>();
        for (ReleaseRecordProject link : links) {
            ReleaseProject project = projectMap.get(link.getProjectId());
            if (project == null) {
                continue;
            }
            projectsByRecord.computeIfAbsent(link.getRecordId(), k -> new ArrayList<>()).add(project);
        }

        class Agg {
            int total;
            int passed;
            ReleaseRecord latest;
        }

        Map<String, Agg> aggMap = new LinkedHashMap<>();
        for (ReleaseRecord r : dayRecords) {
            List<ReleaseProject> ps = projectsByRecord.get(r.getId());
            if (ps == null || ps.isEmpty()) {
                // 无关联机型：按“未知机型”兜底展示
                ps = Collections.singletonList(new ReleaseProject());
            }
            for (ReleaseProject p : ps) {
                String key = r.getBranch() + "#" + (p.getProjectName() == null ? "" : p.getProjectName());
                Agg agg = aggMap.computeIfAbsent(key, k -> new Agg());
                agg.total += nvl(r.getTotalCount());
                agg.passed += nvl(r.getPassedCount());
                if (agg.latest == null || r.getPublishTime().isAfter(agg.latest.getPublishTime())) {
                    agg.latest = r;
                }
            }
        }

        List<DashboardDayStatVO> result = new ArrayList<>();
        for (Map.Entry<String, Agg> entry : aggMap.entrySet()) {
            String[] parts = entry.getKey().split("#", 2);
            Agg agg = entry.getValue();
            DashboardDayStatVO vo = new DashboardDayStatVO();
            vo.setBranch(parts[0]);
            vo.setProjectName(parts[1]);
            vo.setTotalCount(agg.total);
            vo.setPassedCount(agg.passed);
            vo.setFailedCount(Math.max(agg.total - agg.passed, 0));
            vo.setPassRate(rate(agg.passed, agg.total));
            boolean allPassed = agg.total > 0 && agg.passed == agg.total;
            vo.setResult(allPassed ? 1 : 2);
            vo.setResultDesc(allPassed ? "已发布" : "不可发布");
            ReleaseRecord latest = agg.latest;
            if (latest != null) {
                vo.setVersion(latest.getVersion());
                vo.setImageUrl(latest.getImageUrl());
                vo.setPublishTime(latest.getPublishTime());
                vo.setSource(latest.getSource());
                ReleaseSource rs = ReleaseSource.of(latest.getSource());
                vo.setSourceDesc(rs == null ? "-" : rs.getDesc());
            }
            result.add(vo);
        }
        return result;
    }

    private List<DashboardBranchStatVO> buildBranchStats(List<ReleaseRecord> records) {
        Map<String, List<ReleaseRecord>> byBranch = records.stream()
                .filter(r -> r.getBranch() != null)
                .collect(Collectors.groupingBy(ReleaseRecord::getBranch, LinkedHashMap::new, Collectors.toList()));

        List<DashboardBranchStatVO> result = new ArrayList<>();
        for (Map.Entry<String, List<ReleaseRecord>> e : byBranch.entrySet()) {
            DashboardBranchStatVO vo = new DashboardBranchStatVO();
            int success = countSuccess(e.getValue());
            vo.setBranch(e.getKey());
            vo.setTotal(e.getValue().size());
            vo.setSuccess(success);
            vo.setFailed(e.getValue().size() - success);
            vo.setSuccessRate(rate(success, e.getValue().size()));
            result.add(vo);
        }
        return result;
    }

    private List<DashboardProjectStatVO> buildProjectStats(List<ReleaseRecord> records,
                                                           List<ReleaseRecordProject> links,
                                                           List<ReleaseProject> projects) {
        Map<Long, Integer> recordResult = records.stream()
                .collect(Collectors.toMap(ReleaseRecord::getId, r -> r.getResult() == null ? 0 : r.getResult()));
        Map<Long, ReleaseProject> projectMap = projects.stream()
                .collect(Collectors.toMap(ReleaseProject::getId, p -> p));

        // 按机型聚合：一条记录覆盖多个机型，每个机型各计一次
        Map<Long, List<Long>> recordIdsByProject = new LinkedHashMap<>();
        for (ReleaseRecordProject link : links) {
            recordIdsByProject.computeIfAbsent(link.getProjectId(), k -> new ArrayList<>()).add(link.getRecordId());
        }

        List<DashboardProjectStatVO> result = new ArrayList<>();
        for (Map.Entry<Long, List<Long>> e : recordIdsByProject.entrySet()) {
            ReleaseProject project = projectMap.get(e.getKey());
            if (project == null) {
                continue;
            }
            List<Long> ids = e.getValue().stream().distinct().collect(Collectors.toList());
            int success = (int) ids.stream()
                    .filter(id -> recordResult.getOrDefault(id, 0) == ReleaseResult.SUCCESS.getCode()).count();
            DashboardProjectStatVO vo = new DashboardProjectStatVO();
            vo.setProjectId(project.getId());
            vo.setProjectName(project.getProjectName());
            vo.setTotal(ids.size());
            vo.setSuccess(success);
            vo.setFailed(ids.size() - success);
            vo.setSuccessRate(rate(success, ids.size()));
            result.add(vo);
        }
        return result;
    }

    private List<DashboardTrendPointVO> buildTrend(List<ReleaseRecord> records) {
        LocalDate today = LocalDate.now();
        Map<LocalDate, int[]> stat = new TreeMap<>();
        for (int i = 14; i >= 0; i--) {
            stat.put(today.minusDays(i), new int[3]);
        }
        for (ReleaseRecord r : records) {
            if (r.getPublishTime() == null) {
                continue;
            }
            int[] arr = stat.get(r.getPublishTime().toLocalDate());
            if (arr == null) {
                continue;
            }
            arr[0]++;
            if (r.getResult() != null && r.getResult() == ReleaseResult.SUCCESS.getCode()) {
                arr[1]++;
            } else {
                arr[2]++;
            }
        }
        List<DashboardTrendPointVO> result = new ArrayList<>();
        for (Map.Entry<LocalDate, int[]> e : stat.entrySet()) {
            DashboardTrendPointVO vo = new DashboardTrendPointVO();
            vo.setDate(e.getKey().format(DAY));
            vo.setTotal(e.getValue()[0]);
            vo.setSuccess(e.getValue()[1]);
            vo.setFailed(e.getValue()[2]);
            result.add(vo);
        }
        return result;
    }

    private int countSuccess(List<ReleaseRecord> records) {
        return (int) records.stream()
                .filter(r -> r.getResult() != null && r.getResult() == ReleaseResult.SUCCESS.getCode()).count();
    }

    private BigDecimal rate(int part, int total) {
        if (total <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(part)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    private int nvl(Integer v) {
        return v == null ? 0 : v;
    }
}
