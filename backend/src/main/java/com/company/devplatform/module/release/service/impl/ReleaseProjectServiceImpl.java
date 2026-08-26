package com.company.devplatform.module.release.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.devplatform.common.ErrorCode;
import com.company.devplatform.common.exception.BusinessException;
import com.company.devplatform.module.release.dto.ReleaseProjectDTO;
import com.company.devplatform.module.release.entity.ReleaseProject;
import com.company.devplatform.module.release.entity.ReleaseRecordProject;
import com.company.devplatform.module.release.mapper.ReleaseProjectMapper;
import com.company.devplatform.module.release.mapper.ReleaseRecordProjectMapper;
import com.company.devplatform.module.release.service.ReleaseProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 项目(机型)服务实现
 */
@Service
@RequiredArgsConstructor
public class ReleaseProjectServiceImpl implements ReleaseProjectService {

    private final ReleaseProjectMapper projectMapper;
    private final ReleaseRecordProjectMapper recordProjectMapper;

    @Override
    public IPage<ReleaseProject> page(int page, int size, String keyword, Integer status) {
        LambdaQueryWrapper<ReleaseProject> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(ReleaseProject::getProjectName, keyword)
                    .or().like(ReleaseProject::getProjectCode, keyword));
        }
        if (status != null) {
            wrapper.eq(ReleaseProject::getStatus, status);
        }
        wrapper.orderByDesc(ReleaseProject::getCreateTime);
        return projectMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public List<ReleaseProject> listEnabled() {
        return projectMapper.selectList(new LambdaQueryWrapper<ReleaseProject>()
                .eq(ReleaseProject::getStatus, 1)
                .orderByAsc(ReleaseProject::getProjectName));
    }

    @Override
    public List<ReleaseProject> listAll() {
        return projectMapper.selectList(new LambdaQueryWrapper<ReleaseProject>()
                .orderByAsc(ReleaseProject::getProjectName));
    }

    @Override
    public ReleaseProject create(ReleaseProjectDTO dto) {
        checkUnique(dto, null);
        ReleaseProject entity = new ReleaseProject();
        entity.setProjectName(dto.getProjectName().trim());
        entity.setProjectCode(dto.getProjectCode().trim());
        entity.setDescription(dto.getDescription());
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        projectMapper.insert(entity);
        return entity;
    }

    @Override
    public ReleaseProject update(ReleaseProjectDTO dto) {
        ReleaseProject existing = projectMapper.selectById(dto.getId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND);
        }
        checkUnique(dto, dto.getId());
        existing.setProjectName(dto.getProjectName().trim());
        existing.setProjectCode(dto.getProjectCode().trim());
        existing.setDescription(dto.getDescription());
        if (dto.getStatus() != null) {
            existing.setStatus(dto.getStatus());
        }
        projectMapper.updateById(existing);
        return existing;
    }

    @Override
    public void delete(Long id) {
        ReleaseProject existing = projectMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND);
        }
        // 已被发布记录引用时禁止删除
        Long refCount = recordProjectMapper.selectCount(new LambdaQueryWrapper<ReleaseRecordProject>()
                .eq(ReleaseRecordProject::getProjectId, id));
        if (refCount != null && refCount > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该机型已被发布记录引用，无法删除");
        }
        // 先释放名称/编码唯一键，再逻辑删除
        projectMapper.releaseUniqueKeys(id);
        projectMapper.deleteById(id);
    }

    /** 名称与编码唯一性校验（逻辑删除范围） */
    private void checkUnique(ReleaseProjectDTO dto, Long excludeId) {
        if (existsByName(dto.getProjectName(), excludeId)) {
            throw new BusinessException(ErrorCode.DATA_EXIST, "项目名称已存在");
        }
        if (existsByCode(dto.getProjectCode(), excludeId)) {
            throw new BusinessException(ErrorCode.DATA_EXIST, "项目编码已存在");
        }
    }

    private boolean existsByName(String name, Long excludeId) {
        if (name == null || name.isBlank()) {
            return false;
        }
        LambdaQueryWrapper<ReleaseProject> wrapper = new LambdaQueryWrapper<ReleaseProject>()
                .eq(ReleaseProject::getProjectName, name.trim());
        if (excludeId != null) {
            wrapper.ne(ReleaseProject::getId, excludeId);
        }
        Long count = projectMapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    private boolean existsByCode(String code, Long excludeId) {
        if (code == null || code.isBlank()) {
            return false;
        }
        LambdaQueryWrapper<ReleaseProject> wrapper = new LambdaQueryWrapper<ReleaseProject>()
                .eq(ReleaseProject::getProjectCode, code.trim());
        if (excludeId != null) {
            wrapper.ne(ReleaseProject::getId, excludeId);
        }
        Long count = projectMapper.selectCount(wrapper);
        return count != null && count > 0;
    }
}
