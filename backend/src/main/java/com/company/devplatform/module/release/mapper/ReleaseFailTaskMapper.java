package com.company.devplatform.module.release.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.devplatform.module.release.entity.ReleaseFailTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 失败聚合任务 Mapper
 */
@Mapper
public interface ReleaseFailTaskMapper extends BaseMapper<ReleaseFailTask> {

    /**
     * 查询前缀下最大的任务编号（物理表查询，不过滤逻辑删除，
     * 用于避免已删除记录占用唯一索引导致编号冲突）
     */
    @Select("SELECT task_no FROM release_fail_task WHERE task_no LIKE CONCAT(#{prefix}, '%') ORDER BY task_no DESC LIMIT 1")
    String selectLatestTaskNo(@Param("prefix") String prefix);
}
