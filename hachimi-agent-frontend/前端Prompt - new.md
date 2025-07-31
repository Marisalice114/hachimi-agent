你是一位专业的前端开发，请帮我根据下列信息来生成对应的前端项目代码。

## 需求

1）主页：用于切换不同的应用

2）页面 1：AI 恋爱大师应用。页面风格为聊天室，上方是聊天记录（用户信息在右边，AI 信息在左边），下方是输入框，进入页面后自动生成一个聊天室 id，用于区分不同的会话。通过 SSE 的方式调用 doChatWithLoveAppSseEmitter 接口，实时显示对话内容。

3）页面 2：AI 超级智能体应用。页面风格同页面 1，但是调用 doChatWithManus 接口，也是实时显示对话内容。

4）在ai恋爱大师应用页面的侧边栏会显示历史会话记录，点击历史会话记录可以加载对应的聊天内容。用户可以在当前界面自行修改历史会话记录的名字，这个名字是不需要保存到后端的，只在前端做保留，可以设置为cookie

5）在AI 超级智能体的页面可以不设置页边的会话记录调取，因为manus没有相应的会话记录保存功能

6）我在后端设置了主动停止ai回复的功能，希望你在前端的合适位置添加停止ai回复的按钮

7）我现在对我的响应进行了封装，希望你能提取相应的信息

## 技术选型

1. Vue3 项目
2. Axios 请求库

## 后端接口信息

接口地址前缀：http://localhost:8123/api

# 封装信息

```
public class ResultUtils {

    /**
     * 成功
     */
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(0, data, "ok");
    }

    /**
     * 成功(无数据)
     */
    public static <T> BaseResponse<T> success() {
        return new BaseResponse<>(0, null, "ok");
    }

    /**
     * 成功(自定义消息)
     */
    public static <T> BaseResponse<T> success(T data, String message) {
        return new BaseResponse<>(0, data, message);
    }

    // =================== 错误处理 ===================

    /**
     * 失败 - 使用 ErrorCode
     */
    public static <T> BaseResponse<T> error(ErrorCode errorCode) {
        return new BaseResponse<>(errorCode.getCode(), null, errorCode.getMessage());
    }

    /**
     * 失败 - 使用 code + message
     */
    public static <T> BaseResponse<T> error(int code, String message) {
        return new BaseResponse<>(code, null, message);
    }

    /**
     * 失败 - 默认错误码 500
     */
    public static <T> BaseResponse<T> error(String message) {
        return new BaseResponse<>(500, null, message);
    }

```

```
package com.hachimi.hachimiagent.common;

import com.hachimi.hachimiagent.exception.ErrorCode;
import lombok.Data;

import java.io.Serializable;

@Data
public class BaseResponse<T> implements Serializable {

    private int code;

    private T data;

    private String message;

    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public BaseResponse(int code, T data) {
        this(code, data, "");
    }

    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }
}

```



## SpringBoot 后端接口代码



