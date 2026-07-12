DROP TABLE IF EXISTS ip_ban;
CREATE TABLE IF NOT EXISTS ip_ban (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    ip          VARCHAR(45) NOT NULL COMMENT 'IP地址，支持IPv6',
    reason      VARCHAR(100) NOT NULL DEFAULT '' COMMENT '封禁原因',
    banned_at   DATETIME NOT NULL DEFAULT NOW(),
    expires_at  DATETIME COMMENT 'NULL=永久封禁',

    UNIQUE KEY uk_ip (ip)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IP封禁表';

DROP TABLE IF EXISTS audit_log;
CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    -- 用户标识
    user_id BIGINT,
    username VARCHAR(50),
    -- 操作类型
    action TINYINT UNSIGNED DEFAULT 0 NOT NULL,
    -- 设备/网络信息
    ip VARCHAR(45) NOT NULL,
    device_info JSON,
    -- 地理位置（Java 查 IP 归属地）
    country VARCHAR(50),
    province VARCHAR(50),
    city VARCHAR(50),

    status TINYINT UNSIGNED NOT NULL DEFAULT 1, -- 1=成功，0=失败
    fail_reason VARCHAR(64) DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),

    INDEX idx_user_id (user_id),
    INDEX idx_action (action),
    INDEX idx_created_at (created_at),
    INDEX idx_ip (ip)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS user_activity_log;
CREATE TABLE IF NOT EXISTS user_activity_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    action_type TINYINT UNSIGNED NOT NULL COMMENT '操作类型',
    business_type TINYINT UNSIGNED NOT NULL COMMENT '业务类型',
    target_id BIGINT COMMENT '操作对象ID，例如帖子ID、评论ID等',
    ip VARCHAR(45) NOT NULL COMMENT '操作IP地址',
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),

    INDEX idx_user_time (user_id, created_at),
    INDEX idx_action_time (action_type, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS site_statistics;
CREATE TABLE IF NOT EXISTS site_statistics (
    stat_date DATE PRIMARY KEY,
    login_count INT UNSIGNED DEFAULT 0 COMMENT '登陆人次',
    daily_pv INT UNSIGNED DEFAULT 0 COMMENT '访问次数（PV）',
    new_users INT UNSIGNED DEFAULT 0 COMMENT '今日注册',
    new_posts INT UNSIGNED DEFAULT 0 COMMENT '今日新帖',
    peak_online INT UNSIGNED DEFAULT 0 COMMENT '在线峰值',
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)
);