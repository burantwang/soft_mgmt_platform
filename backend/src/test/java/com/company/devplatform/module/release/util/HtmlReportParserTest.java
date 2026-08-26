package com.company.devplatform.module.release.util;

import com.company.devplatform.common.ErrorCode;
import com.company.devplatform.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * HtmlReportParser 单元测试
 */
class HtmlReportParserTest {

    /** 成功报告：通过 + 跳过，无失败 */
    @Test
    void parse_successReport() {
        String html = """
                <!DOCTYPE html>
                <html>
                <head><title>pytest-html report</title></head>
                <body>
                <div id="environment">
                  <div class="attr"><strong>Environment.Version</strong>: 2.1.0</div>
                  <div class="attr"><strong>Browser</strong>: Chrome</div>
                </div>
                <div class="report-time">Generated at 2026-08-26 14:30:00</div>
                <div class="summary">
                  <p class="totals"><span class="passed">10 passed</span>, <span class="skipped">1 skipped</span> in 15.5s</p>
                </div>
                <table id="results-table">
                  <tbody>
                    <tr class="passed"><td class="col-name">test_ok</td><td class="col-status">passed</td></tr>
                  </tbody>
                </table>
                </body>
                </html>
                """;
        HtmlReportParser.ParseResult result = HtmlReportParser.parse(html.getBytes(StandardCharsets.UTF_8));

        assertEquals("2.1.0", result.getVersion());
        assertEquals(11, result.getTotalCount());
        assertEquals(10, result.getPassedCount());
        assertEquals(0, result.getFailedCount());
        assertEquals(0, result.getErrorCount());
        assertEquals(1, result.getSkippedCount());
        assertEquals(new BigDecimal("15.5"), result.getDurationSec());
        assertNotNull(result.getReportTime());
        assertEquals(2026, result.getReportTime().getYear());
        assertTrue(result.getFailCases().isEmpty());
    }

    /** 失败报告：提取失败/错误用例名称与日志 */
    @Test
    void parse_failedReport_extractsCases() {
        String html = """
                <!DOCTYPE html>
                <html>
                <head><title>pytest-html report</title></head>
                <body>
                <div id="environment">
                  <div class="attr"><strong>Environment.Version</strong>: 1.2.3</div>
                </div>
                <div class="summary">
                  <p class="totals"><span class="failed">2 failed</span>, <span class="passed">8 passed</span>, <span class="error">1 error</span> in 25.5s</p>
                </div>
                <table id="results-table">
                  <tbody>
                    <tr class="failed">
                      <td class="col-name"><span class="parametrize-name">test_login_failed[user1]</span></td>
                      <td class="col-status">failed</td>
                      <td class="col-log"><pre>AssertionError: login failed
        expected 'ok', got 'err'</pre></td>
                    </tr>
                    <tr class="error">
                      <td class="col-name">test_connect_timeout</td>
                      <td class="col-status">error</td>
                      <td class="col-log"><pre>TimeoutError: connect timeout</pre></td>
                    </tr>
                  </tbody>
                </table>
                </body>
                </html>
                """;
        HtmlReportParser.ParseResult result = HtmlReportParser.parse(html.getBytes(StandardCharsets.UTF_8));

        assertEquals(11, result.getTotalCount());
        assertEquals(2, result.getFailedCount());
        assertEquals(1, result.getErrorCount());
        assertEquals(2, result.getFailCases().size());

        HtmlReportParser.FailCase first = result.getFailCases().get(0);
        assertEquals("failed", first.getStatus());
        assertEquals("test_login_failed[user1]", first.getName());
        assertTrue(first.getLog().contains("AssertionError: login failed"));

        HtmlReportParser.FailCase second = result.getFailCases().get(1);
        assertEquals("error", second.getStatus());
        assertEquals("test_connect_timeout", second.getName());
        assertTrue(second.getLog().contains("TimeoutError: connect timeout"));
    }

