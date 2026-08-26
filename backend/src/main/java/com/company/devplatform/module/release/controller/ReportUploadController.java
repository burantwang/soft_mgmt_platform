package com.company.devplatform.module.release.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.company.devplatform.common.Result;
import com.company.devplatform.module.release.dto.ReportConfirmDTO;
import com.company.devplatform.module.release.service.ReleaseRecordService;
import com.company.devplatform.module.release.vo.ReportPreviewVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 测试报告上传解析（sonic:edit）
 */
@RestController
@RequestMapping("/api/release/report")
@RequiredArgsConstructor
public class ReportUploadController {

    private final ReleaseRecordService recordService;

    /** 上传报告并解析预览 */
    @PostMapping("/preview")
    @SaCheckPermission("sonic:edit")
    public Result<ReportPreviewVO> preview(@RequestParam("file") MultipartFile file) {
        return Result.ok(recordService.previewReport(file));
    }

    /** 预览确认入库（补录分支/机型） */
    @PostMapping("/confirm")
    @SaCheckPermission("sonic:edit")
    public Result<Long> confirm(@Valid @RequestBody ReportConfirmDTO dto) {
        return Result.ok(recordService.confirmReport(dto));
    }
}
