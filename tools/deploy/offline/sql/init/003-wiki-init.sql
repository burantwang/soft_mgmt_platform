-- ============================================================
-- 阶段5初始化：admin 角色分配 Wiki 权限 + 示例文档
-- 执行方式：mysql -uroot -proot dev_platform < 003-wiki-init.sql
-- 幂等说明：
--   - 角色权限按 NOT EXISTS 去重
--   - 示例文档按 title+parent_id+is_deleted=0 判重（不依赖唯一索引,
--     避免逻辑删除与物理唯一索引冲突,与 release 模块的处理一致）
-- ============================================================

-- 1. admin 角色分配 Wiki 模块权限（查看 + 编辑）
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r, sys_permission p
WHERE r.role_code = 'admin'
  AND p.perm_code IN ('wiki:view', 'wiki:edit')
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- 2. 根级示例文档（目录节点）
INSERT INTO wiki_doc (title, content, parent_id, sort, creator_id, editor_id, is_deleted)
SELECT tmp.* FROM (
    SELECT '平台使用手册' AS title, NULL AS content, 0 AS parent_id, 0 AS sort, 1 AS creator_id, 1 AS editor_id, 0 AS is_deleted
) tmp
WHERE NOT EXISTS (
    SELECT 1 FROM wiki_doc d WHERE d.title = tmp.title AND d.parent_id = tmp.parent_id AND d.is_deleted = 0
);

INSERT INTO wiki_doc (title, content, parent_id, sort, creator_id, editor_id, is_deleted)
SELECT tmp.* FROM (
    SELECT '版本发布流程' AS title, NULL AS content, 0 AS parent_id, 1 AS sort, 1 AS creator_id, 1 AS editor_id, 0 AS is_deleted
) tmp
WHERE NOT EXISTS (
    SELECT 1 FROM wiki_doc d WHERE d.title = tmp.title AND d.parent_id = tmp.parent_id AND d.is_deleted = 0
);

-- 3. 「平台使用手册」的子文档（parent_id 动态取自父文档 id）
INSERT INTO wiki_doc (title, content, parent_id, sort, creator_id, editor_id, is_deleted)
SELECT tmp.* FROM (
    SELECT 'Sonic 测试报告上传' AS title,
           '<h2>Sonic 测试报告上传</h2><p>在<strong>版本发布</strong>页点击<em>上传报告</em>，解析成功后可补录分支与机型后确认入库。</p>' AS content,
           (SELECT id FROM wiki_doc WHERE title = '平台使用手册' AND parent_id = 0 AND is_deleted = 0 LIMIT 1) AS parent_id,
           0 AS sort, 1 AS creator_id, 1 AS editor_id, 0 AS is_deleted
) tmp
WHERE NOT EXISTS (
    SELECT 1 FROM wiki_doc d WHERE d.title = tmp.title AND d.parent_id = tmp.parent_id AND d.is_deleted = 0
);

INSERT INTO wiki_doc (title, content, parent_id, sort, creator_id, editor_id, is_deleted)
SELECT tmp.* FROM (
    SELECT '失败任务处理' AS title,
           '<h2>失败任务处理</h2><p>发布失败后系统自动生成失败任务，可在<strong>失败任务</strong>菜单跟踪处理进度。</p>' AS content,
           (SELECT id FROM wiki_doc WHERE title = '平台使用手册' AND parent_id = 0 AND is_deleted = 0 LIMIT 1) AS parent_id,
           1 AS sort, 1 AS creator_id, 1 AS editor_id, 0 AS is_deleted
) tmp
WHERE NOT EXISTS (
    SELECT 1 FROM wiki_doc d WHERE d.title = tmp.title AND d.parent_id = tmp.parent_id AND d.is_deleted = 0
);
