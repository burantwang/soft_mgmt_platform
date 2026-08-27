package com.company.devplatform.module.wiki.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.company.devplatform.common.Result;
import com.company.devplatform.module.wiki.dto.FileRenameDTO;
import com.company.devplatform.module.wiki.service.FileResourceService;
import com.company.devplatform.module.wiki.vo.FileResourceVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 文件资源接口（阶段5）
 */
@RestController
@RequestMapping("/api/wiki/files")
@RequiredArgsConstructor
public class FileResourceController {

    private final FileResourceService fileResourceService;

    /** 分页查询文件资源 */
    @GetMapping
    @SaCheckPermission("wiki:view")
    public Result<IPage<FileResourceVO>> page(@RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) Integer fileType,
                                              @RequestParam(required = false) Long docId) {
        return Result.ok(fileResourceService.page(keyword, fileType, docId, page, size));
    }

    /** 上传普通附件 */
    @PostMapping("/upload")
    @SaCheckPermission("wiki:edit")
    public Result<FileResourceVO> upload(@RequestParam("file") MultipartFile file,
                                         @RequestParam(required = false) Long docId) {
        return Result.ok(fileResourceService.upload(file, StpUtil.getLoginIdAsLong(), docId));
    }

    /** 重命名（仅允许修改主名,扩展名必须与原文件一致） */
    @PutMapping("/rename")
    @SaCheckPermission("wiki:edit")
    public Result<Void> rename(@Valid @RequestBody FileRenameDTO dto) {
        fileResourceService.rename(dto);
        return Result.ok();
    }

    /** 下载 */
    @GetMapping("/{id}/download")
    @SaCheckPermission("wiki:view")
    public ResponseEntity<InputStreamResource> download(@PathVariable Long id) {
        FileResourceService.DownloadResult result = fileResourceService.download(id);
        String encoded = URLEncoder.encode(result.fileName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded)
                .contentType(MediaType.parseMediaType(result.mimeType() == null || result.mimeType().isBlank()
                        ? "application/octet-stream" : result.mimeType()))
                .contentLength(result.size())
                .body(new InputStreamResource(result.inputStream()));
    }

    /** 删除（逻辑删除记录 + 清理物理文件） */
    @DeleteMapping("/{id}")
    @SaCheckPermission("wiki:edit")
    public Result<Void> delete(@PathVariable Long id) {
        fileResourceService.delete(id);
        return Result.ok();
    }
}
