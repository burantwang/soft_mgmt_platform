-- ============================================================
-- DVS任务模块初始化（独立于 DailySanity/WeeklySanity，数据不共享）
-- 执行方式：mysql -uroot -proot dev_platform < 007-dvs-init.sql
-- 幂等说明：建表使用 CREATE TABLE IF NOT EXISTS
-- 权限复用 Sonic 模块（sonic:view / sonic:edit），无需新增权限点
-- ============================================================

-- 1. DVS测试报告表（每个 HTML 模块一条，按机型拆分）
CREATE TABLE IF NOT EXISTS dvs_report (
  id             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  branch         VARCHAR(128)  NOT NULL COMMENT '代码分支',
  version        VARCHAR(128)  DEFAULT NULL COMMENT '镜像版本号',
  project_id     BIGINT        DEFAULT NULL COMMENT '机型ID(project)',
  module_name    VARCHAR(255)  DEFAULT NULL COMMENT '模块名(HTML文件名)',
  report_file_id BIGINT        DEFAULT NULL COMMENT '测试报告文件ID(file_resource)',
  total_count    INT           NOT NULL DEFAULT 0 COMMENT '用例总数',
  passed_count   INT           NOT NULL DEFAULT 0 COMMENT '通过数',
  failed_count   INT           NOT NULL DEFAULT 0 COMMENT '失败数',
  error_count    INT           NOT NULL DEFAULT 0 COMMENT '错误数',
  skipped_count  INT           NOT NULL DEFAULT 0 COMMENT '跳过数',
  duration_sec   DECIMAL(10,2) DEFAULT NULL COMMENT '总耗时(秒)',
  report_time    DATETIME      DEFAULT NULL COMMENT '报告生成时间',
  publisher_id   BIGINT        DEFAULT NULL COMMENT '上传人(用户ID)',
  publish_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  remark         VARCHAR(255)  DEFAULT NULL COMMENT '备注',
  create_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted     TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除:0未删 1已删',
  PRIMARY KEY (id),
  KEY idx_dvs_report_publish (publish_time),
  KEY idx_dvs_report_project (project_id),
  KEY idx_dvs_report_branch (branch)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='DVS测试报告表(每个HTML一条)';

-- 2. DVS失败用例明细表
CREATE TABLE IF NOT EXISTS dvs_fail_case (
  id                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  report_id         BIGINT        NOT NULL COMMENT 'DVS报告ID(dvs_report.id)',
  case_type         VARCHAR(16)   NOT NULL DEFAULT 'failed' COMMENT '用例类型:failed失败 error错误',
  case_name         VARCHAR(512)  NOT NULL DEFAULT '' COMMENT '用例全名',
  case_log          LONGTEXT      DEFAULT NULL COMMENT '用例运行日志',
  fail_reason       VARCHAR(1000) DEFAULT NULL COMMENT '失败原因(责任人填写)',
  fix_plan          VARCHAR(1000) DEFAULT NULL COMMENT '修改方案(责任人填写)',
  is_bug            TINYINT       DEFAULT 0 COMMENT '是否提Bug:0否 1是',
  progress          VARCHAR(1000) DEFAULT NULL COMMENT '分析进展',
  conclusion        VARCHAR(1000) DEFAULT NULL COMMENT '结论',
  ai_analysis       LONGTEXT      DEFAULT NULL COMMENT 'AI辅助分析描述',
  ai_analysis_correct TINYINT     DEFAULT NULL COMMENT 'AI分析是否正确:1是 0否',
  ai_root_cause     LONGTEXT      DEFAULT NULL COMMENT 'AI分析根因',
  ai_evidence       LONGTEXT      DEFAULT NULL COMMENT 'AI分析佐证',
  ai_solution       LONGTEXT      DEFAULT NULL COMMENT 'AI解决建议',
  bug_no            VARCHAR(64)   DEFAULT NULL COMMENT 'Bug单号(Redmine)',
  issue_category    VARCHAR(64)   DEFAULT NULL COMMENT '问题分类',
  status            TINYINT       NOT NULL DEFAULT 1 COMMENT '状态:1待处理 2处理中 3已修复 4非缺陷',
  assignee_id       BIGINT        DEFAULT NULL COMMENT '责任人(用户ID)',
  handle_time       DATETIME      DEFAULT NULL COMMENT '处理时间',
  create_time       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted        TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除:0未删 1已删',
  PRIMARY KEY (id),
  KEY idx_dvs_case_report (report_id),
  KEY idx_dvs_case_status (status),
  KEY idx_dvs_case_assignee (assignee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='DVS失败用例明细表';
