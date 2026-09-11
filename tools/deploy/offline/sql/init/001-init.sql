-- ============================================================
-- Sonic 版本发布业务平台 数据库初始化脚本 v1.0
-- 版本化管理：所有表结构变更、初始化数据均通过独立 sql 脚本维护
-- 挂载于 docker-compose mysql 容器 /docker-entrypoint-initdb.d，首次启动自动执行
-- ============================================================
CREATE DATABASE IF NOT EXISTS dev_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE dev_platform;

-- ------------------------------------------------------------
-- 1. 用户表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_user (
  id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  username         VARCHAR(64)  NOT NULL COMMENT '登录账号',
  password         VARCHAR(128) NOT NULL COMMENT '密码(BCrypt加密)',
  nickname         VARCHAR(64)  NOT NULL COMMENT '姓名',
  email            VARCHAR(128)          DEFAULT NULL COMMENT '邮箱',
  phone            VARCHAR(32)           DEFAULT NULL COMMENT '手机号',
  status           TINYINT      NOT NULL DEFAULT 1 COMMENT '状态:1正常 0禁用',
  must_change_pwd  TINYINT      NOT NULL DEFAULT 1 COMMENT '是否强制改密:1是 0否',
  remark           VARCHAR(255)          DEFAULT NULL COMMENT '备注',
  create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除:0未删 1已删',
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ------------------------------------------------------------
-- 2. 角色表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_role (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  role_code     VARCHAR(64)  NOT NULL COMMENT '角色编码',
  role_name     VARCHAR(64)  NOT NULL COMMENT '角色名称',
  remark        VARCHAR(255)          DEFAULT NULL COMMENT '备注',
  status        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态:1正常 0停用',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted    TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- ------------------------------------------------------------
-- 3. 权限点表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_permission (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  perm_code     VARCHAR(64)  NOT NULL COMMENT '权限编码(如 sonic:edit)',
  perm_name     VARCHAR(64)  NOT NULL COMMENT '权限名称',
  module        VARCHAR(32)  NOT NULL COMMENT '所属模块(sonic/wiki/system)',
  module_name   VARCHAR(64)  NOT NULL COMMENT '模块名称',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted    TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_perm_code (perm_code),
  KEY idx_module (module)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限点表';

-- ------------------------------------------------------------
-- 4. 角色-权限关联表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_role_permission (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  role_id       BIGINT       NOT NULL COMMENT '角色ID',
  permission_id BIGINT       NOT NULL COMMENT '权限点ID',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted    TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_role (role_id),
  KEY idx_perm (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- ------------------------------------------------------------
-- 5. 用户-角色关联表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_user_role (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id       BIGINT       NOT NULL COMMENT '用户ID',
  role_id       BIGINT       NOT NULL COMMENT '角色ID',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted    TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_user (user_id),
  KEY idx_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- ------------------------------------------------------------
-- 6. 项目(机型)表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS project (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  project_name  VARCHAR(64)  NOT NULL COMMENT '项目/机型名称(如 Gaea)',
  project_code  VARCHAR(64)  NOT NULL COMMENT '项目编码',
  description   VARCHAR(255)          DEFAULT NULL COMMENT '描述',
  status        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态:1启用 0停用',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted    TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_project_name (project_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目(机型)表';

-- ------------------------------------------------------------
-- 7. 版本发布记录表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS release_record (
  id             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  branch         VARCHAR(128)  NOT NULL COMMENT '代码分支(第一展示维度)',
  version        VARCHAR(128)           DEFAULT NULL COMMENT '镜像版本号(Environment.Version)',
  image_url      VARCHAR(500)           DEFAULT NULL COMMENT '镜像地址',
  result         TINYINT       NOT NULL COMMENT '发布结果:1成功 2失败(枚举ReleaseResult)',
  report_file_id BIGINT                 DEFAULT NULL COMMENT '测试报告文件ID(file_resource)',
  total_count    INT           NOT NULL DEFAULT 0 COMMENT '用例总数',
  passed_count   INT           NOT NULL DEFAULT 0,
  failed_count   INT           NOT NULL DEFAULT 0,
  error_count    INT           NOT NULL DEFAULT 0,
  skipped_count  INT           NOT NULL DEFAULT 0,
  duration_sec   DECIMAL(10,2)          DEFAULT NULL COMMENT '总耗时(秒)',
  report_time    DATETIME               DEFAULT NULL COMMENT '报告生成时间',
  publisher_id   BIGINT                 DEFAULT NULL COMMENT '发布人(用户ID)',
  publish_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  source         TINYINT       NOT NULL DEFAULT 1 COMMENT '来源:1人工上传 2Jenkins推送 3手动创建(枚举ReleaseSource)',
  remark         VARCHAR(255)           DEFAULT NULL COMMENT '备注',
  create_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted     TINYINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_branch (branch),
  KEY idx_version (version),
  KEY idx_result (result),
  KEY idx_publish_time (publish_time),
  KEY idx_report_time (report_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='版本发布记录表';

-- ------------------------------------------------------------
-- 8. 发布记录-机型关联表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS release_record_project (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  record_id     BIGINT       NOT NULL COMMENT '发布记录ID',
  project_id    BIGINT       NOT NULL COMMENT '机型ID',
  is_primary    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否主机型:1是 0否',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted    TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_record (record_id),
  KEY idx_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发布记录机型关联表';

-- ------------------------------------------------------------
-- 9. 失败聚合任务表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS release_fail_task (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  task_no       VARCHAR(32)  NOT NULL COMMENT '任务编号(如FT20260811001)',
  record_id     BIGINT       NOT NULL COMMENT '发布记录ID',
  status        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态:1待处理 2处理中 3已完成 4已关闭(枚举FailTaskStatus)',
  assignee_id   BIGINT                DEFAULT NULL COMMENT '任务责任人(用户ID)',
  summary       VARCHAR(512)          DEFAULT NULL COMMENT '失败概述',
  fail_reason   VARCHAR(1000)         DEFAULT NULL COMMENT '失败原因(汇总)',
  fix_plan      VARCHAR(1000)         DEFAULT NULL COMMENT '修改方案(汇总)',
  creator_id    BIGINT                DEFAULT NULL COMMENT '创建人',
  handle_time   DATETIME              DEFAULT NULL COMMENT '处理完成时间',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted    TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_task_no (task_no),
  KEY idx_record (record_id),
  KEY idx_status (status),
  KEY idx_assignee (assignee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='失败聚合任务表';

-- ------------------------------------------------------------
-- 10. 失败用例明细表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS release_fail_case (
  id            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  task_id       BIGINT        NOT NULL COMMENT '聚合任务ID',
  case_type     VARCHAR(16)   NOT NULL DEFAULT 'failed' COMMENT '用例类型:failed失败 error错误',
  case_name     VARCHAR(512)  NOT NULL DEFAULT '' COMMENT '用例全名',
  case_log      LONGTEXT               DEFAULT NULL COMMENT '用例运行日志',
  fail_reason   VARCHAR(1000)          DEFAULT NULL COMMENT '失败原因(责任人填写)',
  fix_plan      VARCHAR(1000)          DEFAULT NULL COMMENT '修改方案(责任人填写)',
  is_bug        TINYINT                DEFAULT 0 COMMENT '是否提Bug:0否 1是',
  progress      TEXT                   DEFAULT NULL COMMENT '分析进展',
  conclusion    VARCHAR(1000)          DEFAULT NULL COMMENT '结论',
  ai_analysis   LONGTEXT               DEFAULT NULL COMMENT 'AI辅助分析描述(原因分析、修改建议等)',
  status        TINYINT       NOT NULL DEFAULT 1 COMMENT '状态:1待处理 2处理中 3已完成 4已关闭',
  assignee_id   BIGINT                 DEFAULT NULL COMMENT '责任人(默认继承任务责任人)',
  handle_time   DATETIME               DEFAULT NULL COMMENT '处理时间',
  create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted    TINYINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_task (task_id),
  KEY idx_status (status),
  KEY idx_assignee (assignee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='失败用例明细表';

-- ------------------------------------------------------------
-- 11. Wiki 文档表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wiki_doc (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  title         VARCHAR(255) NOT NULL COMMENT '文档标题',
  content       LONGTEXT              DEFAULT NULL COMMENT '正文(富文本HTML,入库前XSS白名单过滤)',
  parent_id     BIGINT       NOT NULL DEFAULT 0 COMMENT '父目录ID,0为根',
  sort          INT          NOT NULL DEFAULT 0 COMMENT '排序',
  creator_id    BIGINT                DEFAULT NULL COMMENT '创建人',
  editor_id     BIGINT                DEFAULT NULL COMMENT '最后编辑人',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted    TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Wiki文档表';

-- ------------------------------------------------------------
-- 12. 文件资源表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS file_resource (
  id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  file_name      VARCHAR(255) NOT NULL COMMENT '原始文件名',
  stored_path    VARCHAR(512) NOT NULL COMMENT '存储相对路径(禁止存绝对路径)',
  file_ext       VARCHAR(16)  NOT NULL COMMENT '后缀(小写)',
  mime_type      VARCHAR(128) NOT NULL COMMENT 'MIME类型',
  file_size      BIGINT       NOT NULL DEFAULT 0 COMMENT '文件大小(字节)',
  file_type      TINYINT      NOT NULL DEFAULT 2 COMMENT '类型:1测试报告 2普通附件(枚举FileType)',
  doc_id         BIGINT                DEFAULT NULL COMMENT '关联Wiki文档ID,可为空',
  uploader_id    BIGINT                DEFAULT NULL COMMENT '上传人',
  download_count INT          NOT NULL DEFAULT 0 COMMENT '下载次数',
  create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_doc (doc_id),
  KEY idx_type (file_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件资源表';

-- ============================================================
-- 初始化数据
-- ============================================================

-- 权限点（模块化扩展：新增业务模块时追加对应权限点）
INSERT INTO sys_permission (perm_code, perm_name, module, module_name) VALUES
  ('sonic:view', '版本发布-查看', 'sonic', 'Sonic版本发布'),
  ('sonic:edit', '版本发布-编辑', 'sonic', 'Sonic版本发布'),
  ('wiki:view', 'Wiki-查看', 'wiki', 'Wiki知识库'),
  ('wiki:edit', 'Wiki-编辑', 'wiki', 'Wiki知识库'),
  ('system:manage', '系统管理', 'system', '系统管理');

-- 预置角色：超管 / 普通管理员 / 员工
INSERT INTO sys_role (role_code, role_name, remark) VALUES
  ('super_admin', '超级管理员', '拥有全部权限'),
  ('admin', '普通管理员', '由超管分配指定模块操作权限'),
  ('employee', '普通员工', '全局只读,可处理被指派的失败任务');

-- 超管分配全部权限点
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.role_code = 'super_admin';

-- 员工分配查看权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.role_code = 'employee' AND p.perm_code IN ('sonic:view', 'wiki:view');

-- 注：超管账号 admin 由后端启动时初始化器创建（密码从环境变量 ADMIN_INIT_PASSWORD 读取，
--     BCrypt 加密入库，首次登录强制改密），避免明文密码落盘。
