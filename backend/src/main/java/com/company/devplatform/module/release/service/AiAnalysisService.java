package com.company.devplatform.module.release.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.devplatform.common.ErrorCode;
import com.company.devplatform.common.exception.BusinessException;
import com.company.devplatform.module.release.entity.SysConfig;
import com.company.devplatform.module.release.mapper.SysConfigMapper;
import com.company.devplatform.module.release.vo.AiAnalysisResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 分析服务：调用 OpenAI 兼容接口（chat/completions），将脚本执行记录交由 AI 分析
 * <p>配置项存 sys_config：ai.base.url / ai.api.key / ai.model（由管理员配置）</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAnalysisService {

    private static final String KEY_BASE_URL = "ai.base.url";
    private static final String KEY_API_KEY = "ai.api.key";
    private static final String KEY_MODEL = "ai.model";

    private static final String DEFAULT_SKILL =
            "这是我的 sonic 系统的自动化脚本执行记录，当前脚本失败，需要AI协助分析。"
                    + "请根据脚本执行记录进行分析，并将分析结果按照 JSON 格式返回，"
                    + "JSON 只包含三个字段：rootCause（分析失败根因）、evidence（分析佐证）、solution（修复建议）。"
                    + "只返回 JSON 对象本身，不要包含任何其他文字或 markdown 代码块标记。";

    private final SysConfigMapper sysConfigMapper;
    private final ObjectMapper objectMapper;
    private final AiSkillService aiSkillService;

    /** 调用 AI 分析脚本执行记录，返回结构化结果 */
    public AiAnalysisResult analyze(String caseLog, String module) {
        String baseUrl = getConfig(KEY_BASE_URL);
        String apiKey = getConfig(KEY_API_KEY);
        String model = getConfig(KEY_MODEL);
        if (!StringUtils.hasText(baseUrl) || !StringUtils.hasText(apiKey)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "AI 服务未配置，请联系管理员配置后再试");
        }
        String skill = buildSkillPrompt(module);
        String content = callChat(baseUrl, apiKey, model, skill, caseLog);
        return parseResult(content);
    }

    /** 拼接该板块下所有启用 skill 的 markdown 内容作为 system prompt */
    private String buildSkillPrompt(String module) {
        List<String> contents = aiSkillService.getEnabledContents(module);
        if (contents == null || contents.isEmpty()) {
            return DEFAULT_SKILL;
        }
        return String.join("\n\n", contents);
    }

    private String callChat(String baseUrl, String apiKey, String model, String systemPrompt, String userContent) {
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", StringUtils.hasText(model) ? model : "gpt-4o-mini");
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            messages.add(Map.of("role", "user", "content", userContent == null ? "" : userContent));
            body.put("messages", messages);
            body.put("temperature", 0.3);

            String json = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(trimSlash(baseUrl) + "/chat/completions"))
                    .timeout(Duration.ofSeconds(120))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                log.error("[AI分析] 调用失败 status={} body={}", response.statusCode(), response.body());
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "AI 分析调用失败（" + response.statusCode() + "）");
            }
            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("choices").path(0).path("message").path("content").asText();
            if (!StringUtils.hasText(content)) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "AI 分析返回内容为空");
            }
            return content;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[AI分析] 调用异常", e);
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "AI 分析调用异常：" + e.getMessage());
        }
    }

    private AiAnalysisResult parseResult(String content) {
        try {
            String json = content.trim();
            if (json.startsWith("```")) {
                json = json.replaceAll("```(json)?", "").trim();
            }
            JsonNode node = objectMapper.readTree(json);
            AiAnalysisResult result = new AiAnalysisResult();
            result.setRootCause(textOf(node, "rootCause"));
            result.setEvidence(textOf(node, "evidence"));
            result.setSolution(textOf(node, "solution"));
            return result;
        } catch (Exception e) {
            log.warn("[AI分析] 解析 JSON 失败，将原文作为根因 content={}", content);
            AiAnalysisResult result = new AiAnalysisResult();
            result.setRootCause(content);
            result.setEvidence("");
            result.setSolution("");
            return result;
        }
    }

    private String textOf(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? "" : v.asText();
    }

    private String getConfig(String key) {
        SysConfig cfg = sysConfigMapper.selectOne(
                new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, key));
        return cfg == null ? "" : cfg.getConfigValue();
    }

    private String trimSlash(String url) {
        String u = url == null ? "" : url.trim();
        while (u.endsWith("/")) {
            u = u.substring(0, u.length() - 1);
        }
        return u;
    }
}
