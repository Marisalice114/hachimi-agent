package com.hachimi.hachimiagent.common;

import com.hachimi.hachimiagent.entity.ChatMessage;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

/**
 * 结构化输出转换器
 * 使用BeanOutputConverter将AI输出直接转换为数据库实体
 *
 * @author hachimi
 * @since 2025-01-17
 */
@Component
public class StructuredOutputConverter {

    private final BeanOutputConverter<ChatMessage> beanOutputConverter;

    public StructuredOutputConverter() {
        this.beanOutputConverter = new BeanOutputConverter<>(ChatMessage.class);
    }

    /**
     * 获取用于AI提示的格式说明
     * 指导AI生成正确格式的JSON输出
     *
     * @return 格式说明字符串
     */
    public String getFormat() {
        return beanOutputConverter.getFormat();
    }

    /**
     * 将AI模型的JSON文本输出转换为ChatMessage实体
     *
     * @param jsonText AI模型输出的JSON格式文本
     * @return ChatMessage实体
     */
    public ChatMessage convertToChatMessage(String jsonText) {
        return beanOutputConverter.convert(jsonText);
    }

    /**
     * 获取完整的格式化指令文本
     * 可以直接添加到AI提示中
     *
     * @return 格式化指令
     */
    public String getFormatInstructions() {
        return "请以以下JSON格式返回聊天消息数据：\n" + getFormat() +
               "\n注意：只返回JSON格式，不要包含其他文本。";
    }

    /**
     * 为AI生成的ChatMessage补充元数据
     *
     * @param aiGeneratedMessage AI生成的基础消息
     * @param conversationId 会话ID
     * @param messageOrder 消息顺序
     * @return 完整的ChatMessage实体
     */
    public ChatMessage enhanceWithMetadata(ChatMessage aiGeneratedMessage, String conversationId, Integer messageOrder) {
        if (aiGeneratedMessage == null) {
            return null;
        }

        // 设置会话相关信息
        aiGeneratedMessage.setConversationId(conversationId);
        aiGeneratedMessage.setMessageOrder(messageOrder);

        // 如果AI没有设置消息类型，默认设置为ASSISTANT
        if (aiGeneratedMessage.getMessageType() == null || aiGeneratedMessage.getMessageType().trim().isEmpty()) {
            aiGeneratedMessage.setMessageType("ASSISTANT");
        }

        return aiGeneratedMessage;
    }

    /**
     * 创建一个完整的ChatMessage，结合AI输出和元数据
     *
     * @param jsonText AI输出的JSON文本
     * @param conversationId 会话ID
     * @param messageOrder 消息顺序
     * @return 完整的ChatMessage实体
     */
    public ChatMessage convertAndEnhance(String jsonText, String conversationId, Integer messageOrder) {
        ChatMessage message = convertToChatMessage(jsonText);
        return enhanceWithMetadata(message, conversationId, messageOrder);
    }

    /**
     * 生成用于AI提示的示例格式
     *
     * @return 示例JSON字符串
     */
    public String getExampleFormat() {
        return """
               {
                 "content": "这是消息内容",
                 "messageType": "ASSISTANT"
               }
               """;
    }
}
