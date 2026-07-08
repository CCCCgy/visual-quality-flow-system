SET NAMES utf8mb4;

DROP TABLE IF EXISTS capa_record;
DROP TABLE IF EXISTS ncr_record;
DROP TABLE IF EXISTS review_record;
DROP TABLE IF EXISTS detection_result;
DROP TABLE IF EXISTS inspection_image;
DROP TABLE IF EXISTS inspection_task;
DROP TABLE IF EXISTS production_batch;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户主键',
  username VARCHAR(64) NOT NULL COMMENT '用户名',
  display_name VARCHAR(64) NOT NULL COMMENT '展示名称',
  role_code VARCHAR(32) NOT NULL COMMENT '角色编码',
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '用户状态：ACTIVE/DISABLED',
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_user_username (username),
  KEY idx_sys_user_role_code (role_code),
  KEY idx_sys_user_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统用户表';

CREATE TABLE production_batch (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '批次主键',
  batch_no VARCHAR(64) NOT NULL COMMENT '批次编号',
  product_code VARCHAR(64) NOT NULL COMMENT '产品编码',
  product_name VARCHAR(128) NOT NULL COMMENT '产品名称',
  planned_quantity INT NOT NULL DEFAULT 0 COMMENT '计划数量',
  status VARCHAR(32) NOT NULL DEFAULT 'CREATED' COMMENT '批次状态：CREATED/INSPECTING/NCR_OPEN/CAPA_OPEN/CLOSED',
  created_by BIGINT NOT NULL COMMENT '创建人用户ID，逻辑外键',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_production_batch_batch_no (batch_no),
  KEY idx_production_batch_status (status),
  KEY idx_production_batch_created_by (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='生产批次表';

CREATE TABLE inspection_task (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '检测任务主键',
  task_no VARCHAR(64) NOT NULL COMMENT '检测任务编号',
  batch_id BIGINT NOT NULL COMMENT '所属批次ID，逻辑外键',
  model_name VARCHAR(128) NOT NULL COMMENT '模型名称，不保存权重路径',
  model_version VARCHAR(64) NOT NULL COMMENT '模型版本',
  source_type VARCHAR(32) NOT NULL DEFAULT 'YOLO_JSON' COMMENT '来源类型',
  status VARCHAR(32) NOT NULL DEFAULT 'CREATED' COMMENT '任务状态：CREATED/WAIT_REVIEW/REVIEWED/CLOSED/CANCELLED',
  created_by BIGINT NOT NULL COMMENT '创建人用户ID，逻辑外键',
  imported_time DATETIME DEFAULT NULL COMMENT '导入时间',
  reviewed_time DATETIME DEFAULT NULL COMMENT '复核完成时间',
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_inspection_task_task_no (task_no),
  KEY idx_inspection_task_batch_id (batch_id),
  KEY idx_inspection_task_status (status),
  KEY idx_inspection_task_created_by (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='检测任务表';

CREATE TABLE inspection_image (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '图片主键',
  task_id BIGINT NOT NULL COMMENT '所属检测任务ID，逻辑外键',
  batch_id BIGINT NOT NULL COMMENT '所属批次ID，逻辑外键',
  image_name VARCHAR(128) NOT NULL COMMENT '图片名称',
  image_uri VARCHAR(255) NOT NULL COMMENT '图片相对路径或资源key',
  width INT DEFAULT NULL COMMENT '图片宽度',
  height INT DEFAULT NULL COMMENT '图片高度',
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '图片状态：PENDING/DETECTED/REVIEWED',
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_inspection_image_task_id (task_id),
  KEY idx_inspection_image_batch_id (batch_id),
  KEY idx_inspection_image_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='检测图片表';

CREATE TABLE detection_result (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '检测结果主键',
  task_id BIGINT NOT NULL COMMENT '所属检测任务ID，逻辑外键',
  image_id BIGINT NOT NULL COMMENT '所属图片ID，逻辑外键',
  class_id INT NOT NULL COMMENT '模型类别ID',
  class_name VARCHAR(64) NOT NULL COMMENT '模型类别名称',
  confidence DECIMAL(6,5) NOT NULL COMMENT '模型置信度',
  bbox_x1 DECIMAL(10,2) NOT NULL COMMENT '缺陷框左上角x坐标',
  bbox_y1 DECIMAL(10,2) NOT NULL COMMENT '缺陷框左上角y坐标',
  bbox_x2 DECIMAL(10,2) NOT NULL COMMENT '缺陷框右下角x坐标',
  bbox_y2 DECIMAL(10,2) NOT NULL COMMENT '缺陷框右下角y坐标',
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING_REVIEW' COMMENT '结果状态：PENDING_REVIEW/CONFIRMED_DEFECT/FALSE_POSITIVE/NEED_RECHECK',
  raw_payload JSON DEFAULT NULL COMMENT '原始检测片段JSON',
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_detection_result_task_id (task_id),
  KEY idx_detection_result_image_id (image_id),
  KEY idx_detection_result_status (status),
  KEY idx_detection_result_class_id (class_id),
  KEY idx_detection_result_class_name (class_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='检测结果表';

CREATE TABLE review_record (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '复核记录主键',
  detection_result_id BIGINT NOT NULL COMMENT '检测结果ID，逻辑外键',
  task_id BIGINT NOT NULL COMMENT '检测任务ID，逻辑外键',
  image_id BIGINT NOT NULL COMMENT '图片ID，逻辑外键',
  reviewer_id BIGINT NOT NULL COMMENT '复核人用户ID，逻辑外键',
  review_result VARCHAR(32) NOT NULL COMMENT '复核结果：CONFIRMED_DEFECT/FALSE_POSITIVE/NEED_RECHECK',
  review_comment VARCHAR(500) DEFAULT NULL COMMENT '复核说明',
  reviewed_time DATETIME NOT NULL COMMENT '复核时间',
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_review_detection_result_id (detection_result_id),
  KEY idx_review_record_task_id (task_id),
  KEY idx_review_record_image_id (image_id),
  KEY idx_review_record_reviewer_id (reviewer_id),
  KEY idx_review_record_review_result (review_result)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='人工复核记录表';

CREATE TABLE ncr_record (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'NCR主键',
  ncr_no VARCHAR(64) NOT NULL COMMENT 'NCR编号',
  batch_id BIGINT NOT NULL COMMENT '所属批次ID，逻辑外键',
  task_id BIGINT NOT NULL COMMENT '来源检测任务ID，逻辑外键',
  detection_result_id BIGINT NOT NULL COMMENT '来源检测结果ID，逻辑外键',
  review_id BIGINT NOT NULL COMMENT '来源复核记录ID，逻辑外键',
  severity VARCHAR(32) NOT NULL COMMENT '严重程度：LOW/MEDIUM/HIGH',
  status VARCHAR(32) NOT NULL DEFAULT 'OPEN' COMMENT 'NCR状态：OPEN/CAPA_CREATED/CLOSED/CANCELLED',
  description VARCHAR(1000) NOT NULL COMMENT '问题描述',
  created_by BIGINT NOT NULL COMMENT '创建人用户ID，逻辑外键',
  closed_time DATETIME DEFAULT NULL COMMENT '关闭时间',
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_ncr_record_ncr_no (ncr_no),
  UNIQUE KEY uk_ncr_review_id (review_id),
  KEY idx_ncr_record_batch_id (batch_id),
  KEY idx_ncr_record_task_id (task_id),
  KEY idx_ncr_record_detection_result_id (detection_result_id),
  KEY idx_ncr_record_status (status),
  KEY idx_ncr_record_severity (severity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='不合格记录NCR表';

CREATE TABLE capa_record (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'CAPA主键',
  capa_no VARCHAR(64) NOT NULL COMMENT 'CAPA编号',
  ncr_id BIGINT NOT NULL COMMENT '来源NCR ID，逻辑外键',
  batch_id BIGINT NOT NULL COMMENT '所属批次ID，逻辑外键',
  owner_id BIGINT NOT NULL COMMENT '负责人用户ID，逻辑外键',
  root_cause VARCHAR(1000) DEFAULT NULL COMMENT '根因分析',
  corrective_action VARCHAR(1000) DEFAULT NULL COMMENT '纠正措施',
  preventive_action VARCHAR(1000) DEFAULT NULL COMMENT '预防措施',
  verify_result VARCHAR(1000) DEFAULT NULL COMMENT '验证结果',
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING_ANALYSIS' COMMENT 'CAPA状态：PENDING_ANALYSIS/IN_PROGRESS/PENDING_VERIFY/CLOSED/CANCELLED',
  due_date DATE DEFAULT NULL COMMENT '计划完成日期',
  closed_time DATETIME DEFAULT NULL COMMENT '关闭时间',
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_capa_record_capa_no (capa_no),
  UNIQUE KEY uk_capa_ncr_id (ncr_id),
  KEY idx_capa_record_batch_id (batch_id),
  KEY idx_capa_record_owner_id (owner_id),
  KEY idx_capa_record_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='CAPA整改记录表';
