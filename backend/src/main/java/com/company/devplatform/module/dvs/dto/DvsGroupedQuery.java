package com.company.devplatform.module.dvs.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * DVS 分组查询入参（按日期+分支×机型）
 */
@Data
public class DvsGroupedQuery {

    /** 统计日期（默认今天） */
    private LocalDate date;

    /** 分支关键字 */
    private String branch;

    /** 机型名称关键字 */
    private String projectName;

    /** 用例名称关键字 */
    private String caseName;
}
