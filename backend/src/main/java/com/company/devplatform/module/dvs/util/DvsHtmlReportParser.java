package com.company.devplatform.module.dvs.util;

import com.company.devplatform.common.ErrorCode;
import com.company.devplatform.common.exception.BusinessException;
import com.company.devplatform.module.release.util.HtmlReportParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * DVS 测试报告解析器
 * <p>DVS 报告由 pytest-html 4.x 生成（异步渲染模板）：全部用例数据编码在
 * {@code <div id="data-container" data-jsonblob="...">} 属性的 JSON 中，DOM 由 JS 动态渲染，
 * 静态 HTML 中不存在可解析的统计节点与用例行（Sanity 的 {@link HtmlReportParser} 无法识别此类报告）。
 * 本解析器优先解析 data-jsonblob；若文件非该模板（如 pytest-html 3.x DOM 静态渲染），回退到
 * {@link HtmlReportParser} 兼容解析。</p>
 */
public final class DvsHtmlReportParser {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String JSON_BLOB_ID = "data-container";
    private static final String JSON_BLOB_ATTR = "data-jsonblob";

    private DvsHtmlReportParser() {
    }

    /**
     * 解析 DVS 测试报告内容
     *
     * @param content 报告文件字节
     * @return 解析结果
     * @throws BusinessException REPORT_FORMAT_INVALID 非标准报告 / HTML_PARSE_ERROR 解析失败
     */
    public static HtmlReportParser.ParseResult parse(byte[] content) {
        if (content == null || content.length == 0) {
            throw new BusinessException(ErrorCode.FILE_EMPTY);
        }
        Document doc;
        try {
            doc = Jsoup.parse(new String(content, StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.REPORT_FORMAT_INVALID,
                    "无法解析该文件，仅支持 pytest-html 格式的 HTML 测试报告");
        }
        Element container = doc.getElementById(JSON_BLOB_ID);
        if (container != null) {
            String json = container.attr(JSON_BLOB_ATTR);
            if (json != null && !json.isEmpty()) {
                return parseJsonBlob(json);
            }
        }
        // 回退：pytest-html 3.x / DOM 静态渲染报告
        return HtmlReportParser.parse(content);
    }

    /** 解析 pytest-html 4.x 的 data-jsonblob（JS 渲染数据源） */
    private static HtmlReportParser.ParseResult parseJsonBlob(String json) {
        JsonNode root;
        try {
            root = OBJECT_MAPPER.readTree(json);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.REPORT_FORMAT_INVALID,
                    "无法解析该文件，仅支持 pytest-html 格式的 HTML 测试报告");
        }
        JsonNode tests = root == null ? null : root.path("tests");
        if (tests == null || !tests.isObject()) {
            throw new BusinessException(ErrorCode.REPORT_FORMAT_INVALID);
        }

        HtmlReportParser.ParseResult result = new HtmlReportParser.ParseResult();
        int passed = 0;
        int failed = 0;
        int error = 0;
        int skipped = 0;
        long totalDurationMs = 0;
        List<HtmlReportParser.FailCase> failCases = new ArrayList<>();

        Iterator<Map.Entry<String, JsonNode>> it = tests.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> entry = it.next();
            String testId = entry.getKey();
            JsonNode arr = entry.getValue();
            JsonNode item = null;
            if (arr != null && arr.isArray() && arr.size() > 0) {
                // 重跑（rerun）用例可能含多条记录，取最后一条作为最终状态
                item = arr.get(arr.size() - 1);
            } else if (arr != null && arr.isObject()) {
                item = arr;
            }
            if (item == null) {
                continue;
            }
            String resultText = item.path("result").asText("").trim();
            switch (resultText.toLowerCase(Locale.ROOT)) {
                case "passed":
                case "xpassed":
                    // XPassed：标记 xfail 但实际通过，视为通过
                    passed++;
                    break;
                case "rerun":
                    // rerunfailures 重跑后记录的条目（多记录时取最后一条为最终状态；此处视为通过）
                    passed++;
                    break;
                case "failed":
                    failed++;
                    failCases.add(buildFailCase("failed", testId, item));
                    break;
                case "error":
                    error++;
                    failCases.add(buildFailCase("error", testId, item));
                    break;
                case "skipped":
                case "xfailed":
                    // XFailed：预期失败，计入跳过
                    skipped++;
                    break;
                default:
                    // 未知状态：不参与统计
                    break;
            }
            totalDurationMs += parseDuration(item.path("duration").asText(""));
        }

        result.setPassedCount(passed);
        result.setFailedCount(failed);
        result.setErrorCount(error);
        result.setSkippedCount(skipped);
        result.setTotalCount(passed + failed + error + skipped);
        if (totalDurationMs > 0) {
            result.setDurationSec(BigDecimal.valueOf(totalDurationMs / 1000.0)
                    .setScale(1, RoundingMode.HALF_UP));
        }
        result.setFailCases(failCases);
        parseVersion(root, result);

        // 标准性校验：没有任何统计信息且没有结果表 → 判定非标准报告
        if (result.getTotalCount() <= 0 && result.getFailCases().isEmpty()) {
            throw new BusinessException(ErrorCode.REPORT_FORMAT_INVALID);
        }
        return result;
    }

    /** 从 environment 对象中提取版本号（键名包含 version，不区分大小写） */
    private static void parseVersion(JsonNode root, HtmlReportParser.ParseResult result) {
        JsonNode env = root.path("environment");
        if (env == null || !env.isObject()) {
            return;
        }
        Iterator<Map.Entry<String, JsonNode>> it = env.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> e = it.next();
            if (e.getKey().toLowerCase(Locale.ROOT).contains("version")) {
                String v = e.getValue().asText("").trim();
                if (!v.isEmpty()) {
                    result.setVersion(v);
                    return;
                }
            }
        }
    }

    /** 构建失败/错误用例明细（用例名取完整 testId，日志取 JSON log 字段） */
    private static HtmlReportParser.FailCase buildFailCase(String status, String testId, JsonNode item) {
        HtmlReportParser.FailCase fc = new HtmlReportParser.FailCase();
        fc.setStatus(status);
        fc.setName(testId);
        String log = item.path("log").asText("").trim();
        if (!log.isEmpty()) {
            fc.setLog(log);
        }
        return fc;
    }

    /** 解析 "HH:mm:ss[.fff]" 用例时长 → 毫秒 */
    private static long parseDuration(String hms) {
        if (hms == null || hms.isEmpty()) {
            return 0;
        }
        try {
            String[] parts = hms.split(":");
            if (parts.length != 3) {
                return 0;
            }
            long h = Long.parseLong(parts[0].trim());
            long m = Long.parseLong(parts[1].trim());
            double s = Double.parseDouble(parts[2].trim());
            return (long) ((h * 3600 + m * 60 + s) * 1000);
        } catch (Exception e) {
            return 0;
        }
    }
}
