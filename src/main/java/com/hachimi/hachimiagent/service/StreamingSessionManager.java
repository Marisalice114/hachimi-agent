package com.hachimi.hachimiagent.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 简化的流式会话管理器
 * 只管理中断状态，不重复管理聊天记录
 */
@Service
@Slf4j
public class StreamingSessionManager {

    /**
     * 流式会话状态
     */
    private static class StreamState {
        private final String chatId;
        private final String message;
        private final AtomicBoolean stopped;
        private final long createTime;

        public StreamState(String chatId, String message) {
            this.chatId = chatId;
            this.message = message;
            this.stopped = new AtomicBoolean(false);
            this.createTime = System.currentTimeMillis();
        }

        public String getChatId() { return chatId; }
        public String getMessage() { return message; }
        public boolean isStopped() { return stopped.get(); }
        public void stop() { stopped.set(true); }
        public boolean shouldContinue() { return !stopped.get(); }
        public long getCreateTime() { return createTime; }
    }

    // 活跃的流式会话
    private final ConcurrentHashMap<String, StreamState> activeStreams = new ConcurrentHashMap<>();

    /**
     * 创建流式会话（简化版本）
     */
    public String createStream(String chatId) {
        String streamId = java.util.UUID.randomUUID().toString();
        StreamState state = new StreamState(chatId, "");
        activeStreams.put(streamId, state);
        log.debug("创建流式会话: {} -> {}", streamId, chatId);
        return streamId;
    }

    /**
     * 创建流式会话（完整版本）
     */
    public boolean createStream(String streamId, String chatId, String message) {
        try {
            StreamState state = new StreamState(chatId, message);
            activeStreams.put(streamId, state);
            log.debug("创建流式会话: {} -> {} ({})", streamId, chatId, message);
            return true;
        } catch (Exception e) {
            log.error("创建流式会话失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 停止流式会话
     */
    public boolean stopStream(String streamId) {
        StreamState state = activeStreams.get(streamId);
        if (state != null) {
            state.stop();
            log.info("停止流式会话: {}", streamId);
            return true;
        }
        return false;
    }

    /**
     * 检查是否应该继续
     */
    public boolean shouldContinue(String streamId) {
        StreamState state = activeStreams.get(streamId);
        return state != null && state.shouldContinue();
    }

    /**
     * 清理会话
     */
    public void cleanupStream(String streamId) {
        StreamState removed = activeStreams.remove(streamId);
        if (removed != null) {
            log.debug("清理流式会话: {}", streamId);
        }
    }

    /**
     * 停止所有会话
     */
    public void stopAllStreams() {
        int count = 0;
        for (StreamState state : activeStreams.values()) {
            state.stop();
            count++;
        }
        activeStreams.clear();
        log.info("停止所有流式会话，共 {} 个", count);
    }

    /**
     * 获取活跃会话数量
     */
    public int getActiveStreamCount() {
        return activeStreams.size();
    }

    /**
     * 获取对话ID
     */
    public String getChatId(String streamId) {
        StreamState state = activeStreams.get(streamId);
        return state != null ? state.getChatId() : null;
    }

    /**
     * 获取当前消息
     */
    public String getCurrentMessage(String streamId) {
        StreamState state = activeStreams.get(streamId);
        return state != null ? state.getMessage() : null;
    }

    /**
     * 获取创建时间
     */
    public Long getCreateTime(String streamId) {
        StreamState state = activeStreams.get(streamId);
        return state != null ? state.getCreateTime() : null;
    }

    /**
     * 检查会话是否活跃
     */
    public boolean isStreamActive(String streamId) {
        return activeStreams.containsKey(streamId);
    }
}