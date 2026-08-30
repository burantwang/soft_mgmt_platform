package com.company.devplatform.module.weekly.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.devplatform.module.weekly.entity.WeeklyFailCase;
import org.apache.ibatis.annotations.Mapper;

/**
 * 周度失败用例明细 Mapper
 */
@Mapper
public interface WeeklyFailCaseMapper extends BaseMapper<WeeklyFailCase> {
}
