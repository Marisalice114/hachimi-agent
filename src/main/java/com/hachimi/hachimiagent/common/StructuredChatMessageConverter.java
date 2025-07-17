package com.hachimi.hachimiagent.common;

import com.hachimi.hachimiagent.entity.ChatMessage;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

/**
 * 基于Spring AI StructuredOutputConverter的ChatMessage转换器
 * 用于将AI模型输出直接转换为ChatMessage实体
 *
 * @author hachimi
 * @since 2025-01-17
 */
@Component
public class StructuredChatMessageConverter {

    private final BeanOutputConverter<ChatMessage> beanOutputConverter;

    public StructuredChatMessageConverter() {
        this.beanOutputConverter = new BeanOutputConverter<>(ChatMessage.class);
    }

    /**
     * 获取转换器的格式说明
     * 这可以添加到AI提示中，指导AI生成正确格式的输出
     *
     * @return 格式说明字符串
     */
    public String getFormat() {
        return beanOutputConverter.getFormat();
    }

    /**
     * 将AI模型的文本输出转换为ChatMessage实体
     *
     * @param text AI模型输出的文本
     * @return ChatMessage实体
     */
    public ChatMessage convert(String text) {
        return beanOutputConverter.convert(text);
    }

    /**
     * 获取用于AI提示的指令文本
     * 可以在与AI交互时使用此方法获取格式化指令
     *
     * @return 格式化指令
     */
    public String getFormatInstructions() {
        return "请以以下JSON格式返回聊天消息：\n" + getFormat();
    }

    /**
     * 创建带有会话ID和消息顺序的ChatMessage
     * 这是一个便利方法，用于补充AI生成的数据
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

        aiGeneratedMessage.setConversationId(conversationId);
        aiGeneratedMessage.setMessageOrder(messageOrder);

        // 如果AI没有设置消息类型，默认设置为ASSISTANT
        if (aiGeneratedMessage.getMessageType() == null || aiGeneratedMessage.getMessageType().isEmpty()) {
            aiGeneratedMessage.setMessageType("ASSISTANT");
        }

        return aiGeneratedMessage;
    }
}
