-- ============================================================
-- 系统门户（导航链接聚合页）初始化脚本
-- 说明：
--   - 板块表 portal_category + 系统链接表 portal_link（两级结构）
--   - 权限点 portal:view / portal:edit，admin 角色默认授予编辑权限
--   - 示例数据按名称判重，可重复执行（幂等）
-- 执行方式：mysql -uroot -proot dev_platform < 008-portal-init.sql
-- ============================================================

USE dev_platform;

-- ------------------------------------------------------------
-- 1. 门户板块表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS portal_category (
  id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  category_name  VARCHAR(64)  NOT NULL COMMENT '板块名称(如 研发相关 / 测试相关)',
  icon           VARCHAR(64)  NOT NULL DEFAULT 'Menu' COMMENT '板块图标(Element Plus 图标名或 emoji)',
  color          VARCHAR(32)  NOT NULL DEFAULT '#409EFF' COMMENT '主题色(hex)',
  description    VARCHAR(255)          DEFAULT NULL COMMENT '板块描述',
  sort           INT          NOT NULL DEFAULT 0 COMMENT '排序(小值在前)',
  status         TINYINT      NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用',
  creator_id     BIGINT                DEFAULT NULL COMMENT '创建人',
  create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门户板块表';

-- ------------------------------------------------------------
-- 2. 门户系统链接表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS portal_link (
  id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  category_id    BIGINT       NOT NULL COMMENT '所属板块ID',
  link_name      VARCHAR(64)  NOT NULL COMMENT '系统名称(如 GitLab / Jenkins)',
  url            VARCHAR(500) NOT NULL COMMENT '访问地址(http/https 开头)',
  description    VARCHAR(255)          DEFAULT NULL COMMENT '系统简介',
  icon           VARCHAR(64)  NOT NULL DEFAULT 'Link' COMMENT '图标(Element Plus 图标名或 emoji)',
  color          VARCHAR(32)  NOT NULL DEFAULT '#409EFF' COMMENT '主题色(hex)',
  sort           INT          NOT NULL DEFAULT 0 COMMENT '排序(小值在前)',
  status         TINYINT      NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用',
  creator_id     BIGINT                DEFAULT NULL COMMENT '创建人',
  create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_category (category_id),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门户系统链接表';

-- ------------------------------------------------------------
-- 3. 权限点（portal 模块）
-- ------------------------------------------------------------
INSERT INTO sys_permission (perm_code, perm_name, module, module_name)
SELECT 'portal:view', '系统门户-查看', 'portal', '系统门户'
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE perm_code = 'portal:view' AND is_deleted = 0);

INSERT INTO sys_permission (perm_code, perm_name, module, module_name)
SELECT 'portal:edit', '系统门户-编辑', 'portal', '系统门户'
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE perm_code = 'portal:edit' AND is_deleted = 0);

-- 超管分配全部权限点（幂等）
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r, sys_permission p
WHERE r.role_code = 'super_admin'
  AND p.perm_code IN ('portal:view', 'portal:edit')
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- admin 角色分配门户编辑权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r, sys_permission p
WHERE r.role_code = 'admin'
  AND p.perm_code IN ('portal:view', 'portal:edit')
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- 员工角色分配查看权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r, sys_permission p
WHERE r.role_code = 'employee'
  AND p.perm_code = 'portal:view'
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- ------------------------------------------------------------
-- 4. 示例板块（名称判重，可重复执行）
-- ------------------------------------------------------------
INSERT INTO portal_category (category_name, icon, color, description, sort, status, creator_id, is_deleted)
SELECT tmp.* FROM (
    SELECT '研发相关' AS category_name, 'Monitor' AS icon, '#409EFF' AS color, '代码、构建、镜像等研发基础设施' AS description, 0 AS sort, 1 AS status, 1 AS creator_id, 0 AS is_deleted
) tmp
WHERE NOT EXISTS (
    SELECT 1 FROM portal_category c WHERE c.category_name = tmp.category_name AND c.is_deleted = 0
);

INSERT INTO portal_category (category_name, icon, color, description, sort, status, creator_id, is_deleted)
SELECT tmp.* FROM (
    SELECT '测试相关' AS category_name, 'DataAnalysis' AS icon, '#67C23A' AS color, '自动化测试、用例与缺陷跟踪平台' AS description, 1 AS sort, 1 AS status, 1 AS creator_id, 0 AS is_deleted
) tmp
WHERE NOT EXISTS (
    SELECT 1 FROM portal_category c WHERE c.category_name = tmp.category_name AND c.is_deleted = 0
);

INSERT INTO portal_category (category_name, icon, color, description, sort, status, creator_id, is_deleted)
SELECT tmp.* FROM (
    SELECT '效能工具' AS category_name, 'Tools' AS icon, '#E6A23C' AS color, '协作、文档与效率工具' AS description, 2 AS sort, 1 AS status, 1 AS creator_id, 0 AS is_deleted
) tmp
WHERE NOT EXISTS (
    SELECT 1 FROM portal_category c WHERE c.category_name = tmp.category_name AND c.is_deleted = 0
);

