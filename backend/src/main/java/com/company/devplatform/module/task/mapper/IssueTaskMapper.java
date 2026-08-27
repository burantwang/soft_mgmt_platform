package com.company.devplatform.module.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.devplatform.module.task.entity.IssueTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface IssueTaskMapper extends BaseMapper<IssueTask> {

    /**
     * 查询指定前缀的最大任务编号（用于生成新编号）
     */
    @Select("SELECT task_no FROM issue_task WHERE task_no LIKE CONCAT(#{prefix}, '%') AND is_deleted = 0 ORDER BY task_no DESC LIMIT 1")
    String selectLatestTaskNo(@Param("prefix") String prefix);
}
