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

    @GetMapping(value = "/manus/chat/sse/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter doChatWithManusStream(@RequestParam String message) {
        try {
            return aiStreamingService.startManusStream(message);
        } catch (IllegalArgumentException e) {
            // 对于SSE，我们需要通过emitter发送错误，而不是返回错误响应
            SseEmitter errorEmitter = new SseEmitter(1000L);
            try {
                errorEmitter.send(SseEmitter.event()
                        .name("error")
                        .data("参数错误: " + e.getMessage()));
                errorEmitter.completeWithError(e);
            } catch (IOException ioException) {
                log.error("发送错误事件失败", ioException);
            }
            return errorEmitter;
        } catch (Exception e) {
            log.error("启动Manus SSE流失败", e);
            SseEmitter errorEmitter = new SseEmitter(1000L);
            try {
                errorEmitter.send(SseEmitter.event()
                        .name("error")
                        .data("启动Manus聊天失败: " + e.getMessage()));
                errorEmitter.completeWithError(e);
            } catch (IOException ioException) {
                log.error("发送错误事件失败", ioException);
            }
            return errorEmitter;
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
