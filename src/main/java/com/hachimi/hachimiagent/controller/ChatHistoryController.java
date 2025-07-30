package com.hachimi.hachimiagent.controller;

import com.hachimi.hachimiagent.common.BaseResponse;
import com.hachimi.hachimiagent.common.ResultUtils;
import com.hachimi.hachimiagent.entity.ChatMessage;
import com.hachimi.hachimiagent.exception.ErrorCode;
import com.hachimi.hachimiagent.service.ChatHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 聊天历史管理控制器
 * 基于现有的数据库ChatMemory体系，直接使用Entity
 */
@RestController
@RequestMapping("/chat")
//@CrossOrigin(origins = "*")
@Slf4j
public class ChatHistoryController {

    @Resource
    private ChatHistoryService chatHistoryService;

    /**
     * 获取所有会话列表
     * 返回会话摘要信息的Map列表
     */
    @GetMapping("/sessions")
    public BaseResponse<List<Map<String, Object>>> getAllSessions() {
        try {
            List<Map<String, Object>> sessions = chatHistoryService.getAllSessions();
            log.debug("获取会话列表成功，共 {} 个会话", sessions.size());
            return ResultUtils.success(sessions, "获取会话列表成功");
        } catch (Exception e) {
            log.error("获取会话列表失败: {}", e.getMessage(), e);
            return ResultUtils.error(ErrorCode.AI_PROCESS_ERROR.getCode(), "获取会话列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取指定会话的消息历史
     * 直接返回ChatMessage实体列表
     */
    @GetMapping("/sessions/{sessionId}/messages")
    public BaseResponse<List<ChatMessage>> getSessionMessages(@PathVariable String sessionId) {
        try {
            // 参数验证
            if (sessionId == null || sessionId.trim().isEmpty()) {
                return ResultUtils.error(ErrorCode.INVALID_REQUEST.getCode(), "会话ID不能为空");
            }

            List<ChatMessage> messages = chatHistoryService.getSessionMessages(sessionId);
            log.debug("获取会话 {} 的消息历史成功，共 {} 条消息", sessionId, messages.size());
            return ResultUtils.success(messages, "获取消息历史成功");
        } catch (Exception e) {
            log.error("获取会话 {} 的消息历史失败: {}", sessionId, e.getMessage(), e);
            return ResultUtils.error(ErrorCode.AI_PROCESS_ERROR.getCode(), "获取消息历史失败: " + e.getMessage());
        }
    }

    /**
     * 获取会话信息
     * 返回会话摘要Map
     */
    @GetMapping("/sessions/{sessionId}")
    public BaseResponse<Map<String, Object>> getSession(@PathVariable String sessionId) {
        try {
            // 参数验证
            if (sessionId == null || sessionId.trim().isEmpty()) {
                return ResultUtils.error(ErrorCode.INVALID_REQUEST.getCode(), "会话ID不能为空");
            }

            Map<String, Object> session = chatHistoryService.getSession(sessionId);

            if (session == null || session.isEmpty()) {
                log.warn("会话 {} 不存在", sessionId);
                return ResultUtils.error(ErrorCode.SESSION_NOT_FOUND);
            }

            log.debug("获取会话 {} 信息成功", sessionId);
            return ResultUtils.success(session, "获取会话信息成功");
        } catch (Exception e) {
            log.error("获取会话 {} 信息失败: {}", sessionId, e.getMessage(), e);
            return ResultUtils.error(ErrorCode.AI_PROCESS_ERROR.getCode(), "获取会话信息失败: " + e.getMessage());
        }
    }

    /**
     * 删除会话（使用现有的ChatMemory体系）
     */
    @DeleteMapping("/sessions/{sessionId}")
    public BaseResponse<Map<String, Object>> deleteSession(@PathVariable String sessionId) {
        try {
            // 参数验证
            if (sessionId == null || sessionId.trim().isEmpty()) {
                return ResultUtils.error(ErrorCode.INVALID_REQUEST.getCode(), "会话ID不能为空");
            }

            // 先检查会话是否存在
            Map<String, Object> session = chatHistoryService.getSession(sessionId);
            if (session == null || session.isEmpty()) {
                log.warn("尝试删除不存在的会话: {}", sessionId);
                return ResultUtils.error(ErrorCode.SESSION_NOT_FOUND);
            }

            // 执行删除
            chatHistoryService.deleteSession(sessionId);

            // 构建删除结果
            Map<String, Object> result = Map.of(
                    "sessionId", sessionId,
                    "deleted", true,
                    "deletedAt", System.currentTimeMillis()
            );

            log.info("成功删除会话: {}", sessionId);
            return ResultUtils.success(result, "会话删除成功");
        } catch (Exception e) {
            log.error("删除会话 {} 失败: {}", sessionId, e.getMessage(), e);
            return ResultUtils.error(ErrorCode.AI_PROCESS_ERROR.getCode(), "删除会话失败: " + e.getMessage());
        }
    }

    /**
     * 批量删除会话
     */
    @DeleteMapping("/sessions/batch")
    public BaseResponse<Map<String, Object>> batchDeleteSessions(@RequestBody List<String> sessionIds) {
        try {
            // 参数验证
            if (sessionIds == null || sessionIds.isEmpty()) {
                return ResultUtils.error(ErrorCode.INVALID_REQUEST.getCode(), "会话ID列表不能为空");
            }

            int successCount = 0;
            int failCount = 0;

            for (String sessionId : sessionIds) {
                try {
                    if (sessionId != null && !sessionId.trim().isEmpty()) {
                        chatHistoryService.deleteSession(sessionId);
                        successCount++;
                        log.debug("删除会话成功: {}", sessionId);
                    } else {
                        failCount++;
                        log.warn("跳过无效的会话ID: {}", sessionId);
                    }
                } catch (Exception e) {
                    failCount++;
                    log.error("删除会话 {} 失败: {}", sessionId, e.getMessage());
                }
            }

            Map<String, Object> result = Map.of(
                    "total", sessionIds.size(),
                    "successCount", successCount,
                    "failCount", failCount,
                    "deletedAt", System.currentTimeMillis()
            );

            log.info("批量删除会话完成，成功: {}, 失败: {}", successCount, failCount);
            return ResultUtils.success(result,
                    String.format("批量删除完成，成功 %d 个，失败 %d 个", successCount, failCount));
        } catch (Exception e) {
            log.error("批量删除会话失败: {}", e.getMessage(), e);
            return ResultUtils.error(ErrorCode.AI_PROCESS_ERROR.getCode(), "批量删除会话失败: " + e.getMessage());
        }
    }

    /**
     * 获取会话统计信息
     */
    @GetMapping("/sessions/stats")
    public BaseResponse<Map<String, Object>> getSessionStats() {
        try {
            List<Map<String, Object>> allSessions = chatHistoryService.getAllSessions();

            // 统计信息
            int totalSessions = allSessions.size();
            long totalMessages = allSessions.stream()
                    .mapToLong(session -> {
                        Object messageCount = session.get("messageCount");
                        return messageCount instanceof Number ? ((Number) messageCount).longValue() : 0L;
                    })
                    .sum();

            Map<String, Object> stats = Map.of(
                    "totalSessions", totalSessions,
                    "totalMessages", totalMessages,
                    "averageMessagesPerSession", totalSessions > 0 ? (double) totalMessages / totalSessions : 0.0,
                    "generatedAt", System.currentTimeMillis()
            );

            log.debug("获取会话统计信息成功");
            return ResultUtils.success(stats, "获取统计信息成功");
        } catch (Exception e) {
            log.error("获取会话统计信息失败: {}", e.getMessage(), e);
            return ResultUtils.error(ErrorCode.AI_PROCESS_ERROR.getCode(), "获取统计信息失败: " + e.getMessage());
        }
    }
}