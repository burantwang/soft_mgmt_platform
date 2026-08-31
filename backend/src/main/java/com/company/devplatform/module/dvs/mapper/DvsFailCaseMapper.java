package com.company.devplatform.module.dvs.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.devplatform.module.dvs.entity.DvsFailCase;
import org.apache.ibatis.annotations.Mapper;

/**
 * DVS 失败用例明细 Mapper
 */
@Mapper
public interface DvsFailCaseMapper extends BaseMapper<DvsFailCase> {
}
