use waterfun;
-- 用户主表
CREATE TABLE IF NOT EXISTS user (
  id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
  username             VARCHAR(50) UNIQUE NOT NULL,
  password_hash        VARCHAR(255) NOT NULL,
  role                 ENUM ('USER', 'ADMIN', 'MANAGER','OPERATOR')           DEFAULT 'USER',
  account_status       ENUM ('ACTIVE', 'SUSPENDED', 'DEACTIVATED', 'DELETED') DEFAULT 'ACTIVE',
  status_changed_at    TIMESTAMP    NULL,
  status_change_reason VARCHAR(255),
  created_at           TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  updated_at           TIMESTAMP    NULL ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB ROW_FORMAT=COMPRESSED;

-- 用户敏感信息表
CREATE TABLE IF NOT EXISTS user_data (
  user_id BIGINT NOT NULL,
  email             VARCHAR(255) NULL, -- AES
  phone             VARCHAR(20)  NULL, -- AES
  address           TEXT NULL,
  created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at        TIMESTAMP    NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id),
  FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE
) ENGINE=InnoDB ENCRYPTION='Y';

CREATE TABLE IF NOT EXISTS user_profile(
    user_id BIGINT NOT NULL ,
    nickname VARCHAR(50),
    avatar_url VARCHAR(255),
    bio TEXT,
    gender ENUM('MALE', 'FEMALE', 'OTHER', 'UNKNOWN') DEFAULT 'UNKNOWN',
    PRIMARY KEY (user_id),
    FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE
)ENGINE=InnoDB;

-- 数据归档表
CREATE TABLE IF NOT EXISTS user_data_archive (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  table_name VARCHAR(50) NOT NULL,
  original_data JSON NOT NULL,
  archived_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  archived_by VARCHAR(50) NOT NULL,
  reason VARCHAR(255) NOT NULL,
  INDEX idx_archive_user (user_id),
  INDEX idx_archive_time (archived_at)
) ENGINE=InnoDB;

-- 审计日志表
CREATE TABLE IF NOT EXISTS account_audit_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  action ENUM('create','update','disable','delete','restore','status_change','level_change') NOT NULL,
  action_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  performed_by VARCHAR(50),
  ip_address VARCHAR(45),
  device_info VARCHAR(255),
  old_value JSON,
  new_value JSON,
  INDEX idx_user_actions (user_id, action, action_time),
  INDEX idx_action_time (action_time)
) ENGINE=InnoDB;

-- 创建等级表
CREATE TABLE  IF NOT EXISTS user_level (
      id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(50) NOT NULL,
  description VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建用户等级关联表
CREATE TABLE IF NOT EXISTS user_level_mapping (
  user_id BIGINT NOT NULL,
  level_id INT NOT NULL,
  is_active BOOLEAN DEFAULT TRUE,
  valid_from TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  valid_to TIMESTAMP NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, level_id),
  FOREIGN KEY (user_id) REFERENCES user(id),
  FOREIGN KEY (level_id) REFERENCES user_level(id)
);

-- 添加索引提高查询性能
CREATE INDEX idx_user_level_mapping ON user_level_mapping(user_id, is_active);