-- ======================================
-- 重置并创建 hachimi_agent 数据库
-- ======================================

-- 1. 删除现有数据库（如果存在）
DROP DATABASE IF EXISTS hachimi_agent;

-- 2. 创建新数据库
CREATE DATABASE hachimi_agent
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- 3. 使用数据库
USE hachimi_agent;

-- 4. 创建聊天对话表
CREATE TABLE chat_conversation (
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

-- 5. 创建聊天消息表
CREATE TABLE chat_message (
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

-- 6. 插入测试数据
INSERT INTO chat_conversation (conversation_id, user_id) VALUES
                                                             ('test_conversation_1', 'user_001'),
                                                             ('test_conversation_2', 'user_002');

INSERT INTO chat_message (conversation_id, message_type, content, message_order) VALUES
                                                                                     ('test_conversation_1', 'USER', '你好，我想咨询恋爱问题', 0),
                                                                                     ('test_conversation_1', 'ASSISTANT', '你好！我是恋爱心理专家，很高兴为你提供帮助。请告诉我你遇到了什么问题？', 1),
                                                                                     ('test_conversation_1', 'USER', '我和女朋友经常吵架，不知道怎么办', 2),
                                                                                     ('test_conversation_1', 'ASSISTANT', '吵架是情侣关系中常见的问题。首先，我想了解一下你们通常因为什么事情吵架？', 3);

-- 7. 验证表结构和数据
SELECT 'Database created successfully' AS status;

SELECT 'Tables created:' AS info;
SHOW TABLES;

SELECT 'Table structures:' AS info;
DESCRIBE chat_conversation;
DESCRIBE chat_message;

SELECT 'Sample data in chat_conversation:' AS info;
SELECT * FROM chat_conversation;

SELECT 'Sample data in chat_message:' AS info;
SELECT * FROM chat_message ORDER BY conversation_id, message_order;

-- 8. 验证查询功能
SELECT 'Test queries:' AS info;

-- 查询所有对话ID
SELECT DISTINCT conversation_id FROM chat_conversation WHERE deleted = 0;

-- 查询特定对话的消息
SELECT message_type, content, message_order
FROM chat_message
WHERE conversation_id = 'test_conversation_1' AND deleted = 0
ORDER BY message_order;

SELECT 'Database initialization completed successfully!' AS final_status;