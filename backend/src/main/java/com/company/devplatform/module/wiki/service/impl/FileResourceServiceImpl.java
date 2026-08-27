package com.company.devplatform.module.wiki.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.devplatform.common.ErrorCode;
import com.company.devplatform.common.exception.BusinessException;
import com.company.devplatform.module.auth.entity.SysUser;
import com.company.devplatform.module.auth.mapper.SysUserMapper;
import com.company.devplatform.module.release.entity.FileResource;
import com.company.devplatform.module.release.enums.FileType;
import com.company.devplatform.module.release.mapper.FileResourceMapper;
import com.company.devplatform.module.release.service.FileStorageService;
import com.company.devplatform.module.wiki.dto.FileRenameDTO;
import com.company.devplatform.module.wiki.entity.WikiDoc;
import com.company.devplatform.module.wiki.mapper.WikiDocMapper;
import com.company.devplatform.module.wiki.service.FileResourceService;
import com.company.devplatform.module.wiki.vo.FileResourceVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 文件资源服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileResourceServiceImpl implements FileResourceService {

    private final FileResourceMapper fileResourceMapper;
    private final FileStorageService fileStorageService;
    private final SysUserMapper sysUserMapper;
    private final WikiDocMapper wikiDocMapper;

    @Override
    public IPage<FileResourceVO> page(String keyword, Integer fileType, Long docId, int page, int size) {
        LambdaQueryWrapper<FileResource> qw = new LambdaQueryWrapper<FileResource>()
                .eq(fileType != null, FileResource::getFileType, fileType)
                .eq(docId != null, FileResource::getDocId, docId)
                .like(StringUtils.hasText(keyword), FileResource::getFileName, keyword == null ? null : keyword.trim())
                .orderByDesc(FileResource::getId);
        IPage<FileResource> p = fileResourceMapper.selectPage(new Page<>(page, size), qw);
        // 批量补充上传人与关联文档信息
        Set<Long> userIds = new HashSet<>();
        Set<Long> docIds = new HashSet<>();
        for (FileResource r : p.getRecords()) {
            if (r.getUploaderId() != null) {
                userIds.add(r.getUploaderId());
            }
            if (r.getDocId() != null) {
                docIds.add(r.getDocId());
            }
        }
        Map<Long, String> nickMap = loadNicknames(userIds);
        Map<Long, String> docTitleMap = loadDocTitles(docIds);
        return p.convert(r -> toVO(r, nickMap, docTitleMap));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileResourceVO upload(MultipartFile file, Long uploaderId, Long docId) {
        if (docId != null) {
            WikiDoc doc = wikiDocMapper.selectById(docId);
            if (doc == null) {
                throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "关联的Wiki文档不存在");
            }
        }
        FileResource resource = fileStorageService.saveAttachment(file, uploaderId, docId);
        log.info("[Wiki] 上传附件 id={}, name={}, size={}", resource.getId(), resource.getFileName(), resource.getFileSize());
        Map<Long, String> nickMap = loadNicknames(uploaderId == null ? Set.of() : Set.of(uploaderId));
        Map<Long, String> docTitleMap = loadDocTitles(docId == null ? Set.of() : Set.of(docId));
        return toVO(resource, nickMap, docTitleMap);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rename(FileRenameDTO dto) {
        FileResource resource = mustExist(dto.getFileId());
        String newName = dto.getNewName().trim();
        if (!StringUtils.hasText(newName)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件名不能为空");
        }
        String targetExt = extractExt(newName);
        String expectExt = resource.getFileExt() == null ? "" : resource.getFileExt().toLowerCase(Locale.ROOT);
        if (targetExt.isEmpty()) {
            // 未带后缀:自动补原后缀
            newName = newName + "." + expectExt;
        } else if (!targetExt.equalsIgnoreCase(expectExt)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "扩展名不允许修改,必须保持为 ." + expectExt);
        }
        // 检查同名
        Long count = fileResourceMapper.selectCount(new LambdaQueryWrapper<FileResource>()
                .eq(FileResource::getFileName, newName)
                .ne(FileResource::getId, resource.getId()));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.DATA_EXIST, "已存在同名文件");
        }
        resource.setFileName(newName);
        fileResourceMapper.updateById(resource);
        log.info("[Wiki] 重命名附件 id={}, newName={}", resource.getId(), newName);
    }

    @Override
    public DownloadResult download(Long id) {
        FileResource resource = mustExist(id);
        java.nio.file.Path absolute = fileStorageService.resolveAbsolutePath(resource.getStoredPath());
        if (!Files.exists(absolute)) {
            throw new BusinessException(ErrorCode.FILE_READ_ERROR, "物理文件不存在,可能已被清理");
        }
        // 累计下载次数
        fileResourceMapper.updateById(resource);
        resource.setDownloadCount((resource.getDownloadCount() == null ? 0 : resource.getDownloadCount()) + 1);
        fileResourceMapper.updateById(resource);
        try {
            InputStream in = Files.newInputStream(absolute);
            return new DownloadResult(in, resource.getFileName(), resource.getMimeType(), resource.getFileSize());
        } catch (IOException e) {
            log.error("[Wiki] 附件读取失败 id={}", id, e);
            throw new BusinessException(ErrorCode.FILE_READ_ERROR);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        fileStorageService.delete(id);
        log.info("[Wiki] 删除附件 id={}", id);
    }

    // ---------------- 私有方法 ----------------

    private FileResource mustExist(Long id) {
        FileResource r = fileResourceMapper.selectById(id);
        if (r == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "文件不存在或已被删除");
        }
        return r;
    }

    private FileResourceVO toVO(FileResource r, Map<Long, String> nickMap, Map<Long, String> docTitleMap) {
        FileResourceVO vo = new FileResourceVO();
        vo.setId(r.getId());
        vo.setFileName(r.getFileName());
        vo.setFileExt(r.getFileExt());
        vo.setMimeType(r.getMimeType());
        vo.setFileSize(r.getFileSize());
        vo.setSizeDesc(formatSize(r.getFileSize()));
        vo.setFileType(r.getFileType());
        FileType ft = FileType.of(r.getFileType());
        vo.setFileTypeDesc(ft == null ? "" : ft.getDesc());
        vo.setDocId(r.getDocId());
        vo.setDocTitle(docTitleMap.getOrDefault(r.getDocId(), ""));
        vo.setUploaderId(r.getUploaderId());
        vo.setUploaderName(nickMap.getOrDefault(r.getUploaderId(), ""));
        vo.setDownloadCount(r.getDownloadCount() == null ? 0 : r.getDownloadCount());
        vo.setCreateTime(r.getCreateTime());
        return vo;
    }

    private Map<Long, String> loadNicknames(Set<Long> userIds) {
        Map<Long, String> map = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return map;
        }
        List<Long> ids = userIds.stream().filter(Objects::nonNull).collect(Collectors.toList());
        if (ids.isEmpty()) {
            return map;
        }
        List<SysUser> users = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>().in(SysUser::getId, ids));
        for (SysUser u : users) {
            map.put(u.getId(), StringUtils.hasText(u.getNickname()) ? u.getNickname() : u.getUsername());
        }
        return map;
    }

    private Map<Long, String> loadDocTitles(Set<Long> docIds) {
        Map<Long, String> map = new HashMap<>();
        if (docIds == null || docIds.isEmpty()) {
            return map;
        }
        List<WikiDoc> docs = wikiDocMapper.selectList(new LambdaQueryWrapper<WikiDoc>().in(WikiDoc::getId, docIds));
        for (WikiDoc d : docs) {
            map.put(d.getId(), d.getTitle());
        }
        return map;
    }

    private String extractExt(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024L * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        }
        if (bytes < 1024L * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        }
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
