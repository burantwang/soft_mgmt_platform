package com.company.devplatform.module.wiki.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.company.devplatform.module.wiki.dto.FileRenameDTO;
import com.company.devplatform.module.wiki.vo.FileResourceVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * 文件资源服务
 */
public interface FileResourceService {

    /** 分页查询文件资源（docId 非空时按关联文档过滤） */
    IPage<FileResourceVO> page(String keyword, Integer fileType, Long docId, int page, int size);

    /** 上传普通附件 */
    FileResourceVO upload(MultipartFile file, Long uploaderId, Long docId);

    /** 重命名（仅允许修改主名,扩展名必须与原文件一致） */
    void rename(FileRenameDTO dto);

    /** 下载（返回文件流,并累计下载次数） */
    DownloadResult download(Long id);

    /** 删除（逻辑删除记录 + 清理物理文件） */
    void delete(Long id);

    /** 下载结果 */
    record DownloadResult(InputStream inputStream, String fileName, String mimeType, long size) {
    }
}
