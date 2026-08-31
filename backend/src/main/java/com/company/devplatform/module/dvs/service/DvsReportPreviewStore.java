package com.company.devplatform.module.dvs.service;

import com.company.devplatform.module.release.service.FileStorageService;
import com.company.devplatform.module.dvs.vo.DvsPreviewItemVO;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DVS 报告预览令牌缓存
 * <p>一次上传可包含多个 HTML 模块文件，全部解析后以列表形式缓存；
 * 确认入库时凭令牌取回全部解析数据与已落盘文件信息；令牌有效期 30 分钟，超时自动清理。</p>
 */
@Slf4j
@Component
public class DvsReportPreviewStore {

    /** 有效期 30 分钟 */
    private static final long TTL_MILLIS = 30 * 60 * 1000L;

    private final Map<String, Entry> store = new ConcurrentHashMap<>();
    private final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

    @PostConstruct
    public void init() {
        scheduler.setThreadNamePrefix("dvs-preview-cleaner-");
        scheduler.initialize();
        scheduler.scheduleWithFixedDelay(this::cleanExpired, 5 * 60 * 1000L);
    }

    @PreDestroy
    public void destroy() {
        scheduler.shutdown();
    }

    /**
     * 存入多个模块解析数据（每个模块含预览结果与已落盘文件信息），返回令牌
     */
    public String put(List<Item> items) {
        String token = UUID.randomUUID().toString().replace("-", "");
        store.put(token, new Entry(items, System.currentTimeMillis() + TTL_MILLIS));
        return token;
    }

    /**
     * 原子取回并移除预览数据（事务提交成功后调用）
     */
    public Entry remove(String token) {
        return store.remove(token);
    }

    /**
     * 只读查看预览数据（事务中使用，提交成功后再 remove，避免失败重试丢令牌）
     */
    public Entry peek(String token) {
        return store.get(token);
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

    /** 单模块预览缓存条目 */
    @Data
    public static class Item {
        private final DvsPreviewItemVO preview;
        private final FileStorageService.StoredFile storedFile;

        public Item(DvsPreviewItemVO preview, FileStorageService.StoredFile storedFile) {
            this.preview = preview;
            this.storedFile = storedFile;
        }
    }

    /** 预览缓存条目（含全部模块与过期时间） */
    @Data
    public static class Entry {
        private final List<Item> items;
        private final long expireAt;

        public Entry(List<Item> items, long expireAt) {
            this.items = items;
            this.expireAt = expireAt;
        }
    }
}
