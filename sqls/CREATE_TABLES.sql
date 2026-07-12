use waterfun;

use waterfun;
DROP TABLE IF EXISTS account_audit_log;
DROP TABLE IF EXISTS user_level_mapping;
DROP TABLE IF EXISTS encryption_data_key;


DROP TABLE IF EXISTS post_tag;
DROP TABLE IF EXISTS post;
DROP TABLE IF EXISTS category;
DROP TABLE IF EXISTS tag;


DROP TABLE IF EXISTS user_data_archive;
DROP TABLE IF EXISTS user_data;
DROP TABLE IF EXISTS user_profile;
DROP TABLE IF EXISTS role_permission;
DROP TABLE IF EXISTS user_permission;
DROP TABLE IF EXISTS user_follow;
DROP TABLE IF EXISTS user_counter;
DROP TABLE IF EXISTS user_role;
DROP TABLE IF EXISTS user_preference;
DROP TABLE IF EXISTS permission;

DROP TABLE IF EXISTS user;
DROP TABLE IF EXISTS role;

-- 用户主表
CREATE TABLE IF NOT EXISTS user (
    uid BIGINT UNSIGNED PRIMARY KEY,
    username VARCHAR(32) UNIQUE NOT NULL,
    nickname    varchar(12) DEFAULT NULL,
    avatar_resource_uuid varchar(36) NULL,
    password_hash        VARCHAR(64) NULL,
    account_status        ENUM ('ACTIVE', 'SUSPENDED', 'DEACTIVATE') DEFAULT 'ACTIVE',
    user_type TINYINT UNSIGNED DEFAULT 0 COMMENT '用户类型: 0=普通用户, 1=管理员, 2=机器人, 3=版主, 4=VIP, 5-9=预留',
    level           TINYINT UNSIGNED DEFAULT 1,
    exp             INT UNSIGNED     DEFAULT 0,
    status_changed_at    TIMESTAMP(3)    NULL,
    created_at TIMESTAMP(3)    DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3)    NULL ON UPDATE CURRENT_TIMESTAMP(3),
    last_active_at       TIMESTAMP(3)    NULL,
    CONSTRAINT fk_avatar_resource_uuid FOREIGN KEY (avatar_resource_uuid) REFERENCES resource(uuid) ON DELETE SET NULL
) ENGINE=InnoDB ROW_FORMAT=COMPRESSED;

# SET FOREIGN_KEY_CHECKS = 0;
# DROP TABLE IF EXISTS role;
# SET FOREIGN_KEY_CHECKS = 1;
-- 角色表
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

-- 权限表
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS permission;
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

DROP TABLE IF EXISTS user_permission;
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
SET FOREIGN_KEY_CHECKS = 1;

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS role_permission;
CREATE TABLE IF NOT EXISTS role_permission (
    id INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    role_id INT UNSIGNED NOT NULL,
    permission_id INT UNSIGNED NOT NULL,
    expires_at TIMESTAMP NULL COMMENT '过期时间，NULL表示永久',
    created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    update_at    TIMESTAMP(3) NULL ON UPDATE CURRENT_TIMESTAMP(3),
    FOREIGN KEY (role_id) REFERENCES role(id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permission(id) ON DELETE CASCADE,
    UNIQUE KEY (role_id, permission_id),
    INDEX idx_expires (expires_at)
) ENGINE=InnoDB;


CREATE TABLE IF NOT EXISTS user_profile
(
    user_uid    BIGINT UNSIGNED NOT NULL,
    bio        text        NULL,
    gender     enum ('MALE', 'FEMALE', 'OTHER', 'UNKNOWN') default 'UNKNOWN' null,
    birth_date    DATE        NULL COMMENT '出生日期',
    residence    VARCHAR(50) NULL COMMENT '居住地',
    update_at TIMESTAMP(3) NULL ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_uid),
    FOREIGN KEY (user_uid) REFERENCES user (uid) ON DELETE CASCADE
) ENGINE=InnoDB ROW_FORMAT=COMPRESSED;

DROP TABLE IF EXISTS user_counter;
CREATE TABLE user_counter (
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
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_uid),
    FOREIGN KEY (user_uid) REFERENCES user (uid) ON DELETE CASCADE
) ENGINE=InnoDB;

DROP TABLE IF EXISTS user_follow;
CREATE TABLE IF NOT EXISTS user_follow (
    user_uid     BIGINT UNSIGNED NOT NULL COMMENT '用户UID',
    follower_uid BIGINT UNSIGNED NOT NULL COMMENT '关注人UID',
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY uk_user_follower (user_uid, follower_uid),
    INDEX idx_following (follower_uid, created_at DESC)
) ENGINE=InnoDB;

