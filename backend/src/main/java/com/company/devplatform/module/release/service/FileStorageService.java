package com.company.devplatform.module.release.service;

import com.company.devplatform.common.ErrorCode;
import com.company.devplatform.common.exception.BusinessException;
import com.company.devplatform.config.StorageProperties;
import com.company.devplatform.module.release.entity.FileResource;
import com.company.devplatform.module.release.enums.FileType;
import com.company.devplatform.module.release.mapper.FileResourceMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

/**
 * 文件存储服务
 * <p>约定：数据库仅保存相对路径，根路径来自配置 {@code file.storage.root}（环境变量注入）；
 * 所有路径解析均做防目录穿越校验；逻辑删除记录时同步清理物理文件。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private static final DateTimeFormatter DATE_DIR = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final FileResourceMapper fileResourceMapper;
    private final StorageProperties storageProperties;

    /**
     * 保存测试报告文件（写盘 + 入库）
     *
     * @param file       上传文件
     * @param uploaderId 上传人
     * @return 文件资源记录
     */
    public FileResource saveReport(MultipartFile file, Long uploaderId) {
        StoredFile stored = storeReportFile(file);
        return register(stored, uploaderId, FileType.REPORT);
    }

    /**
     * 测试报告文件仅写盘（预览阶段使用，未入库）
     *
     * @param file 上传文件
     * @return 存储信息
     */
    public StoredFile storeReportFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_EMPTY);
        }
        String ext = extractExt(file.getOriginalFilename());
        if (!"html".equals(ext) && !"htm".equals(ext)) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED, "仅支持 .html 格式的测试报告");
        }
        String storedPath = storageProperties.getReportDir() + "/"
                + LocalDate.now().format(DATE_DIR) + "/"
                + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8) + "." + ext;
        Path absolute = resolveAbsolutePath(storedPath);
        try {
            Files.createDirectories(absolute.getParent());
            try (var in = file.getInputStream()) {
                Files.copy(in, absolute, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.error("[存储] 测试报告保存失败: {}", storedPath, e);
            throw new BusinessException(ErrorCode.FILE_SAVE_ERROR);
        }
        String mime = file.getContentType();
        return new StoredFile(file.getOriginalFilename(), storedPath, ext,
                mime == null || mime.isEmpty() ? "text/html" : mime, file.getSize());
    }

    /**
     * 登记文件资源记录（预览确认后 / Jenkins 推送后调用）
     *
     * @param stored     已落盘的存储信息
     * @param uploaderId 上传人
     * @param fileType   文件类型
     * @return 文件资源记录
     */
    public FileResource register(StoredFile stored, Long uploaderId, FileType fileType) {
        FileResource resource = new FileResource();
        resource.setFileName(stored.getFileName());
        resource.setStoredPath(stored.getStoredPath());
        resource.setFileExt(stored.getFileExt());
        resource.setMimeType(stored.getMimeType());
        resource.setFileSize(stored.getFileSize());
        resource.setFileType(fileType.getCode());
        resource.setUploaderId(uploaderId);
        fileResourceMapper.insert(resource);
        log.info("[存储] 文件资源已登记 id={}, path={}", resource.getId(), stored.getStoredPath());
        return resource;
    }

    /** 删除已落盘但未入库的临时文件 */
    public void deletePhysical(StoredFile stored) {
        if (stored == null) {
            return;
        }
        try {
            Path absolute = resolveAbsolutePath(stored.getStoredPath());
            Files.deleteIfExists(absolute);
        } catch (Exception e) {
            log.warn("[存储] 临时文件清理失败: {}", stored.getStoredPath());
        }
    }

    /** 已落盘文件信息 */
    @Data
    public static class StoredFile {
        private final String fileName;
        private final String storedPath;
        private final String fileExt;
        private final String mimeType;
        private final long fileSize;

        public StoredFile(String fileName, String storedPath, String fileExt, String mimeType, long fileSize) {
            this.fileName = fileName;
            this.storedPath = storedPath;
            this.fileExt = fileExt;
            this.mimeType = mimeType;
            this.fileSize = fileSize;
        }
    }

    /**
     * 逻辑删除文件记录并清理物理文件（物理 + 逻辑联动）
     *
     * @param fileId 文件资源ID
     */
    public void delete(Long fileId) {
        if (fileId == null) {
            return;
        }
        FileResource resource = fileResourceMapper.selectById(fileId);
        if (resource == null) {
            return;
        }
        fileResourceMapper.deleteById(fileId);
        try {
            Path absolute = resolveAbsolutePath(resource.getStoredPath());
            Files.deleteIfExists(absolute);
        } catch (Exception e) {
            log.warn("[存储] 物理文件清理失败 fileId={}, path={}", fileId, resource.getStoredPath());
        }
    }

    /**
     * 根据相对路径解析绝对路径（防目录穿越）
     *
     * @param storedPath 存储相对路径
     * @return 规范化后的绝对路径
     */
    public Path resolveAbsolutePath(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            throw new BusinessException(ErrorCode.FILE_READ_ERROR, "文件路径为空");
        }
        Path root = rootPath();
        Path target = root.resolve(storedPath).normalize();
        if (!target.startsWith(root)) {
            log.warn("[存储] 检测到路径穿越风险: {}", storedPath);
            throw new BusinessException(ErrorCode.FILE_READ_ERROR, "非法文件路径");
        }
        return target;
    }

    /** 存储根路径（绝对路径） */
    private Path rootPath() {
        return Paths.get(storageProperties.getRoot()).toAbsolutePath().normalize();
    }

    private String extractExt(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
