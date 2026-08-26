package com.company.devplatform.module.release.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.company.devplatform.common.Result;
import com.company.devplatform.module.release.dto.ReleaseRecordCreateDTO;
import com.company.devplatform.module.release.dto.ReleaseRecordUpdateDTO;
import com.company.devplatform.module.release.service.ReleaseRecordService;
import com.company.devplatform.module.release.vo.ReleaseRecordExcelVO;
import com.company.devplatform.module.release.vo.ReleaseRecordVO;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 版本发布记录管理
 */
@RestController
@RequestMapping("/api/release/records")
@RequiredArgsConstructor
public class ReleaseRecordController {

    private final ReleaseRecordService recordService;

    /** 分页查询（sonic:view） */
    @GetMapping
    @SaCheckPermission("sonic:view")
    public Result<IPage<ReleaseRecordVO>> page(@RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "10") int size,
                                               @RequestParam(required = false) String branch,
                                               @RequestParam(required = false) Integer result,
                                               @RequestParam(required = false) Integer source,
                                               @RequestParam(required = false) Long projectId,
                                               @RequestParam(required = false) String startTime,
                                               @RequestParam(required = false) String endTime) {
        return Result.ok(recordService.page(page, size, branch, result, source, projectId,
                parseTime(startTime), parseTime(endTime)));
    }

    /** 查看原始测试报告文件（失败任务追踪入口，sonic:view） */
    @GetMapping("/reports/{fileId}/content")
    @SaCheckPermission("sonic:view")
    public void reportContent(@PathVariable Long fileId, HttpServletResponse response) {
        recordService.outputReportContent(fileId, response);
    }

    /** 分支下拉（sonic:view） */
    @GetMapping("/branches")
    @SaCheckPermission("sonic:view")
    public Result<List<String>> branches() {
        return Result.ok(recordService.listBranches());
    }

    /** 导出 Excel（sonic:view） */
    @GetMapping("/export")
    @SaCheckPermission("sonic:view")
    public void export(HttpServletResponse response,
                       @RequestParam(required = false) String branch,
                       @RequestParam(required = false) Integer result,
                       @RequestParam(required = false) Integer source,
                       @RequestParam(required = false) Long projectId,
                       @RequestParam(required = false) String startTime,
                       @RequestParam(required = false) String endTime) throws IOException {
        String fileName = "发布记录_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + URLEncoder.encode(fileName, StandardCharsets.UTF_8) + "\"");
        List<ReleaseRecordExcelVO> data = recordService.exportRecords(branch, result, source, projectId,
                parseTime(startTime), parseTime(endTime));
        EasyExcel.write(response.getOutputStream(), ReleaseRecordExcelVO.class)
                .sheet("发布记录")
                .doWrite(data);
    }

    private LocalDateTime parseTime(String time) {
        if (time == null || time.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(time, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /** 详情（sonic:view） */
    @GetMapping("/{id}")
    @SaCheckPermission("sonic:view")
    public Result<ReleaseRecordVO> detail(@PathVariable Long id) {
        return Result.ok(recordService.detail(id));
    }

    /** 手动创建（sonic:edit） */
    @PostMapping
    @SaCheckPermission("sonic:edit")
    public Result<Long> create(@Valid @RequestBody ReleaseRecordCreateDTO dto) {
        return Result.ok(recordService.createManual(dto));
    }

    /** 编辑（sonic:edit） */
    @PutMapping
    @SaCheckPermission("sonic:edit")
    public Result<Void> update(@Valid @RequestBody ReleaseRecordUpdateDTO dto) {
        recordService.update(dto);
        return Result.ok();
    }

    /** 删除（sonic:edit） */
    @DeleteMapping("/{id}")
    @SaCheckPermission("sonic:edit")
    public Result<Void> delete(@PathVariable Long id) {
        recordService.delete(id);
        return Result.ok();
    }
}