```
package com.hachimi.hachimiagent.controller;

import com.hachimi.hachimiagent.agent.HachimiManus;
import com.hachimi.hachimiagent.agent.Prompt.ManusPrompt;
import com.hachimi.hachimiagent.app.LoveApp;
import com.hachimi.hachimiagent.common.BaseResponse;
import com.hachimi.hachimiagent.common.ResultUtils;
import com.hachimi.hachimiagent.exception.ErrorCode;
import com.hachimi.hachimiagent.service.AIStreamingService;
import com.hachimi.hachimiagent.service.AISyncService;
import com.hachimi.hachimiagent.service.StreamingSessionManager;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Slf4j
public class AIController {

    private final AISyncService aiSyncService;
    private final AIStreamingService aiStreamingService;

    // ================================
    // 保持原有接口不变
    // ================================

    /**
     * 同步聊天接口
     */
    @GetMapping("/love_app/chat/sync")
    public String doChatWithLoveAppSync(String message, String chatId) {
        return aiSyncService.doChatWithLoveAppSync(message, chatId);
    }

    /**
     * 基础流式聊天接口
     */
    @GetMapping(value = "/love_app/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithLoveAppSSE(String message, String chatId) {
        return aiSyncService.doChatWithLoveAppSSE(message, chatId);
    }

    /**
     * 基础SSE emitter接口
     */
    @GetMapping("/love_app/chat/sse/emitter")
    public SseEmitter doChatWithLoveAppSseEmitter(String message, String chatId) {
        return aiSyncService.doChatWithLoveAppSseEmitter(message, chatId);
    }

    /**
     * 流式调用manus
     */
    @GetMapping("/manus/chat")
    public SseEmitter doChatWithManus(String message) {
        return aiSyncService.doChatWithManus(message);
    }

    @GetMapping("/manus/chat/sse/emitter")
    public SseEmitter doChatWithManusSSE(String message, String chatId) {
        return aiSyncService.doChatWithManus(message);
    }

    // ================================
    // 新增：可中断的流式聊天接口
    // ================================

    /**
     * 可中断的流式聊天 - 直接返回SSE，包含streamId
     */
    @GetMapping("/love_app/chat/sse/interruptible")
    public BaseResponse<SseEmitter> doChatStreamInterruptible(
            @RequestParam String message,
            @RequestParam String chatId) {

        // 参数验证
        if (message == null || message.trim().isEmpty()) {
            return ResultUtils.error(ErrorCode.INVALID_REQUEST.getCode(), "消息内容不能为空");
        }
        if (chatId == null || chatId.trim().isEmpty()) {
            return ResultUtils.error(ErrorCode.INVALID_REQUEST.getCode(), "会话ID不能为空");
        }

        try {
            SseEmitter emitter = aiStreamingService.startLoveAppStream(message, chatId);
            return ResultUtils.success(emitter);
        } catch (Exception e) {
            log.error("启动可中断流式聊天失败", e);
            return ResultUtils.error(ErrorCode.AI_PROCESS_ERROR.getCode(), "启动流式聊天失败: " + e.getMessage());
        }
    }

    /**
     * 可中断的Manus聊天
     */
    @GetMapping("/manus/chat/interruptible")
    public BaseResponse<SseEmitter> doChatWithManusInterruptible(@RequestParam String message) {
        try {
            SseEmitter emitter = aiStreamingService.startManusStream(message);
            return ResultUtils.success(emitter);
        } catch (IllegalArgumentException e) {
            return ResultUtils.error(ErrorCode.INVALID_REQUEST.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("启动可中断Manus聊天失败", e);
            return ResultUtils.error(ErrorCode.AI_PROCESS_ERROR.getCode(), "启动Manus聊天失败: " + e.getMessage());
        }
    }

    // ================================
    // 中断控制接口
    // ================================

    /**
     * 停止指定的流式对话
     */
    @PostMapping("/chat/stop/{streamId}")
    public BaseResponse<Map<String, Object>> stopChatStream(@PathVariable String streamId) {
        try {
            boolean stopped = aiStreamingService.stopStream(streamId);
            String chatId = aiStreamingService.getChatId(streamId);

            Map<String, Object> result = Map.of(
                    "streamId", streamId,
                    "stopped", stopped,
                    "chatId", chatId != null ? chatId : "",
                    "message", stopped ? "会话已停止" : "会话不存在或已结束"
            );

            if (stopped) {
                log.info("手动停止流式会话: {}", streamId);
            }

            return ResultUtils.success(result);

        } catch (IllegalArgumentException e) {
            return ResultUtils.error(ErrorCode.INVALID_REQUEST.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("停止会话失败: {}", e.getMessage(), e);
            return ResultUtils.error(ErrorCode.SESSION_STOP_ERROR.getCode(), "停止会话失败: " + e.getMessage());
        }
    }

    /**
     * 停止所有活跃的流式对话
     */
    @PostMapping("/chat/stop/all")
    public BaseResponse<Map<String, Object>> stopAllChatStreams() {
        try {
            int count = aiStreamingService.getActiveStreamCount();
            aiStreamingService.stopAllStreams();

            Map<String, Object> result = Map.of(
                    "stoppedCount", count,
                    "message", "已停止所有活跃会话"
            );

            log.info("停止所有流式会话，共 {} 个", count);
            return ResultUtils.success(result);

        } catch (Exception e) {
            log.error("停止所有会话失败: {}", e.getMessage(), e);
            return ResultUtils.error(ErrorCode.SESSION_STOP_ERROR.getCode(), "停止所有会话失败: " + e.getMessage());
        }
    }

    /**
     * 获取活跃会话状态
     */
    @GetMapping("/chat/sessions/active")
    public BaseResponse<Map<String, Object>> getActiveStreams() {
        try {
            Map<String, Object> result = aiStreamingService.getActiveStreamsInfo();
            return ResultUtils.success(result);
        } catch (Exception e) {
            log.error("获取活跃会话失败: {}", e.getMessage(), e);
            return ResultUtils.error(ErrorCode.AI_PROCESS_ERROR.getCode(), "获取会话状态失败: " + e.getMessage());
        }
    }
}

```

```
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
```








