package com.company.devplatform.module.dvs.vo;

import lombok.Data;

import java.util.List;

/**
 * DVS 多文件报告解析预览结果
 * <p>一次上传可包含多个 HTML 模块，每个文件独立解析并单独入库。</p>
 */
@Data
public class DvsReportPreviewVO {

    /** 预览令牌（确认入库时回传） */
    private String previewToken;

    /** 各模块(HTML文件)解析结果 */
    private List<DvsPreviewItemVO> items;
}
