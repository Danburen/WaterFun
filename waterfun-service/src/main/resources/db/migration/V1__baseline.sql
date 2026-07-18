-- ============================================================
-- WaterFun V1: Baseline Schema
-- Merged from sqls/CREATE_TABLES.sql, CREATE_TABLES_2.sql, CREATE_DATABASE_3.sql
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;

-- ==================== 用户模块 ====================

CREATE TABLE IF NOT EXISTS user (
    uid BIGINT UNSIGNED PRIMARY KEY,
    username VARCHAR(32) UNIQUE NOT NULL,
    nickname    VARCHAR(12) DEFAULT NULL,
    avatar_resource_uuid VARCHAR(36) NULL,
    password_hash        VARCHAR(64) NULL,
    account_status        TINYINT UNSIGNED DEFAULT 0 COMMENT '0=ACTIVE, 1=SUSPENDED, 2=DEACTIVATED',
    user_type TINYINT UNSIGNED DEFAULT 0 COMMENT '用户类型: 0=普通用户, 1=管理员, 2=机器人, 3=版主, 4=VIP, 5-9=预留',
    level           TINYINT UNSIGNED DEFAULT 1,
    exp             INT UNSIGNED     DEFAULT 0,
    status_changed_at    TIMESTAMP(3)    NULL,
    created_at TIMESTAMP(3)    DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3)    NULL ON UPDATE CURRENT_TIMESTAMP(3),
    last_active_at       TIMESTAMP(3)    NULL,
    CONSTRAINT fk_avatar_resource_uuid FOREIGN KEY (avatar_resource_uuid) REFERENCES resource(uuid) ON DELETE SET NULL
) ENGINE=InnoDB ROW_FORMAT=COMPRESSED;

