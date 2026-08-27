package com.company.devplatform.module.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.devplatform.module.task.entity.IssueTaskItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface IssueTaskItemMapper extends BaseMapper<IssueTaskItem> {

    /**
     * 统计某任务下各状态明细数量
     */
    @Select("SELECT status, COUNT(*) AS cnt FROM issue_task_item WHERE task_id = #{taskId} AND is_deleted = 0 GROUP BY status")
    List<Map<String, Object>> countByTaskId(@Param("taskId") Long taskId);
}
