-- 初始化模型数据
-- 用于检测任务的默认模型

INSERT INTO model (id, name, version, status, accuracy, loaded_at, activated_at) 
VALUES (1, '胸部影像检测模型', '1.0', 'inactive', 0.4, NOW(), NOW());

INSERT INTO model (id, name, version, status, accuracy, loaded_at, activated_at) 
VALUES (2, '胸部影像检测模型', '2.0', 'inactive', 0.7, NOW(), NOW());
