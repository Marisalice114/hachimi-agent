package com.hachimi.hachimiagent.controller;

import com.hachimi.hachimiagent.agent.HachimiManus;
import com.hachimi.hachimiagent.agent.Prompt.ManusPrompt;
import com.hachimi.hachimiagent.app.LoveApp;
import com.hachimi.hachimiagent.common.BaseResponse;
import com.hachimi.hachimiagent.common.ResultUtils;
import com.hachimi.hachimiagent.exception.ErrorCode;
import com.hachimi.hachimiagent.service.StreamingSessionManager;
import jakarta.annotation.Resource;
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
@Slf4j
public class AIController {

    @Resource
    private LoveApp loveApp;

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private ChatModel dashscopeChatModel;

    @Resource
    private ManusPrompt manusPrompt;

    @Resource
    private StreamingSessionManager streamingManager;

    // ================================
    // 保持原有接口不变
    // ================================

    /**
     * 同步聊天接口
     */
    @GetMapping("/love_app/chat/sync")
    public String doChatWithLoveAppSync(String message, String chatId) {
        return loveApp.doChatWithRAG(message, chatId);
    }

    /**
     * 基础流式聊天接口
     */
    @GetMapping(value = "/love_app/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithLoveAppSSE(String message, String chatId) {
        return loveApp.doChatByStream(message, chatId);
    }

