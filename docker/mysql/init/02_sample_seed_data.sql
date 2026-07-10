-- 文件职责：
-- Docker MySQL 首次初始化演示数据，构造用户、批次、检测任务、图片、检测结果、人工复核、NCR 和 CAPA 的可追溯样例。
-- 注意事项：
-- 本文件使用固定 ID 方便演示和接口调试；集成测试不会依赖这些固定自增 ID，而是生成唯一编号。
USE visual_qms;

SET NAMES utf8mb4;

-- 清空顺序从下游到上游，保证逻辑关联链不会残留旧数据。
TRUNCATE TABLE capa_record;
TRUNCATE TABLE ncr_record;
TRUNCATE TABLE review_record;
TRUNCATE TABLE detection_result;
TRUNCATE TABLE inspection_image;
TRUNCATE TABLE inspection_task;
TRUNCATE TABLE production_batch;
TRUNCATE TABLE sys_user;

-- 样例用户：
-- 1=管理员，2=检测/复核人员，3=质量工程师；后端会用这些 ID 校验 createdBy、reviewerId、ownerId。
INSERT INTO sys_user (
  id, username, display_name, role_code, status, created_time, updated_time
) VALUES
  (1, 'admin_demo', 'Demo Admin', 'ADMIN', 'ACTIVE', '2026-07-05 09:00:00', '2026-07-05 09:00:00'),
  (2, 'inspector_demo', 'Demo Inspector', 'INSPECTOR', 'ACTIVE', '2026-07-05 09:00:00', '2026-07-05 09:00:00'),
  (3, 'qe_demo', 'Demo Quality Engineer', 'QUALITY_ENGINEER', 'ACTIVE', '2026-07-05 09:00:00', '2026-07-05 09:00:00');

-- 样例批次：
-- 1001 已进入 CAPA_OPEN，演示确认缺陷到 CAPA 的链路；
-- 1002 仍在 INSPECTING，演示待复核数据。
INSERT INTO production_batch (
  id, batch_no, product_code, product_name, planned_quantity, status, created_by, remark, created_time, updated_time
) VALUES
  (1001, 'BATCH-DEMO-20260705-001', 'PROD-DEMO-CERAMIC', 'Demo Ceramic Surface Part', 120, 'CAPA_OPEN', 1, 'Demo batch for closed visual inspection workflow.', '2026-07-05 09:10:00', '2026-07-05 10:20:00'),
  (1002, 'BATCH-DEMO-20260705-002', 'PROD-DEMO-CERAMIC', 'Demo Ceramic Surface Part', 80, 'INSPECTING', 1, 'Demo batch with pending review.', '2026-07-05 09:20:00', '2026-07-05 09:50:00');

-- 样例检测任务：
-- 2001 属于批次 1001，已完成复核；2002 属于批次 1002，等待人工复核。
INSERT INTO inspection_task (
  id, task_no, batch_id, model_name, model_version, source_type, status, created_by, imported_time, reviewed_time, created_time, updated_time
) VALUES
  (2001, 'TASK-DEMO-20260705-001', 1001, 'surface-defect-yolo', 'demo-v1.0', 'YOLO_JSON', 'REVIEWED', 2, '2026-07-05 09:30:00', '2026-07-05 10:00:00', '2026-07-05 09:25:00', '2026-07-05 10:20:00'),
  (2002, 'TASK-DEMO-20260705-002', 1002, 'surface-defect-yolo', 'demo-v1.0', 'YOLO_JSON', 'WAIT_REVIEW', 2, '2026-07-05 09:55:00', NULL, '2026-07-05 09:45:00', '2026-07-05 09:58:00');

-- 样例图片：
-- 3001/3002 归属任务 2001，已复核；3003 归属任务 2002，处于 DETECTED。
INSERT INTO inspection_image (
  id, task_id, batch_id, image_name, image_uri, width, height, status, created_time, updated_time
) VALUES
  (3001, 2001, 1001, 'demo-ceramic-001.jpg', 'sample-data/images/demo-ceramic-001.jpg', 1280, 960, 'REVIEWED', '2026-07-05 09:30:00', '2026-07-05 10:00:00'),
  (3002, 2001, 1001, 'demo-ceramic-002.jpg', 'sample-data/images/demo-ceramic-002.jpg', 1280, 960, 'REVIEWED', '2026-07-05 09:30:00', '2026-07-05 10:00:00'),
  (3003, 2002, 1002, 'demo-ceramic-003.jpg', 'sample-data/images/demo-ceramic-003.jpg', 1024, 768, 'DETECTED', '2026-07-05 09:55:00', '2026-07-05 09:55:00');

