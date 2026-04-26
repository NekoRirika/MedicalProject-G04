-- 与 init.sql 一致：必须先选中库，否则在 IDEA 里单独执行本文件时会在默认库（或无库）下执行导致全部失败
USE chest_imaging;

-- 插入管理员数据
INSERT INTO `user` (`username`, `name`, `department`, `role`, `password`, `status`)
VALUES
('admin001', '管理员001', '管理部', 'admin', 'admin001@123', 'active'),
('admin002', '管理员002', '管理部', 'admin', 'admin002@123', 'active'),
('admin003', '管理员003', '管理部', 'admin', 'admin003@123', 'active'),
('admin004', '管理员004', '管理部', 'admin', 'admin004@123', 'active'),
('admin005', '管理员005', '管理部', 'admin', 'admin005@123', 'active');

-- 插入科研人员数据
INSERT INTO `user` (`username`, `name`, `department`, `role`, `password`, `status`)
VALUES
('researcher001', '科研人员001', '科研部', 'researcher', 'researcher001@123', 'active'),
('researcher002', '科研人员002', '科研部', 'researcher', 'researcher002@123', 'active'),
('researcher003', '科研人员003', '科研部', 'researcher', 'researcher003@123', 'active'),
('researcher004', '科研人员004', '科研部', 'researcher', 'researcher004@123', 'active'),
('researcher005', '科研人员005', '科研部', 'researcher', 'researcher005@123', 'active'),
('researcher006', '科研人员006', '科研部', 'researcher', 'researcher006@123', 'active'),
('researcher007', '科研人员007', '科研部', 'researcher', 'researcher007@123', 'active'),
('researcher008', '科研人员008', '科研部', 'researcher', 'researcher008@123', 'active'),
('researcher009', '科研人员009', '科研部', 'researcher', 'researcher009@123', 'active'),
('researcher010', '科研人员010', '科研部', 'researcher', 'researcher010@123', 'active');

-- 插入医生数据（50位，10个部门，每个部门5位）
-- 心内科
INSERT INTO `user` (`username`, `name`, `department`, `role`, `password`, `status`)
VALUES
('doctor_xnk001', '心内科医生001', '心内科', 'doctor', 'doctor_xnk001@123', 'active'),
('doctor_xnk002', '心内科医生002', '心内科', 'doctor', 'doctor_xnk002@123', 'active'),
('doctor_xnk003', '心内科医生003', '心内科', 'doctor', 'doctor_xnk003@123', 'active'),
('doctor_xnk004', '心内科医生004', '心内科', 'doctor', 'doctor_xnk004@123', 'active'),
('doctor_xnk005', '心内科医生005', '心内科', 'doctor', 'doctor_xnk005@123', 'active');

-- 呼吸科
INSERT INTO `user` (`username`, `name`, `department`, `role`, `password`, `status`)
VALUES
('doctor_hxk001', '呼吸科医生001', '呼吸科', 'doctor', 'doctor_hxk001@123', 'active'),
('doctor_hxk002', '呼吸科医生002', '呼吸科', 'doctor', 'doctor_hxk002@123', 'active'),
('doctor_hxk003', '呼吸科医生003', '呼吸科', 'doctor', 'doctor_hxk003@123', 'active'),
('doctor_hxk004', '呼吸科医生004', '呼吸科', 'doctor', 'doctor_hxk004@123', 'active'),
('doctor_hxk005', '呼吸科医生005', '呼吸科', 'doctor', 'doctor_hxk005@123', 'active');

-- 消化科
INSERT INTO `user` (`username`, `name`, `department`, `role`, `password`, `status`)
VALUES
('doctor_xhk001', '消化科医生001', '消化科', 'doctor', 'doctor_xhk001@123', 'active'),
('doctor_xhk002', '消化科医生002', '消化科', 'doctor', 'doctor_xhk002@123', 'active'),
('doctor_xhk003', '消化科医生003', '消化科', 'doctor', 'doctor_xhk003@123', 'active'),
('doctor_xhk004', '消化科医生004', '消化科', 'doctor', 'doctor_xhk004@123', 'active'),
('doctor_xhk005', '消化科医生005', '消化科', 'doctor', 'doctor_xhk005@123', 'active');

-- 神经内科
INSERT INTO `user` (`username`, `name`, `department`, `role`, `password`, `status`)
VALUES
('doctor_sjnk001', '神经内科医生001', '神经内科', 'doctor', 'doctor_sjnk001@123', 'active'),
('doctor_sjnk002', '神经内科医生002', '神经内科', 'doctor', 'doctor_sjnk002@123', 'active'),
('doctor_sjnk003', '神经内科医生003', '神经内科', 'doctor', 'doctor_sjnk003@123', 'active'),
('doctor_sjnk004', '神经内科医生004', '神经内科', 'doctor', 'doctor_sjnk004@123', 'active'),
('doctor_sjnk005', '神经内科医生005', '神经内科', 'doctor', 'doctor_sjnk005@123', 'active');