    /**
     * 基础SSE emitter接口
     */
    @GetMapping("/love_app/chat/sse/emitter")
    public SseEmitter doChatWithLoveAppSseEmitter(String message, String chatId) {
        SseEmitter emitter = new SseEmitter(180000L);

        loveApp.doChatByStream(message, chatId)
                .doOnNext(chunk -> {
                    try {
                        emitter.send(chunk);
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                })
                .doOnComplete(emitter::complete)
                .doOnError(emitter::completeWithError)
                .subscribe();

        return emitter;
    }

    /**
     * 流式调用manus
     */
    @GetMapping("/manus/chat")
    public SseEmitter doChatWithManus(String message) {
        HachimiManus hachimiManus = new HachimiManus(allTools, dashscopeChatModel, manusPrompt);
        return hachimiManus.runStream(message);
    }

    @GetMapping("/manus/chat/sse/emitter")
    public SseEmitter doChatWithManusSSE(String message, String chatId) {
        HachimiManus hachimiManus = new HachimiManus(allTools, dashscopeChatModel, manusPrompt);
        return hachimiManus.runStream(message);
    }

    // ================================
    // 新增：可中断的流式聊天接口（在现有接口基础上优化）
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

        // 生成streamId用于中断控制
        String streamId = UUID.randomUUID().toString();

        // 创建会话管理
        streamingManager.createStream(streamId, chatId, message);

        SseEmitter emitter = new SseEmitter(300000L);

        // ⭐⭐⭐ 关键修改：立即发送stream信息 ⭐⭐⭐
        try {
            emitter.send(SseEmitter.event()
                    .name("stream_info")  // 建议用stream_info而不是stream_start
                    .data(Map.of(
                            "streamId", streamId,
                            "chatId", chatId,
                            "message", message,
                            "timestamp", System.currentTimeMillis()
                    )));
            log.info("已发送stream_info事件: streamId={}", streamId);
        } catch (IOException e) {
            log.error("发送stream_info事件失败: {}", e.getMessage());
            return ResultUtils.error(ErrorCode.AI_PROCESS_ERROR.getCode(), "初始化流失败");
        }

        // 异步处理AI调用
        CompletableFuture.runAsync(() -> {
            try {
                // ⚠️ 注意：这里不再发送stream_start事件了！

                // 调用AI并添加中断检查
                loveApp.doChatByStream(message, chatId)
                        .takeWhile(chunk -> streamingManager.shouldContinue(streamId))
                        .delayElements(Duration.ofMillis(50)) // 及时检查中断
                        .doOnNext(chunk -> {
                            if (streamingManager.shouldContinue(streamId)) {
                                try {
                                    emitter.send(SseEmitter.event()
                                            .name("data")
                                            .data(chunk));
                                } catch (IOException e) {
                                    log.error("发送数据失败: {}", e.getMessage());
                                    streamingManager.stopStream(streamId);
                                }
                            }
                        })
                        .doOnComplete(() -> {
                            if (streamingManager.shouldContinue(streamId)) {
                                try {
                                    emitter.send(SseEmitter.event()
                                            .name("complete")
                                            .data("AI回复完成"));
                                    emitter.complete();
                                } catch (IOException e) {
                                    log.error("完成事件发送失败: {}", e.getMessage());
                                }
                            }
                            streamingManager.cleanupStream(streamId);
                        })
                        .doOnError(error -> {
                            log.error("AI调用错误: {}", error.getMessage());
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("error")
                                        .data("AI处理失败: " + error.getMessage()));
                            } catch (IOException e) {
                                log.error("错误事件发送失败: {}", e.getMessage());
                            }
                            emitter.completeWithError(error);
                            streamingManager.cleanupStream(streamId);
                        })
                        .subscribe();

            } catch (Exception e) {
                log.error("启动AI流式处理失败: {}", e.getMessage());
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("启动AI处理失败: " + e.getMessage()));
                } catch (IOException ioException) {
                    log.error("发送启动失败消息失败: {}", ioException.getMessage());
                }
                emitter.completeWithError(e);
                streamingManager.cleanupStream(streamId);
            }
        });

        // 设置清理回调
        emitter.onCompletion(() -> {
            log.info("SSE连接完成: streamId={}", streamId);
            streamingManager.cleanupStream(streamId);
        });
        emitter.onTimeout(() -> {
            log.warn("SSE连接超时: streamId={}", streamId);
            streamingManager.cleanupStream(streamId);
        });
        emitter.onError(ex -> {
            log.error("SSE连接错误: streamId={}, error={}", streamId, ex.getMessage());
            streamingManager.cleanupStream(streamId);
        });

        return ResultUtils.success(emitter);
    }

    /**
     * 可中断的Manus聊天
     */
    @GetMapping("/manus/chat/interruptible")
    public BaseResponse<SseEmitter> doChatWithManusInterruptible(@RequestParam String message) {

        if (message == null || message.trim().isEmpty()) {
            return ResultUtils.error(ErrorCode.INVALID_REQUEST.getCode(), "消息内容不能为空");
        }

        String streamId = UUID.randomUUID().toString();
        String chatId = "manus-" + System.currentTimeMillis();
        streamingManager.createStream(streamId, chatId, message);

        SseEmitter emitter = new SseEmitter(300000L);

        CompletableFuture.runAsync(() -> {
            try {
                // 发送streamId
                emitter.send(SseEmitter.event()
                        .name("stream_start")
                        .data(Map.of("streamId", streamId, "type", "manus", "message", message)));

                // 创建Manus实例
                HachimiManus hachimiManus = new HachimiManus(allTools, dashscopeChatModel, manusPrompt);

                // 这里需要修改HachimiManus的runStream方法支持中断
                // 或者使用类似的流式处理逻辑
                // 暂时使用简单的文本响应演示

                String response = "Manus处理: " + message + " (这里需要集成实际的Manus流式调用)";

                // 模拟流式输出
                for (int i = 0; i < response.length() && streamingManager.shouldContinue(streamId); i += 5) {
                    if (!streamingManager.shouldContinue(streamId)) break;

                    String chunk = response.substring(i, Math.min(i + 5, response.length()));
                    emitter.send(SseEmitter.event().name("data").data(chunk));

                    Thread.sleep(100); // 模拟处理时间
                }

                if (streamingManager.shouldContinue(streamId)) {
                    emitter.send(SseEmitter.event().name("complete").data("Manus处理完成"));
                    emitter.complete();
                }

            } catch (Exception e) {
                log.error("Manus处理失败: {}", e.getMessage());
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("Manus处理失败: " + e.getMessage()));
                } catch (IOException ioException) {
                    log.error("发送Manus错误消息失败: {}", ioException.getMessage());
                }
                emitter.completeWithError(e);
            } finally {
                streamingManager.cleanupStream(streamId);
            }
        });

        emitter.onCompletion(() -> streamingManager.cleanupStream(streamId));
        emitter.onTimeout(() -> streamingManager.cleanupStream(streamId));
        emitter.onError(ex -> streamingManager.cleanupStream(streamId));

        return ResultUtils.success(emitter);
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
            if (streamId == null || streamId.trim().isEmpty()) {
                return ResultUtils.error(ErrorCode.INVALID_REQUEST.getCode(), "流ID不能为空");
            }

            boolean stopped = streamingManager.stopStream(streamId);
            String chatId = streamingManager.getChatId(streamId);

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
            int count = streamingManager.getActiveStreamCount();
            streamingManager.stopAllStreams();

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
            int activeCount = streamingManager.getActiveStreamCount();

            Map<String, Object> result = Map.of(
                    "activeCount", activeCount,
                    "timestamp", System.currentTimeMillis()
            );

            return ResultUtils.success(result);

        } catch (Exception e) {
            log.error("获取活跃会话失败: {}", e.getMessage(), e);
            return ResultUtils.error(ErrorCode.AI_PROCESS_ERROR.getCode(), "获取会话状态失败: " + e.getMessage());
        }
    }
}
