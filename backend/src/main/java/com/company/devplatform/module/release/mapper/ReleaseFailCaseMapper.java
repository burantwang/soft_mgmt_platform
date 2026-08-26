package com.company.devplatform.module.release.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.devplatform.module.release.entity.ReleaseFailCase;
import org.apache.ibatis.annotations.Mapper;

/**
 * 失败用例明细 Mapper
 */
@Mapper
public interface ReleaseFailCaseMapper extends BaseMapper<ReleaseFailCase> {
}
