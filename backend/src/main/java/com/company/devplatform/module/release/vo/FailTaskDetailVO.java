package com.company.devplatform.module.release.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 失败聚合任务详情（含失败用例明细）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FailTaskDetailVO extends FailTaskVO {

    /** 失败用例明细 */
    private List<FailCaseVO> cases;
}
