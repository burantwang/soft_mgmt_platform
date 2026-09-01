package com.company.devplatform.module.portal.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.company.devplatform.common.Result;
import com.company.devplatform.module.portal.dto.CategoryDTO;
import com.company.devplatform.module.portal.dto.LinkDTO;
import com.company.devplatform.module.portal.dto.SortDTO;
import com.company.devplatform.module.portal.dto.StatusDTO;
import com.company.devplatform.module.portal.service.PortalService;
import com.company.devplatform.module.portal.vo.PortalCategoryVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 系统门户接口
 * <p>展示：GET /overview；管理：板块/链接的增删改、启停、排序</p>
 */
@RestController
@RequestMapping("/api/portal")
@RequiredArgsConstructor
public class PortalController {

    private final PortalService portalService;

    /** 门户展示数据（仅启用板块与链接，按 sort 排序） */
    @GetMapping("/overview")
    @SaCheckPermission("portal:view")
    public Result<List<PortalCategoryVO>> overview() {
        return Result.ok(portalService.overview());
    }

    /** 管理列表（全部板块及全部链接，含停用） */
    @GetMapping("/categories")
    @SaCheckPermission("portal:view")
    public Result<List<PortalCategoryVO>> listCategories() {
        return Result.ok(portalService.listCategories());
    }

    /** 新增板块 */
    @PostMapping("/categories")
    @SaCheckPermission("portal:edit")
    public Result<Long> createCategory(@Valid @RequestBody CategoryDTO dto) {
        return Result.ok(portalService.createCategory(dto));
    }

    /** 更新板块 */
    @PutMapping("/categories/{id}")
    @SaCheckPermission("portal:edit")
    public Result<Void> updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryDTO dto) {
        portalService.updateCategory(id, dto);
        return Result.ok();
    }

    /** 删除板块（板块下存在链接时拒绝） */
    @DeleteMapping("/categories/{id}")
    @SaCheckPermission("portal:edit")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        portalService.deleteCategory(id);
        return Result.ok();
    }

    /** 板块启停 */
    @PutMapping("/categories/status")
    @SaCheckPermission("portal:edit")
    public Result<Void> toggleCategoryStatus(@Valid @RequestBody StatusDTO dto) {
        portalService.toggleCategoryStatus(dto);
        return Result.ok();
    }

    /** 板块排序调整 */
    @PutMapping("/categories/sort")
    @SaCheckPermission("portal:edit")
    public Result<Void> sortCategories(@Valid @RequestBody SortDTO dto) {
        portalService.sortCategories(dto);
        return Result.ok();
    }

    /** 新增链接 */
    @PostMapping("/links")
    @SaCheckPermission("portal:edit")
    public Result<Long> createLink(@Valid @RequestBody LinkDTO dto) {
        return Result.ok(portalService.createLink(dto));
    }

    /** 更新链接 */
    @PutMapping("/links/{id}")
    @SaCheckPermission("portal:edit")
    public Result<Void> updateLink(@PathVariable Long id, @Valid @RequestBody LinkDTO dto) {
        portalService.updateLink(id, dto);
        return Result.ok();
    }

    /** 删除链接 */
    @DeleteMapping("/links/{id}")
    @SaCheckPermission("portal:edit")
    public Result<Void> deleteLink(@PathVariable Long id) {
        portalService.deleteLink(id);
        return Result.ok();
    }

    /** 链接启停 */
    @PutMapping("/links/status")
    @SaCheckPermission("portal:edit")
    public Result<Void> toggleLinkStatus(@Valid @RequestBody StatusDTO dto) {
        portalService.toggleLinkStatus(dto);
        return Result.ok();
    }

    /** 链接排序调整 */
    @PutMapping("/links/sort")
    @SaCheckPermission("portal:edit")
    public Result<Void> sortLinks(@Valid @RequestBody SortDTO dto) {
        portalService.sortLinks(dto);
        return Result.ok();
    }
}
