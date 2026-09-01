package com.company.devplatform.module.portal.service;

import com.company.devplatform.module.portal.dto.CategoryDTO;
import com.company.devplatform.module.portal.dto.LinkDTO;
import com.company.devplatform.module.portal.dto.SortDTO;
import com.company.devplatform.module.portal.dto.StatusDTO;
import com.company.devplatform.module.portal.vo.PortalCategoryVO;

import java.util.List;

/**
 * 系统门户服务
 */
public interface PortalService {

    /** 门户展示数据：仅启用板块 + 启用链接，按 sort 排序 */
    List<PortalCategoryVO> overview();

    /** 管理列表：全部板块（含停用）及全部链接，用于管理页面 */
    List<PortalCategoryVO> listCategories();

    /** 新增板块,返回板块ID */
    Long createCategory(CategoryDTO dto);

    /** 更新板块 */
    void updateCategory(Long id, CategoryDTO dto);

    /** 删除板块（板块下存在链接时拒绝） */
    void deleteCategory(Long id);

    /** 启停板块 */
    void toggleCategoryStatus(StatusDTO dto);

    /** 板块排序调整(批量写入 sort) */
    void sortCategories(SortDTO dto);

    /** 新增链接,返回链接ID */
    Long createLink(LinkDTO dto);

    /** 更新链接 */
    void updateLink(Long id, LinkDTO dto);

    /** 删除链接 */
    void deleteLink(Long id);

    /** 启停链接 */
    void toggleLinkStatus(StatusDTO dto);

    /** 链接排序调整(批量写入 sort) */
    void sortLinks(SortDTO dto);
}
