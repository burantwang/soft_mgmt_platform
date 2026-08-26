package com.company.devplatform.module.release.util;

import com.company.devplatform.common.ErrorCode;
import com.company.devplatform.common.exception.BusinessException;
import lombok.Data;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * pytest-html 测试报告解析器
 * <p>兼容 pytest-html v3.x 与 v4.x 模板结构；非 pytest-html 标准报告直接拒绝。</p>
 * <p>提取内容：用例统计、总耗时、Environment.Version、报告生成时间、失败/错误用例明细。</p>
 */
public final class HtmlReportParser {

    /** 单个失败用例日志最多保留字符数 */
    private static final int MAX_LOG_LENGTH = 4000;
    /** 最多提取失败用例数（超出部分截断，防超大报告拖垮内存） */
    private static final int MAX_FAIL_CASE = 100;

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
        Elements spans = doc.select(".totals span, p.totals span, span.totals span");
        for (Element span : spans) {
            String cls = span.className() == null ? "" : span.className();
            String text = span.text();
            int num = extractNumber(text);
            if (cls.contains("passed")) {
                passed = num;
            } else if (cls.contains("failed")) {
                failed = num;
            } else if (cls.contains("error")) {
                error = num;
            } else if (cls.contains("skipped")) {
                skipped = num;
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

    /** 提取失败/错误用例 */
    private static void parseFailCases(Document doc, ParseResult result) {
        Elements rows = doc.select("tr.failed, tr.error");
        int count = 0;
        for (Element row : rows) {
            if (count >= MAX_FAIL_CASE) {
                break;
            }
            HtmlReportParser.FailCase fc = new HtmlReportParser.FailCase();
            Element nameEl = row.selectFirst(".col-name");
            if (nameEl != null) {
                fc.setName(nameEl.text().trim());
            }
            Element logEl = row.selectFirst(".col-log");
            if (logEl != null) {
                Element pre = logEl.selectFirst("pre");
                String log = pre != null ? pre.text() : logEl.text();
                fc.setLog(truncate(log, MAX_LOG_LENGTH));
            }
            if (fc.getName() != null && !fc.getName().isEmpty()) {
                result.getFailCases().add(fc);
                count++;
            }
        }
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

    private static String truncate(String s, int max) {
        if (s == null || s.length() <= max) {
            return s;
        }
        return s.substring(0, max);
    }
}