-- 神经外科
INSERT INTO `user` (`username`, `name`, `department`, `role`, `password`, `status`)
VALUES
('doctor_sjwk001', '神经外科医生001', '神经外科', 'doctor', 'doctor_sjwk001@123', 'active'),
('doctor_sjwk002', '神经外科医生002', '神经外科', 'doctor', 'doctor_sjwk002@123', 'active'),
('doctor_sjwk003', '神经外科医生003', '神经外科', 'doctor', 'doctor_sjwk003@123', 'active'),
('doctor_sjwk004', '神经外科医生004', '神经外科', 'doctor', 'doctor_sjwk004@123', 'active'),
('doctor_sjwk005', '神经外科医生005', '神经外科', 'doctor', 'doctor_sjwk005@123', 'active');

-- 骨科
INSERT INTO `user` (`username`, `name`, `department`, `role`, `password`, `status`)
VALUES
('doctor_gk001', '骨科医生001', '骨科', 'doctor', 'doctor_gk001@123', 'active'),
('doctor_gk002', '骨科医生002', '骨科', 'doctor', 'doctor_gk002@123', 'active'),
('doctor_gk003', '骨科医生003', '骨科', 'doctor', 'doctor_gk003@123', 'active'),
('doctor_gk004', '骨科医生004', '骨科', 'doctor', 'doctor_gk004@123', 'active'),
('doctor_gk005', '骨科医生005', '骨科', 'doctor', 'doctor_gk005@123', 'active');

-- 普外科
INSERT INTO `user` (`username`, `name`, `department`, `role`, `password`, `status`)
VALUES
('doctor_pwk001', '普外科医生001', '普外科', 'doctor', 'doctor_pwk001@123', 'active'),
('doctor_pwk002', '普外科医生002', '普外科', 'doctor', 'doctor_pwk002@123', 'active'),
('doctor_pwk003', '普外科医生003', '普外科', 'doctor', 'doctor_pwk003@123', 'active'),
('doctor_pwk004', '普外科医生004', '普外科', 'doctor', 'doctor_pwk004@123', 'active'),
('doctor_pwk005', '普外科医生005', '普外科', 'doctor', 'doctor_pwk005@123', 'active');

-- 胸外科
INSERT INTO `user` (`username`, `name`, `department`, `role`, `password`, `status`)
VALUES
('doctor_xwk001', '胸外科医生001', '胸外科', 'doctor', 'doctor_xwk001@123', 'active'),
('doctor_xwk002', '胸外科医生002', '胸外科', 'doctor', 'doctor_xwk002@123', 'active'),
('doctor_xwk003', '胸外科医生003', '胸外科', 'doctor', 'doctor_xwk003@123', 'active'),
('doctor_xwk004', '胸外科医生004', '胸外科', 'doctor', 'doctor_xwk004@123', 'active'),
('doctor_xwk005', '胸外科医生005', '胸外科', 'doctor', 'doctor_xwk005@123', 'active');

-- 妇产科
INSERT INTO `user` (`username`, `name`, `department`, `role`, `password`, `status`)
VALUES
('doctor_fck001', '妇产科医生001', '妇产科', 'doctor', 'doctor_fck001@123', 'active'),
('doctor_fck002', '妇产科医生002', '妇产科', 'doctor', 'doctor_fck002@123', 'active'),
('doctor_fck003', '妇产科医生003', '妇产科', 'doctor', 'doctor_fck003@123', 'active'),
('doctor_fck004', '妇产科医生004', '妇产科', 'doctor', 'doctor_fck004@123', 'active'),
('doctor_fck005', '妇产科医生005', '妇产科', 'doctor', 'doctor_fck005@123', 'active');

-- 儿科
INSERT INTO `user` (`username`, `name`, `department`, `role`, `password`, `status`)
VALUES
('doctor_ek001', '儿科医生001', '儿科', 'doctor', 'doctor_ek001@123', 'active'),
('doctor_ek002', '儿科医生002', '儿科', 'doctor', 'doctor_ek002@123', 'active'),
('doctor_ek003', '儿科医生003', '儿科', 'doctor', 'doctor_ek003@123', 'active'),
('doctor_ek004', '儿科医生004', '儿科', 'doctor', 'doctor_ek004@123', 'active'),
('doctor_ek005', '儿科医生005', '儿科', 'doctor', 'doctor_ek005@123', 'active');