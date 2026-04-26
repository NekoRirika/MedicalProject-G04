-- 创建数据库
CREATE DATABASE IF NOT EXISTS chest_imaging CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE chest_imaging;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `username` VARCHAR(50) UNIQUE NOT NULL,
  `name` VARCHAR(100) NOT NULL,
  `department` VARCHAR(100) NOT NULL,
  `role` ENUM('admin', 'doctor', 'researcher') NOT NULL,
  `password` VARCHAR(100) NOT NULL,
  `status` ENUM('active', 'locked') DEFAULT 'active',
  `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `last_login` DATETIME DEFAULT NULL
);

-- 病例表
CREATE TABLE IF NOT EXISTS `medical_case` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `case_id` BIGINT UNIQUE NOT NULL,
  `patient_name` VARCHAR(100) NOT NULL,
  `patient_gender` ENUM('男', '女'),
  `patient_id_card` VARCHAR(18),
  `patient_phone` VARCHAR(20),
  `patient_birthday` DATE,
  `case_description` TEXT NOT NULL,
  `check_date` DATE,
  `check_note` TEXT,
  `status` ENUM('待上传影像', '待检测', '已检测') DEFAULT '待上传影像',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `created_by` VARCHAR(50) NOT NULL
);

-- 影像表
CREATE TABLE IF NOT EXISTS `image` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `case_id` BIGINT NOT NULL,
  `file_name` VARCHAR(255) NOT NULL,
  `file_path` VARCHAR(500) NOT NULL,
  `file_size` BIGINT NOT NULL,
  `uploaded_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `uploaded_by` VARCHAR(50) NOT NULL,
  FOREIGN KEY (`case_id`) REFERENCES `medical_case`(`id`) ON DELETE CASCADE
);

-- 模型表
CREATE TABLE IF NOT EXISTS `model` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL,
  `version` VARCHAR(50) NOT NULL,
  `status` ENUM('loaded', 'active') DEFAULT 'loaded',
  `accuracy` DECIMAL(5,2),
  `loaded_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `activated_at` TIMESTAMP NULL
);

-- 检测表
CREATE TABLE IF NOT EXISTS `detection` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `case_id` BIGINT NOT NULL,
  `model_id` BIGINT NOT NULL,
  `status` ENUM('pending', 'processing', 'completed', 'failed') DEFAULT 'pending',
  `result` JSON,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `completed_at` TIMESTAMP NULL,
  FOREIGN KEY (`case_id`) REFERENCES `medical_case`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`model_id`) REFERENCES `model`(`id`)
);

-- 反馈表
CREATE TABLE IF NOT EXISTS `feedback` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `detection_id` BIGINT NOT NULL,
  `evaluation` ENUM('准确', '漏检', '误检') NOT NULL,
  `feedback` TEXT,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `created_by` VARCHAR(50) NOT NULL,
  FOREIGN KEY (`detection_id`) REFERENCES `detection`(`id`) ON DELETE CASCADE
);

-- 审计日志表
CREATE TABLE IF NOT EXISTS `audit_log` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `operation_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `operator` VARCHAR(50) NOT NULL,
  `operation_type` VARCHAR(50) NOT NULL,
  `operation_content` TEXT NOT NULL,
  `ip_address` VARCHAR(50) NOT NULL,
  `status` VARCHAR(20) NOT NULL,
  `details` TEXT
);

-- 用户token表
CREATE TABLE IF NOT EXISTS `user_token` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `username` VARCHAR(50) NOT NULL,
  `token` TEXT NOT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `expires_at` DATETIME NOT NULL,
  INDEX `idx_username` (`username`),
  INDEX `idx_expires_at` (`expires_at`)
);

-- 创建user_token表
CREATE TABLE IF NOT EXISTS `user_token` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `username` VARCHAR(50) NOT NULL,
  `token` TEXT NOT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `expires_at` DATETIME NOT NULL,
  INDEX `idx_username` (`username`),
  INDEX `idx_expires_at` (`expires_at`)
);
