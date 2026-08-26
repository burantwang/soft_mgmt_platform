package com.company.devplatform.module.release.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.company.devplatform.common.Result;
import com.company.devplatform.module.release.dto.ReleaseRecordCreateDTO;
import com.company.devplatform.module.release.dto.ReleaseRecordUpdateDTO;
import com.company.devplatform.module.release.service.ReleaseRecordService;
import com.company.devplatform.module.release.vo.ReleaseRecordVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 版本发布记录管理
 */
@RestController
@RequestMapping("/api/release/records")
@RequiredArgsConstructor
public class ReleaseRecordController {

    private final ReleaseRecordService recordService;

    /** 分页查询（sonic:view） */
    @GetMapping
    @SaCheckPermission("sonic:view")
    public Result<IPage<ReleaseRecordVO>> page(@RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "10") int size,
                                               @RequestParam(required = false) String branch,
                                               @RequestParam(required = false) Integer result,
                                               @RequestParam(required = false) Integer source) {
        return Result.ok(recordService.page(page, size, branch, result, source));
    }

    /** 详情（sonic:view） */
    @GetMapping("/{id}")
    @SaCheckPermission("sonic:view")
    public Result<ReleaseRecordVO> detail(@PathVariable Long id) {
        return Result.ok(recordService.detail(id));
    }

    /** 手动创建（sonic:edit） */
    @PostMapping
    @SaCheckPermission("sonic:edit")
    public Result<Long> create(@Valid @RequestBody ReleaseRecordCreateDTO dto) {
        return Result.ok(recordService.createManual(dto));
    }

    /** 编辑（sonic:edit） */
    @PutMapping
    @SaCheckPermission("sonic:edit")
    public Result<Void> update(@Valid @RequestBody ReleaseRecordUpdateDTO dto) {
        recordService.update(dto);
        return Result.ok();
    }

    /** 删除（sonic:edit） */
    @DeleteMapping("/{id}")
    @SaCheckPermission("sonic:edit")
    public Result<Void> delete(@PathVariable Long id) {
        recordService.delete(id);
        return Result.ok();
    }
}
