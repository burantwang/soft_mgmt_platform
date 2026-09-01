package com.company.devplatform.module.portal.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.devplatform.common.ErrorCode;
import com.company.devplatform.common.exception.BusinessException;
import com.company.devplatform.module.portal.dto.CategoryDTO;
import com.company.devplatform.module.portal.dto.LinkDTO;
import com.company.devplatform.module.portal.dto.SortDTO;
import com.company.devplatform.module.portal.dto.StatusDTO;
import com.company.devplatform.module.portal.entity.PortalCategory;
import com.company.devplatform.module.portal.entity.PortalLink;
import com.company.devplatform.module.portal.mapper.PortalCategoryMapper;
import com.company.devplatform.module.portal.mapper.PortalLinkMapper;
import com.company.devplatform.module.portal.service.PortalService;
import com.company.devplatform.module.portal.vo.PortalCategoryVO;
import com.company.devplatform.module.portal.vo.PortalLinkVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统门户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortalServiceImpl implements PortalService {

    private static final String DEFAULT_ICON = "Menu";
    private static final String DEFAULT_COLOR = "#409EFF";

    private final PortalCategoryMapper categoryMapper;
    private final PortalLinkMapper linkMapper;

    @Override
    public List<PortalCategoryVO> overview() {
        List<PortalCategory> categories = categoryMapper.selectList(new LambdaQueryWrapper<PortalCategory>()
                .eq(PortalCategory::getStatus, 1)
                .orderByAsc(PortalCategory::getSort)
                .orderByAsc(PortalCategory::getId));
        List<PortalLink> links = linkMapper.selectList(new LambdaQueryWrapper<PortalLink>()
                .eq(PortalLink::getStatus, 1)
                .orderByAsc(PortalLink::getSort)
                .orderByAsc(PortalLink::getId));
        return groupByCategory(categories, links, false);
    }

    @Override
    public List<PortalCategoryVO> listCategories() {
        List<PortalCategory> categories = categoryMapper.selectList(new LambdaQueryWrapper<PortalCategory>()
                .orderByAsc(PortalCategory::getSort)
                .orderByAsc(PortalCategory::getId));
        List<PortalLink> links = linkMapper.selectList(new LambdaQueryWrapper<PortalLink>()
                .orderByAsc(PortalLink::getSort)
                .orderByAsc(PortalLink::getId));
        return groupByCategory(categories, links, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCategory(CategoryDTO dto) {
        checkCategoryNameUnique(dto.getCategoryName(), null);
        PortalCategory c = new PortalCategory();
        c.setCategoryName(dto.getCategoryName().trim());
        c.setIcon(StringUtils.hasText(dto.getIcon()) ? dto.getIcon() : DEFAULT_ICON);
        c.setColor(StringUtils.hasText(dto.getColor()) ? dto.getColor() : DEFAULT_COLOR);
        c.setDescription(dto.getDescription());
        c.setSort(resolveCategorySort());
        c.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        c.setCreatorId(currentUserId());
        categoryMapper.insert(c);
        log.info("[Portal] 新增板块 id={}, name={}", c.getId(), c.getCategoryName());
        return c.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCategory(Long id, CategoryDTO dto) {
        PortalCategory c = mustExistCategory(id);
        checkCategoryNameUnique(dto.getCategoryName(), id);
        c.setCategoryName(dto.getCategoryName().trim());
        c.setIcon(StringUtils.hasText(dto.getIcon()) ? dto.getIcon() : DEFAULT_ICON);
        c.setColor(StringUtils.hasText(dto.getColor()) ? dto.getColor() : DEFAULT_COLOR);
        c.setDescription(dto.getDescription());
        if (dto.getSort() != null) {
            c.setSort(dto.getSort());
        }
        if (dto.getStatus() != null) {
            c.setStatus(dto.getStatus());
        }
        categoryMapper.updateById(c);
        log.info("[Portal] 更新板块 id={}, name={}", id, c.getCategoryName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long id) {
        mustExistCategory(id);
        Long linkCount = linkMapper.selectCount(new LambdaQueryWrapper<PortalLink>().eq(PortalLink::getCategoryId, id));
        if (linkCount != null && linkCount > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该板块下存在 " + linkCount + " 个系统链接，请先删除或移出后再删除板块");
        }
        categoryMapper.deleteById(id);
        log.info("[Portal] 删除板块 id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleCategoryStatus(StatusDTO dto) {
        PortalCategory c = mustExistCategory(dto.getId());
        c.setStatus(dto.getStatus());
        categoryMapper.updateById(c);
        log.info("[Portal] 板块启停 id={}, status={}", dto.getId(), dto.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sortCategories(SortDTO dto) {
        for (SortDTO.SortItem item : dto.getItems()) {
            PortalCategory c = mustExistCategory(item.getId());
            c.setSort(item.getSort());
            categoryMapper.updateById(c);
        }
        log.info("[Portal] 板块排序调整, 数量={}", dto.getItems().size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createLink(LinkDTO dto) {
        mustExistCategory(dto.getCategoryId());
        checkLinkNameUnique(dto.getLinkName(), dto.getCategoryId(), null);
        PortalLink l = new PortalLink();
        l.setCategoryId(dto.getCategoryId());
        l.setLinkName(dto.getLinkName().trim());
        l.setUrl(dto.getUrl().trim());
        l.setDescription(dto.getDescription());
        l.setIcon(StringUtils.hasText(dto.getIcon()) ? dto.getIcon() : DEFAULT_ICON);
        l.setColor(StringUtils.hasText(dto.getColor()) ? dto.getColor() : DEFAULT_COLOR);
        l.setSort(resolveLinkSort(dto.getCategoryId()));
        l.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        l.setCreatorId(currentUserId());
        linkMapper.insert(l);
        log.info("[Portal] 新增链接 id={}, name={}, categoryId={}", l.getId(), l.getLinkName(), dto.getCategoryId());
        return l.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLink(Long id, LinkDTO dto) {
        PortalLink l = mustExistLink(id);
        if (dto.getCategoryId() != null && !dto.getCategoryId().equals(l.getCategoryId())) {
            mustExistCategory(dto.getCategoryId());
            l.setCategoryId(dto.getCategoryId());
        }
        checkLinkNameUnique(dto.getLinkName(), l.getCategoryId(), id);
        l.setLinkName(dto.getLinkName().trim());
        l.setUrl(dto.getUrl().trim());
        l.setDescription(dto.getDescription());
        l.setIcon(StringUtils.hasText(dto.getIcon()) ? dto.getIcon() : DEFAULT_ICON);
        l.setColor(StringUtils.hasText(dto.getColor()) ? dto.getColor() : DEFAULT_COLOR);
        if (dto.getSort() != null) {
            l.setSort(dto.getSort());
        }
        if (dto.getStatus() != null) {
            l.setStatus(dto.getStatus());
        }
        linkMapper.updateById(l);
        log.info("[Portal] 更新链接 id={}, name={}", id, l.getLinkName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteLink(Long id) {
        mustExistLink(id);
        linkMapper.deleteById(id);
        log.info("[Portal] 删除链接 id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleLinkStatus(StatusDTO dto) {
        PortalLink l = mustExistLink(dto.getId());
        l.setStatus(dto.getStatus());
        linkMapper.updateById(l);
        log.info("[Portal] 链接启停 id={}, status={}", dto.getId(), dto.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sortLinks(SortDTO dto) {
        for (SortDTO.SortItem item : dto.getItems()) {
            PortalLink l = mustExistLink(item.getId());
            l.setSort(item.getSort());
            linkMapper.updateById(l);
        }
        log.info("[Portal] 链接排序调整, 数量={}", dto.getItems().size());
    }

    // ---------------- 私有方法 ----------------

    /** 板块分组组装(links 可为空或仅含启用项；includeDisabled 控制是否返回停用板块/链接) */
    private List<PortalCategoryVO> groupByCategory(List<PortalCategory> categories, List<PortalLink> links, boolean includeDisabled) {
        Map<Long, List<PortalLink>> byCategory = new HashMap<>();
        for (PortalLink l : links) {
            byCategory.computeIfAbsent(l.getCategoryId(), k -> new ArrayList<>()).add(l);
        }
        List<PortalCategoryVO> result = new ArrayList<>();
        for (PortalCategory c : categories) {
            if (!includeDisabled && c.getStatus() != null && c.getStatus() != 1) {
                continue;
            }
            PortalCategoryVO vo = new PortalCategoryVO();
            vo.setId(c.getId());
            vo.setCategoryName(c.getCategoryName());
            vo.setIcon(c.getIcon());
            vo.setColor(c.getColor());
            vo.setDescription(c.getDescription());
            vo.setSort(c.getSort());
            vo.setStatus(c.getStatus());
            List<PortalLink> catLinks = byCategory.getOrDefault(c.getId(), new ArrayList<>());
            vo.setLinkCount(catLinks.size());
            for (PortalLink l : catLinks) {
                if (!includeDisabled && l.getStatus() != null && l.getStatus() != 1) {
                    continue;
                }
                vo.getLinks().add(toLinkVO(l));
            }
            result.add(vo);
        }
        return result;
    }

    private PortalLinkVO toLinkVO(PortalLink l) {
        PortalLinkVO vo = new PortalLinkVO();
        vo.setId(l.getId());
        vo.setCategoryId(l.getCategoryId());
        vo.setLinkName(l.getLinkName());
        vo.setUrl(l.getUrl());
        vo.setDescription(l.getDescription());
        vo.setIcon(l.getIcon());
        vo.setColor(l.getColor());
        vo.setSort(l.getSort());
        vo.setStatus(l.getStatus());
        return vo;
    }

    private PortalCategory mustExistCategory(Long id) {
        PortalCategory c = categoryMapper.selectById(id);
        if (c == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "板块不存在或已被删除");
        }
        return c;
    }

    private PortalLink mustExistLink(Long id) {
        PortalLink l = linkMapper.selectById(id);
        if (l == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "系统链接不存在或已被删除");
        }
        return l;
    }

    private void checkCategoryNameUnique(String name, Long excludeId) {
        LambdaQueryWrapper<PortalCategory> qw = new LambdaQueryWrapper<PortalCategory>()
                .eq(PortalCategory::getCategoryName, name.trim());
        if (excludeId != null) {
            qw.ne(PortalCategory::getId, excludeId);
        }
        Long count = categoryMapper.selectCount(qw);
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.DATA_EXIST, "已存在同名板块");
        }
    }

    private void checkLinkNameUnique(String name, Long categoryId, Long excludeId) {
        LambdaQueryWrapper<PortalLink> qw = new LambdaQueryWrapper<PortalLink>()
                .eq(PortalLink::getLinkName, name.trim())
                .eq(PortalLink::getCategoryId, categoryId);
        if (excludeId != null) {
            qw.ne(PortalLink::getId, excludeId);
        }
        Long count = linkMapper.selectCount(qw);
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.DATA_EXIST, "该板块下已存在同名系统");
        }
    }

    private Integer resolveCategorySort() {
        List<PortalCategory> list = categoryMapper.selectList(new LambdaQueryWrapper<PortalCategory>()
                .orderByDesc(PortalCategory::getSort));
        if (list.isEmpty()) {
            return 0;
        }
        Integer max = list.get(0).getSort();
        return max == null ? 0 : max + 1;
    }

    private Integer resolveLinkSort(Long categoryId) {
        List<PortalLink> list = linkMapper.selectList(new LambdaQueryWrapper<PortalLink>()
                .eq(PortalLink::getCategoryId, categoryId)
                .orderByDesc(PortalLink::getSort));
        if (list.isEmpty()) {
            return 0;
        }
        Integer max = list.get(0).getSort();
        return max == null ? 0 : max + 1;
    }

    private Long currentUserId() {
        return StpUtil.getLoginIdAsLong();
    }
}
