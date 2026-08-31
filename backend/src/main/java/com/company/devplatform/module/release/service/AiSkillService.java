package com.company.devplatform.module.release.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.devplatform.common.ErrorCode;
import com.company.devplatform.common.exception.BusinessException;
import com.company.devplatform.module.release.vo.AiSkillVO;
import com.company.devplatform.module.wiki.entity.WikiDoc;
import com.company.devplatform.module.wiki.mapper.WikiDocMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 技能集（skill）服务
 * <p>skill 文档复用 wiki_doc 表：wiki 树下预留根节点 {@code ai_skill_doc}，
 * 下面按板块（如 weekly_sanity）建目录节点，skill 文档挂在其下。
 * 启用开关复用 wiki_doc.enabled 字段。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiSkillService {

    private static final String ROOT_TITLE = "ai_skill_doc";

    private final WikiDocMapper wikiDocMapper;

    /** 列出某板块下所有 skill 文档 */
    public List<AiSkillVO> listSkills(String module) {
        Long sectionId = getOrCreateSection(module);
        List<WikiDoc> docs = wikiDocMapper.selectList(new LambdaQueryWrapper<WikiDoc>()
                .eq(WikiDoc::getParentId, sectionId)
                .orderByAsc(WikiDoc::getSort)
                .orderByAsc(WikiDoc::getId));
        return docs.stream().map(this::toVO).collect(Collectors.toList());
    }

    /** 新增 skill 文档 */
    @Transactional(rollbackFor = Exception.class)
    public AiSkillVO createSkill(String module, String title, String content) {
        Long sectionId = getOrCreateSection(module);
        String t = title == null ? "" : title.trim();
        if (!StringUtils.hasText(t)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "技能名称不能为空");
        }
        checkTitleUnique(sectionId, t, null);
        WikiDoc doc = new WikiDoc();
        doc.setTitle(t);
        doc.setParentId(sectionId);
        doc.setContent(content == null ? "" : content);
        doc.setSort(resolveSort(sectionId));
        doc.setEnabled(1);
        Long uid = StpUtil.getLoginIdAsLong();
        doc.setCreatorId(uid);
        doc.setEditorId(uid);
        wikiDocMapper.insert(doc);
        return toVO(doc);
    }

    /** 更新 skill 文档 */
    @Transactional(rollbackFor = Exception.class)
    public void updateSkill(Long docId, String title, String content) {
        WikiDoc doc = mustExist(docId);
        String t = title == null ? "" : title.trim();
        if (!StringUtils.hasText(t)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "技能名称不能为空");
        }
        checkTitleUnique(doc.getParentId(), t, docId);
        doc.setTitle(t);
        doc.setContent(content == null ? "" : content);
        doc.setEditorId(StpUtil.getLoginIdAsLong());
        wikiDocMapper.updateById(doc);
    }

    /** 启用/停用 skill 文档 */
    @Transactional(rollbackFor = Exception.class)
    public void toggleSkill(Long docId, Integer enabled) {
        WikiDoc doc = mustExist(docId);
        doc.setEnabled(enabled != null && enabled == 1 ? 1 : 0);
        wikiDocMapper.updateById(doc);
    }

    /** 删除 skill 文档 */
    @Transactional(rollbackFor = Exception.class)
    public void deleteSkill(Long docId) {
        mustExist(docId);
        wikiDocMapper.deleteById(docId);
    }

    /** 获取某板块下所有「启用」的 skill 内容（AI 调用时拼接） */
    public List<String> getEnabledContents(String module) {
        WikiDoc root = findRoot();
        if (root == null) {
            return new ArrayList<>();
        }
        WikiDoc section = wikiDocMapper.selectOne(new LambdaQueryWrapper<WikiDoc>()
                .eq(WikiDoc::getTitle, module).eq(WikiDoc::getParentId, root.getId()));
        if (section == null) {
            return new ArrayList<>();
        }
        List<WikiDoc> docs = wikiDocMapper.selectList(new LambdaQueryWrapper<WikiDoc>()
                .eq(WikiDoc::getParentId, section.getId())
                .eq(WikiDoc::getEnabled, 1)
                .orderByAsc(WikiDoc::getSort)
                .orderByAsc(WikiDoc::getId));
        return docs.stream()
                .map(WikiDoc::getContent)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }

    // ---------------- 私有方法 ----------------

    private Long getOrCreateSection(String module) {
        Long rootId = getOrCreateRoot();
        WikiDoc section = wikiDocMapper.selectOne(new LambdaQueryWrapper<WikiDoc>()
                .eq(WikiDoc::getTitle, module).eq(WikiDoc::getParentId, rootId));
        if (section == null) {
            section = new WikiDoc();
            section.setTitle(module);
            section.setParentId(rootId);
            section.setSort(0);
            section.setEnabled(1);
            wikiDocMapper.insert(section);
        }
        return section.getId();
    }

    private Long getOrCreateRoot() {
        WikiDoc root = findRoot();
        if (root == null) {
            root = new WikiDoc();
            root.setTitle(ROOT_TITLE);
            root.setParentId(0L);
            root.setSort(99);
            root.setEnabled(1);
            wikiDocMapper.insert(root);
        }
        return root.getId();
    }

    private WikiDoc findRoot() {
        return wikiDocMapper.selectOne(new LambdaQueryWrapper<WikiDoc>()
                .eq(WikiDoc::getTitle, ROOT_TITLE).eq(WikiDoc::getParentId, 0L));
    }

    private WikiDoc mustExist(Long id) {
        WikiDoc doc = wikiDocMapper.selectById(id);
        if (doc == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "技能文档不存在或已被删除");
        }
        return doc;
    }

    private void checkTitleUnique(Long parentId, String title, Long excludeId) {
        LambdaQueryWrapper<WikiDoc> qw = new LambdaQueryWrapper<WikiDoc>()
                .eq(WikiDoc::getTitle, title)
                .eq(WikiDoc::getParentId, parentId);
        if (excludeId != null) {
            qw.ne(WikiDoc::getId, excludeId);
        }
        Long count = wikiDocMapper.selectCount(qw);
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.DATA_EXIST, "已存在同名技能文档");
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

    private AiSkillVO toVO(WikiDoc d) {
        AiSkillVO vo = new AiSkillVO();
        vo.setId(d.getId());
        vo.setTitle(d.getTitle());
        vo.setContent(d.getContent());
        vo.setEnabled(d.getEnabled() == null ? 1 : d.getEnabled());
        vo.setUpdateTime(d.getUpdateTime());
        return vo;
    }
}
