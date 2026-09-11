-- ============================================================
-- 阶段2初始化：示例机型数据 + admin 角色分配 Sonic 权限
-- 执行方式：mysql -uroot -p sonicsync < 002-release-init.sql
-- 幂等：机型按唯一键去重；角色权限按 NOT EXISTS 去重
-- ============================================================

-- 1. 示例机型（任务清单示例机型）
INSERT INTO project (project_name, project_code, description, status, is_deleted) VALUES
  ('Gaea',  'GAEA',   '盖亚机型',       1, 0),
  ('Ares',  'ARES',   '阿瑞斯机型',     1, 0),
  ('Gemini','GEMINI', '双子机型',       1, 0),
  ('Uranus','URANUS', '天王星机型',     1, 0),
  ('Hera',  'HERA',   '赫拉机型',       1, 0)
ON DUPLICATE KEY UPDATE project_code = VALUES(project_code);

-- 2. 给 admin 角色分配 Sonic 模块操作权限（查看 + 编辑）
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r, sys_permission p
WHERE r.role_code = 'admin'
  AND p.perm_code IN ('sonic:view', 'sonic:edit')
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- 3. 员工角色补充 Sonic 查看权限（与 init.sql 保持一致，幂等）
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r, sys_permission p
WHERE r.role_code = 'employee'
  AND p.perm_code IN ('sonic:view')
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
