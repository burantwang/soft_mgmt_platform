-- ============================================================
-- 阶段6初始化：任务追踪模块（任务类型 + 问题单 + 明细）
-- 执行方式：mysql -uroot -proot dev_platform < 004-task-init.sql
-- 幂等说明：
--   - 建表使用 CREATE TABLE IF NOT EXISTS
--   - 任务类型按 code 判重
--   - 权限与角色分配按 NOT EXISTS 去重
-- ============================================================

-- 1. 任务类型表
CREATE TABLE IF NOT EXISTS task_type (
  id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  code        VARCHAR(32) NOT NULL COMMENT '类型编码(唯一,如 atdd/dvs/pylint/jingan)',
  name        VARCHAR(64) NOT NULL COMMENT '类型名称(如 ATDD自动化测试)',
  icon        VARCHAR(64) DEFAULT NULL COMMENT '图标标识',
  sort        INT         NOT NULL DEFAULT 0 COMMENT '排序(越小越靠前)',
  enabled     TINYINT     NOT NULL DEFAULT 1 COMMENT '启用:1启用 0停用',
  remark      VARCHAR(255) DEFAULT NULL COMMENT '备注',
  create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除:0未删 1已删',
  PRIMARY KEY (id),
  UNIQUE KEY uk_task_type_code (code),
  KEY idx_task_type_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务追踪-任务类型表';

-- 2. 问题单任务表（通用化：不绑定发布记录，可追踪任意任务）
CREATE TABLE IF NOT EXISTS issue_task (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  task_no       VARCHAR(64)  NOT NULL COMMENT '任务编号(如 ATDD20260827001)',
  task_type_id  BIGINT       NOT NULL COMMENT '任务类型ID',
  title         VARCHAR(200) NOT NULL COMMENT '任务标题/概述',
  status        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态:1待处理 2处理中 3已完成 4已关闭',
  priority      TINYINT      NOT NULL DEFAULT 2 COMMENT '优先级:1低 2中 3高',
  branch        VARCHAR(100) DEFAULT NULL COMMENT '关联代码分支(可选)',
  version       VARCHAR(64)  DEFAULT NULL COMMENT '关联版本号(可选)',
  project_name  VARCHAR(100) DEFAULT NULL COMMENT '关联项目/机型(可选)',
  assignee_id   BIGINT       DEFAULT NULL COMMENT '责任人(用户ID)',
  creator_id    BIGINT       DEFAULT NULL COMMENT '创建人(用户ID)',
  summary       TEXT         COMMENT '问题描述',
  reason        TEXT         COMMENT '原因分析(汇总)',
  fix_plan      TEXT         COMMENT '解决方案(汇总)',
  handle_time   DATETIME     DEFAULT NULL COMMENT '完成时间',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除:0未删 1已删',
  PRIMARY KEY (id),
  UNIQUE KEY uk_issue_task_no (task_no),
  KEY idx_issue_task_type (task_type_id),
  KEY idx_issue_task_status (status),
  KEY idx_issue_task_assignee (assignee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务追踪-问题单任务表';

-- 3. 问题单明细表（具体失败项/问题项）
CREATE TABLE IF NOT EXISTS issue_task_item (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  task_id     BIGINT       NOT NULL COMMENT '问题单任务ID',
  item_type   VARCHAR(20)  NOT NULL DEFAULT 'failed' COMMENT '明细类别:failed失败 error错误 warning警告 note备注',
  item_name   VARCHAR(500) NOT NULL COMMENT '明细名称(如用例名/问题项)',
  item_log    TEXT         COMMENT '原始信息/日志',
  status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态:1待处理 2处理中 3已修复 4非缺陷',
  assignee_id BIGINT       DEFAULT NULL COMMENT '责任人',
  fail_reason TEXT         COMMENT '失败原因',
  fix_plan    TEXT         COMMENT '修改方案',
  is_bug      TINYINT      DEFAULT 0 COMMENT '是否提Bug:0否 1是',
  progress    TEXT         COMMENT '分析进展',
  conclusion  TEXT         COMMENT '结论',
  ai_analysis TEXT         COMMENT 'AI分析描述',
  handle_time DATETIME     DEFAULT NULL COMMENT '处理完成时间',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除:0未删 1已删',
  PRIMARY KEY (id),
  KEY idx_issue_item_task (task_id),
  KEY idx_issue_item_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务追踪-问题单明细表';

-- 4. 预置任务类型（幂等：按 code 判重）
--    DailySanity 为原始设计核心类型（DailySanity 测试失败任务追踪）
INSERT INTO task_type (code, name, icon, sort, enabled, remark, is_deleted)
SELECT tmp.* FROM (
    SELECT 'daily_sanity' AS code, 'DailySanity任务' AS name, 'Odometer' AS icon, 1 AS sort, 1 AS enabled, 'DailySanity 测试失败任务追踪（原始设计）' AS remark, 0 AS is_deleted
    UNION ALL SELECT 'atdd', 'ATDD自动化测试', 'Cpu', 2, 1, '自动化测试任务追踪', 0
    UNION ALL SELECT 'dvs', 'DVS', 'Monitor', 3, 1, 'DVS 动态验证任务追踪', 0
    UNION ALL SELECT 'pylint', 'Pylint扫描', 'Aim', 4, 1, 'Pylint 代码扫描任务追踪', 0
    UNION ALL SELECT 'jingan', '静安代码扫描', 'MagicStick', 5, 1, '静安代码扫描任务追踪', 0
) tmp
WHERE NOT EXISTS (
    SELECT 1 FROM task_type t WHERE t.code = tmp.code AND t.is_deleted = 0
);

-- 5. 新增权限点（幂等：perm_code 唯一）
INSERT INTO sys_permission (perm_code, perm_name, module, module_name)
SELECT tmp.* FROM (
    SELECT 'task:view' AS perm_code, '任务追踪-查看' AS perm_name, 'task' AS module, '任务追踪' AS module_name
    UNION ALL SELECT 'task:edit', '任务追踪-编辑', 'task', '任务追踪'
) tmp
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission p WHERE p.perm_code = tmp.perm_code AND p.is_deleted = 0
);

-- 6. 超管自动拥有新权限（后续新增权限需手动为其他角色分配）
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r, sys_permission p
WHERE r.role_code = 'super_admin'
  AND p.perm_code IN ('task:view', 'task:edit')
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- 7. admin 角色分配任务追踪权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r, sys_permission p
WHERE r.role_code = 'admin'
  AND p.perm_code IN ('task:view', 'task:edit')
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
