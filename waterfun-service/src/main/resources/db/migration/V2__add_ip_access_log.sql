-- ============================================================
-- V2: Add ip_access_log table
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS ip_access_log (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    ip VARCHAR(45) NOT NULL COMMENT '访问IP',
    request_path VARCHAR(255) NOT NULL COMMENT '请求完整路径',
    request_method VARCHAR(10) NOT NULL COMMENT '请求方法',
    user_uid BIGINT UNSIGNED NULL COMMENT '用户UID(已认证时)',
    http_status SMALLINT UNSIGNED NULL COMMENT 'HTTP响应状态码',
    country VARCHAR(64) NOT NULL DEFAULT '' COMMENT '国家',
    province VARCHAR(64) NOT NULL DEFAULT '' COMMENT '省份/州',
    city VARCHAR(64) NOT NULL DEFAULT '' COMMENT '城市',
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    INDEX idx_ip (ip),
    INDEX idx_country (country),
    INDEX idx_created_at (created_at),
    INDEX idx_ip_created (ip, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IP访问日志表';

SET FOREIGN_KEY_CHECKS = 1;
