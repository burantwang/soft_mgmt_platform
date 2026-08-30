package com.company.devplatform.module.weekly.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.devplatform.module.weekly.entity.WeeklyReport;
import org.apache.ibatis.annotations.Mapper;

/**
 * 周度测试报告 Mapper
 */
@Mapper
public interface WeeklyReportMapper extends BaseMapper<WeeklyReport> {
}
