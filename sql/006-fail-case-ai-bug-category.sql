-- ============================================================
-- 失败用例扩展：AI 结构化分析、Bug 单号、问题分类、系统配置
-- 迁移脚本 006
-- ============================================================

-- 1. release_fail_case 新增字段
ALTER TABLE release_fail_case
  ADD COLUMN ai_root_cause   TEXT         DEFAULT NULL COMMENT 'AI分析根因' AFTER ai_analysis,
  ADD COLUMN ai_evidence     TEXT         DEFAULT NULL COMMENT 'AI分析佐证' AFTER ai_root_cause,
  ADD COLUMN ai_solution     TEXT         DEFAULT NULL COMMENT 'AI解决建议' AFTER ai_evidence,
  ADD COLUMN bug_no          VARCHAR(32)  DEFAULT NULL COMMENT 'Bug单号(Redmine)' AFTER ai_solution,
  ADD COLUMN issue_category  VARCHAR(64)  DEFAULT NULL COMMENT '问题分类' AFTER bug_no;

-- 2. weekly_fail_case 新增字段
ALTER TABLE weekly_fail_case
  ADD COLUMN ai_root_cause   TEXT         DEFAULT NULL COMMENT 'AI分析根因' AFTER ai_analysis,
  ADD COLUMN ai_evidence     TEXT         DEFAULT NULL COMMENT 'AI分析佐证' AFTER ai_root_cause,
  ADD COLUMN ai_solution     TEXT         DEFAULT NULL COMMENT 'AI解决建议' AFTER ai_evidence,
  ADD COLUMN bug_no          VARCHAR(32)  DEFAULT NULL COMMENT 'Bug单号(Redmine)' AFTER ai_solution,
  ADD COLUMN issue_category  VARCHAR(64)  DEFAULT NULL COMMENT '问题分类' AFTER bug_no;

-- 3. 系统配置表（key-value）
CREATE TABLE IF NOT EXISTS sys_config (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  config_key    VARCHAR(64)  NOT NULL COMMENT '配置键',
  config_value  VARCHAR(512) DEFAULT NULL COMMENT '配置值',
  remark        VARCHAR(255) DEFAULT NULL COMMENT '备注',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除:0未删 1已删',
  PRIMARY KEY (id),
  UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- 4. 问题分类表
CREATE TABLE IF NOT EXISTS issue_category (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  category_name VARCHAR(64)  NOT NULL COMMENT '分类名称(展示)',
  category_code VARCHAR(64)  NOT NULL COMMENT '分类编码',
  status        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用',
  sort_order    INT          NOT NULL DEFAULT 0 COMMENT '排序',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除:0未删 1已删',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问题分类表';

-- 5. 默认配置：Redmine Bug 地址前缀
INSERT INTO sys_config (config_key, config_value, remark)
VALUES ('redmine.url.prefix', 'http://100.60.183.51:300/issues/', 'Redmine Bug 地址前缀')
ON DUPLICATE KEY UPDATE remark = VALUES(remark);

-- 6. 默认问题分类
INSERT INTO issue_category (category_name, category_code, status, sort_order) VALUES
  ('UXOS_Bug', 'UXOS_BUG', 1, 1),
  ('脚本Bug', 'SCRIPT_BUG', 1, 2),
  ('版本行为改变', 'VERSION_CHANGE', 1, 3),
  ('其他', 'OTHER', 1, 4);
