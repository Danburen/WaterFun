use waterfun;
DROP TABLE IF EXISTS audit_task_resource;
DROP TABLE IF EXISTS audit_task;
DROP TABLE IF EXISTS user_inbox;
DROP TABLE IF EXISTS audit_task_resource;
DROP TABLE IF EXISTS banner;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS audit_task;
CREATE TABLE IF NOT EXISTS audit_task (
    id              BIGINT UNSIGNED PRIMARY KEY,
    target_id       VARCHAR(64) DEFAULT NOT NULL COMMENT '目标ID，例如帖子ID、评论ID、用户ID等，具体含义由target_type决定',
    target_type     TINYINT UNSIGNED NOT NULL COMMENT '目标类型：0-未知/默认 1-头像 2-...',
    trigger_type    TINYINT UNSIGNED DEFAULT 0 NOT NULL COMMENT '触发类型：0-未知/默认 1-用户提交 2-系统检测...',
    trigger_source  VARCHAR(255) NULL COMMENT '触发来源，例如用户提交的内容摘要、系统检测的证据等',
    priority        TINYINT UNSIGNED DEFAULT 2 NOT NULL COMMENT '优先级：越小越紧急',
    format          TINYINT UNSIGNED DEFAULT 0 NOT NULL COMMENT 'payload的格式，0-未知/默认 1-富文本 2-图片...',
    status          TINYINT UNSIGNED DEFAULT 1 NOT NULL COMMENT '审计状态: 1-待审 2-通过 3-拒绝',
    payload         JSON NULL COMMENT '审计的主要内容，根据上层决定',
    audit_at        TIMESTAMP(3) NULL,
    auditor         BIGINT UNSIGNED NULL,
    reject_type     TINYINT UNSIGNED NULL COMMENT '拒绝类型',
    reject_reason   VARCHAR(255) NULL,
    submitter       BIGINT UNSIGNED NOT NULL COMMENT '提交人ID',
    submit_at       TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    updated_at      TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) NOT NULL,
    pending_target_id VARCHAR(64) GENERATED ALWAYS AS (
            CASE WHEN status = 1 THEN target_id ELSE NULL END
            ) STORED COMMENT '待审时的目标ID，非待审时为NULL',
    FOREIGN KEY (auditor) REFERENCES user(uid) ON DELETE SET NULL,
    FOREIGN KEY (submitter) REFERENCES user(uid) ON DELETE RESTRICT,
    UNIQUE KEY uk_target_type_pending_only (target_type, pending_target_id),
    INDEX idx_target_status (target_id, status),
    INDEX idx_status_submit (status, submit_at)
)ENGINE=InnoDB COMMENT '审计任务表';

DROP TABLE IF EXISTS audit_task_resource;
CREATE TABLE IF NOT EXISTS audit_task_resource (
    task_id BIGINT UNSIGNED NOT NULL COMMENT '审计任务ID',
    resource_uuid VARCHAR(36) NOT NULL COMMENT '资源UUID',
    status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '资源审计状态 1-待审 2-通过 3-拒绝',
    audit_at TIMESTAMP(3) NULL,
    auditor BIGINT UNSIGNED NULL,
    reject_type TINYINT UNSIGNED NULL COMMENT '拒绝类型：1-涉黄 2-暴恐 3-政治敏感 4-广告 5-其他',
    reject_reason VARCHAR(255) NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) NOT NULL,
    PRIMARY KEY (task_id, resource_uuid),
    FOREIGN KEY (task_id) REFERENCES audit_task(id) ON DELETE CASCADE,
    FOREIGN KEY (auditor) REFERENCES user(uid) ON DELETE SET NULL,
    FOREIGN KEY (resource_uuid) REFERENCES resource(uuid) ON DELETE CASCADE ,
    INDEX idx_task_resource (task_id),
    INDEX idx_task_status (task_id, status),
    INDEX idx_status_created (status, created_at)
)ENGINE=InnoDB COMMENT '审计任务资源表';