-- ------------------------------------------------------------
-- 5. 示例链接（按 板块+名称 判重）
-- ------------------------------------------------------------
-- 研发相关
INSERT INTO portal_link (category_id, link_name, url, description, icon, color, sort, status, creator_id, is_deleted)
SELECT tmp.* FROM (
    SELECT (SELECT id FROM portal_category WHERE category_name = '研发相关' AND is_deleted = 0 LIMIT 1) AS category_id,
           'GitLab' AS link_name, 'https://git.example.com' AS url, '代码仓库与 MR 评审' AS description,
           'Github' AS icon, '#409EFF' AS color, 0 AS sort, 1 AS status, 1 AS creator_id, 0 AS is_deleted
) tmp
WHERE NOT EXISTS (
    SELECT 1 FROM portal_link l WHERE l.category_id = tmp.category_id AND l.link_name = tmp.link_name AND l.is_deleted = 0
);

INSERT INTO portal_link (category_id, link_name, url, description, icon, color, sort, status, creator_id, is_deleted)
SELECT tmp.* FROM (
    SELECT (SELECT id FROM portal_category WHERE category_name = '研发相关' AND is_deleted = 0 LIMIT 1) AS category_id,
           'Jenkins' AS link_name, 'https://jenkins.example.com' AS url, '持续集成与构建流水线' AS description,
           'Connection' AS icon, '#F56C6C' AS color, 1 AS sort, 1 AS status, 1 AS creator_id, 0 AS is_deleted
) tmp
WHERE NOT EXISTS (
    SELECT 1 FROM portal_link l WHERE l.category_id = tmp.category_id AND l.link_name = tmp.link_name AND l.is_deleted = 0
);

INSERT INTO portal_link (category_id, link_name, url, description, icon, color, sort, status, creator_id, is_deleted)
SELECT tmp.* FROM (
    SELECT (SELECT id FROM portal_category WHERE category_name = '研发相关' AND is_deleted = 0 LIMIT 1) AS category_id,
           'Nexus' AS link_name, 'https://nexus.example.com' AS url, '制品仓库 / 镜像仓库' AS description,
           'Box' AS icon, '#E6A23C' AS color, 2 AS sort, 1 AS status, 1 AS creator_id, 0 AS is_deleted
) tmp
WHERE NOT EXISTS (
    SELECT 1 FROM portal_link l WHERE l.category_id = tmp.category_id AND l.link_name = tmp.link_name AND l.is_deleted = 0
);

-- 测试相关
INSERT INTO portal_link (category_id, link_name, url, description, icon, color, sort, status, creator_id, is_deleted)
SELECT tmp.* FROM (
    SELECT (SELECT id FROM portal_category WHERE category_name = '测试相关' AND is_deleted = 0 LIMIT 1) AS category_id,
           'Sonic' AS link_name, 'https://sonic.example.com' AS url, '云真机自动化测试平台' AS description,
           'Aim' AS icon, '#67C23A' AS color, 0 AS sort, 1 AS status, 1 AS creator_id, 0 AS is_deleted
) tmp
WHERE NOT EXISTS (
    SELECT 1 FROM portal_link l WHERE l.category_id = tmp.category_id AND l.link_name = tmp.link_name AND l.is_deleted = 0
);

INSERT INTO portal_link (category_id, link_name, url, description, icon, color, sort, status, creator_id, is_deleted)
SELECT tmp.* FROM (
    SELECT (SELECT id FROM portal_category WHERE category_name = '测试相关' AND is_deleted = 0 LIMIT 1) AS category_id,
           'Redmine' AS link_name, 'https://redmine.example.com' AS url, '缺陷管理与任务跟踪' AS description,
           'Warning' AS icon, '#F56C6C' AS color, 1 AS sort, 1 AS status, 1 AS creator_id, 0 AS is_deleted
) tmp
WHERE NOT EXISTS (
    SELECT 1 FROM portal_link l WHERE l.category_id = tmp.category_id AND l.link_name = tmp.link_name AND l.is_deleted = 0
);

-- 效能工具
INSERT INTO portal_link (category_id, link_name, url, description, icon, color, sort, status, creator_id, is_deleted)
SELECT tmp.* FROM (
    SELECT (SELECT id FROM portal_category WHERE category_name = '效能工具' AND is_deleted = 0 LIMIT 1) AS category_id,
           'Wiki 知识库' AS link_name, '/wiki' AS url, '部门文档与技术规范' AS description,
           'Reading' AS icon, '#909399' AS color, 0 AS sort, 1 AS status, 1 AS creator_id, 0 AS is_deleted
) tmp
WHERE NOT EXISTS (
    SELECT 1 FROM portal_link l WHERE l.category_id = tmp.category_id AND l.link_name = tmp.link_name AND l.is_deleted = 0
);

INSERT INTO portal_link (category_id, link_name, url, description, icon, color, sort, status, creator_id, is_deleted)
SELECT tmp.* FROM (
    SELECT (SELECT id FROM portal_category WHERE category_name = '效能工具' AND is_deleted = 0 LIMIT 1) AS category_id,
           '钉钉' AS link_name, 'https://www.dingtalk.com' AS url, '团队即时沟通' AS description,
           'ChatDotRound' AS icon, '#409EFF' AS color, 1 AS sort, 1 AS status, 1 AS creator_id, 0 AS is_deleted
) tmp
WHERE NOT EXISTS (
    SELECT 1 FROM portal_link l WHERE l.category_id = tmp.category_id AND l.link_name = tmp.link_name AND l.is_deleted = 0
);
