-- ----------------------------------------------------------------------------
-- OA · 请假申请（P2 第一条业务流）
-- 执行方式（由你执行）：
--   mysql --host=127.0.0.1 --port=3306 --user=root --password=*** --default-character-set=utf8mb4 ry-vue \
--         -e "source D:/coder_data/RuoYi-Vue/sql/oa/01_oa_leave.sql"
-- 说明：只新建 oa_leave 表与 OA 菜单，不修改任何 sys_* 既有数据。
-- ----------------------------------------------------------------------------

-- ----------------------------
-- 1. 请假申请表（只新建，不删除任何已有对象）
-- ----------------------------
CREATE TABLE IF NOT EXISTS oa_leave (
  leave_id            BIGINT(20)      NOT NULL AUTO_INCREMENT COMMENT '请假单ID',
  apply_user_id       BIGINT(20)      NOT NULL                COMMENT '申请人ID（sys_user.user_id）',
  apply_user_name     VARCHAR(64)     DEFAULT ''              COMMENT '申请人姓名（冗余，列表直出）',
  apply_dept_id       BIGINT(20)      DEFAULT NULL            COMMENT '申请部门ID（sys_dept.dept_id）',
  leave_type          CHAR(1)         NOT NULL                COMMENT '请假类型（字典 oa_leave_type：1事假 2病假 3年假 4调休 5婚假 6产假）',
  start_time          DATETIME        NOT NULL                COMMENT '开始时间',
  end_time            DATETIME        NOT NULL                COMMENT '结束时间',
  duration            DECIMAL(5,1)    NOT NULL DEFAULT 0.0    COMMENT '请假时长（天）',
  reason              VARCHAR(500)    DEFAULT ''              COMMENT '请假事由',
  attachment_url      VARCHAR(500)    DEFAULT ''              COMMENT '附件地址',
  process_instance_id VARCHAR(64)     DEFAULT ''              COMMENT 'Flowable 流程实例ID',
  status              CHAR(1)         DEFAULT '0'             COMMENT '状态（0草稿 1审批中 2已通过 3已驳回 4已撤销）',
  apply_time          DATETIME        DEFAULT NULL            COMMENT '提交时间',
  create_by           VARCHAR(64)     DEFAULT ''              COMMENT '创建者',
  create_time         DATETIME        DEFAULT NULL            COMMENT '创建时间',
  update_by           VARCHAR(64)     DEFAULT ''              COMMENT '更新者',
  update_time         DATETIME        DEFAULT NULL            COMMENT '更新时间',
  remark              VARCHAR(500)    DEFAULT NULL            COMMENT '备注',
  PRIMARY KEY (leave_id),
  KEY idx_leave_user (apply_user_id),
  KEY idx_leave_status (status),
  KEY idx_leave_proc (process_instance_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='OA-请假申请';

-- ----------------------------
-- 2. 字典：请假类型
-- ----------------------------
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
VALUES ('OA-请假类型', 'oa_leave_type', '0', 'admin', SYSDATE(), 'OA 请假类型');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
VALUES
 (1, '事假', '1', 'oa_leave_type', '', 'info',    'Y', '0', 'admin', SYSDATE(), ''),
 (2, '病假', '2', 'oa_leave_type', '', 'warning', 'N', '0', 'admin', SYSDATE(), ''),
 (3, '年假', '3', 'oa_leave_type', '', 'success', 'N', '0', 'admin', SYSDATE(), ''),
 (4, '调休', '4', 'oa_leave_type', '', 'primary', 'N', '0', 'admin', SYSDATE(), ''),
 (5, '婚假', '5', 'oa_leave_type', '', 'danger',  'N', '0', 'admin', SYSDATE(), ''),
 (6, '产假', '6', 'oa_leave_type', '', 'danger',  'N', '0', 'admin', SYSDATE(), '');

-- ----------------------------
-- 3. 菜单：OA办公 / 请假申请
-- ----------------------------
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES ('OA办公', 0, 5, 'oa', NULL, 1, 0, 'M', '0', '0', '', 'documentation', 'admin', SYSDATE(), 'OA 业务目录');

SET @oa_dir_id = LAST_INSERT_ID();

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES ('请假申请', @oa_dir_id, 1, 'leave', 'oa/leave/index', 1, 0, 'C', '0', '0', 'oa:leave:list', 'date', 'admin', SYSDATE(), '请假申请菜单');

SET @oa_leave_menu_id = LAST_INSERT_ID();

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES
 ('请假查询', @oa_leave_menu_id, 1, '', NULL, 1, 0, 'F', '0', '0', 'oa:leave:query',  '#', 'admin', SYSDATE(), ''),
 ('请假新增', @oa_leave_menu_id, 2, '', NULL, 1, 0, 'F', '0', '0', 'oa:leave:add',    '#', 'admin', SYSDATE(), ''),
 ('请假修改', @oa_leave_menu_id, 3, '', NULL, 1, 0, 'F', '0', '0', 'oa:leave:edit',   '#', 'admin', SYSDATE(), ''),
 ('请假删除', @oa_leave_menu_id, 4, '', NULL, 1, 0, 'F', '0', '0', 'oa:leave:remove', '#', 'admin', SYSDATE(), ''),
 ('请假导出', @oa_leave_menu_id, 5, '', NULL, 1, 0, 'F', '0', '0', 'oa:leave:export', '#', 'admin', SYSDATE(), ''),
 ('请假提交', @oa_leave_menu_id, 6, '', NULL, 1, 0, 'F', '0', '0', 'oa:leave:submit', '#', 'admin', SYSDATE(), ''),
 ('请假审批', @oa_leave_menu_id, 7, '', NULL, 1, 0, 'F', '0', '0', 'oa:leave:approve','#', 'admin', SYSDATE(), '');
