package com.company.devplatform.module.wiki.service;

import com.company.devplatform.module.wiki.dto.WikiDocDTO;
import com.company.devplatform.module.wiki.vo.WikiDocDetailVO;
import com.company.devplatform.module.wiki.vo.WikiDocNodeVO;

import java.util.List;

/**
 * Wiki 文档服务
 */
public interface WikiDocService {

    /** 目录树（一次性返回全部节点,前端构建树） */
    List<WikiDocNodeVO> tree();

    /** 文档详情 */
    WikiDocDetailVO detail(Long id);

    /** 新建文档,返回文档ID */
    Long create(WikiDocDTO dto);

    /** 更新文档 */
    void update(Long id, WikiDocDTO dto);

    /** 删除文档（级联删除全部子孙文档,并解除其关联附件） */
    void delete(Long id);
}