CREATE TABLE IF NOT EXISTS role(
    id INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '角色唯一标识码',
    name VARCHAR(50) NOT NULL COMMENT '角色名称',
    order_weight INT DEFAULT 1 COMMENT '排序权重',
    description VARCHAR(255) COMMENT '角色描述',
    parent_id   INT UNSIGNED NULL COMMENT '父角色ID',
    is_system TINYINT UNSIGNED DEFAULT 0 COMMENT '是否系统内置角色',
    created_at  TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    update_at   TIMESTAMP(3) NULL ON UPDATE CURRENT_TIMESTAMP(3),
    FOREIGN KEY (parent_id) REFERENCES role(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS permission (
    id INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '权限唯一标识码',
    name VARCHAR(50) NOT NULL COMMENT '权限名称',
    order_weight INT DEFAULT 1 COMMENT '排序权重',
    description VARCHAR(255) COMMENT '权限描述',
    type TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '权限类型',
    resource VARCHAR(255) COMMENT '资源标识(如URL路径)',
    parent_id INT UNSIGNED NULL COMMENT '父权限ID',
    is_system TINYINT UNSIGNED DEFAULT 0 COMMENT '系统权限，禁止删除',
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    update_at    TIMESTAMP(3) NULL ON UPDATE CURRENT_TIMESTAMP(3),
    FOREIGN KEY (parent_id) REFERENCES permission(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS user_permission(
    id INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    user_uid BIGINT UNSIGNED NOT NULL,
    permission_id INT UNSIGNED NOT NULL,
    ban_reason_type TINYINT UNSIGNED NULL COMMENT '权限禁用原因类型',
    expires_at TIMESTAMP(3) NULL COMMENT '过期时间，NULL表示永久',
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    FOREIGN KEY (user_uid) REFERENCES user(uid) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permission(id) ON DELETE CASCADE,
    UNIQUE KEY (user_uid, permission_id),
    INDEX    idx_expires (expires_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS role_permission (
    id INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    role_id INT UNSIGNED NOT NULL,
    permission_id INT UNSIGNED NOT NULL,
    expires_at TIMESTAMP(3) NULL COMMENT '过期时间，NULL表示永久',
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    update_at    TIMESTAMP(3) NULL ON UPDATE CURRENT_TIMESTAMP(3),
    FOREIGN KEY (role_id) REFERENCES role(id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permission(id) ON DELETE CASCADE,
    UNIQUE KEY (role_id, permission_id),
    INDEX idx_expires (expires_at)
) ENGINE=InnoDB;

-- 用户资料表
CREATE TABLE IF NOT EXISTS user_profile
(
    user_uid    BIGINT UNSIGNED NOT NULL,
    bio        TEXT        NULL,
    gender     TINYINT UNSIGNED DEFAULT 3 COMMENT '0=MALE, 1=FEMALE, 2=OTHER, 3=UNKNOWN',
    birth_date    DATE    NULL COMMENT '出生日期',
    residence    VARCHAR(50) NULL COMMENT '居住地',
    update_at TIMESTAMP(3) NULL ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_uid),
    FOREIGN KEY (user_uid) REFERENCES user (uid) ON DELETE CASCADE
) ENGINE=InnoDB ROW_FORMAT=COMPRESSED;

CREATE TABLE IF NOT EXISTS user_counter (
    user_uid        BIGINT UNSIGNED NOT NULL,
    follower_cnt    INT UNSIGNED DEFAULT 0,
    following_cnt   INT UNSIGNED DEFAULT 0,
    like_cnt        INT UNSIGNED DEFAULT 0,
    collect_cnt     INT UNSIGNED DEFAULT 0,
    post_cnt        INT UNSIGNED DEFAULT 0,
    submit_cnt      INT UNSIGNED DEFAULT 0,
    reject_cnt      INT UNSIGNED DEFAULT 0,
    report_cnt      INT UNSIGNED DEFAULT 0,
    report_hit_cnt  INT UNSIGNED DEFAULT 0,
    updated_at      TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_uid),
    FOREIGN KEY (user_uid) REFERENCES user (uid) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS user_follow (
    user_uid     BIGINT UNSIGNED NOT NULL COMMENT '用户UID',
    follower_uid BIGINT UNSIGNED NOT NULL COMMENT '关注人UID',
    created_at   TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY uk_user_follower (user_uid, follower_uid),
    INDEX idx_following (follower_uid, created_at DESC)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS user_role (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    user_uid BIGINT UNSIGNED NOT NULL,
    role_id INT UNSIGNED NOT NULL,
    expires_at TIMESTAMP(3) NULL COMMENT '过期时间，NULL表示永久',
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    FOREIGN KEY (user_uid) REFERENCES user(uid) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES role(id) ON DELETE CASCADE,
    UNIQUE KEY (user_uid, role_id),
    INDEX idx_expires (expires_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS user_preference (
    user_uid  BIGINT UNSIGNED PRIMARY KEY,
    locale    VARCHAR(10) NOT NULL DEFAULT 'zh-CN',
    timezone  VARCHAR(50) DEFAULT 'Asia/Shanghai',
    updated_at   TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    FOREIGN KEY (user_uid) REFERENCES user(uid) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT '用户偏好设置表';

CREATE TABLE IF NOT EXISTS user_setting (
    user_uid             BIGINT UNSIGNED PRIMARY KEY,
    profile_visibility   TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0=PUBLIC 1=FOLLOWERS 2=PRIVATE',
    work_visibility      TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0=PUBLIC 1=FOLLOWERS 2=PRIVATE',
    comment_permission   TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0=ALL 1=FOLLOWERS 2=NONE',
    message_permission   TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0=ALL 1=FOLLOWERS 2=NONE',
    allow_follow         BIT(1) NOT NULL DEFAULT 1,
    show_active_status   BIT(1) NOT NULL DEFAULT 1,
    message_notifications  BIT(1) NOT NULL DEFAULT 1,
    comment_notifications  BIT(1) NOT NULL DEFAULT 1,
    like_notifications     BIT(1) NOT NULL DEFAULT 1,
    follow_notifications   BIT(1) NOT NULL DEFAULT 1,
    event_notifications    BIT(1) NOT NULL DEFAULT 1,
    email_notifications    BIT(1) NOT NULL DEFAULT 0,
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    FOREIGN KEY (user_uid) REFERENCES user(uid) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT '用户隐私与通知设置';

CREATE TABLE IF NOT EXISTS user_data (
    user_uid BIGINT UNSIGNED NOT NULL,
    email_encrypted    VARCHAR(128) NULL,
    email_hash        VARCHAR(65) NULL,
    email_verified    BOOLEAN    DEFAULT FALSE,
    email_expire_at   TIMESTAMP(3) NULL,
    phone_encrypted   VARCHAR(128) NULL,
    phone_hash        VARCHAR(65) NULL,
    phone_verified    BOOLEAN    DEFAULT FALSE,
    encryption_key_id VARCHAR(50) NOT NULL,
    created_at        TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at        TIMESTAMP(3)    NULL ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_uid),
    FOREIGN KEY (user_uid) REFERENCES user (uid) ON DELETE CASCADE,
    INDEX idx_verified_data (email_verified, phone_verified)
) ENGINE=InnoDB ENCRYPTION='Y';

CREATE TABLE IF NOT EXISTS user_data_archive (
     id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
     user_uid    BIGINT UNSIGNED NOT NULL,
     table_name VARCHAR(50) NOT NULL,
     original_data JSON NOT NULL,
     archived_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
     archived_by VARCHAR(50) NOT NULL,
     reason VARCHAR(255) NOT NULL,
     INDEX idx_archive_user (user_uid),
     INDEX idx_archive_time (archived_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS user_history (
    user_uid     BIGINT UNSIGNED NOT NULL,
    post_id     BIGINT UNSIGNED NOT NULL,
    viewed_at   TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_uid, post_id),
    INDEX idx_user_viewed_asc (user_uid, viewed_at),
    INDEX idx_user_viewed_desc (user_uid, viewed_at DESC)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS user_like (
    user_uid     BIGINT UNSIGNED NOT NULL,
    post_id     BIGINT UNSIGNED NOT NULL,
    created_at  TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_uid, post_id),
    INDEX idx_user_created_asc (user_uid, created_at),
    INDEX idx_user_created_desc (user_uid, created_at DESC)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS user_collect (
    user_uid     BIGINT UNSIGNED NOT NULL,
    post_id      BIGINT UNSIGNED NOT NULL,
    created_at   TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_uid, post_id),
    INDEX idx_user_created (user_uid, created_at DESC)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS user_penalty_history(
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL COMMENT '被处罚的用户ID',
    penalty_type TINYINT UNSIGNED NOT NULL COMMENT '处罚类型',
    target_id VARCHAR(64) NULL COMMENT '处罚对象ID',
    target_type TINYINT UNSIGNED NOT NULL COMMENT '处罚对象类型',
    penalty_reason_type TINYINT UNSIGNED NULL COMMENT '处罚原因类型',
    reason VARCHAR(255) NULL COMMENT '处罚原因',
    operator_id BIGINT UNSIGNED NULL COMMENT '操作者ID，NULL表示系统执行',
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    FOREIGN KEY (user_id) REFERENCES user(uid) ON DELETE CASCADE,
    FOREIGN KEY (operator_id) REFERENCES user(uid) ON DELETE SET NULL,
    INDEX idx_user_time (user_id, created_at DESC),
    INDEX idx_target (target_type, target_id)
) ENGINE=InnoDB COMMENT '用户处罚历史表';

CREATE TABLE IF NOT EXISTS account_audit_log (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_uid BIGINT UNSIGNED NOT NULL,
    action LONGTEXT NOT NULL,
    action_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    performed_by VARCHAR(50),
    ip_address VARCHAR(45),
    device_info VARCHAR(255),
    old_value JSON,
    new_value JSON,
    INDEX idx_user_actions (user_uid, action_time),
    INDEX idx_action_time (action_time)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS encryption_data_key (
    id INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    key_id VARCHAR(50) unique ,
    encrypted_key VARCHAR(512) NOT NULL,
    algorithm   TINYINT UNSIGNED DEFAULT 0 NOT NULL COMMENT '0=AES, 1=SM4',
    key_length SMALLINT UNSIGNED NOT NULL DEFAULT 256,
    key_status TINYINT UNSIGNED DEFAULT 0 NOT NULL COMMENT '0=PENDING_ACTIVATION, 1=ACTIVE, 2=DECRYPT_ONLY, 3=SUSPENDED, 4=DEACTIVATED, 5=DESTROYED',
    key_purpose VARCHAR(30) NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    description VARCHAR(255)
) ENGINE=InnoDB;

-- ==================== 资源模块 ====================

CREATE TABLE IF NOT EXISTS resource (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL COMMENT '资源UUID',
    resource_key VARCHAR(255) NOT NULL COMMENT 'COS对象Key',
    resource_type TINYINT UNSIGNED NOT NULL COMMENT '资源类型',
    mime_type VARCHAR(128) NULL COMMENT 'MIME类型',
    size_bytes BIGINT UNSIGNED NULL COMMENT '资源大小（字节）',
    file_meta JSON NULL COMMENT '文件元信息',
    source_type TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '来源类型',
    uploader_id BIGINT UNSIGNED NULL COMMENT '上传者ID',
    expired_at TIMESTAMP(3) NULL COMMENT '过期时间',
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) NOT NULL,
    status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '资源状态：0-上传挂起 1-有效 2-游离 3-已删除',
    UNIQUE KEY uk_resource_key (resource_key),
    UNIQUE KEY uk_resource_uuid (uuid),
    INDEX idx_type_created (resource_type, created_at)
) ENGINE=InnoDB COMMENT '资源表';

CREATE TABLE IF NOT EXISTS post_resource (
    post_id        BIGINT UNSIGNED NOT NULL COMMENT '帖子ID',
    resource_uuid  VARCHAR(36) NOT NULL COMMENT '资源UUID',
    created_at     TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    PRIMARY KEY (post_id, resource_uuid),
    INDEX idx_resource_uuid (resource_uuid),
    FOREIGN KEY (post_id) REFERENCES post(id) ON DELETE CASCADE,
    FOREIGN KEY (resource_uuid) REFERENCES resource(uuid) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT '帖子-资源关联表';

-- ==================== 内容模块 ====================

CREATE TABLE IF NOT EXISTS category(
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '分类ID',
    name VARCHAR(50) UNIQUE NOT NULL COMMENT '分类名称',
    slug VARCHAR(50) UNIQUE NOT NULL COMMENT 'URL标识',
    description TEXT COMMENT '分类描述',
    parent_id BIGINT UNSIGNED DEFAULT NULL COMMENT '父级分类ID',
    sort_order INT DEFAULT 0 COMMENT '排序权重',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    usage_count BIGINT UNSIGNED DEFAULT 0 COMMENT '使用次数',
    creator_id BIGINT UNSIGNED NOT NULL COMMENT '创建者ID',
    update_at TIMESTAMP(3) NULL ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    is_deleted  BOOLEAN DEFAULT FALSE,
    INDEX idx_parent_order (parent_id, sort_order),
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES category(id) ON DELETE SET NULL,
    CONSTRAINT fk_category_creator FOREIGN KEY (creator_id) REFERENCES user(uid)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS tag(
 id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '标签ID',
 name VARCHAR(30) NOT NULL COMMENT '标签名称',
 slug VARCHAR(50) UNIQUE NOT NULL COMMENT 'URL标识',
 description TEXT COMMENT '标签描述',
 usage_count BIGINT UNSIGNED DEFAULT 0 COMMENT '使用次数',
 creator_id BIGINT UNSIGNED NOT NULL COMMENT '创建者ID',
 update_at TIMESTAMP(3) NULL ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
 created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
 is_deleted  BOOLEAN DEFAULT FALSE,
 INDEX idx_usage (usage_count),
 FULLTEXT INDEX ft_tag_name (name) WITH PARSER ngram,
 CONSTRAINT fk_tag_creator FOREIGN KEY (creator_id) REFERENCES user(uid)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS post(
    id BIGINT UNSIGNED PRIMARY KEY NOT NULL COMMENT '帖子ID',
    title VARCHAR(32) NOT NULL COMMENT '帖子标题',
    subtitle VARCHAR(64) DEFAULT NULL COMMENT '帖子副标题',
    content LONGTEXT COMMENT '帖子内容',
    summary VARCHAR(500) DEFAULT NULL COMMENT '内容摘要',
    coverage_resource_uuid VARCHAR(36) DEFAULT NULL COMMENT '封面图片',
    category_id BIGINT UNSIGNED DEFAULT NULL COMMENT '分类ID',
    type TINYINT UNSIGNED DEFAULT 0 COMMENT '帖子类型: 0普通帖 1公告',
    is_pinned BOOLEAN DEFAULT FALSE COMMENT '是否置顶',
    edit_status TINYINT UNSIGNED DEFAULT 0 COMMENT '0-无待审核编辑 1-有编辑待审核',
    edited_title VARCHAR(32) DEFAULT NULL COMMENT '待审核标题',
    edited_subtitle VARCHAR(64) DEFAULT NULL COMMENT '待审核副标题',
    edited_content TEXT COMMENT '待审核内容',
    edited_summary VARCHAR(500) DEFAULT NULL COMMENT '待审核摘要',
    edited_cover_img VARCHAR(255) COMMENT '待审核封面图',
    edited_category_id BIGINT UNSIGNED DEFAULT NULL COMMENT '待审核分类ID',
    edited_tag_ids JSON DEFAULT NULL COMMENT '待审核的已有标签ID列表',
    edited_new_tags JSON DEFAULT NULL COMMENT '待审核的新标签名称列表',
    version INT UNSIGNED DEFAULT 1 COMMENT '当前版本号',
    status TINYINT UNSIGNED DEFAULT 0 COMMENT '帖子状态: 0草稿 1待审核 2已发布 3驳回 4归档',
    visibility TINYINT UNSIGNED DEFAULT 0 COMMENT '可见性: 0公开 1私密 2粉丝可见',
    author_id BIGINT UNSIGNED NULL COMMENT '作者ID',
    view_count INT UNSIGNED DEFAULT 0 COMMENT '浏览数',
    like_count INT UNSIGNED DEFAULT 0 COMMENT '点赞数',
    comment_count INT UNSIGNED DEFAULT 0 COMMENT '评论数',
    collect_count INT UNSIGNED DEFAULT 0 COMMENT '收藏数',
    slug VARCHAR(200) UNIQUE COMMENT 'SEO友好URL',
    published_at TIMESTAMP(3) NULL COMMENT '发布时间',
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    is_deleted  BOOLEAN DEFAULT FALSE,
    INDEX idx_status_published (status, published_at),
    INDEX idx_author_status (author_id, status),
    INDEX idx_category_status (category_id, status),
    INDEX idx_type_status (type, status),
    INDEX idx_pinned_status (is_pinned, status, published_at),
    INDEX idx_created (created_at DESC),
    INDEX idx_author_created (author_id, created_at DESC),
    FULLTEXT INDEX ft_post_title_content (title, summary, content) WITH PARSER ngram,
    CONSTRAINT fk_post_category FOREIGN KEY (category_id) REFERENCES category(id),
    CONSTRAINT fk_post_edited_category FOREIGN KEY (edited_category_id) REFERENCES category(id),
    CONSTRAINT fk_post_author FOREIGN KEY (author_id) REFERENCES user(uid) ON DELETE CASCADE,
    CONSTRAINT fk_post_coverage_resource FOREIGN KEY (coverage_resource_uuid) REFERENCES resource(uuid)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS post_tag (
    post_id BIGINT UNSIGNED NOT NULL,
    tag_id BIGINT UNSIGNED NOT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (post_id, tag_id),
    CONSTRAINT fk_post_tag_post FOREIGN KEY (post_id) REFERENCES post(id),
    CONSTRAINT fk_post_tag_tag  FOREIGN KEY (tag_id)  REFERENCES tag(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS post_like (
    post_id BIGINT UNSIGNED NOT NULL,
    user_uid BIGINT UNSIGNED NOT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (post_id, user_uid),
    INDEX idx_user (user_uid)
) ENGINE=InnoDB;

-- ==================== 评论模块 ====================

CREATE TABLE IF NOT EXISTS comment (
    id              BIGINT UNSIGNED PRIMARY KEY,
    post_id         BIGINT UNSIGNED NOT NULL COMMENT '所属帖子',
    root_id         BIGINT UNSIGNED DEFAULT NULL COMMENT '一级评论ID',
    parent_id       BIGINT UNSIGNED DEFAULT NULL COMMENT '直接回复哪条评论',
    author_uid      BIGINT UNSIGNED NOT NULL,
    content         LONGTEXT NOT NULL,
    status          TINYINT UNSIGNED DEFAULT 1 COMMENT '0-删除 1-正常 2-审核中',
    like_count      INT UNSIGNED DEFAULT 0 COMMENT '点赞数',
    reply_count     INT UNSIGNED DEFAULT 0 COMMENT '回复数',
    is_pined          BOOLEAN DEFAULT FALSE COMMENT '是否置顶',
    created_at      TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    INDEX idx_root_sort (post_id, root_id, is_pined DESC, like_count DESC, created_at DESC),
    INDEX idx_replies (root_id, created_at ASC),
    INDEX idx_author (author_uid, created_at DESC),
    CONSTRAINT fk_comment_post FOREIGN KEY (post_id) REFERENCES post(id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_root FOREIGN KEY (root_id) REFERENCES comment(id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_parent FOREIGN KEY (parent_id) REFERENCES comment(id),
    FULLTEXT INDEX ft_comment_content (content) WITH PARSER ngram,
    CONSTRAINT fk_comment_author FOREIGN KEY (author_uid) REFERENCES user(uid)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS comment_like (
    comment_id  BIGINT UNSIGNED NOT NULL,
    user_uid    BIGINT UNSIGNED NOT NULL,
    created_at  TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (comment_id, user_uid),
    INDEX idx_user (user_uid)
) ENGINE=InnoDB;

-- ==================== 工单模块 ====================

CREATE TABLE IF NOT EXISTS user_ticket (
    user_uid        BIGINT UNSIGNED NOT NULL COMMENT '举报人UID',
    ticket_id         BIGINT UNSIGNED NOT NULL COMMENT '工单ID',
    created_at      TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_uid, ticket_id),
    INDEX idx_report_user (user_uid, created_at DESC)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS ticket (
    id              BIGINT UNSIGNED PRIMARY KEY,
    ticket_type     TINYINT UNSIGNED NOT NULL COMMENT '工单类型',
    target_id       VARCHAR(64) DEFAULT NULL COMMENT '目标ID',
    target_type     TINYINT UNSIGNED NOT NULL COMMENT '目标类型',
    status          TINYINT UNSIGNED DEFAULT 1 NOT NULL COMMENT '审计状态: 1-待审 2-通过 3-拒绝',
    penalty_type    TINYINT UNSIGNED DEFAULT 0 COMMENT '处罚类型',
    content         TEXT NOT NULL COMMENT '工单内容',
    submitter       BIGINT UNSIGNED NOT NULL COMMENT '提交人ID',
    auditor         BIGINT UNSIGNED NULL,
    audit_at        TIMESTAMP(3) NULL,
    audit_note      VARCHAR(255) NULL COMMENT '审核备注',
    reply_content   VARCHAR(2000) NULL COMMENT '管理员回复内容',
    target_user_uid BIGINT UNSIGNED NULL COMMENT '目标用户UID',
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
    resource_uuid VARCHAR(36) NOT NULL COMMENT '资源UUID',
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    PRIMARY KEY (ticket_id, resource_uuid),
    FOREIGN KEY (ticket_id) REFERENCES ticket(id) ON DELETE CASCADE,
    FOREIGN KEY (resource_uuid) REFERENCES resource(uuid) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT '工单资源表';

-- ==================== 审核模块 ====================

CREATE TABLE IF NOT EXISTS audit_task (
    id              BIGINT UNSIGNED PRIMARY KEY,
    target_id       VARCHAR(64) NULL COMMENT '目标ID',
    target_type     TINYINT UNSIGNED NOT NULL COMMENT '目标类型',
    trigger_type    TINYINT UNSIGNED DEFAULT 0 NOT NULL COMMENT '触发类型',
    trigger_source  VARCHAR(255) NULL COMMENT '触发来源',
    priority        TINYINT UNSIGNED DEFAULT 2 NOT NULL COMMENT '优先级',
    format          TINYINT UNSIGNED DEFAULT 0 NOT NULL COMMENT 'payload格式',
    status          TINYINT UNSIGNED DEFAULT 1 NOT NULL COMMENT '审计状态: 1-待审 2-通过 3-拒绝',
    payload         JSON NULL COMMENT '审计主要内容',
    audit_at        TIMESTAMP(3) NULL,
    auditor         BIGINT UNSIGNED NULL,
    reject_type     TINYINT UNSIGNED NULL COMMENT '拒绝类型',
    reject_reason   VARCHAR(255) NULL,
    submitter       BIGINT UNSIGNED NOT NULL COMMENT '提交人ID',
    submit_at       TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    updated_at      TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) NOT NULL,
    pending_target_id VARCHAR(64) GENERATED ALWAYS AS (
            CASE WHEN status = 1 THEN target_id ELSE NULL END
            ) STORED COMMENT '待审时的目标ID',
    FOREIGN KEY (auditor) REFERENCES user(uid) ON DELETE SET NULL,
    FOREIGN KEY (submitter) REFERENCES user(uid) ON DELETE RESTRICT,
    UNIQUE KEY uk_target_type_pending_only (target_type, pending_target_id),
    INDEX idx_target_status (target_id, status),
    INDEX idx_status_submit (status, submit_at)
)ENGINE=InnoDB COMMENT '审计任务表';

CREATE TABLE IF NOT EXISTS audit_task_resource (
    task_id BIGINT UNSIGNED NOT NULL COMMENT '审计任务ID',
    resource_uuid VARCHAR(36) NOT NULL COMMENT '资源UUID',
    status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '资源审计状态 1-待审 2-通过 3-拒绝',
    audit_at TIMESTAMP(3) NULL,
    auditor BIGINT UNSIGNED NULL,
    reject_type TINYINT UNSIGNED NULL COMMENT '拒绝类型',
    reject_reason VARCHAR(255) NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) NOT NULL,
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) NOT NULL,
    PRIMARY KEY (task_id, resource_uuid),
    FOREIGN KEY (task_id) REFERENCES audit_task(id) ON DELETE CASCADE,
    FOREIGN KEY (auditor) REFERENCES user(uid) ON DELETE SET NULL,
    FOREIGN KEY (resource_uuid) REFERENCES resource(uuid) ON DELETE CASCADE,
    INDEX idx_task_resource (task_id),
    INDEX idx_task_status (task_id, status),
    INDEX idx_status_created (status, created_at)
)ENGINE=InnoDB COMMENT '审计任务资源表';

CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT UNSIGNED,
    username VARCHAR(50),
    action TINYINT UNSIGNED DEFAULT 0 NOT NULL,
    ip VARCHAR(45) NOT NULL,
    device_info JSON,
    country VARCHAR(50),
    province VARCHAR(50),
    city VARCHAR(50),
    status TINYINT UNSIGNED NOT NULL DEFAULT 1,
    fail_reason VARCHAR(64) DEFAULT NULL,
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    INDEX idx_user_id (user_id),
    INDEX idx_action (action),
    INDEX idx_created_at (created_at),
    INDEX idx_ip (ip)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_activity_log (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    action_type TINYINT UNSIGNED NOT NULL COMMENT '操作类型',
    business_type TINYINT UNSIGNED NOT NULL COMMENT '业务类型',
    target_id BIGINT UNSIGNED COMMENT '操作对象ID',
    ip VARCHAR(45) NOT NULL COMMENT '操作IP地址',
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    INDEX idx_user_time (user_id, created_at),
    INDEX idx_action_time (action_type, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== 通知模块 ====================

CREATE TABLE IF NOT EXISTS inbox (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_uid BIGINT UNSIGNED NOT NULL,
    notice_type TINYINT UNSIGNED NOT NULL COMMENT '0-通用消息 1-LIKE消息...',
    business_type TINYINT UNSIGNED NULL COMMENT '业务类型',
    target_id VARCHAR(64) NULL COMMENT '关联资源ID',
    sender_id BIGINT UNSIGNED NULL COMMENT 'NULL表示无目标用户',
    title VARCHAR(64) NOT NULL,
    content JSON NOT NULL COMMENT '结构化内容',
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

CREATE TABLE IF NOT EXISTS site_statistics (
    stat_date DATE PRIMARY KEY,
    login_count INT UNSIGNED DEFAULT 0 COMMENT '登陆人次',
    daily_pv INT UNSIGNED DEFAULT 0 COMMENT '访问次数（PV）',
    new_users INT UNSIGNED DEFAULT 0 COMMENT '今日注册',
    new_posts INT UNSIGNED DEFAULT 0 COMMENT '今日新帖',
    peak_online INT UNSIGNED DEFAULT 0 COMMENT '在线峰值',
    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== 安全模块 ====================

CREATE TABLE IF NOT EXISTS ip_ban (
    id          BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    ip          VARCHAR(45) NOT NULL COMMENT 'IP地址，支持IPv6',
    reason      VARCHAR(100) NOT NULL DEFAULT '' COMMENT '封禁原因',
    banned_at   TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    expires_at  TIMESTAMP(3) NULL COMMENT 'NULL=永久封禁',
    UNIQUE KEY uk_ip (ip)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IP封禁表';

-- ==================== 轮播图 ====================

CREATE TABLE IF NOT EXISTS banner (
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

SET FOREIGN_KEY_CHECKS = 1;
