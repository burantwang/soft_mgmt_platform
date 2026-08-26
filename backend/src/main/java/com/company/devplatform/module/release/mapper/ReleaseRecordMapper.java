package com.company.devplatform.module.release.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.devplatform.module.release.entity.ReleaseRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 版本发布记录 Mapper
 */
@Mapper
public interface ReleaseRecordMapper extends BaseMapper<ReleaseRecord> {
}
