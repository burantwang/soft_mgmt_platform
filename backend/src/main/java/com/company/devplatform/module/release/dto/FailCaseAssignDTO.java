package com.company.devplatform.module.release.dto;

import lombok.Data;

/**
 * 快速指派失败用例责任人请求（id 由路径参数提供）
 */
@Data
public class FailCaseAssignDTO {

    /** 责任人（用户ID），null 表示取消指派 */
    private Long assigneeId;
}