DROP TABLE IF EXISTS resource;
CREATE TABLE IF NOT EXISTS resource (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    uuid CHAR(36) NOT NULL COMMENT '资源UUID',
    resource_key VARCHAR(255) NOT NULL COMMENT 'COS对象Key',
    resource_type TINYINT UNSIGNED NOT NULL COMMENT '资源类型',
    mime_type VARCHAR(128) NULL COMMENT 'MIME类型',
    size_bytes BIGINT UNSIGNED NULL COMMENT '资源大小（字节）',
    file_meta JSON NULL COMMENT '文件元信息，例如图片的宽高、视频的时长等',

    source_type TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '来源类型',
    uploader_id BIGINT UNSIGNED NULL COMMENT '上传者ID, 空表示系统',
    expired_at TIMESTAMP(3) NULL COMMENT '过期时间，空表示不过期',
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) NOT NULL,
    status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '资源状态：0-上传挂起 1-有效 2-游离 3-已删除',
    UNIQUE KEY uk_resource_key (resource_key),
    UNIQUE KEY uk_resource_uuid (uuid),
    INDEX idx_type_created (resource_type, created_at)
)ENGINE=InnoDB COMMENT '资源表';

DROP TABLE IF EXISTS post_resource;
CREATE TABLE IF NOT EXISTS post_resource (
    post_id        BIGINT UNSIGNED NOT NULL COMMENT '帖子ID',
    resource_uuid  CHAR(36) NOT NULL COMMENT '资源UUID',
    created_at     TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,

    PRIMARY KEY (post_id, resource_uuid),
    INDEX idx_resource_uuid (resource_uuid),

    FOREIGN KEY (post_id) REFERENCES post(id) ON DELETE CASCADE,
    FOREIGN KEY (resource_uuid) REFERENCES resource(uuid) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT '帖子-资源关联表';
# 用户收件箱
DROP TABLE IF EXISTS inbox;

CREATE TABLE inbox (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_uid BIGINT UNSIGNED NOT NULL,
    notice_type TINYINT UNSIGNED NOT NULL COMMENT '0- 通用消息 1-LIKE消息...',
    business_type TINYINT UNSIGNED NULL COMMENT '业务类型，NULL 表示不区分业务, 0-通用...',
    target_id VARCHAR(64) NULL COMMENT '关联资源ID',

    sender_id BIGINT UNSIGNED NULL COMMENT 'NULL表示无目标用户（例如系统通知）',

    title VARCHAR(64) NOT NULL,
    content JSON NOT NULL COMMENT '结构化内容，包含title、summary、extra等',
    priority TINYINT UNSIGNED DEFAULT 3 COMMENT '1-紧急 2-重要 3-普通 4-低优先级',

    is_aggregated BOOLEAN DEFAULT FALSE COMMENT '是否聚合通知',
    aggregate_count INT UNSIGNED DEFAULT 1 COMMENT '聚合通知的计数',

    is_read BOOLEAN DEFAULT FALSE NOT NULL COMMENT '是否已读',
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL COMMENT '是否已删除',
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    FOREIGN KEY (user_uid) REFERENCES user(uid) ON DELETE CASCADE,
    FOREIGN KEY (sender_id) REFERENCES user(uid) ON DELETE SET NULL,
    INDEX idx_user_time (user_uid, created_at DESC),
    INDEX idx_user_read (user_uid, is_read, created_at DESC),
    INDEX idx_user_read_type (user_uid, is_read, notice_type, created_at DESC),
    INDEX idx_dedupe (user_uid, notice_type, business_type, target_id, created_at DESC)

) ENGINE=InnoDB COMMENT='用户系统通知收件箱';

DROP TABLE IF EXISTS banner;
CREATE TABLE banner (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    resource_uuid VARCHAR(36) NULL COMMENT '资源uuid',
    title VARCHAR(64) NOT NULL,
    subtitle VARCHAR(128) NULL,
    link_url VARCHAR(255) NULL,
    position TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '显示位置',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号（从0开始）',
    status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：1-显示 2-隐藏',
    start_at TIMESTAMP(3) NULL COMMENT '开始显示时间',
    end_at TIMESTAMP(3) NULL COMMENT '结束显示时间',
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL COMMENT '是否已删除',
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) NOT NULL,
    FOREIGN KEY (resource_uuid) REFERENCES resource(uuid) ON DELETE SET NULL,
    INDEX idx_position_sort (position, sort_no),
    INDEX idx_status_sort (status, sort_no),
    INDEX idx_time(start_at, end_at, status)
) ENGINE=InnoDB COMMENT='系统轮播图表';


