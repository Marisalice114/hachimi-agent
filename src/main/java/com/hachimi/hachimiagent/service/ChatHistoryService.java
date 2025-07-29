package com.hachimi.hachimiagent.service;

import com.hachimi.hachimiagent.chatmemory.MysqlBasedChatMemoryRepository;
import com.hachimi.hachimiagent.entity.ChatMessage;
import com.hachimi.hachimiagent.mapper.ChatMessageMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 基于现有数据库ChatMemory体系的聊天历史服务
 * 直接使用Entity，无需DTO转换
 */
@Service
@Slf4j
public class ChatHistoryService {

    @Resource
    private MysqlBasedChatMemoryRepository chatMemoryRepository;

    @Resource
    private ChatMessageMapper messageMapper;

    /**
     * 获取所有会话列表
     * 返回会话基本信息的Map
     */
    public List<Map<String, Object>> getAllSessions() {
        try {
            // 使用现有的ChatMemoryRepository获取所有对话ID
            List<String> conversationIds = chatMemoryRepository.findConversationIds();

            return conversationIds.stream()
                    .map(this::buildSessionSummary)
                    .filter(session -> session != null)
                    .sorted((a, b) -> {
                        LocalDateTime timeA = (LocalDateTime) a.get("lastMessageTime");
                        LocalDateTime timeB = (LocalDateTime) b.get("lastMessageTime");
                        return timeB.compareTo(timeA);
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("获取会话列表失败", e);
            return List.of();
        }
    }

    /**
     * 获取指定会话的消息历史
     * 直接返回ChatMessage实体列表
     */
    public List<ChatMessage> getSessionMessages(String sessionId) {
        try {
            // 直接使用现有的mapper获取数据库中的完整消息信息
            List<ChatMessage> messages = messageMapper.findByConversationIdOrderByOrder(sessionId);

            log.debug("获取会话 {} 的消息 {} 条", sessionId, messages.size());
            return messages;

        } catch (Exception e) {
            log.error("获取会话消息失败, sessionId: {}", sessionId, e);
            return List.of();
        }
    }

    /**
     * 获取会话信息
     * 返回会话摘要Map
     */
    public Map<String, Object> getSession(String sessionId) {
        return buildSessionSummary(sessionId);
    }

    /**
     * 删除会话（使用现有的ChatMemoryRepository）
     */
    public void deleteSession(String sessionId) {
        try {
            chatMemoryRepository.deleteByConversationId(sessionId);
            log.info("已删除会话: {}", sessionId);
        } catch (Exception e) {
            log.error("删除会话失败, sessionId: {}", sessionId, e);
            throw new RuntimeException("删除会话失败", e);
        }
    }

    /**
     * 构建会话摘要信息
     */
    private Map<String, Object> buildSessionSummary(String conversationId) {
        try {
            // 从数据库查询消息来构建会话信息
            List<ChatMessage> messages = messageMapper.findByConversationIdOrderByOrder(conversationId);

            if (messages.isEmpty()) {
                return null;
            }

            // 获取最后一条消息
            ChatMessage lastMessage = messages.get(messages.size() - 1);

            // 生成默认会话名称
            String sessionName = generateDefaultSessionName(messages);

            Map<String, Object> sessionSummary = new HashMap<>();
            sessionSummary.put("sessionId", conversationId);
            sessionSummary.put("sessionName", sessionName);
            sessionSummary.put("lastMessageTime", lastMessage.getCreateTime());
            sessionSummary.put("lastMessage", lastMessage.getContent());
            sessionSummary.put("messageCount", messages.size());

            return sessionSummary;

        } catch (Exception e) {
            log.error("构建会话摘要失败, conversationId: {}", conversationId, e);
            return null;
        }
    }

    /**
     * 生成默认会话名称
     */
    // 更稳健的解决方案：兼容大小写
    private String generateDefaultSessionName(List<ChatMessage> messages) {
        // 查找第一条用户消息 - 兼容大小写
        ChatMessage firstUserMessage = messages.stream()
                .filter(msg -> msg.getMessageType() != null &&
                        msg.getMessageType().toUpperCase().equals("USER"))  // ✅ 兼容大小写
                .findFirst()
                .orElse(null);

        if (firstUserMessage != null && firstUserMessage.getContent() != null) {
            String content = firstUserMessage.getContent().trim();

            // 清理内容
            content = content.replaceAll("\\s+", " ")  // 多个空格合并为单个
                    .replaceAll("[\\r\\n]+", "")  // 移除换行符
                    .trim();

            if (content.isEmpty()) {
                return "新对话";
            }

            // 适当增加长度限制
            int maxLength = 15;

            if (content.length() <= maxLength) {
                return content;
            } else {
                return content.substring(0, maxLength) + "...";
            }
        }

        return "新对话";
    }
}

