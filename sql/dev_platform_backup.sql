-- MySQL dump 10.13  Distrib 8.0.29, for Win64 (x86_64)
--
-- Host: localhost    Database: dev_platform
-- ------------------------------------------------------
-- Server version	8.0.29

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `file_resource`
--

DROP TABLE IF EXISTS `file_resource`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `file_resource` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `file_name` varchar(255) NOT NULL COMMENT '原始文件名',
  `stored_path` varchar(512) NOT NULL COMMENT '存储相对路径(禁止存绝对路径)',
  `file_ext` varchar(16) NOT NULL COMMENT '后缀(小写)',
  `mime_type` varchar(128) NOT NULL COMMENT 'MIME类型',
  `file_size` bigint NOT NULL DEFAULT '0' COMMENT '文件大小(字节)',
  `file_type` tinyint NOT NULL DEFAULT '2' COMMENT '类型:1测试报告 2普通附件(枚举FileType)',
  `doc_id` bigint DEFAULT NULL COMMENT '关联Wiki文档ID,可为空',
  `uploader_id` bigint DEFAULT NULL COMMENT '上传人',
  `download_count` int NOT NULL DEFAULT '0' COMMENT '下载次数',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_doc` (`doc_id`),
  KEY `idx_type` (`file_type`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件资源表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `file_resource`
--

LOCK TABLES `file_resource` WRITE;
/*!40000 ALTER TABLE `file_resource` DISABLE KEYS */;
INSERT INTO `file_resource` VALUES (1,'success.html','report/2026/08/26/1787742366241_32658272.html','html','text/html',740,1,NULL,1,0,'2026-08-26 19:06:06','2026-08-26 19:06:07',1),(2,'failed.html','report/2026/08/26/1787742366446_50f64275.html','html','text/html',1288,1,NULL,1,0,'2026-08-26 19:06:06','2026-08-26 19:06:07',1),(3,'success.html','report/2026/08/26/1787742387111_890e08d1.html','html','text/html',740,1,NULL,1,0,'2026-08-26 19:06:27','2026-08-26 19:06:27',1),(6,'success.html','report/2026/08/26/1787742612831_5bdcbaf0.html','html','text/html',740,1,NULL,1,0,'2026-08-26 19:10:13','2026-08-26 19:10:13',1),(7,'failed.html','report/2026/08/26/1787742613067_6eae95d9.html','html','text/html',1288,1,NULL,1,0,'2026-08-26 19:10:13','2026-08-26 19:10:13',1),(8,'jenkins-report.html','report/2026/08/26/1787742613302_71a04c54.html','html','text/html',1288,1,NULL,NULL,0,'2026-08-26 19:10:13','2026-08-26 19:10:13',1),(9,'UXOS_GAEA_Daily_B20260811025754_UXOSV2-2-5-82-40-JY-20260707_uxos_25_TOPO01_2026-08-11-02-57-54_All_testReport.html','report/2026/08/26/1787742781645_f497c612.html','html','text/html',34998875,1,NULL,1,0,'2026-08-26 19:13:49','2026-08-26 19:13:49',0);
/*!40000 ALTER TABLE `file_resource` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `project`
--

DROP TABLE IF EXISTS `project`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_name` varchar(64) NOT NULL COMMENT '项目/机型名称(如 Gaea)',
  `project_code` varchar(64) NOT NULL COMMENT '项目编码',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态:1启用 0停用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_name` (`project_name`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目(机型)表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `project`
--

LOCK TABLES `project` WRITE;
/*!40000 ALTER TABLE `project` DISABLE KEYS */;
INSERT INTO `project` VALUES (1,'Gaea','GAEA','盖亚机型',1,'2026-08-26 19:01:13','2026-08-26 19:01:13',0),(2,'Ares','ARES','阿瑞斯机型',1,'2026-08-26 19:01:13','2026-08-26 19:01:13',0),(3,'Gemini','GEMINI','双子机型',1,'2026-08-26 19:01:13','2026-08-26 19:01:13',0),(4,'Uranus','URANUS','天王星机型',1,'2026-08-26 19:01:13','2026-08-26 19:01:13',0),(5,'Hera','HERA','赫拉机型',1,'2026-08-26 19:01:13','2026-08-26 19:01:13',0),(6,'E2E验证机X_del_6','E2E_del_6','自动化验证',1,'2026-08-26 19:06:05','2026-08-26 19:07:56',1),(7,'E2E验证机_del_7','E2E_del_7',NULL,1,'2026-08-26 19:06:26','2026-08-26 19:07:56',1),(8,'E2E验证机X_del_8','E2E_del_8','自动化验证',1,'2026-08-26 19:10:12','2026-08-26 19:10:13',1);
/*!40000 ALTER TABLE `project` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `release_fail_case`
--

DROP TABLE IF EXISTS `release_fail_case`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `release_fail_case` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_id` bigint NOT NULL COMMENT '聚合任务ID',
  `case_name` varchar(512) NOT NULL COMMENT '用例全名',
  `case_type` varchar(16) NOT NULL DEFAULT 'failed' COMMENT '用例类型:failed失败 error错误',
  `case_log` longtext COMMENT '用例运行日志',
  `fail_reason` varchar(1000) DEFAULT NULL COMMENT '失败原因(责任人填写)',
  `fix_plan` varchar(1000) DEFAULT NULL COMMENT '修改方案(责任人填写)',
  `is_bug` tinyint DEFAULT '0' COMMENT '是否提Bug:0否 1是',
  `progress` varchar(255) DEFAULT NULL COMMENT '分析进展',
  `conclusion` varchar(1000) DEFAULT NULL COMMENT '结论',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态:1待处理 2处理中 3已完成 4已关闭',
  `assignee_id` bigint DEFAULT NULL COMMENT '责任人(默认继承任务责任人)',
  `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_task` (`task_id`),
  KEY `idx_status` (`status`),
  KEY `idx_assignee` (`assignee_id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='失败用例明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `release_fail_case`
--

LOCK TABLES `release_fail_case` WRITE;
/*!40000 ALTER TABLE `release_fail_case` DISABLE KEYS */;
INSERT INTO `release_fail_case` VALUES (1,1,'test_login_failed[admin]','AssertionError: login failed\r\nexpected \'ok\', got \'err\'',NULL,NULL,1,NULL,NULL,'2026-08-26 19:06:06','2026-08-26 19:06:06',0),(2,1,'test_gaea_network','ConnectionError: network unreachable',NULL,NULL,1,NULL,NULL,'2026-08-26 19:06:06','2026-08-26 19:06:06',0),(3,1,'test_connect_timeout','TimeoutError: connect timeout',NULL,NULL,1,NULL,NULL,'2026-08-26 19:06:06','2026-08-26 19:06:06',0),(4,4,'test_login_failed[admin]','AssertionError: login failed\r\nexpected \'ok\', got \'err\'',NULL,NULL,1,NULL,NULL,'2026-08-26 19:10:13','2026-08-26 19:10:13',0),(5,4,'test_gaea_network','ConnectionError: network unreachable',NULL,NULL,1,NULL,NULL,'2026-08-26 19:10:13','2026-08-26 19:10:13',0),(6,4,'test_connect_timeout','TimeoutError: connect timeout',NULL,NULL,1,NULL,NULL,'2026-08-26 19:10:13','2026-08-26 19:10:13',0),(7,5,'test_login_failed[admin]','AssertionError: login failed\r\nexpected \'ok\', got \'err\'',NULL,NULL,1,NULL,NULL,'2026-08-26 19:10:13','2026-08-26 19:10:13',0),(8,5,'test_gaea_network','ConnectionError: network unreachable',NULL,NULL,1,NULL,NULL,'2026-08-26 19:10:13','2026-08-26 19:10:13',0),(9,5,'test_connect_timeout','TimeoutError: connect timeout',NULL,NULL,1,NULL,NULL,'2026-08-26 19:10:13','2026-08-26 19:10:13',0);
/*!40000 ALTER TABLE `release_fail_case` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `release_fail_task`
--

DROP TABLE IF EXISTS `release_fail_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `release_fail_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_no` varchar(32) NOT NULL COMMENT '任务编号(如FT20260811001)',
  `record_id` bigint NOT NULL COMMENT '发布记录ID',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态:1待处理 2处理中 3已完成 4已关闭(枚举FailTaskStatus)',
  `assignee_id` bigint DEFAULT NULL COMMENT '任务责任人(用户ID)',
  `summary` varchar(512) DEFAULT NULL COMMENT '失败概述',
  `fail_reason` varchar(1000) DEFAULT NULL COMMENT '失败原因(汇总)',
  `fix_plan` varchar(1000) DEFAULT NULL COMMENT '修改方案(汇总)',
  `creator_id` bigint DEFAULT NULL COMMENT '创建人',
  `handle_time` datetime DEFAULT NULL COMMENT '处理完成时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_no` (`task_no`),
  KEY `idx_record` (`record_id`),
  KEY `idx_status` (`status`),
  KEY `idx_assignee` (`assignee_id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='失败聚合任务表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `release_fail_task`
--

LOCK TABLES `release_fail_task` WRITE;
/*!40000 ALTER TABLE `release_fail_task` DISABLE KEYS */;
INSERT INTO `release_fail_task` VALUES (1,'FT20260826001',4,1,NULL,'发布 release/v3.1@3.0.1 存在 2 个失败用例','发布报告解析出 2 个失败用例、1 个错误用例，待定位',NULL,1,NULL,'2026-08-26 19:06:06','2026-08-26 19:06:07',1),(4,'FT20260826002',14,1,NULL,'发布 release/v3.1@3.0.1 存在 2 个失败用例','发布报告解析出 2 个失败用例、1 个错误用例，待定位',NULL,1,NULL,'2026-08-26 19:10:13','2026-08-26 19:10:13',1),(5,'FT20260826003',16,1,NULL,'发布 hotfix/multi@3.0.3 存在 2 个失败用例','发布报告解析出 2 个失败用例、1 个错误用例，待定位',NULL,NULL,NULL,'2026-08-26 19:10:13','2026-08-26 19:10:13',1);
/*!40000 ALTER TABLE `release_fail_task` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `release_record`
--

DROP TABLE IF EXISTS `release_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `release_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `branch` varchar(128) NOT NULL COMMENT '代码分支(第一展示维度)',
  `version` varchar(128) DEFAULT NULL COMMENT '镜像版本号(Environment.Version)',
  `image_url` varchar(500) DEFAULT NULL COMMENT '镜像地址',
  `result` tinyint NOT NULL COMMENT '发布结果:1成功 2失败(枚举ReleaseResult)',
  `report_file_id` bigint DEFAULT NULL COMMENT '测试报告文件ID(file_resource)',
  `total_count` int NOT NULL DEFAULT '0' COMMENT '用例总数',
  `passed_count` int NOT NULL DEFAULT '0',
  `failed_count` int NOT NULL DEFAULT '0',
  `error_count` int NOT NULL DEFAULT '0',
  `skipped_count` int NOT NULL DEFAULT '0',
  `duration_sec` decimal(10,2) DEFAULT NULL COMMENT '总耗时(秒)',
  `report_time` datetime DEFAULT NULL COMMENT '报告生成时间',
  `publisher_id` bigint DEFAULT NULL COMMENT '发布人(用户ID)',
  `publish_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `source` tinyint NOT NULL DEFAULT '1' COMMENT '来源:1人工上传 2Jenkins推送 3手动创建(枚举ReleaseSource)',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_branch` (`branch`),
  KEY `idx_version` (`version`),
  KEY `idx_result` (`result`),
  KEY `idx_publish_time` (`publish_time`),
  KEY `idx_report_time` (`report_time`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='版本发布记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `release_record`
--

LOCK TABLES `release_record` WRITE;
/*!40000 ALTER TABLE `release_record` DISABLE KEYS */;
INSERT INTO `release_record` VALUES (1,'release/v3.0','3.0.1',1,NULL,0,0,0,0,0,NULL,NULL,1,'2026-08-26 19:06:06',3,'手动创建-成功','2026-08-26 19:06:06','2026-08-26 19:06:07',1),(2,'release/v3.0','3.0.1',2,NULL,0,0,0,0,0,NULL,NULL,1,'2026-08-26 19:06:06',3,'手动创建-失败','2026-08-26 19:06:06','2026-08-26 19:06:07',1),(3,'master','3.0.1',1,1,12,10,0,0,2,18.00,'2026-08-26 16:00:00',1,'2026-08-26 19:06:06',1,'上传-成功','2026-08-26 19:06:06','2026-08-26 19:06:07',1),(4,'release/v3.1','3.0.1',2,2,12,8,2,1,1,22.50,'2026-08-26 16:30:00',1,'2026-08-26 19:06:06',1,'上传-失败','2026-08-26 19:06:06','2026-08-26 19:06:07',1),(5,'release/v3.0','3.0.1',1,NULL,0,0,0,0,0,NULL,NULL,1,'2026-08-26 19:06:27',3,'手动创建-成功','2026-08-26 19:06:26','2026-08-26 19:06:27',1),(6,'release/v3.0','3.0.1',2,NULL,0,0,0,0,0,NULL,NULL,1,'2026-08-26 19:06:27',3,'手动创建-失败','2026-08-26 19:06:27','2026-08-26 19:06:27',1),(7,'master','3.0.1',1,3,12,10,0,0,2,18.00,'2026-08-26 16:00:00',1,'2026-08-26 19:06:27',1,'上传-成功','2026-08-26 19:06:27','2026-08-26 19:06:27',1),(9,'hotfix/json','3.0.2',1,NULL,30,30,0,0,0,45.20,NULL,NULL,'2026-08-26 19:06:27',2,NULL,'2026-08-26 19:06:27','2026-08-26 19:06:27',1),(11,'release/v3.0','3.0.1',1,NULL,0,0,0,0,0,NULL,NULL,1,'2026-08-26 19:10:13',3,'手动创建-成功','2026-08-26 19:10:12','2026-08-26 19:10:13',1),(12,'release/v3.0','3.0.1',2,NULL,0,0,0,0,0,NULL,NULL,1,'2026-08-26 19:10:13',3,'手动创建-失败','2026-08-26 19:10:12','2026-08-26 19:10:13',1),(13,'master','3.0.1',1,6,12,10,0,0,2,18.00,'2026-08-26 16:00:00',1,'2026-08-26 19:10:13',1,'上传-成功','2026-08-26 19:10:13','2026-08-26 19:10:13',1),(14,'release/v3.1','3.0.1',2,7,12,8,2,1,1,22.50,'2026-08-26 16:30:00',1,'2026-08-26 19:10:13',1,'上传-失败','2026-08-26 19:10:13','2026-08-26 19:10:13',1),(15,'hotfix/json','3.0.2',1,NULL,30,30,0,0,0,45.20,NULL,NULL,'2026-08-26 19:10:13',2,NULL,'2026-08-26 19:10:13','2026-08-26 19:10:13',1),(16,'hotfix/multi','3.0.3',2,8,12,8,2,1,1,22.50,'2026-08-26 16:30:00',NULL,'2026-08-26 19:10:13',2,NULL,'2026-08-26 19:10:13','2026-08-26 19:10:13',1),(17,'UXOS_202205_main','UXOSV2_2.5.2.0_20260826',1,9,137,127,0,0,10,NULL,NULL,1,'2026-08-26 19:13:50',1,'','2026-08-26 19:13:49','2026-08-26 19:13:49',0);
/*!40000 ALTER TABLE `release_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `release_record_project`
--

DROP TABLE IF EXISTS `release_record_project`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `release_record_project` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `record_id` bigint NOT NULL COMMENT '发布记录ID',
  `project_id` bigint NOT NULL COMMENT '机型ID',
  `is_primary` tinyint NOT NULL DEFAULT '0' COMMENT '是否主机型:1是 0否',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_record` (`record_id`),
  KEY `idx_project` (`project_id`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='发布记录机型关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `release_record_project`
--

LOCK TABLES `release_record_project` WRITE;
/*!40000 ALTER TABLE `release_record_project` DISABLE KEYS */;
INSERT INTO `release_record_project` VALUES (1,1,6,1,'2026-08-26 19:06:06','2026-08-26 19:06:06',1),(2,2,6,1,'2026-08-26 19:06:06','2026-08-26 19:06:07',1),(3,3,6,1,'2026-08-26 19:06:06','2026-08-26 19:06:07',1),(4,4,6,1,'2026-08-26 19:06:06','2026-08-26 19:06:07',1),(5,5,7,1,'2026-08-26 19:06:26','2026-08-26 19:06:27',1),(6,6,7,1,'2026-08-26 19:06:27','2026-08-26 19:06:27',1),(7,7,7,1,'2026-08-26 19:06:27','2026-08-26 19:06:27',1),(9,9,1,1,'2026-08-26 19:06:27','2026-08-26 19:06:27',1),(12,11,8,1,'2026-08-26 19:10:12','2026-08-26 19:10:13',1),(13,12,8,1,'2026-08-26 19:10:12','2026-08-26 19:10:13',1),(14,13,8,1,'2026-08-26 19:10:13','2026-08-26 19:10:13',1),(15,14,8,1,'2026-08-26 19:10:13','2026-08-26 19:10:13',1),(16,15,1,1,'2026-08-26 19:10:13','2026-08-26 19:10:13',1),(17,16,1,1,'2026-08-26 19:10:13','2026-08-26 19:10:13',1),(18,16,2,0,'2026-08-26 19:10:13','2026-08-26 19:10:13',1),(19,17,1,1,'2026-08-26 19:13:49','2026-08-26 19:13:49',0);
/*!40000 ALTER TABLE `release_record_project` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_permission`
--

DROP TABLE IF EXISTS `sys_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `perm_code` varchar(64) NOT NULL COMMENT '权限编码(如 sonic:edit)',
  `perm_name` varchar(64) NOT NULL COMMENT '权限名称',
  `module` varchar(32) NOT NULL COMMENT '所属模块(sonic/wiki/system)',
  `module_name` varchar(64) NOT NULL COMMENT '模块名称',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_perm_code` (`perm_code`),
  KEY `idx_module` (`module`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='权限点表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_permission`
--

LOCK TABLES `sys_permission` WRITE;
/*!40000 ALTER TABLE `sys_permission` DISABLE KEYS */;
INSERT INTO `sys_permission` VALUES (1,'sonic:view','版本发布-查看','sonic','Sonic版本发布','2026-08-26 18:08:15','2026-08-26 18:08:15',0),(2,'sonic:edit','版本发布-编辑','sonic','Sonic版本发布','2026-08-26 18:08:15','2026-08-26 18:08:15',0),(3,'wiki:view','Wiki-查看','wiki','Wiki知识库','2026-08-26 18:08:15','2026-08-26 18:08:15',0),(4,'wiki:edit','Wiki-编辑','wiki','Wiki知识库','2026-08-26 18:08:15','2026-08-26 18:08:15',0),(5,'system:manage','系统管理','system','系统管理','2026-08-26 18:08:15','2026-08-26 18:08:15',0);
/*!40000 ALTER TABLE `sys_permission` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role`
--

DROP TABLE IF EXISTS `sys_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role_code` varchar(64) NOT NULL COMMENT '角色编码',
  `role_name` varchar(64) NOT NULL COMMENT '角色名称',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态:1正常 0停用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role`
--

LOCK TABLES `sys_role` WRITE;
/*!40000 ALTER TABLE `sys_role` DISABLE KEYS */;
INSERT INTO `sys_role` VALUES (1,'super_admin','超级管理员','拥有全部权限',1,'2026-08-26 18:08:15','2026-08-26 18:08:15',0),(2,'admin','普通管理员','由超管分配指定模块操作权限',1,'2026-08-26 18:08:15','2026-08-26 18:08:15',0),(3,'employee','普通员工','全局只读,可处理被指派的失败任务',1,'2026-08-26 18:08:15','2026-08-26 18:08:15',0),(4,'developer','Developer','test role',1,'2026-08-26 18:21:02','2026-08-26 18:21:58',1);
/*!40000 ALTER TABLE `sys_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role_permission`
--

DROP TABLE IF EXISTS `sys_role_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `permission_id` bigint NOT NULL COMMENT '权限点ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_role` (`role_id`),
  KEY `idx_perm` (`permission_id`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色权限关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role_permission`
--

LOCK TABLES `sys_role_permission` WRITE;
/*!40000 ALTER TABLE `sys_role_permission` DISABLE KEYS */;
INSERT INTO `sys_role_permission` VALUES (1,1,1,'2026-08-26 18:08:15','2026-08-26 18:08:15',0),(2,1,2,'2026-08-26 18:08:15','2026-08-26 18:08:15',0),(3,1,5,'2026-08-26 18:08:15','2026-08-26 18:08:15',0),(4,1,3,'2026-08-26 18:08:15','2026-08-26 18:08:15',0),(5,1,4,'2026-08-26 18:08:15','2026-08-26 18:08:15',0),(8,3,1,'2026-08-26 18:08:15','2026-08-26 18:08:15',0),(9,3,3,'2026-08-26 18:08:15','2026-08-26 18:08:15',0),(11,4,1,'2026-08-26 18:21:03','2026-08-26 18:21:58',1),(12,4,3,'2026-08-26 18:21:03','2026-08-26 18:21:58',1),(13,2,2,'2026-08-26 19:01:13','2026-08-26 19:01:13',0),(14,2,1,'2026-08-26 19:01:13','2026-08-26 19:01:13',0);
/*!40000 ALTER TABLE `sys_role_permission` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user`
--

DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` varchar(64) NOT NULL COMMENT '登录账号',
  `password` varchar(128) NOT NULL COMMENT '密码(BCrypt加密)',
  `nickname` varchar(64) NOT NULL COMMENT '姓名',
  `email` varchar(128) DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(32) DEFAULT NULL COMMENT '手机号',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态:1正常 0禁用',
  `must_change_pwd` tinyint NOT NULL DEFAULT '1' COMMENT '是否强制改密:1是 0否',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除:0未删 1已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user`
--

LOCK TABLES `sys_user` WRITE;
/*!40000 ALTER TABLE `sys_user` DISABLE KEYS */;
INSERT INTO `sys_user` VALUES (1,'admin','$2a$10$VBEtrb3mOpTM3nAfjIH.gOW2qHkfdlno4YY4D/.nbnQYXGBRDjqzO','超级管理员',NULL,NULL,1,0,NULL,'2026-08-26 18:17:39','2026-08-26 18:17:39',0),(2,'test01','$2a$10$tWlO7NwqplAmW4AzZ/KkBebT/r.oBLOYPQmaYPr0ZdgclO2JpJ6Ym','Test One',NULL,NULL,1,1,NULL,'2026-08-26 18:21:03','2026-08-26 18:21:58',1);
/*!40000 ALTER TABLE `sys_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user_role`
--

DROP TABLE IF EXISTS `sys_user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_role` (`role_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户角色关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user_role`
--

LOCK TABLES `sys_user_role` WRITE;
/*!40000 ALTER TABLE `sys_user_role` DISABLE KEYS */;
INSERT INTO `sys_user_role` VALUES (1,1,1,'2026-08-26 18:17:39','2026-08-26 18:17:39',0),(2,2,4,'2026-08-26 18:21:03','2026-08-26 18:21:58',1);
/*!40000 ALTER TABLE `sys_user_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `wiki_doc`
--

DROP TABLE IF EXISTS `wiki_doc`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wiki_doc` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title` varchar(255) NOT NULL COMMENT '文档标题',
  `content` longtext COMMENT '正文(富文本HTML,入库前XSS白名单过滤)',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父目录ID,0为根',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `creator_id` bigint DEFAULT NULL COMMENT '创建人',
  `editor_id` bigint DEFAULT NULL COMMENT '最后编辑人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Wiki文档表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wiki_doc`
--

LOCK TABLES `wiki_doc` WRITE;
/*!40000 ALTER TABLE `wiki_doc` DISABLE KEYS */;
/*!40000 ALTER TABLE `wiki_doc` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-08-26 19:16:52
