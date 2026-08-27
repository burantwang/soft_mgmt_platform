package com.company.devplatform.common.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;

/**
 * 富文本 HTML 净化工具（XSS 白名单双防线之【后端防线】）
 * <p>使用 jsoup 白名单过滤 + 属性级协议/样式二次校验：
 * 1. 白名单标签与属性（Safelist.relaxed 增强：保留 class/style）；
 * 2. 遍历属性剔除危险协议（javascript: vbscript: data: 非图片）与危险样式
 *    （expression / url( / @import / behavior / -moz-binding / position:fixed）。
 * </p>
 */
public final class HtmlSanitizer {

    private HtmlSanitizer() {
    }

    /** 富文本白名单（wangeditor 输出所需标签均在 relaxed 内，另补 class/style 属性） */
    private static final Safelist SAFELIST = buildSafelist();

    private static Safelist buildSafelist() {
        Safelist sl = Safelist.relaxed();
        // relaxed 已覆盖: h1-h6, p, div, span, blockquote, pre, code, ul, ol, li,
        // strong/b, em/i, u, s, strike, del, a, img, table/thead/tbody/tr/td/th/caption 等
        // 保留 class 与 style，但 style 值随后会做危险片段校验
        sl.addAttributes(":all", "class", "style");
        return sl;
    }

    /**
     * 净化富文本 HTML
     *
     * @param html 原始内容（可能包含恶意脚本）
     * @return 净化后的安全 HTML（null 原样返回）
     */
    public static String sanitize(String html) {
        if (html == null) {
            return null;
        }
        if (html.isBlank()) {
            return "";
        }
        // 第一步：白名单过滤标签/属性
        String cleaned = Jsoup.clean(html, "", SAFELIST);
        // 第二步：属性级二次校验（协议 + 危险样式）
        Document doc = Jsoup.parseBodyFragment(cleaned, "");
        for (Element el : doc.getAllElements()) {
            for (Attribute attr : el.attributes().asList()) {
                String key = attr.getKey().toLowerCase();
                String value = attr.getValue().trim();
                if (value.isEmpty()) {
                    el.removeAttr(attr.getKey());
                    continue;
                }
                // 协议白名单：href/src 仅允许 http/https（img 额外允许 data:image 内联图）
                if ("href".equals(key) || "src".equals(key)) {
                    String lower = value.toLowerCase();
                    boolean allowed = lower.startsWith("http://") || lower.startsWith("https://")
                            || lower.startsWith("//")
                            || ("src".equals(key) && lower.startsWith("data:image/"));
                    if (!allowed) {
                        el.removeAttr(attr.getKey());
                    }
                    continue;
                }
                // 危险样式：剔除包含脚本/CSS 注入片段的 style
                if ("style".equals(key)) {
                    String lower = value.toLowerCase();
                    if (lower.contains("expression") || lower.contains("url(")
                            || lower.contains("@import") || lower.contains("behavior")
                            || lower.contains("-moz-binding") || lower.contains("position:fixed")) {
                        el.removeAttr(attr.getKey());
                    }
                }
            }
        }
        return doc.body().html();
    }
}
