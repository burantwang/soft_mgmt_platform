package com.company.devplatform.module.release.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.company.devplatform.module.release.dto.ReleaseProjectDTO;
import com.company.devplatform.module.release.entity.ReleaseProject;

import java.util.List;

/**
 * 项目(机型)服务
 */
public interface ReleaseProjectService {

    /** 分页查询 */
    IPage<ReleaseProject> page(int page, int size, String keyword, Integer status);

    /** 启用的机型列表（下拉选择用） */
    List<ReleaseProject> listEnabled();

    /** 全部机型（含停用） */
    List<ReleaseProject> listAll();

    ReleaseProject create(ReleaseProjectDTO dto);

    ReleaseProject update(ReleaseProjectDTO dto);

    void delete(Long id);
}
