package com.company.devplatform.module.release.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.devplatform.common.ErrorCode;
import com.company.devplatform.common.Result;
import com.company.devplatform.common.exception.BusinessException;
import com.company.devplatform.module.release.entity.IssueCategory;
import com.company.devplatform.module.release.entity.SysConfig;
import com.company.devplatform.module.release.mapper.IssueCategoryMapper;
import com.company.devplatform.module.release.mapper.SysConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 失败用例元数据：问题分类管理 + Redmine 前缀配置
 */
@RestController
@RequestMapping("/api/release")
@RequiredArgsConstructor
public class FailCaseMetaController {

    private static final String REDMINE_PREFIX_KEY = "redmine.url.prefix";

    private final IssueCategoryMapper issueCategoryMapper;
    private final SysConfigMapper sysConfigMapper;

    /** 元数据：启用的问题分类列表 + Redmine 前缀（所有登录用户可读） */
    @GetMapping("/fail-cases/meta")
    @SaCheckPermission("sonic:view")
    public Result<Map<String, Object>> meta() {
        List<IssueCategory> categories = issueCategoryMapper.selectList(
                new LambdaQueryWrapper<IssueCategory>()
                        .eq(IssueCategory::getStatus, 1)
                        .orderByAsc(IssueCategory::getSortOrder)
                        .orderByAsc(IssueCategory::getId));
        SysConfig cfg = sysConfigMapper.selectOne(
                new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, REDMINE_PREFIX_KEY));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("categories", categories);
        data.put("redminePrefix", cfg == null ? "" : cfg.getConfigValue());
        return Result.ok(data);
    }

    /** 新增问题分类（仅管理员） */
    @PostMapping("/issue-categories")
    @SaCheckPermission("sonic:view")
    public Result<Void> addCategory(@RequestBody Map<String, String> body) {
        checkAdmin();
        String name = body.get("categoryName");
        if (!StringUtils.hasText(name)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "分类名称不能为空");
        }
        String code = body.get("categoryCode");
        IssueCategory category = new IssueCategory();
        category.setCategoryName(name.trim());
        category.setCategoryCode(StringUtils.hasText(code) ? code.trim() : name.trim());
        category.setStatus(1);
        category.setSortOrder(99);
        issueCategoryMapper.insert(category);
        return Result.ok();
    }

    /** 停用/启用分类（仅管理员） */
    @PutMapping("/issue-categories/{id}/status")
    @SaCheckPermission("sonic:view")
    public Result<Void> updateCategoryStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        checkAdmin();
        IssueCategory category = issueCategoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "分类不存在");
        }
        Integer status = body.getOrDefault("status", 0);
        category.setStatus(status == null || status != 1 ? 0 : 1);
        issueCategoryMapper.updateById(category);
        return Result.ok();
    }

    /** 更新 Redmine 前缀（仅管理员） */
    @PutMapping("/config/redmine-prefix")
    @SaCheckPermission("sonic:view")
    public Result<Void> updateRedminePrefix(@RequestBody Map<String, String> body) {
        checkAdmin();
        String prefix = body.get("redminePrefix");
        if (!StringUtils.hasText(prefix)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "前缀不能为空");
        }
        SysConfig cfg = sysConfigMapper.selectOne(
                new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, REDMINE_PREFIX_KEY));
        if (cfg == null) {
            cfg = new SysConfig();
            cfg.setConfigKey(REDMINE_PREFIX_KEY);
            cfg.setConfigValue(prefix.trim());
            sysConfigMapper.insert(cfg);
        } else {
            cfg.setConfigValue(prefix.trim());
            sysConfigMapper.updateById(cfg);
        }
        return Result.ok();
    }

    /** 读取 AI 配置（仅管理员，含 api key） */
    @GetMapping("/config/ai")
    @SaCheckPermission("sonic:view")
    public Result<Map<String, String>> getAiConfig() {
        checkAdmin();
        Map<String, String> data = new LinkedHashMap<>();
        data.put("baseUrl", getConfigValue("ai.base.url"));
        data.put("apiKey", getConfigValue("ai.api.key"));
        data.put("model", getConfigValue("ai.model"));
        return Result.ok(data);
    }

    /** 更新 AI 配置（仅管理员） */
    @PutMapping("/config/ai")
    @SaCheckPermission("sonic:view")
    public Result<Void> updateAiConfig(@RequestBody Map<String, String> body) {
        checkAdmin();
        saveConfig("ai.base.url", body.get("baseUrl"));
        saveConfig("ai.api.key", body.get("apiKey"));
        saveConfig("ai.model", body.get("model"));
        return Result.ok();
    }

    private String getConfigValue(String key) {
        SysConfig cfg = sysConfigMapper.selectOne(
                new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, key));
        return cfg == null ? "" : cfg.getConfigValue();
    }

    private void saveConfig(String key, String value) {
        SysConfig cfg = sysConfigMapper.selectOne(
                new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, key));
        if (cfg == null) {
            cfg = new SysConfig();
            cfg.setConfigKey(key);
            cfg.setConfigValue(value == null ? "" : value.trim());
            sysConfigMapper.insert(cfg);
        } else {
            cfg.setConfigValue(value == null ? "" : value.trim());
            sysConfigMapper.updateById(cfg);
        }
    }

    private void checkAdmin() {
        if (!(StpUtil.hasRole("super_admin") || StpUtil.hasRole("admin"))) {
            throw new BusinessException(ErrorCode.NO_PERMISSION, "仅管理员可操作");
        }
    }
}
