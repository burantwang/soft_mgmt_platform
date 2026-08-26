package com.company.devplatform.module.release.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.devplatform.module.release.entity.ReleaseProject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 项目(机型) Mapper
 */
@Mapper
public interface ReleaseProjectMapper extends BaseMapper<ReleaseProject> {

    /**
     * 释放名称/编码唯一键（逻辑删除前调用，避免已删除记录占用唯一索引）
     */
    @Update("UPDATE project SET project_name = CONCAT(project_name, '_del_', id), project_code = CONCAT(project_code, '_del_', id) WHERE id = #{id}")
    int releaseUniqueKeys(@Param("id") Long id);
}
