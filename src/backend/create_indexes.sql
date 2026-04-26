USE chest_imaging;

-- ============================================
-- 1. medical_case 表索引
-- ============================================

-- case_id 字段索引（精确查询频繁）
CREATE INDEX idx_case_id ON medical_case(case_id);

-- created_at 字段索引（时间范围查询）
CREATE INDEX idx_created_at ON medical_case(created_at);

-- status 字段索引（状态筛选）
CREATE INDEX idx_status ON medical_case(status);

-- created_by 字段索引（按创建人查询）
CREATE INDEX idx_created_by ON medical_case(created_by);

-- 复合索引：用于病例列表分页查询（状态+时间）
CREATE INDEX idx_status_created_at ON medical_case(status, created_at);

-- ============================================
-- 2. image 表索引
-- ============================================

-- case_id 字段索引（外键查询）
CREATE INDEX idx_image_case_id ON image(case_id);

-- uploaded_at 字段索引（时间排序）
CREATE INDEX idx_uploaded_at ON image(uploaded_at);

-- 复合索引：按病例查询影像并按时间排序
CREATE INDEX idx_case_uploaded ON image(case_id, uploaded_at);

-- ============================================
-- 3. detection 表索引
-- ============================================

-- case_id 字段索引（关联查询）
CREATE INDEX idx_detection_case_id ON detection(case_id);

-- status 字段索引（状态筛选）
CREATE INDEX idx_detection_status ON detection(status);

-- 复合索引：按病例查询检测记录并按状态筛选
CREATE INDEX idx_case_status ON detection(case_id, status);

-- created_at 字段索引（时间范围查询）
CREATE INDEX idx_detection_created_at ON detection(created_at);

-- ============================================
-- 4. audit_log 表索引
-- ============================================

-- operator 字段索引（按操作人查询）
CREATE INDEX idx_audit_operator ON audit_log(operator);

-- operation_time 字段索引（时间范围查询）
CREATE INDEX idx_audit_operation_time ON audit_log(operation_time);

-- operation_type 字段索引（操作类型筛选）
CREATE INDEX idx_audit_operation_type ON audit_log(operation_type);

-- 复合索引：按操作人和时间查询
CREATE INDEX idx_operator_time ON audit_log(operator, operation_time);

-- ============================================
-- 5. user 表索引
-- ============================================

-- username 字段索引（登录查询，UNIQUE已包含索引，此处为显式说明）
-- 注意：username已有UNIQUE约束，会自动创建索引

-- role 字段索引（权限筛选）
CREATE INDEX idx_user_role ON user(role);

-- department 字段索引（部门筛选）
CREATE INDEX idx_user_department ON user(department);

-- ============================================
-- 6. feedback 表索引
-- ============================================

-- detection_id 字段索引（外键查询）
CREATE INDEX idx_feedback_detection_id ON feedback(detection_id);

-- created_at 字段索引（时间范围查询）
CREATE INDEX idx_feedback_created_at ON feedback(created_at);

-- created_by 字段索引（按创建人查询）
CREATE INDEX idx_feedback_created_by ON feedback(created_by);

-- ============================================
-- 7. model 表索引
-- ============================================

-- status 字段索引（状态筛选）
CREATE INDEX idx_model_status ON model(status);

-- name 字段索引（名称查询）
CREATE INDEX idx_model_name ON model(name);

-- ============================================
-- 验证索引创建结果
-- ============================================

-- 查看各表索引
SHOW INDEX FROM medical_case;
SHOW INDEX FROM image;
SHOW INDEX FROM detection;
SHOW INDEX FROM audit_log;
SHOW INDEX FROM user;
SHOW INDEX FROM feedback;
SHOW INDEX FROM model;
