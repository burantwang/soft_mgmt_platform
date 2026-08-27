package com.company.devplatform.module.wiki.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.devplatform.common.ErrorCode;
import com.company.devplatform.common.exception.BusinessException;
import com.company.devplatform.common.util.HtmlSanitizer;
import com.company.devplatform.module.auth.entity.SysUser;
import com.company.devplatform.module.auth.mapper.SysUserMapper;
import com.company.devplatform.module.release.entity.FileResource;
import com.company.devplatform.module.release.mapper.FileResourceMapper;
import com.company.devplatform.module.wiki.dto.WikiDocDTO;
import com.company.devplatform.module.wiki.entity.WikiDoc;
import com.company.devplatform.module.wiki.mapper.WikiDocMapper;
import com.company.devplatform.module.wiki.service.WikiDocService;
import com.company.devplatform.module.wiki.vo.WikiDocDetailVO;
import com.company.devplatform.module.wiki.vo.WikiDocNodeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Wiki 文档服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WikiDocServiceImpl implements WikiDocService {

    private final WikiDocMapper wikiDocMapper;
    private final SysUserMapper sysUserMapper;
    private final FileResourceMapper fileResourceMapper;

    @Override
    public List<WikiDocNodeVO> tree() {
        List<WikiDoc> all = wikiDocMapper.selectList(new LambdaQueryWrapper<WikiDoc>()
                .orderByAsc(WikiDoc::getSort)
                .orderByAsc(WikiDoc::getId));
        if (all.isEmpty()) {
            return new ArrayList<>();
        }
        // 批量加载用户昵称
        Set<Long> userIds = new HashSet<>();
        for (WikiDoc d : all) {
            if (d.getEditorId() != null) {
                userIds.add(d.getEditorId());
            }
        }
        Map<Long, String> nickMap = loadNicknames(userIds);
        // 组装节点并按父ID分组（LinkedHashMap 保持排序）
        Map<Long, WikiDocNodeVO> nodeMap = new LinkedHashMap<>();
        Map<Long, List<WikiDocNodeVO>> byParent = new HashMap<>();
        for (WikiDoc d : all) {
            WikiDocNodeVO node = toNode(d, nickMap);
            nodeMap.put(d.getId(), node);
            byParent.computeIfAbsent(d.getParentId(), k -> new ArrayList<>()).add(node);
        }
        // 构建树：根 = parentId 0 或父节点不存在的孤儿节点（兼容脏数据）
        List<WikiDocNodeVO> roots = new ArrayList<>();
        for (WikiDocNodeVO node : nodeMap.values()) {
            if (node.getParentId() == null || node.getParentId() == 0L) {
                roots.add(node);
                continue;
            }
            List<WikiDocNodeVO> siblings = byParent.get(node.getParentId());
            if (siblings != null && siblings.contains(node)) {
                WikiDocNodeVO parent = nodeMap.get(node.getParentId());
                if (parent != null) {
                    parent.getChildren().add(node);
                } else {
                    roots.add(node);
                }
            }
        }
        return roots;
    }

    @Override
    public WikiDocDetailVO detail(Long id) {
        WikiDoc doc = mustExist(id);
        WikiDocDetailVO vo = new WikiDocDetailVO();
        vo.setId(doc.getId());
        vo.setTitle(doc.getTitle());
        vo.setParentId(doc.getParentId());
        vo.setSort(doc.getSort());
        vo.setContent(doc.getContent());
        vo.setHasContent(doc.getContent() != null && !doc.getContent().isBlank());
        vo.setCreateTime(doc.getCreateTime());
        vo.setUpdateTime(doc.getUpdateTime());
        Set<Long> idSet = new HashSet<>();
        if (doc.getCreatorId() != null) {
            idSet.add(doc.getCreatorId());
        }
        if (doc.getEditorId() != null) {
            idSet.add(doc.getEditorId());
        }
        Map<Long, String> nickMap = loadNicknames(idSet);
        vo.setCreatorName(nickMap.getOrDefault(doc.getCreatorId(), ""));
        vo.setEditorName(nickMap.getOrDefault(doc.getEditorId(), ""));
        Long childCount = wikiDocMapper.selectCount(new LambdaQueryWrapper<WikiDoc>().eq(WikiDoc::getParentId, id));
        vo.setChildCount(childCount == null ? 0 : childCount.intValue());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(WikiDocDTO dto) {
        Long parentId = dto.getParentId() == null ? 0L : dto.getParentId();
        validateParent(parentId, null);
        checkTitleUnique(dto.getTitle(), parentId, null);
        WikiDoc doc = new WikiDoc();
        doc.setTitle(dto.getTitle().trim());
        doc.setParentId(parentId);
        doc.setContent(HtmlSanitizer.sanitize(dto.getContent()));
        doc.setSort(resolveSort(parentId));
        Long uid = currentUserId();
        doc.setCreatorId(uid);
        doc.setEditorId(uid);
        wikiDocMapper.insert(doc);
        log.info("[Wiki] 创建文档 id={}, title={}, parentId={}", doc.getId(), doc.getTitle(), parentId);
        return doc.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, WikiDocDTO dto) {
        WikiDoc doc = mustExist(id);
        Long newParentId = dto.getParentId() == null ? 0L : dto.getParentId();
        // 校验父节点合法性（含移动时禁止移入自身子树）
        validateParent(newParentId, id);
        if (!newParentId.equals(doc.getParentId())) {
            checkMoveCycle(id, newParentId);
        }
        checkTitleUnique(dto.getTitle(), newParentId, id);
        doc.setTitle(dto.getTitle().trim());
        doc.setParentId(newParentId);
        doc.setContent(HtmlSanitizer.sanitize(dto.getContent()));
        doc.setEditorId(currentUserId());
        wikiDocMapper.updateById(doc);
        log.info("[Wiki] 更新文档 id={}, title={}", id, doc.getTitle());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        mustExist(id);
        List<Long> ids = new ArrayList<>();
        collectSubtreeIds(id, ids);
        // 级联删除文档（逻辑删除）
        wikiDocMapper.deleteByIds(ids);
        // 解除被删文档关联的附件（物理文件保留,可在文件页单独删除）
        int unbound = fileResourceMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<FileResource>()
                .in(FileResource::getDocId, ids)
                .set(FileResource::getDocId, null));
        log.info("[Wiki] 删除文档 id={}, 级联数量={}, 解除附件关联={}", id, ids.size(), unbound);
    }

    // ---------------- 私有方法 ----------------

    private WikiDoc mustExist(Long id) {
        WikiDoc doc = wikiDocMapper.selectById(id);
        if (doc == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "文档不存在或已被删除");
        }
        return doc;
    }

    private void validateParent(Long parentId, Long selfId) {
        if (parentId == 0L) {
            return;
        }
        if (parentId.equals(selfId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "父节点不能是自身");
        }
        WikiDoc parent = wikiDocMapper.selectById(parentId);
        if (parent == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "父目录不存在或已被删除");
        }
    }

    /** 防止把节点移动到自己的子孙节点下形成环 */
    private void checkMoveCycle(Long nodeId, Long newParentId) {
        Long cursor = newParentId;
        int depth = 0;
        while (cursor != null && cursor != 0L) {
            if (cursor.equals(nodeId)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "不能将文档移动到其子节点下");
            }
            if (++depth > 100) {
                break;
            }
            WikiDoc p = wikiDocMapper.selectById(cursor);
            cursor = p == null ? null : p.getParentId();
        }
    }

    private void checkTitleUnique(String title, Long parentId, Long excludeId) {
        LambdaQueryWrapper<WikiDoc> qw = new LambdaQueryWrapper<WikiDoc>()
                .eq(WikiDoc::getTitle, title.trim())
                .eq(WikiDoc::getParentId, parentId);
        if (excludeId != null) {
            qw.ne(WikiDoc::getId, excludeId);
        }
        Long count = wikiDocMapper.selectCount(qw);
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.DATA_EXIST, "同级目录下已存在同名文档");
        }
    }

    private Integer resolveSort(Long parentId) {
        List<WikiDoc> siblings = wikiDocMapper.selectList(new LambdaQueryWrapper<WikiDoc>()
                .eq(WikiDoc::getParentId, parentId)
                .orderByDesc(WikiDoc::getSort));
        if (siblings.isEmpty()) {
            return 0;
        }
        Integer max = siblings.get(0).getSort();
        return max == null ? 0 : max + 1;
    }

    private void collectSubtreeIds(Long id, List<Long> result) {
        result.add(id);
        List<WikiDoc> children = wikiDocMapper.selectList(new LambdaQueryWrapper<WikiDoc>().eq(WikiDoc::getParentId, id));
        for (WikiDoc child : children) {
            collectSubtreeIds(child.getId(), result);
        }
    }

    private WikiDocNodeVO toNode(WikiDoc d, Map<Long, String> nickMap) {
        WikiDocNodeVO node = new WikiDocNodeVO();
        node.setId(d.getId());
        node.setTitle(d.getTitle());
        node.setParentId(d.getParentId());
        node.setSort(d.getSort());
        node.setHasContent(d.getContent() != null && !d.getContent().isBlank());
        node.setEditorName(nickMap.getOrDefault(d.getEditorId(), ""));
        node.setUpdateTime(d.getUpdateTime());
        return node;
    }

    private Map<Long, String> loadNicknames(Set<Long> userIds) {
        Map<Long, String> map = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return map;
        }
        List<Long> ids = userIds.stream().filter(java.util.Objects::nonNull).collect(Collectors.toList());
        if (ids.isEmpty()) {
            return map;
        }
        List<SysUser> users = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>().in(SysUser::getId, ids));
        for (SysUser u : users) {
            map.put(u.getId(), StringUtils.hasText(u.getNickname()) ? u.getNickname() : u.getUsername());
        }
        return map;
    }

    private Long currentUserId() {
        return StpUtil.getLoginIdAsLong();
    }
}
