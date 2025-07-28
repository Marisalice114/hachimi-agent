package com.hachimi.hachimiagent.service;

import com.hachimi.hachimiagent.chatmemory.MysqlBasedChatMemoryRepository;
import com.hachimi.hachimiagent.entity.ChatMessage;
import com.hachimi.hachimiagent.mapper.ChatConversationMapper;
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
 * 直接使用Entity，基于您的实际数据库结构
 */
@Service
@Slf4j
public class ChatHistoryService {

    @Resource
    private MysqlBasedChatMemoryRepository chatMemoryRepository;

    @Resource
    private ChatMessageMapper messageMapper;

    @Resource
    private ChatConversationMapper conversationMapper;

    /**
     * 获取所有会话列表
     * 返回会话摘要信息的Map列表
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
     * 基于您的实际Entity字段结构
     */
    private Map<String, Object> buildSessionSummary(String conversationId) {
        try {
            // 从数据库查询消息来构建会话信息
            List<ChatMessage> messages = messageMapper.findByConversationIdOrderByOrder(conversationId);

            if (messages.isEmpty()) {
                return null;
            }

            // 获取最后一条消息（按messageOrder排序后的最后一条）
            ChatMessage lastMessage = messages.get(messages.size() - 1);

            // 生成默认会话名称（基于第一条用户消息）
            String sessionName = generateDefaultSessionName(messages);

            // 构建会话摘要Map
            Map<String, Object> sessionSummary = new HashMap<>();
            sessionSummary.put("sessionId", conversationId);
            sessionSummary.put("sessionName", sessionName);
            sessionSummary.put("lastMessageTime", lastMessage.getCreateTime());
            sessionSummary.put("lastMessage", truncateContent(lastMessage.getContent()));
            sessionSummary.put("messageCount", messages.size());

            // 添加额外的元数据
            sessionSummary.put("lastMessageType", lastMessage.getMessageType());

            return sessionSummary;

        } catch (Exception e) {
            log.error("构建会话摘要失败, conversationId: {}", conversationId, e);
            return null;
        }
    }

    /**
     * 生成默认会话名称
     * 基于第一条用户消息的内容
     */
    private String generateDefaultSessionName(List<ChatMessage> messages) {
        // 查找第一条用户消息（messageType = "USER" 或 "user"）
        ChatMessage firstUserMessage = messages.stream()
                .filter(msg -> "USER".equalsIgnoreCase(msg.getMessageType()) || "user".equalsIgnoreCase(msg.getMessageType()))
                .findFirst()
                .orElse(null);

        if (firstUserMessage != null && firstUserMessage.getContent() != null) {
            String content = firstUserMessage.getContent().trim();
            if (content.length() <= 15) {
                return content;
            } else {
                return content.substring(0, 15) + "...";
            }
        }

        // 如果没有用户消息，使用第一条消息
        if (!messages.isEmpty()) {
            ChatMessage firstMessage = messages.get(0);
            if (firstMessage.getContent() != null) {
                String content = firstMessage.getContent().trim();
                if (content.length() <= 15) {
                    return content;
                } else {
                    return content.substring(0, 15) + "...";
                }
            }
        }

        return "新对话";
    }

    /**
     * 截断消息内容用于预览
     */
    private String truncateContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "...";
        }

        String trimmed = content.trim();
        if (trimmed.length() <= 50) {
            return trimmed;
        } else {
            return trimmed.substring(0, 50) + "...";
        }
    }
}
