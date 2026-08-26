package com.company.devplatform.module.release.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.devplatform.module.release.entity.ReleaseRecordProject;
import org.apache.ibatis.annotations.Mapper;

/**
 * 发布记录-机型关联 Mapper
 */
@Mapper
public interface ReleaseRecordProjectMapper extends BaseMapper<ReleaseRecordProject> {
}