-- 样例检测结果：
-- 4001/4002/4003 已有人工结论；4004 保持 PENDING_REVIEW，供前端人工复核演示。
-- raw_payload 保留模型原始 box 片段，便于和 bbox_x1...bbox_y2 对照。
INSERT INTO detection_result (
  id, task_id, image_id, class_id, class_name, confidence,
  bbox_x1, bbox_y1, bbox_x2, bbox_y2,
  status, raw_payload, created_time, updated_time
) VALUES
  (
    4001, 2001, 3001, 1, 'DS', 0.93215,
    246.40, 188.20, 389.20, 224.70,
    'CONFIRMED_DEFECT',
    JSON_OBJECT(
      'class_id', 1,
      'class_name', 'DS',
      'confidence', 0.93215,
      'bbox_xyxy', JSON_ARRAY(246.40, 188.20, 389.20, 224.70)
    ),
    '2026-07-05 09:30:00', '2026-07-05 09:58:00'
  ),
  (
    4002, 2001, 3001, 2, 'GS', 0.81730,
    806.10, 512.90, 901.10, 601.30,
    'FALSE_POSITIVE',
    JSON_OBJECT(
      'class_id', 2,
      'class_name', 'GS',
      'confidence', 0.81730,
      'bbox_xyxy', JSON_ARRAY(806.10, 512.90, 901.10, 601.30)
    ),
    '2026-07-05 09:30:00', '2026-07-05 09:59:00'
  ),
  (
    4003, 2001, 3002, 3, 'SS', 0.76480,
    533.00, 276.70, 594.50, 334.90,
    'NEED_RECHECK',
    JSON_OBJECT(
      'class_id', 3,
      'class_name', 'SS',
      'confidence', 0.76480,
      'bbox_xyxy', JSON_ARRAY(533.00, 276.70, 594.50, 334.90)
    ),
    '2026-07-05 09:30:00', '2026-07-05 10:00:00'
  ),
  (
    4004, 2002, 3003, 1, 'DS', 0.88120,
    118.60, 220.30, 195.00, 245.20,
    'PENDING_REVIEW',
    JSON_OBJECT(
      'class_id', 1,
      'class_name', 'DS',
      'confidence', 0.88120,
      'bbox_xyxy', JSON_ARRAY(118.60, 220.30, 195.00, 245.20)
    ),
    '2026-07-05 09:55:00', '2026-07-05 09:55:00'
  );

-- 样例复核记录：
-- 5001 确认 4001 为缺陷，可追溯到后续 NCR；
-- 5002 标记误报，5003 标记需复检，二者不会进入 NCR。
INSERT INTO review_record (
  id, detection_result_id, task_id, image_id, reviewer_id,
  review_result, review_comment, reviewed_time, created_time, updated_time
) VALUES
  (5001, 4001, 2001, 3001, 2, 'CONFIRMED_DEFECT', 'Confirmed DS defect after manual review.', '2026-07-05 09:58:00', '2026-07-05 09:58:00', '2026-07-05 09:58:00'),
  (5002, 4002, 2001, 3001, 2, 'FALSE_POSITIVE', 'Marked as false positive after visual check.', '2026-07-05 09:59:00', '2026-07-05 09:59:00', '2026-07-05 09:59:00'),
  (5003, 4003, 2001, 3002, 2, 'NEED_RECHECK', 'Needs recheck due to ambiguous local texture.', '2026-07-05 10:00:00', '2026-07-05 10:00:00', '2026-07-05 10:00:00');

-- 样例 NCR：
-- 6001 来自复核记录 5001，并追溯到检测结果 4001、任务 2001、批次 1001。
-- 状态 CAPA_CREATED 表示已进入 CAPA 阶段。
INSERT INTO ncr_record (
  id, ncr_no, batch_id, task_id, detection_result_id, review_id,
  severity, status, description, created_by, closed_time, created_time, updated_time
) VALUES
  (
    6001,
    'NCR-DEMO-20260705-001',
    1001,
    2001,
    4001,
    5001,
    'MEDIUM',
    'CAPA_CREATED',
    'Confirmed DS defect in demo ceramic surface inspection batch.',
    3,
    NULL,
    '2026-07-05 10:05:00',
    '2026-07-05 10:10:00'
  );

-- 样例 CAPA：
-- 7001 来自 NCR 6001，负责人为用户 3；状态 IN_PROGRESS 表示整改仍在进行中。
INSERT INTO capa_record (
  id, capa_no, ncr_id, batch_id, owner_id,
  root_cause, corrective_action, preventive_action, verify_result,
  status, due_date, closed_time, created_time, updated_time
) VALUES
  (
    7001,
    'CAPA-DEMO-20260705-001',
    6001,
    1001,
    3,
    'Possible local surface process fluctuation in the demo workflow.',
    'Adjust process parameters and perform additional visual inspection on the affected batch.',
    'Add review checklist for similar DS-like defects in future batches.',
    NULL,
    'IN_PROGRESS',
    '2026-07-12',
    NULL,
    '2026-07-05 10:10:00',
    '2026-07-05 10:20:00'
  );
