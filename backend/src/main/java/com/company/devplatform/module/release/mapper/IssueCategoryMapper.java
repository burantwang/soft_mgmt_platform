package com.company.devplatform.module.release.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.devplatform.module.release.entity.IssueCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 问题分类 Mapper
 */
@Mapper
public interface IssueCategoryMapper extends BaseMapper<IssueCategory> {
}
