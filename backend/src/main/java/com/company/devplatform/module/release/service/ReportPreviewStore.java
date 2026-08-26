package com.company.devplatform.module.release.service;

import com.company.devplatform.module.release.vo.ReportPreviewVO;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 报告预览令牌缓存
 * <p>上传解析后生成预览令牌，确认入库时凭令牌取回解析数据；
 * 令牌有效期 30 分钟，超时自动清理，防止孤儿内存累积。</p>
 */
@Slf4j
@Component
public class ReportPreviewStore {

    /** 有效期 30 分钟 */
    private static final long TTL_MILLIS = 30 * 60 * 1000L;

    private final Map<String, Entry> store = new ConcurrentHashMap<>();
    private final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

    @PostConstruct
    public void init() {
        scheduler.setThreadNamePrefix("preview-cleaner-");
        scheduler.initialize();
        scheduler.scheduleWithFixedDelay(this::cleanExpired, 5 * 60 * 1000L);
    }

    @PreDestroy
    public void destroy() {
        scheduler.shutdown();
    }

    /**
     * 存入预览数据与已落盘文件信息，返回令牌
     */
    public String put(ReportPreviewVO preview, FileStorageService.StoredFile storedFile) {
        String token = UUID.randomUUID().toString().replace("-", "");
        store.put(token, new Entry(preview, storedFile, System.currentTimeMillis() + TTL_MILLIS));
        return token;
    }

    /**
     * 原子取回并移除预览数据（含已落盘文件信息）
     */
    public Entry take(String token) {
        return store.remove(token);
    }

    private void cleanExpired() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Entry>> it = store.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Entry> e = it.next();
            if (e.getValue().expireAt < now) {
                it.remove();
            }
        }
    }

    /** 预览缓存条目 */
    @Data
    public static class Entry {
        private final ReportPreviewVO preview;
        private final FileStorageService.StoredFile storedFile;
        private final long expireAt;

        public Entry(ReportPreviewVO preview, FileStorageService.StoredFile storedFile, long expireAt) {
            this.preview = preview;
            this.storedFile = storedFile;
            this.expireAt = expireAt;
        }
    }
}
