package com.company.devplatform.module.portal.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 排序调整入参
 */
@Data
public class SortDTO {

    /** 排序项(id + sort) */
    @NotNull(message = "排序列表不能为空")
    private List<SortItem> items;

    @Data
    public static class SortItem {

        @NotNull(message = "数据ID不能为空")
        private Long id;

        @NotNull(message = "排序值不能为空")
        private Integer sort;
    }
}