-- 用户敏感信息表
CREATE TABLE IF NOT EXISTS user_data (
    user_uid BIGINT UNSIGNED NOT NULL,
    email_encrypted    VARCHAR(64) NULL,
    email_hash        VARCHAR(65) NULL, # HMAC SHA-256
    email_verified    BOOLEAN    DEFAULT FALSE,
    email_expire_at   TIMESTAMP(3) NULL,
    phone_encrypted   VARCHAR(64) NULL,
    phone_hash        VARCHAR(65) NULL, # HMAC SHA-256
    phone_verified    BOOLEAN    DEFAULT FALSE,
    encryption_key_id VARCHAR(50)    NOT NULL,
    created_at        TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at        TIMESTAMP(3)    NULL ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_uid),
    FOREIGN KEY (user_uid) REFERENCES user (uid) ON DELETE CASCADE,
    INDEX idx_verified_data (email_verified, phone_verified)
) ENGINE=InnoDB ENCRYPTION='Y';

-- 数据归档表
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

DROP TABLE IF EXISTS user_preference;
CREATE TABLE user_preference (
    user_uid  BIGINT UNSIGNED PRIMARY KEY,
    locale    VARCHAR(10) NOT NULL DEFAULT 'zh-CN',
    timezone  VARCHAR(50) DEFAULT 'Asia/Shanghai',
    updated_at   DATETIME DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    FOREIGN KEY (user_uid) REFERENCES user(uid) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT '用户偏好设置表';

DROP TABLE IF EXISTS user_setting;
CREATE TABLE user_setting (
    user_uid             BIGINT UNSIGNED PRIMARY KEY,

    profile_visibility   TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0=PUBLIC 1=FOLLOWERS 2=PRIVATE',
    work_visibility      TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0=PUBLIC 1=FOLLOWERS 2=PRIVATE',
    comment_permission   TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0=ALL 1=FOLLOWERS 2=NONE',
    message_permission   TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0=ALL 1=FOLLOWERS 2=NONE',
    allow_follow         TINYINT(1) NOT NULL DEFAULT 1,
    show_active_status   TINYINT(1) NOT NULL DEFAULT 1,

    message_notifications  TINYINT(1) NOT NULL DEFAULT 1,
    comment_notifications  TINYINT(1) NOT NULL DEFAULT 1,
    like_notifications     TINYINT(1) NOT NULL DEFAULT 1,
    follow_notifications   TINYINT(1) NOT NULL DEFAULT 1,
    event_notifications    TINYINT(1) NOT NULL DEFAULT 1,
    email_notifications    TINYINT(1) NOT NULL DEFAULT 0,

    updated_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    FOREIGN KEY (user_uid) REFERENCES user(uid) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT '用户隐私与通知设置';

DROP TABLE IF EXISTS user_history;
CREATE TABLE IF NOT EXISTS user_history (
    user_uid     BIGINT UNSIGNED NOT NULL,
    post_id     BIGINT UNSIGNED NOT NULL,
    viewed_at   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (user_uid, post_id),
    INDEX idx_user_viewed_asc (user_uid, viewed_at),
    INDEX idx_user_viewed_desc (user_uid, viewed_at DESC)
);

DROP TABLE IF EXISTS user_like;
CREATE TABLE IF NOT EXISTS user_like (
    user_uid     BIGINT UNSIGNED NOT NULL,
    post_id     BIGINT UNSIGNED NOT NULL,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (user_uid, post_id),
    INDEX idx_user_created_asc (user_uid, created_at),
    INDEX idx_user_created_desc (user_uid, created_at DESC)
) ENGINE=InnoDB;

DROP TABLE IF EXISTS user_collect;
CREATE TABLE IF NOT EXISTS user_collect (
    user_uid     BIGINT UNSIGNED NOT NULL,
    post_id     BIGINT UNSIGNED NOT NULL,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (user_uid, post_id),
    INDEX idx_user_created (user_uid, created_at DESC)
) ENGINE=InnoDB;

# DROP TABLE IF EXISTS user_badge;
# DROP TABLE IF EXISTS badge;
# -- 称号定义表（系统预设 + 运营可配置）
# CREATE TABLE badge (
#     id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
#     code VARCHAR(32) UNIQUE NOT NULL COMMENT '唯一标识',
#     name VARCHAR(32) NOT NULL COMMENT '称号名称',
#     icon_resource_uuid VARCHAR(36) NULL COMMENT '图标',
#     color VARCHAR(7) DEFAULT '#666666' COMMENT '前端展示色值',
#     description VARCHAR(200) NULL,
#     badge_type TINYINT UNSIGNED DEFAULT 0 COMMENT '0=身份角色,1=成就,2=活动,3=自定义',
#     sort_order INT UNSIGNED DEFAULT 0,
#     created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
#
#     FOREIGN KEY (icon_resource_uuid) REFERENCES resource(uuid) ON DELETE SET NULL
# ) ENGINE=InnoDB;
#
# -- 用户获得的称号（多对多）
# CREATE TABLE user_badge (
#     id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
#     uid BIGINT UNSIGNED NOT NULL,
#     badge_id BIGINT UNSIGNED NOT NULL,
#     acquired_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
#     is_pinned BOOLEAN DEFAULT FALSE COMMENT '是否置顶展示',
#     display_order INT UNSIGNED DEFAULT 0,
#
#     UNIQUE KEY uk_uid_badge (uid, badge_id),
#     FOREIGN KEY (uid) REFERENCES user(uid) ON DELETE CASCADE,
#     FOREIGN KEY (badge_id) REFERENCES badge(id) ON DELETE CASCADE
# ) ENGINE=InnoDB;



-- 审计日志表
CREATE TABLE IF NOT EXISTS account_audit_log (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED UNIQUE NOT NULL,
    action ENUM('CREATE','UPDATE','DISABLE','DELETE','RESTORE','STATUS_CHANGE') NOT NULL,
    action_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
    performed_by VARCHAR(50),
    ip_address VARCHAR(45),
    device_info VARCHAR(255),
    old_value JSON,
    new_value JSON,
    INDEX idx_user_actions (user_id, action, action_time),
    INDEX idx_action_time (action_time)
) ENGINE=InnoDB;

-- 秘钥表
CREATE TABLE IF NOT EXISTS encryption_data_key (
    id INT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    key_id VARCHAR(50) unique ,
    encrypted_key VARBINARY(512) NOT NULL,
    algorithm VARCHAR(20) NOT NULL DEFAULT 'AES',
    key_length SMALLINT UNSIGNED NOT NULL DEFAULT 256,
    key_status ENUM('PENDING_ACTIVATION','ACTIVE','DECRYPT_ONLY','SUSPENDED','DEACTIVATED','DESTROYED') DEFAULT 'PENDING_ACTIVATION',
    key_purpose VARCHAR(30) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    description VARCHAR(255)
) ENGINE=InnoDB;


-- 分类表
CREATE TABLE IF NOT EXISTS category(
    id INT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '分类ID',
    name VARCHAR(50) UNIQUE NOT NULL COMMENT '分类名称',
    slug VARCHAR(50) UNIQUE NOT NULL COMMENT 'URL标识',
    description TEXT COMMENT '分类描述',
    parent_id INT UNSIGNED DEFAULT NULL COMMENT '父级分类ID',
    sort_order INT DEFAULT 0 COMMENT '排序权重',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    usage_count INT UNSIGNED DEFAULT 0 COMMENT '使用次数',
    creator_id BIGINT UNSIGNED NOT NULL COMMENT '创建者ID',
    update_at TIMESTAMP(3) NULL ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    is_deleted  BOOLEAN DEFAULT FALSE,
    INDEX idx_parent_order (parent_id, sort_order),
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES category(id) ON DELETE SET NULL,
    CONSTRAINT fk_category_creator FOREIGN KEY (creator_id) REFERENCES user(uid)
) ENGINE=InnoDB;

-- 标签表
CREATE TABLE IF NOT EXISTS tag(
 id INT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '标签ID',
 name VARCHAR(30) NOT NULL COMMENT '标签名称',
 slug VARCHAR(50) UNIQUE NOT NULL COMMENT 'URL标识',
 description TEXT COMMENT '标签描述',
 usage_count INT UNSIGNED DEFAULT 0 COMMENT '使用次数',
 creator_id BIGINT UNSIGNED NOT NULL COMMENT '创建者ID',
 update_at TIMESTAMP(3) NULL ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
 created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
 is_deleted  BOOLEAN DEFAULT FALSE,
 INDEX idx_usage (usage_count),
 FULLTEXT INDEX ft_tag_name (name) WITH PARSER ngram,
 CONSTRAINT fk_tag_creator FOREIGN KEY (creator_id) REFERENCES user(uid)
);

DROP TABLE IF EXISTS post_like;
CREATE TABLE IF NOT EXISTS post_like (
    post_id BIGINT UNSIGNED NOT NULL,
    user_uid BIGINT UNSIGNED NOT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (post_id, user_uid),
    INDEX idx_user (user_uid)
) ENGINE=InnoDB;

-- 帖子表
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS post;
CREATE TABLE IF NOT EXISTS post(
    id BIGINT UNSIGNED PRIMARY KEY NOT NULL COMMENT '帖子ID',
    title VARCHAR(32) NOT NULL COMMENT '帖子标题',
    subtitle varchar(64) DEFAULT NULL COMMENT '帖子副标题',
    content TEXT COMMENT '帖子内容',
    summary VARCHAR(500) DEFAULT NULL COMMENT '内容摘要',
    coverage_resource_uuid VARCHAR(36) DEFAULT NULL COMMENT '封面图片',
    category_id INT UNSIGNED DEFAULT NULL COMMENT '分类ID',
    type TINYINT UNSIGNED DEFAULT 0 COMMENT '帖子类型: 0普通帖 1公告',
    is_pinned BOOLEAN DEFAULT FALSE COMMENT '是否置顶',
    -- 编辑状态
    edit_status TINYINT UNSIGNED DEFAULT 0 COMMENT '0-无待审核编辑 1-有编辑待审核',
    edited_title VARCHAR(32) DEFAULT NULL COMMENT '待审核标题',
    edited_subtitle VARCHAR(64) DEFAULT NULL COMMENT '待审核副标题',
    edited_content TEXT COMMENT '待审核内容',
    edited_summary VARCHAR(500) DEFAULT NULL COMMENT '待审核摘要',
    edited_cover_img VARCHAR(255) COMMENT '待审核封面图',
    edited_category_id INT UNSIGNED DEFAULT NULL COMMENT '待审核分类ID',
    edited_tag_ids JSON DEFAULT NULL COMMENT '待审核的已有标签ID列表',
    edited_new_tags JSON DEFAULT NULL COMMENT '待审核的新标签名称列表',

    version INT UNSIGNED DEFAULT 1 COMMENT '当前版本号',
    status TINYINT UNSIGNED DEFAULT 0 COMMENT '帖子状态: 0草稿 1待审核 2已发布 3驳回 4归档',
    visibility TINYINT UNSIGNED DEFAULT 0 COMMENT '可见性: 0公开 1私密 2粉丝可见',
    author_id BIGINT UNSIGNED NULL COMMENT '作者ID，空则表示系统发布',
    -- 统计字段
    view_count BIGINT UNSIGNED DEFAULT 0 COMMENT '浏览数',
    like_count BIGINT UNSIGNED DEFAULT 0 COMMENT '点赞数',
    comment_count BIGINT UNSIGNED DEFAULT 0 COMMENT '评论数',
    collect_count BIGINT UNSIGNED DEFAULT 0 COMMENT '收藏数',
    -- SEO优化
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
    CONSTRAINT fk_post_author FOREIGN KEY (author_id) REFERENCES user(uid) ON DELETE CASCADE ,
    CONSTRAINT fk_post_coverage_resource FOREIGN KEY (coverage_resource_uuid) REFERENCES resource(uuid)
) ENGINE=InnoDB;
SET FOREIGN_KEY_CHECKS = 1;

DROP TABLE IF EXISTS post_tag;
CREATE TABLE post_tag (
    post_id BIGINT UNSIGNED NOT NULL,
    tag_id INT UNSIGNED NOT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (post_id, tag_id),
    CONSTRAINT fk_post_tag_post FOREIGN KEY (post_id) REFERENCES post(id),
    CONSTRAINT fk_post_tag_tag  FOREIGN KEY (tag_id)  REFERENCES tag(id)
);

DROP TABLE IF EXISTS comment_like;
CREATE TABLE IF NOT EXISTS comment_like (
    comment_id  BIGINT UNSIGNED NOT NULL,
    user_uid    BIGINT UNSIGNED NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (comment_id, user_uid),
    INDEX idx_user (user_uid)
) ENGINE=InnoDB;

DROP TABLE IF EXISTS comment;
CREATE TABLE IF NOT EXISTS comment (
    id              BIGINT UNSIGNED PRIMARY KEY,
    post_id         BIGINT UNSIGNED NOT NULL COMMENT '所属帖子',
    root_id         BIGINT UNSIGNED DEFAULT NULL COMMENT '一级评论ID，NULL表示自己是一级',
    parent_id       BIGINT UNSIGNED DEFAULT NULL COMMENT '直接回复哪条评论',
    author_uid      BIGINT UNSIGNED NOT NULL,
    content         TEXT NOT NULL,
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

DROP TABLE IF EXISTS user_ticket;
CREATE TABLE IF NOT EXISTS user_ticket (
    user_uid        BIGINT UNSIGNED NOT NULL COMMENT '举报人UID',
    ticket_id         BIGINT UNSIGNED NOT NULL COMMENT '工单ID',
    created_at      TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_uid, ticket_id),
    INDEX idx_report_user (user_uid, created_at DESC)
) ENGINE=InnoDB;