    /**
     * 真实 pytest-html v3.x 报告结构：每个用例一个
     * <code>&lt;tbody class="failed results-table-row"&gt;</code>，日志位于内部
     * <code>td.extra div.log</code>（状态在 tbody class 上，而非 tr class）。
     */
    @Test
    void parse_realPytestHtmlV3Structure() {
        String html = """
                <!DOCTYPE html>
                <html>
                <head><title>pytest-html report</title></head>
                <body>
                <div class="summary">
                  <p class="totals"><span class="passed">1 passed</span>, <span class="failed">2 failed</span>, <span class="error">1 error</span>, <span class="xfailed">1 xfailed</span>, <span class="skipped">1 skipped</span> in 60.0s</p>
                </div>
                <table id="results-table">
                  <tbody class="passed results-table-row">
                    <tr><td class="col-result">Passed</td><td class="col-name">test_ok</td><td class="col-duration">0.1</td><td class="col-links"></td></tr>
                    <tr><td class="extra" colspan="4"></td></tr>
                  </tbody>
                  <tbody class="failed results-table-row">
                    <tr><td class="col-result">Failed</td><td class="col-name">function/alarm/test_alarm.py::test_alarm_case1</td><td class="col-duration">2.5</td><td class="col-links"></td></tr>
                    <tr><td class="extra" colspan="4"><div class="log"><span class="warning">W</span> warning line<br/><span class="failed">F</span> AssertionError: alarm not triggered<br/>expected 'on', got 'off'</div></td></tr>
                  </tbody>
                  <tbody class="error results-table-row">
                    <tr><td class="col-result">Error</td><td class="col-name">function/alarm/test_alarm.py::test_alarm_setup::teardown</td><td class="col-duration">0.5</td><td class="col-links"></td></tr>
                    <tr><td class="extra" colspan="4"><div class="log"><span class="error">E</span> TimeoutError: connect to 10.0.0.1:8080 timed out</div></td></tr>
                  </tbody>
                  <tbody class="failed results-table-row">
                    <tr><td class="col-result">Failed</td><td class="col-name">function/portal/test_portal.py::test_portal_login</td><td class="col-duration">3.0</td><td class="col-links"></td></tr>
                    <tr><td class="extra" colspan="4"><div class="log"><span class="failed">F</span> AssertionError: login failed</div></td></tr>
                  </tbody>
                  <tbody class="xfailed results-table-row">
                    <tr><td class="col-result">XFailed</td><td class="col-name">test_xfail_case</td><td class="col-duration">0.1</td><td class="col-links"></td></tr>
                    <tr><td class="extra" colspan="4"></td></tr>
                  </tbody>
                  <tbody class="skipped results-table-row">
                    <tr><td class="col-result">Skipped</td><td class="col-name">test_skip_case</td><td class="col-duration">0.0</td><td class="col-links"></td></tr>
                    <tr><td class="extra" colspan="4"></td></tr>
                  </tbody>
                </table>
                </body>
                </html>
                """;
        HtmlReportParser.ParseResult result = HtmlReportParser.parse(html.getBytes(StandardCharsets.UTF_8));

        // totalCount = passed + failed + error + skipped（xfailed 不计入，且不得覆盖 failed 计数）
        assertEquals(5, result.getTotalCount());
        assertEquals(2, result.getFailedCount());
        assertEquals(1, result.getErrorCount());
        // 仅 failed + error 用例被提取，xfailed/skipped/passed 不误入
        assertEquals(3, result.getFailCases().size());

        HtmlReportParser.FailCase first = result.getFailCases().get(0);
        assertEquals("failed", first.getStatus());
        assertEquals("function/alarm/test_alarm.py::test_alarm_case1", first.getName());
        assertTrue(first.getLog().contains("AssertionError: alarm not triggered"));

        HtmlReportParser.FailCase second = result.getFailCases().get(1);
        assertEquals("error", second.getStatus());
        assertEquals("function/alarm/test_alarm.py::test_alarm_setup::teardown", second.getName());
        assertTrue(second.getLog().contains("TimeoutError: connect to 10.0.0.1:8080"));

        HtmlReportParser.FailCase third = result.getFailCases().get(2);
        assertEquals("failed", third.getStatus());
        assertEquals("function/portal/test_portal.py::test_portal_login", third.getName());
    }

    /** 大量失败用例应全部提取，不做数量截断 */
    @Test
    void parse_manyFailCases_noTruncation() {
        StringBuilder sb = new StringBuilder("""
                <!DOCTYPE html>
                <html><body>
                <div class="summary"><p class="totals"><span class="failed">120 failed</span> in 60.0s</p></div>
                <table id="results-table">
                """);
        for (int i = 0; i < 120; i++) {
            sb.append("<tbody class=\"failed results-table-row\">")
                    .append("<tr><td class=\"col-result\">Failed</td><td class=\"col-name\">test_case_").append(i).append("</td><td class=\"col-duration\">0.1</td><td class=\"col-links\"></td></tr>")
                    .append("<tr><td class=\"extra\" colspan=\"4\"><div class=\"log\">F log_").append(i).append("</div></td></tr>")
                    .append("</tbody>\n");
        }
        sb.append("</table></body></html>");

        HtmlReportParser.ParseResult result = HtmlReportParser.parse(sb.toString().getBytes(StandardCharsets.UTF_8));

        assertEquals(120, result.getFailedCount());
        assertEquals(120, result.getFailCases().size(), "超过 100 条失败用例也应全部提取");
        assertEquals("test_case_119", result.getFailCases().get(119).getName());
        assertTrue(result.getFailCases().get(119).getLog().contains("log_119"));
    }

    /** 缺字段报告：缺少 Environment.Version 与报告时间，仍可解析，字段为空 */
    @Test
    void parse_reportMissingFields() {
        String html = """
                <!DOCTYPE html>
                <html>
                <head><title>pytest-html report</title></head>
                <body>
                <div class="summary">
                  <p class="totals"><span class="passed">5 passed</span></p>
                </div>
                </body>
                </html>
                """;
        HtmlReportParser.ParseResult result = HtmlReportParser.parse(html.getBytes(StandardCharsets.UTF_8));

        assertEquals(5, result.getTotalCount());
        assertEquals(5, result.getPassedCount());
        assertNull(result.getVersion(), "缺少 Environment.Version 时应为 null");
        assertNull(result.getDurationSec());
        assertNull(result.getReportTime());
    }

    /** 非法格式：非 pytest-html 报告被拒绝 */
    @Test
    void parse_invalidFormat_throws() {
        String html = "<!DOCTYPE html><html><body><h1>普通页面</h1><p>没有任何 pytest-html 标记</p></body></html>";
        BusinessException ex = assertThrows(BusinessException.class,
                () -> HtmlReportParser.parse(html.getBytes(StandardCharsets.UTF_8)));
        assertEquals(ErrorCode.REPORT_FORMAT_INVALID.getCode(), ex.getCode());
    }

    /** 空内容被拒绝 */
    @Test
    void parse_empty_throws() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> HtmlReportParser.parse(new byte[0]));
        assertEquals(ErrorCode.FILE_EMPTY.getCode(), ex.getCode());
    }
}
