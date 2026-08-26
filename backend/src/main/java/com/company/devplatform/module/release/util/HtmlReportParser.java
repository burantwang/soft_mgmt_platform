package com.company.devplatform.module.release.util;

import com.company.devplatform.common.ErrorCode;
import com.company.devplatform.common.exception.BusinessException;
import lombok.Data;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * pytest-html 测试报告解析器
 * <p>兼容 pytest-html v3.x 与 v4.x 模板结构；非 pytest-html 标准报告直接拒绝。</p>
 * <p>提取内容：用例统计、总耗时、Environment.Version、报告生成时间、失败/错误用例明细。</p>
 */
public final class HtmlReportParser {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");
    private static final Pattern DURATION_PATTERN = Pattern.compile("in\\s+(\\d+(?:\\.\\d+)?)s", Pattern.CASE_INSENSITIVE);
    private static final Pattern DATETIME_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}[ T]\\d{2}:\\d{2}(?::\\d{2})?");
    private static final Pattern VERSION_PATTERN = Pattern.compile("Environment\\.Version\\s*[:：]?\\s*(.+)", Pattern.CASE_INSENSITIVE);

    private HtmlReportParser() {
    }

    /** 解析结果 */
    @Data
    public static class ParseResult {
        /** 镜像版本号(Environment.Version) */
        private String version;
        private int totalCount;
        private int passedCount;
        private int failedCount;
        private int errorCount;
        private int skippedCount;
        /** 总耗时(秒) */
        private BigDecimal durationSec;
        /** 报告生成时间 */
        private LocalDateTime reportTime;
        /** 失败/错误用例明细 */
        private List<FailCase> failCases = new ArrayList<>();
    }

    /** 失败用例明细 */
    @Data
    public static class FailCase {
        /** 用例状态：failed / error */
        private String status;
        private String name;
        private String log;
    }

    /**
     * 解析 pytest-html 报告内容
     *
     * @param content 报告文件字节
     * @return 解析结果
     * @throws BusinessException REPORT_FORMAT_INVALID 非标准报告 / HTML_PARSE_ERROR 解析失败
     */
    public static ParseResult parse(byte[] content) {
        if (content == null || content.length == 0) {
            throw new BusinessException(ErrorCode.FILE_EMPTY);
        }
        Document doc;
        try {
            doc = Jsoup.parse(new String(content, StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.REPORT_FORMAT_INVALID, "无法解析该文件，仅支持 pytest-html 格式的 HTML 测试报告");
        }

        ParseResult result = new ParseResult();
        parseTotals(doc, result);
        parseVersion(doc, result);
        parseReportTime(doc, result);
        parseFailCases(doc, result);

        // 标准性校验：没有任何统计信息且没有结果表 → 判定非标准报告
        if (result.getTotalCount() <= 0 && result.getFailCases().isEmpty()) {
            throw new BusinessException(ErrorCode.REPORT_FORMAT_INVALID);
        }
        return result;
    }

    /** 解析用例统计与总耗时 */
    private static void parseTotals(Document doc, ParseResult result) {
        int passed = 0;
        int failed = 0;
        int error = 0;
        int skipped = 0;

        // 优先取 totals 中的 span（v3/v4 通用）
        // 注意按 class token 精确匹配，避免 xfailed/xpassed 被误判为 failed
        Elements spans = doc.select(".totals span, p.totals span, span.totals span");
        for (Element span : spans) {
            String cls = span.className() == null ? "" : span.className();
            String text = span.text();
            int num = extractNumber(text);
            for (String token : cls.toLowerCase().split("\\s+")) {
                switch (token) {
                    case "passed":
                        passed = num;
                        break;
                    case "failed":
                        failed = num;
                        break;
                    case "error":
                        error = num;
                        break;
                    case "skipped":
                        skipped = num;
                        break;
                    default:
                        break;
                }
            }
        }

        // 兜底：从 totals 区域全文正则提取
        if (passed == 0 && failed == 0 && error == 0 && skipped == 0) {
            String totalsText = "";
            Elements totals = doc.select(".totals");
            if (!totals.isEmpty()) {
                totalsText = totals.first().text();
            }
            if (totalsText.isEmpty()) {
                totalsText = doc.body() == null ? "" : doc.body().text();
            }
            passed = matchCount(totalsText, "passed");
            failed = matchCount(totalsText, "failed");
            error = matchCount(totalsText, "error");
            skipped = matchCount(totalsText, "skipped");
        }

        result.setPassedCount(passed);
        result.setFailedCount(failed);
        result.setErrorCount(error);
        result.setSkippedCount(skipped);
        result.setTotalCount(passed + failed + error + skipped);

        // 总耗时
        String totalsText = "";
        Elements totals = doc.select(".totals");
        if (!totals.isEmpty()) {
            totalsText = totals.first().text();
        }
        if (!totalsText.isEmpty()) {
            Matcher m = DURATION_PATTERN.matcher(totalsText);
            if (m.find()) {
                result.setDurationSec(new BigDecimal(m.group(1)));
            }
        }
    }

    /** 解析 Environment.Version（v3/v4 兼容） */
    private static void parseVersion(Document doc, ParseResult result) {
        Elements attrs = doc.select(".environment .attr, .environment .attribute, #environment .attr, #environment .attribute");
        for (Element attr : attrs) {
            Matcher m = VERSION_PATTERN.matcher(attr.text());
            if (m.matches()) {
                String v = m.group(1).trim();
                if (!v.isEmpty()) {
                    result.setVersion(v);
                    return;
                }
            }
        }
        // 兜底：environment 全文中查找
        Elements env = doc.select(".environment, #environment");
        if (!env.isEmpty()) {
            Matcher m = VERSION_PATTERN.matcher(env.first().text());
            if (m.matches() || (m = VERSION_PATTERN.matcher(env.first().text())).find()) {
                String v = m.group(1).trim().split("\\s+")[0];
                if (!v.isEmpty()) {
                    result.setVersion(v);
                }
            }
        }
    }

    /** 解析报告生成时间 */
    private static void parseReportTime(Document doc, ParseResult result) {
        Elements timeEls = doc.select("[class*='report-time'], [id*='report-time']");
        for (Element el : timeEls) {
            Matcher m = DATETIME_PATTERN.matcher(el.text());
            if (m.find()) {
                result.setReportTime(parseDateTime(m.group()));
                return;
            }
        }
    }

    /**
     * 提取失败/错误用例（全部记录，不做数量截断）。
     * 兼容两种报告结构：
     * <ol>
     *   <li>pytest-html v3.x+：每个用例一个 <code>&lt;tbody class="error results-table-row"&gt;</code>，
     *       状态在 tbody class 上，日志位于内部的 <code>td.extra div.log</code></li>
     *   <li>旧版/自定义样例：<code>&lt;tr class="failed"&gt;</code> / <code>&lt;tr class="error"&gt;</code>，
     *       日志位于本行 <code>td.col-log pre</code></li>
     * </ol>
     */
    private static void parseFailCases(Document doc, ParseResult result) {
        Elements rows = doc.select("#results-table tbody.results-table-row, #results-table tr.failed, #results-table tr.error");
        for (Element row : rows) {
            String status = extractFailStatus(row);
            if (status == null) {
                continue;
            }
            FailCase fc = new FailCase();
            fc.setStatus(status);
            Element nameEl = row.selectFirst("td.col-name, .col-name");
            if (nameEl != null) {
                fc.setName(nameEl.text().trim());
            }
            if (fc.getName() == null || fc.getName().isEmpty()) {
                continue;
            }
            Element logEl = row.selectFirst("td.extra .log, .col-log pre, .col-log, div.log");
            if (logEl != null) {
                String log = extractLogText(logEl);
                if (!log.isEmpty()) {
                    fc.setLog(log);
                }
            }
            result.getFailCases().add(fc);
        }
    }

    /**
     * 判定用例行状态：优先按 class token 精确匹配 failed/error，
     * 兜底按 <code>td.col-result</code> 单元格文本（Failed/Error）判定。
     * 非失败/错误返回 null。
     */
    private static String extractFailStatus(Element row) {
        String cls = row.className();
        if (cls != null) {
            for (String token : cls.toLowerCase().split("\\s+")) {
                if ("failed".equals(token)) {
                    return "failed";
                }
                if ("error".equals(token)) {
                    return "error";
                }
            }
        }
        Element resultEl = row.selectFirst("td.col-result");
        if (resultEl != null) {
            String t = resultEl.text().trim().toLowerCase();
            if ("failed".equals(t)) {
                return "failed";
            }
            if ("error".equals(t)) {
                return "error";
            }
        }
        return null;
    }

    /** 从 span 文本提取数字 */
    private static int extractNumber(String text) {
        Matcher m = NUMBER_PATTERN.matcher(text == null ? "" : text);
        return m.find() ? Integer.parseInt(m.group()) : 0;
    }

    /** 从文本匹配 "N passed" 等计数 */
    private static int matchCount(String text, String keyword) {
        Pattern p = Pattern.compile("(\\d+)\\s*" + keyword + "s?\\b");
        Matcher m = p.matcher(text);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    private static LocalDateTime parseDateTime(String dt) {
        try {
            String pattern = dt.length() > 16 ? "yyyy-MM-dd HH:mm:ss" : "yyyy-MM-dd HH:mm";
            return LocalDateTime.parse(dt.replace("T", " "), DateTimeFormatter.ofPattern(pattern));
        } catch (Exception e) {
            return null;
        }
    }

    /** 提取元素文本并保留原始换行与空白（不再做任何删减） */
    private static String extractLogText(Element el) {
        if (el == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        collectText(el, sb);
        String text = sb.toString()
                .replaceAll("[ \t]+\n", "\n")
                .replaceAll("\n{3,}", "\n\n");
        return text.trim();
    }

    /** 递归收集文本节点；&lt;br&gt; 转换行，块级标签后补换行 */
    private static void collectText(Node node, StringBuilder sb) {
        if (node instanceof TextNode) {
            sb.append(((TextNode) node).getWholeText());
            return;
        }
        if (!(node instanceof Element)) {
            return;
        }
        Element e = (Element) node;
        String tag = e.normalName();
        if ("br".equals(tag)) {
            sb.append('\n');
            return;
        }
        if ("script".equals(tag) || "style".equals(tag)) {
            return;
        }
        for (Node child : e.childNodes()) {
            collectText(child, sb);
        }
        if (BLOCK_TAGS.contains(tag)) {
            sb.append('\n');
        }
    }

    /** 块级标签（结束后追加换行） */
    private static final Set<String> BLOCK_TAGS = new HashSet<>(Arrays.asList(
            "p", "div", "pre", "li", "ul", "ol", "tr", "td", "th", "table",
            "h1", "h2", "h3", "h4", "h5", "h6", "blockquote", "section", "article", "br"));
}
