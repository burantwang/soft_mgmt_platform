package com.company.devplatform.module.release.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 失败用例分组查询参数
 */
@Data
public class FailCaseGroupedQuery {

    /** 查询日期（默认今天） */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;

    /** 分支筛选 */
    private String branch;

    /** 机型名称筛选 */
    private String projectName;

    /** 用例名称模糊搜索 */
    private String caseName;
}
