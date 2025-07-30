package com.hachimi.hachimiagent.chatmemory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hachimi.hachimiagent.common.MessageConverter;
import com.hachimi.hachimiagent.entity.ChatConversation;
import com.hachimi.hachimiagent.entity.ChatMessage;
import com.hachimi.hachimiagent.mapper.ChatConversationMapper;
import com.hachimi.hachimiagent.mapper.ChatMessageMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 基于MySQL的ChatMemoryRepository实现
 * 完全解决重复保存问题
 *
 * @author hachimi
 * @since 2025-01-17
 */
@Repository
@Slf4j
public class MysqlBasedChatMemoryRepository implements ChatMemoryRepository {

    private final ChatConversationMapper conversationMapper;
    private final ChatMessageMapper messageMapper;

    // 缓存最后保存的消息摘要，避免重复保存
    private final Map<String, String> lastSavedFingerprint = new ConcurrentHashMap<>();

    public MysqlBasedChatMemoryRepository(ChatConversationMapper conversationMapper,
                                          ChatMessageMapper messageMapper) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        log.info("MysqlBasedChatMemoryRepository 初始化完成");
    }

    @Override
    public List<String> findConversationIds() {
        try {
            List<String> ids = conversationMapper.findAllConversationIds();
            log.debug("查询到 {} 个对话ID", ids.size());
            return ids;
        } catch (Exception e) {
            log.error("查询对话ID列表失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        Assert.hasText(conversationId, "conversationId cannot be null or empty");

        try {
            List<ChatMessage> entities = messageMapper.findByConversationIdOrderByOrder(conversationId);
            List<Message> messages = entities.stream()
                    .map(MessageConverter::toMessage)
                    .collect(Collectors.toList());

            log.debug("对话 {} 查询到 {} 条消息", conversationId, messages.size());
            return messages;
        } catch (Exception e) {
            log.error("查询对话消息失败, conversationId: {}", conversationId, e);
            return new ArrayList<>();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAll(String conversationId, List<Message> messages) {
        Assert.hasText(conversationId, "conversationId cannot be null or empty");
        Assert.notNull(messages, "messages cannot be null");

        if (messages.isEmpty()) {
            log.debug("消息列表为空，跳过保存: conversationId={}", conversationId);
            return;
        }

        log.info("收到saveAll请求: conversationId={}, messageCount={}", conversationId, messages.size());

        try {
            // 1. 生成当前消息的指纹
            String currentFingerprint = generateMessagesFingerprint(messages);
            String lastFingerprint = lastSavedFingerprint.get(conversationId);

            // 2. 如果指纹相同，直接跳过
            if (currentFingerprint.equals(lastFingerprint)) {
                log.info("消息指纹相同，跳过保存: conversationId={}, fingerprint={}",
                        conversationId, currentFingerprint.substring(0, 8));
                return;
            }

            // 3. 确保会话记录存在
            ensureConversationExists(conversationId);

            // 4. 执行智能保存
            performIntelligentSave(conversationId, messages);

            // 5. 更新指纹缓存
            lastSavedFingerprint.put(conversationId, currentFingerprint);

            log.info("saveAll完成: conversationId={}, finalMessageCount={}, newFingerprint={}",
                    conversationId, getMessageCount(conversationId), currentFingerprint.substring(0, 8));
        } catch (Exception e) {
            log.error("保存对话失败: conversationId={}", conversationId, e);
            throw new RuntimeException("保存对话失败", e);
        }
    }

    /**
     * 生成消息列表的指纹
     * 基于消息类型、内容和顺序生成唯一标识
     */
    private String generateMessagesFingerprint(List<Message> messages) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < messages.size(); i++) {
            Message msg = messages.get(i);
            sb.append(i).append(":")
                    .append(msg.getMessageType().name()).append(":")
                    .append(msg.getText().length()).append(":")
                    .append(msg.getText().hashCode()).append("|");
        }

        // 生成简单的哈希
        String content = sb.toString();
        return String.valueOf(content.hashCode()) + "_" + messages.size();
    }

    /**
     * 执行智能保存
     * 只有在真正需要时才进行数据库操作
     */
    private void performIntelligentSave(String conversationId, List<Message> messages) {
        // ✅ 简化版本 - 因为指纹已经验证过内容不同，直接更新即可
        log.info("确认需要更新数据库: conversationId={}", conversationId);

        // 删除现有消息（逻辑删除）
        deleteExistingMessages(conversationId);

        // 保存新消息
        saveMessages(conversationId, messages);
    }

    /**
     * 详细检查是否需要更新
     */
    private boolean isUpdateRequired(List<Message> newMessages, List<ChatMessage> existingMessages) {
        // 1. 数量检查
        if (newMessages.size() != existingMessages.size()) {
            log.debug("消息数量不同: DB={}, New={}", existingMessages.size(), newMessages.size());
            return true;
        }

        // 2. 内容详细比较
        for (int i = 0; i < newMessages.size(); i++) {
            Message newMsg = newMessages.get(i);
            ChatMessage existingMsg = existingMessages.get(i);

            // 比较消息类型
            if (!newMsg.getMessageType().name().equals(existingMsg.getMessageType())) {
                log.debug("消息类型不同，位置{}: DB={}, New={}",
                        i, existingMsg.getMessageType(), newMsg.getMessageType().name());
                return true;
            }

            // 比较消息内容
            String newContent = newMsg.getText();
            String existingContent = existingMsg.getContent();

            if (!Objects.equals(newContent, existingContent)) {
                log.debug("消息内容不同，位置{}: 长度差异={}",
                        i, Math.abs(newContent.length() - existingContent.length()));
                return true;
            }
        }

        log.debug("所有消息内容完全相同，无需更新");
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByConversationId(String conversationId) {
        Assert.hasText(conversationId, "conversationId cannot be null or empty");

        log.info("开始删除对话: conversationId={}", conversationId);

        try {
            // 逻辑删除消息
            LambdaUpdateWrapper<ChatMessage> messageUpdateWrapper = new LambdaUpdateWrapper<>();
            messageUpdateWrapper.eq(ChatMessage::getConversationId, conversationId)
                    .set(ChatMessage::getDeleted, 1);
            int deletedMessages = messageMapper.update(null, messageUpdateWrapper);

            // 逻辑删除会话
            LambdaUpdateWrapper<ChatConversation> conversationUpdateWrapper = new LambdaUpdateWrapper<>();
            conversationUpdateWrapper.eq(ChatConversation::getConversationId, conversationId)
                    .set(ChatConversation::getDeleted, 1)
                    .set(ChatConversation::getUpdateTime, LocalDateTime.now());
            int deletedConversations = conversationMapper.update(null, conversationUpdateWrapper);

            // 清除缓存
            lastSavedFingerprint.remove(conversationId);

            log.info("成功删除对话: conversationId={}, deletedMessages={}, deletedConversations={}",
                    conversationId, deletedMessages, deletedConversations);
        } catch (Exception e) {
            log.error("删除对话失败: conversationId={}", conversationId, e);
            throw new RuntimeException("删除对话失败", e);
        }
    }

    /**
     * 确保会话记录存在
     */
    private void ensureConversationExists(String conversationId) {
        LambdaQueryWrapper<ChatConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatConversation::getConversationId, conversationId)
                .eq(ChatConversation::getDeleted, 0);

        ChatConversation existing = conversationMapper.selectOne(wrapper);
        if (existing == null) {
            ChatConversation conversation = new ChatConversation();
            conversation.setConversationId(conversationId);
            conversation.setCreateTime(LocalDateTime.now());
            conversation.setUpdateTime(LocalDateTime.now());
            conversation.setDeleted(0);

            conversationMapper.insert(conversation);
            log.debug("创建新的对话记录: conversationId={}", conversationId);
        } else {
            // 更新最后活跃时间
            existing.setUpdateTime(LocalDateTime.now());
            conversationMapper.updateById(existing);
            log.debug("更新对话记录时间: conversationId={}", conversationId);
        }
    }

    /**
     * 删除现有消息（逻辑删除）
     */
    private void deleteExistingMessages(String conversationId) {
        LambdaUpdateWrapper<ChatMessage> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ChatMessage::getConversationId, conversationId)
                .eq(ChatMessage::getDeleted, 0)
                .set(ChatMessage::getDeleted, 1);

        int updatedCount = messageMapper.update(null, updateWrapper);
        log.debug("逻辑删除了 {} 条消息", updatedCount);
    }

    /**
     * 保存消息列表
     */
    private void saveMessages(String conversationId, List<Message> messages) {
        for (int i = 0; i < messages.size(); i++) {
            Message message = messages.get(i);
            ChatMessage entity = MessageConverter.toEntity(message, conversationId, i);
            entity.setCreateTime(LocalDateTime.now());
            entity.setDeleted(0);

            messageMapper.insert(entity);
            log.debug("保存消息: conversationId={}, order={}, type={}",
                    conversationId, i, entity.getMessageType());
        }
        log.debug("成功保存 {} 条消息", messages.size());
    }

    /**
     * 获取消息数量
     */
    public long getMessageCount(String conversationId) {
        Assert.hasText(conversationId, "conversationId cannot be null or empty");

        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getConversationId, conversationId)
                .eq(ChatMessage::getDeleted, 0);

        return messageMapper.selectCount(wrapper);
    }

    /**
     * 清除指纹缓存（用于测试或维护）
     */
    public void clearFingerprintCache() {
        lastSavedFingerprint.clear();
        log.info("已清除指纹缓存");
    }

    /**
     * 获取缓存统计信息
     */
    public void printCacheStats() {
        log.info("=== 指纹缓存统计 ===");
        log.info("缓存的对话数: {}", lastSavedFingerprint.size());
        lastSavedFingerprint.forEach((conversationId, fingerprint) ->
                log.info("  {} -> {}", conversationId.substring(0, 8), fingerprint)
        );
        log.info("===================");
    }
}