-- ============================================================
-- 门户「其他地址清单」板块支持脚本
-- 说明：
--   - portal_category 增加 layout 字段：card 卡片 / table 表格
--   - portal_link 增加 username / password 字段（表格板块展示账号密码）
--   - 插入「其他地址清单」板块（layout=table）+ 示例地址
-- 执行方式：mysql -uroot -proot dev_platform < 009-portal-address-init.sql
-- 幂等：ALTER 前做存在性判断；示例数据按名称判重
-- ============================================================

USE dev_platform;

-- ------------------------------------------------------------
-- 1. 板块表增加展示方式字段
-- ------------------------------------------------------------
SET @has_layout := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'dev_platform' AND TABLE_NAME = 'portal_category' AND COLUMN_NAME = 'layout');
SET @sql := IF(@has_layout = 0,
  'ALTER TABLE portal_category ADD COLUMN layout VARCHAR(8) NOT NULL DEFAULT ''card'' COMMENT ''展示方式:card卡片 table表格'' AFTER description',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------
-- 2. 链接表增加账号密码字段
-- ------------------------------------------------------------
SET @has_username := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'dev_platform' AND TABLE_NAME = 'portal_link' AND COLUMN_NAME = 'username');
SET @sql := IF(@has_username = 0,
  'ALTER TABLE portal_link ADD COLUMN username VARCHAR(128) DEFAULT NULL COMMENT ''登录用户名'' AFTER description',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_password := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'dev_platform' AND TABLE_NAME = 'portal_link' AND COLUMN_NAME = 'password');
SET @sql := IF(@has_password = 0,
  'ALTER TABLE portal_link ADD COLUMN password VARCHAR(255) DEFAULT NULL COMMENT ''登录密码'' AFTER username',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------
-- 3. 「其他地址清单」板块（表格展示）
-- ------------------------------------------------------------
INSERT INTO portal_category (category_name, icon, color, description, layout, sort, status, creator_id, is_deleted)
SELECT tmp.* FROM (
    SELECT '其他地址清单' AS category_name, 'Position' AS icon, '#16A085' AS color,
           '服务器、数据库等系统登录地址及账号信息' AS description,
           'table' AS layout, 3 AS sort, 1 AS status, 1 AS creator_id, 0 AS is_deleted
) tmp
WHERE NOT EXISTS (
    SELECT 1 FROM portal_category c WHERE c.category_name = tmp.category_name AND c.is_deleted = 0
);

-- ------------------------------------------------------------
-- 4. 示例地址（按 板块+名称 判重）
-- ------------------------------------------------------------
INSERT INTO portal_link (category_id, link_name, url, username, password, description, icon, color, sort, status, creator_id, is_deleted)
SELECT tmp.* FROM (
    SELECT (SELECT id FROM portal_category WHERE category_name = '其他地址清单' AND is_deleted = 0 LIMIT 1) AS category_id,
           '测试环境应用服务器' AS link_name, '192.168.1.100:22' AS url,
           'root' AS username, 'Test@123456' AS password,
           '应用服务部署主机，SSH 登录' AS description,
           'Cpu' AS icon, '#16A085' AS color, 0 AS sort, 1 AS status, 1 AS creator_id, 0 AS is_deleted
) tmp
WHERE NOT EXISTS (
    SELECT 1 FROM portal_link l WHERE l.category_id = tmp.category_id AND l.link_name = tmp.link_name AND l.is_deleted = 0
);

INSERT INTO portal_link (category_id, link_name, url, username, password, description, icon, color, sort, status, creator_id, is_deleted)
SELECT tmp.* FROM (
    SELECT (SELECT id FROM portal_category WHERE category_name = '其他地址清单' AND is_deleted = 0 LIMIT 1) AS category_id,
           'MySQL 主库' AS link_name, '192.168.1.101:3306' AS url,
           'dba' AS username, 'Dba@2026' AS password,
           '业务主数据库，仅 DBA 可直连' AS description,
           'Coin' AS icon, '#16A085' AS color, 1 AS sort, 1 AS status, 1 AS creator_id, 0 AS is_deleted
) tmp
WHERE NOT EXISTS (
    SELECT 1 FROM portal_link l WHERE l.category_id = tmp.category_id AND l.link_name = tmp.link_name AND l.is_deleted = 0
);
