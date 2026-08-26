package com.company.devplatform.module.release.controller;

import com.company.devplatform.common.ErrorCode;
import com.company.devplatform.common.Result;
import com.company.devplatform.common.exception.BusinessException;
import com.company.devplatform.module.release.dto.JenkinsPushDTO;
import com.company.devplatform.module.release.service.FileStorageService;
import com.company.devplatform.module.release.service.ReleaseRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

/**
 * Jenkins 开放接口（无需登录，API Token 鉴权）
 * <p>支持两种模式：</p>
 * <ol>
 *   <li>multipart：上传 pytest-html 报告文件 + branch/projectCodes 等表单参数，解析后直接入库</li>
 *   <li>JSON：直接推送用例统计数字</li>
 * </ol>
 */
@Slf4j
@RestController
@RequestMapping("/api/open/jenkins")
@RequiredArgsConstructor
public class JenkinsPushController {

    private final ReleaseRecordService recordService;
    private final FileStorageService fileStorageService;

    @Value("${api.push.token:}")
    private String pushToken;

    /** 模式一：携带报告文件（pytest-html），解析后直接入库 */
    @PostMapping(value = "/push", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Long> pushWithReport(@RequestHeader(value = "X-Api-Token", required = false) String token,
                                       @RequestParam("branch") String branch,
                                       @RequestParam(value = "version", required = false) String version,
                                       @RequestParam("projectCodes") String projectCodes,
                                       @RequestParam(value = "remark", required = false) String remark,
                                       @RequestPart("file") MultipartFile file) {
        checkToken(token);
        if (!StringUtils.hasText(branch)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "branch 不能为空");
        }
        List<String> codes = splitCodes(projectCodes);
        if (codes.isEmpty()) {
            throw new BusinessException(ErrorCode.PROJECT_REQUIRED);
        }
        FileStorageService.StoredFile stored = fileStorageService.storeReportFile(file);
        try {
            Long id = recordService.createFromJenkinsWithReport(branch.trim(), version, codes, remark, stored);
            log.info("[Jenkins] 推送入库成功 recordId={}, branch={}", id, branch);
            return Result.ok(id);
        } catch (BusinessException e) {
            fileStorageService.deletePhysical(stored);
            throw e;
        } catch (Exception e) {
            fileStorageService.deletePhysical(stored);
            log.error("[Jenkins] 推送入库失败 branch={}", branch, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "推送解析失败，请检查报告格式");
        }
    }

    /** 模式二：JSON 推送统计数字，直接入库 */
    @PostMapping(value = "/push", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Result<Long> pushJson(@RequestHeader(value = "X-Api-Token", required = false) String token,
                                 @Valid @RequestBody JenkinsPushDTO dto) {
        checkToken(token);
        Long id = recordService.createFromJenkinsJson(dto);
        log.info("[Jenkins] JSON 推送入库成功 recordId={}, branch={}", id, dto.getBranch());
        return Result.ok(id);
    }

    private void checkToken(String token) {
        if (!StringUtils.hasText(pushToken) || !pushToken.equals(token)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
    }

    private List<String> splitCodes(String projectCodes) {
        if (!StringUtils.hasText(projectCodes)) {
            return List.of();
        }
        return Arrays.stream(projectCodes.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }
}
