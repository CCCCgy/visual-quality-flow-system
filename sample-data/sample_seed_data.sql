SET NAMES utf8mb4;

TRUNCATE TABLE capa_record;
TRUNCATE TABLE ncr_record;
TRUNCATE TABLE review_record;
TRUNCATE TABLE detection_result;
TRUNCATE TABLE inspection_image;
TRUNCATE TABLE inspection_task;
TRUNCATE TABLE production_batch;
TRUNCATE TABLE sys_user;

INSERT INTO sys_user (
  id, username, display_name, role_code, status, created_time, updated_time
) VALUES
  (1, 'admin_demo', 'Demo Admin', 'ADMIN', 'ACTIVE', '2026-07-05 09:00:00', '2026-07-05 09:00:00'),
  (2, 'inspector_demo', 'Demo Inspector', 'INSPECTOR', 'ACTIVE', '2026-07-05 09:00:00', '2026-07-05 09:00:00'),
  (3, 'qe_demo', 'Demo Quality Engineer', 'QUALITY_ENGINEER', 'ACTIVE', '2026-07-05 09:00:00', '2026-07-05 09:00:00');

INSERT INTO production_batch (
  id, batch_no, product_code, product_name, planned_quantity, status, created_by, remark, created_time, updated_time
) VALUES
  (1001, 'BATCH-DEMO-20260705-001', 'PROD-DEMO-PANEL', 'Demo Surface Panel', 120, 'CAPA_OPEN', 1, 'Demo batch for visual inspection workflow.', '2026-07-05 09:10:00', '2026-07-05 10:20:00'),
  (1002, 'BATCH-DEMO-20260705-002', 'PROD-DEMO-HOUSING', 'Demo Metal Housing', 80, 'INSPECTING', 1, 'Demo batch with pending review.', '2026-07-05 09:20:00', '2026-07-05 09:50:00');

INSERT INTO inspection_task (
  id, task_no, batch_id, model_name, model_version, source_type, status, created_by, imported_time, reviewed_time, created_time, updated_time
) VALUES
  (2001, 'TASK-DEMO-20260705-001', 1001, 'surface-defect-yolo', 'demo-v1.0', 'YOLO_JSON', 'CAPA_OPEN', 2, '2026-07-05 09:30:00', '2026-07-05 10:00:00', '2026-07-05 09:25:00', '2026-07-05 10:20:00'),
  (2002, 'TASK-DEMO-20260705-002', 1002, 'surface-defect-yolo', 'demo-v1.0', 'YOLO_JSON', 'REVIEWING', 2, '2026-07-05 09:55:00', NULL, '2026-07-05 09:45:00', '2026-07-05 09:58:00');

INSERT INTO inspection_image (
  id, task_id, batch_id, image_name, image_uri, width, height, status, created_time, updated_time
) VALUES
  (3001, 2001, 1001, 'demo-panel-001.jpg', 'sample-data/images/demo-panel-001.jpg', 1280, 960, 'REVIEWED', '2026-07-05 09:30:00', '2026-07-05 10:00:00'),
  (3002, 2001, 1001, 'demo-panel-002.jpg', 'sample-data/images/demo-panel-002.jpg', 1280, 960, 'REVIEWED', '2026-07-05 09:30:00', '2026-07-05 10:00:00'),
  (3003, 2002, 1002, 'demo-housing-001.jpg', 'sample-data/images/demo-housing-001.jpg', 1024, 768, 'DETECTED', '2026-07-05 09:55:00', '2026-07-05 09:55:00');

INSERT INTO detection_result (
  id, task_id, image_id, defect_type, confidence, bbox_x, bbox_y, bbox_w, bbox_h, status, raw_payload, created_time, updated_time
) VALUES
  (4001, 2001, 3001, 'scratch', 0.93215, 246.40, 188.20, 142.80, 36.50, 'CONFIRMED_DEFECT',
   JSON_OBJECT('defect_type', 'scratch', 'confidence', 0.93215, 'bbox', JSON_OBJECT('x', 246.40, 'y', 188.20, 'w', 142.80, 'h', 36.50)),
   '2026-07-05 09:30:00', '2026-07-05 09:58:00'),
  (4002, 2001, 3001, 'stain', 0.81730, 806.10, 512.90, 95.00, 88.40, 'FALSE_POSITIVE',
   JSON_OBJECT('defect_type', 'stain', 'confidence', 0.81730, 'bbox', JSON_OBJECT('x', 806.10, 'y', 512.90, 'w', 95.00, 'h', 88.40)),
   '2026-07-05 09:30:00', '2026-07-05 09:59:00'),
  (4003, 2001, 3002, 'dent', 0.76480, 533.00, 276.70, 61.50, 58.20, 'NEED_RECHECK',
   JSON_OBJECT('defect_type', 'dent', 'confidence', 0.76480, 'bbox', JSON_OBJECT('x', 533.00, 'y', 276.70, 'w', 61.50, 'h', 58.20)),
   '2026-07-05 09:30:00', '2026-07-05 10:00:00'),
  (4004, 2002, 3003, 'scratch', 0.88120, 118.60, 220.30, 76.40, 24.90, 'PENDING_REVIEW',
   JSON_OBJECT('defect_type', 'scratch', 'confidence', 0.88120, 'bbox', JSON_OBJECT('x', 118.60, 'y', 220.30, 'w', 76.40, 'h', 24.90)),
   '2026-07-05 09:55:00', '2026-07-05 09:55:00');

INSERT INTO review_record (
  id, detection_result_id, task_id, image_id, reviewer_id, review_result, review_comment, reviewed_time, created_time, updated_time
) VALUES
  (5001, 4001, 2001, 3001, 2, 'CONFIRM_DEFECT', 'Visible linear mark confirmed on demo image.', '2026-07-05 09:58:00', '2026-07-05 09:58:00', '2026-07-05 09:58:00'),
  (5002, 4002, 2001, 3001, 2, 'FALSE_POSITIVE', 'Lighting reflection marked as false positive.', '2026-07-05 09:59:00', '2026-07-05 09:59:00', '2026-07-05 09:59:00'),
  (5003, 4003, 2001, 3002, 2, 'NEED_RECHECK', 'Image requires recheck due to uncertain edge mark.', '2026-07-05 10:00:00', '2026-07-05 10:00:00', '2026-07-05 10:00:00');

INSERT INTO ncr_record (
  id, ncr_no, batch_id, task_id, detection_result_id, severity, status, description, created_by, closed_time, created_time, updated_time
) VALUES
  (6001, 'NCR-DEMO-20260705-001', 1001, 2001, 4001, 'MEDIUM', 'CAPA_CREATED', 'Confirmed scratch defect found during demo visual inspection.', 3, NULL, '2026-07-05 10:05:00', '2026-07-05 10:15:00');

INSERT INTO capa_record (
  id, capa_no, ncr_id, batch_id, owner_id, root_cause, corrective_action, preventive_action, status, due_date, closed_time, created_time, updated_time
) VALUES
  (7001, 'CAPA-DEMO-20260705-001', 6001, 1001, 3, 'Demo root cause: surface handling step requires confirmation.', 'Review handling checklist and isolate affected demo batch for recheck.', 'Add inspection checkpoint in demo process before final release.', 'IN_PROGRESS', '2026-07-12', NULL, '2026-07-05 10:15:00', '2026-07-05 10:20:00');
