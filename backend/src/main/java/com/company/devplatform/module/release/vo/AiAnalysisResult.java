package com.company.devplatform.module.release.vo;

import lombok.Data;

/**
 * AI 分析结果（结构化：根因/佐证/修复建议）
 */
@Data
public class AiAnalysisResult {

    /** 分析失败根因 */
    private String rootCause;

    /** 分析佐证 */
    private String evidence;

    /** 修复建议 */
    private String solution;
}
