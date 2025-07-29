-- ======================================
-- hachimi_agent 数据库表结构
-- ======================================

-- 注意：移除 DROP DATABASE 和 CREATE DATABASE 语句
-- 微信云托管会自动提供数据库

-- 创建聊天对话表
CREATE TABLE IF NOT EXISTS chat_conversation (
                                                 id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
                                                 conversation_id VARCHAR(255) NOT NULL UNIQUE COMMENT '对话ID',
    user_id VARCHAR(255) DEFAULT NULL COMMENT '用户ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标志',

    INDEX idx_conversation_id (conversation_id),
    INDEX idx_user_id (user_id),
    INDEX idx_create_time (create_time)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天对话表';

-- 创建聊天消息表
CREATE TABLE IF NOT EXISTS chat_message (
                                            id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
                                            conversation_id VARCHAR(255) NOT NULL COMMENT '对话ID',
    message_type VARCHAR(50) NOT NULL COMMENT '消息类型：USER/ASSISTANT/SYSTEM',
    content TEXT NOT NULL COMMENT '消息内容',
    message_order INT NOT NULL COMMENT '消息顺序',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标志',

    INDEX idx_conversation_id (conversation_id),
    INDEX idx_message_order (conversation_id, message_order),
    INDEX idx_create_time (create_time)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天消息表';