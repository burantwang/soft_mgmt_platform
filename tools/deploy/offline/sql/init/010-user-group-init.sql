-- ============================================================
-- 用户组(组织归属)板块支持脚本
-- 说明：
--   - sys_group       组表：组织/团队归属(如 软件研发一处/二处)，与角色权限解耦
--   - sys_group_user  用户-组关联表：多对多，一个用户可加入多个组
-- 执行方式：mysql -uroot -proot dev_platform < 010-user-group-init.sql
-- 幂等：建表均使用 IF NOT EXISTS
-- ============================================================

USE dev_platform;

-- ------------------------------------------------------------
-- 1. 组表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_group (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  group_name    VARCHAR(64)  NOT NULL COMMENT '组名(如 软件研发一处)',
  remark        VARCHAR(255)          DEFAULT NULL COMMENT '备注',
  status        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_deleted    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除:0未删 1已删',
  PRIMARY KEY (id),
  UNIQUE KEY uk_group_name (group_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户组表';

-- ------------------------------------------------------------
-- 2. 用户-组关联表(多对多)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_group_user (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  group_id      BIGINT       NOT NULL COMMENT '组ID',
  user_id       BIGINT       NOT NULL COMMENT '用户ID',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted    TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_group_user (group_id, user_id),
  KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户组成员关联表';
