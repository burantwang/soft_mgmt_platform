package com.company.devplatform.module.release.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.company.devplatform.common.ErrorCode;
import com.company.devplatform.common.Result;
import com.company.devplatform.common.exception.BusinessException;
import com.company.devplatform.module.release.service.AiSkillService;
import com.company.devplatform.module.release.vo.AiSkillVO;
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
import java.util.Map;

/**
 * AI 技能集接口（仅管理员可新增/编辑/删除，读取对登录用户开放）
 */
@RestController
@RequestMapping("/api/release/skills")
@RequiredArgsConstructor
public class AiSkillController {

    private final AiSkillService aiSkillService;

    /** 某板块 skill 列表 */
    @GetMapping("/{module}")
    @SaCheckPermission("sonic:view")
    public Result<List<AiSkillVO>> list(@PathVariable String module) {
        return Result.ok(aiSkillService.listSkills(module));
    }

    /** 新增 skill */
    @PostMapping("/{module}")
    @SaCheckPermission("sonic:view")
    public Result<AiSkillVO> create(@PathVariable String module, @RequestBody Map<String, String> body) {
        checkAdmin();
        return Result.ok(aiSkillService.createSkill(module, body.get("title"), body.get("content")));
    }

    /** 更新 skill */
    @PutMapping("/{docId}")
    @SaCheckPermission("sonic:view")
    public Result<Void> update(@PathVariable Long docId, @RequestBody Map<String, String> body) {
        checkAdmin();
        aiSkillService.updateSkill(docId, body.get("title"), body.get("content"));
        return Result.ok();
    }

    /** 启用/停用 skill */
    @PutMapping("/{docId}/enabled")
    @SaCheckPermission("sonic:view")
    public Result<Void> toggle(@PathVariable Long docId, @RequestBody Map<String, Integer> body) {
        checkAdmin();
        aiSkillService.toggleSkill(docId, body.get("enabled"));
        return Result.ok();
    }

    /** 删除 skill */
    @DeleteMapping("/{docId}")
    @SaCheckPermission("sonic:view")
    public Result<Void> delete(@PathVariable Long docId) {
        checkAdmin();
        aiSkillService.deleteSkill(docId);
        return Result.ok();
    }

    private void checkAdmin() {
        if (!(StpUtil.hasRole("super_admin") || StpUtil.hasRole("admin"))) {
            throw new BusinessException(ErrorCode.NO_PERMISSION, "仅管理员可操作");
        }
    }
}
