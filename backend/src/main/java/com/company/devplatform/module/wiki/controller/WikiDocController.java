package com.company.devplatform.module.wiki.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.company.devplatform.common.Result;
import com.company.devplatform.module.wiki.dto.WikiDocDTO;
import com.company.devplatform.module.wiki.service.WikiDocService;
import com.company.devplatform.module.wiki.vo.WikiDocDetailVO;
import com.company.devplatform.module.wiki.vo.WikiDocNodeVO;
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
 * Wiki 文档接口（阶段5）
 */
@RestController
@RequestMapping("/api/wiki/docs")
@RequiredArgsConstructor
public class WikiDocController {

    private final WikiDocService wikiDocService;

    /** 目录树 */
    @GetMapping("/tree")
    @SaCheckPermission("wiki:view")
    public Result<List<WikiDocNodeVO>> tree() {
        return Result.ok(wikiDocService.tree());
    }

    /** 文档详情 */
    @GetMapping("/{id}")
    @SaCheckPermission("wiki:view")
    public Result<WikiDocDetailVO> detail(@PathVariable Long id) {
        return Result.ok(wikiDocService.detail(id));
    }

    /** 新建文档 */
    @PostMapping
    @SaCheckPermission("wiki:edit")
    public Result<Long> create(@Valid @RequestBody WikiDocDTO dto) {
        return Result.ok(wikiDocService.create(dto));
    }

    /** 更新文档 */
    @PutMapping("/{id}")
    @SaCheckPermission("wiki:edit")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody WikiDocDTO dto) {
        wikiDocService.update(id, dto);
        return Result.ok();
    }

    /** 删除文档（级联删除全部子孙文档） */
    @DeleteMapping("/{id}")
    @SaCheckPermission("wiki:edit")
    public Result<Void> delete(@PathVariable Long id) {
        wikiDocService.delete(id);
        return Result.ok();
    }
}