DROP TABLE IF EXISTS ticket_resource;
DROP TABLE IF EXISTS ticket;
CREATE TABLE IF NOT EXISTS ticket (
    id              BIGINT UNSIGNED PRIMARY KEY,
    ticket_type     TINYINT UNSIGNED NOT NULL COMMENT '工单类型',
    target_id       VARCHAR(64) DEFAULT NULL COMMENT '目标ID，例如帖子ID、评论ID、用户ID等，具体含义由target_type决定',
    target_type     TINYINT UNSIGNED NOT NULL COMMENT '目标类型：0-未知/默认 1-头像 2-...',
    status          TINYINT UNSIGNED DEFAULT 1 NOT NULL COMMENT '审计状态: 1-待审 2-通过 3-拒绝',
    penalty_type    TINYINT UNSIGNED DEFAULT 0 COMMENT '处罚类型，通常是申诉/举报特有',
    content         TEXT NOT NULL COMMENT '工单内容',
    submitter       BIGINT UNSIGNED NOT NULL COMMENT '提交人ID',
    auditor         BIGINT UNSIGNED NULL,
    audit_at        TIMESTAMP(3) NULL,
    audit_note      VARCHAR(255) NULL COMMENT '审核备注',
    reply_content   VARCHAR(2000) NULL COMMENT '管理员回复内容',
    target_user_uid BIGINT UNSIGNED NULL COMMENT '目标用户UID（举报/处罚涉及的用户）',
    reject_type     TINYINT UNSIGNED NULL COMMENT '拒绝类型',
    updated_at      TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) NOT NULL,
    created_at      TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    FOREIGN KEY (auditor) REFERENCES user(uid) ON DELETE SET NULL,
    FOREIGN KEY (submitter) REFERENCES user(uid) ON DELETE RESTRICT,
    INDEX idx_target_status (target_id, status),
    INDEX idx_status_submit (status, created_at ASC),
    INDEX idx_audit_time (audit_at)
) ENGINE =InnoDB COMMENT='用户工单表';

CREATE TABLE IF NOT EXISTS ticket_resource (
    ticket_id BIGINT UNSIGNED NOT NULL COMMENT '工单ID',
    resource_uuid CHAR(36) NOT NULL COMMENT '资源UUID',
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,

    PRIMARY KEY (ticket_id, resource_uuid),
    FOREIGN KEY (ticket_id) REFERENCES ticket(id) ON DELETE CASCADE,
    FOREIGN KEY (resource_uuid) REFERENCES resource(uuid) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT '工单资源表';

DROP TABLE IF EXISTS user_penalty_history;
CREATE TABLE IF NOT EXISTS user_penalty_history(
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL COMMENT '被处罚的用户ID',
    penalty_type TINYINT UNSIGNED NOT NULL COMMENT '处罚类型',
    target_id VARCHAR(64) NULL COMMENT '处罚对象ID，例如帖子ID、评论ID等，具体含义由target_type决定',
    target_type TINYINT UNSIGNED NOT NULL COMMENT '处罚对象类型：0-未知/默认 1-帖子 2-评论 3-用户等',
    penalty_reason_type TINYINT UNSIGNED NULL COMMENT '处罚原因类型',
    reason VARCHAR(255) NULL COMMENT '处罚原因',
    operator_id BIGINT UNSIGNED NULL COMMENT '操作者ID，NULL表示系统执行',
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    FOREIGN KEY (user_id) REFERENCES user(uid) ON DELETE CASCADE,
    FOREIGN KEY (operator_id) REFERENCES user(uid) ON DELETE SET NULL,
    INDEX idx_user_time (user_id, created_at DESC),
    INDEX idx_target (target_type, target_id)
) ENGINE=InnoDB COMMENT '用户处罚历史表';


SET FOREIGN_KEY_CHECKS = 1;
