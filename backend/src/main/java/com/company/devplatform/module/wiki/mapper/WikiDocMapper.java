package com.company.devplatform.module.wiki.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.devplatform.module.wiki.entity.WikiDoc;
import org.apache.ibatis.annotations.Mapper;

/**
 * Wiki 文档 Mapper
 */
@Mapper
public interface WikiDocMapper extends BaseMapper<WikiDoc> {
}
