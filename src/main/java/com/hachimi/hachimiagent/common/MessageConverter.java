package com.hachimi.hachimiagent.common;

import com.hachimi.hachimiagent.entity.ChatMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

/**
 * Message转换器工具类
 *
 * @author hachimi
 * @since 2025-01-17
 */
public class MessageConverter {

    /**
     * 将Spring AI的Message转换为数据库实体
     *
     * @param message Spring AI消息
     * @param conversationId 对话ID
     * @param messageOrder 消息顺序
     * @return 数据库实体
     */
    public static ChatMessage toEntity(Message message, String conversationId, Integer messageOrder) {
        ChatMessage entity = new ChatMessage();
        entity.setConversationId(conversationId);
        entity.setContent(message.getText()); // 修正：使用getText()而不是getContent()
        entity.setMessageOrder(messageOrder);

        // 根据消息类型设置message_type
        if (message instanceof UserMessage) {
            entity.setMessageType("USER");
        } else if (message instanceof AssistantMessage) {
            entity.setMessageType("ASSISTANT");
        } else if (message instanceof SystemMessage) {
            entity.setMessageType("SYSTEM");
        } else {
            entity.setMessageType("UNKNOWN");
        }

        return entity;
    }

    /**
     * 将数据库实体转换为Spring AI的Message
     *
     * @param entity 数据库实体
     * @return Spring AI消息
     */
    public static Message toMessage(ChatMessage entity) {
        String content = entity.getContent();
        String messageType = entity.getMessageType();

        return switch (messageType) {
            case "USER" -> new UserMessage(content);
            case "ASSISTANT" -> new AssistantMessage(content);
            case "SYSTEM" -> new SystemMessage(content);
            default -> new UserMessage(content); // 默认作为用户消息处理
        };
    }
}
