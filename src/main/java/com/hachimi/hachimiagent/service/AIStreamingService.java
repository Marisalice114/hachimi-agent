package com.hachimi.hachimiagent.service;

import com.hachimi.hachimiagent.agent.HachimiManus;
import com.hachimi.hachimiagent.agent.Prompt.ManusPrompt;
import com.hachimi.hachimiagent.agent.model.AgentState;
import com.hachimi.hachimiagent.app.LoveApp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIStreamingService {

    private final LoveApp loveApp;
    private final StreamingSessionManager streamingManager;
    private final ToolCallback[] allTools;
    private final ChatModel dashscopeChatModel;
    private final ManusPrompt manusPrompt;

    public SseEmitter startLoveAppStream(String message, String chatId) {
        String streamId = UUID.randomUUID().toString();
        streamingManager.createStream(streamId, chatId, message);

        SseEmitter emitter = new SseEmitter(300000L);

        try {
            emitter.send(SseEmitter.event()
                    .name("stream_info")
                    .data(Map.of(
                            "streamId", streamId,
                            "chatId", chatId,
                            "message", message,
                            "timestamp", System.currentTimeMillis()
                    )));
            log.info("已发送stream_info事件: streamId={}", streamId);
        } catch (IOException e) {
            log.error("发送stream_info事件失败: {}", e.getMessage());
            throw new RuntimeException("初始化流失败", e);
        }

        CompletableFuture.runAsync(() -> {
            try {
                loveApp.doChatByStream(message, chatId)
                        .takeWhile(chunk -> streamingManager.shouldContinue(streamId))
                        .delayElements(Duration.ofMillis(50))
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

        setupEmitterCallbacks(emitter, streamId);
        return emitter;
    }

    public SseEmitter startManusStream(String message) {
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("消息内容不能为空");
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

                // 【关键修改】设置SSE发送器用于思考过程可视化
                hachimiManus.setSseEmitter(emitter);

                hachimiManus.setNextStepPrompt(message);

                int stepCount = 0;

                // 执行Agent循环
                while (hachimiManus.getState() != AgentState.FINISHED &&
                        stepCount < hachimiManus.getMaxSteps() &&
                        streamingManager.shouldContinue(streamId)) {

                    stepCount++;

                    // 【修改】移除手动发送思考信号，由think方法内部发送
                    // emitter.send("THINK:正在分析问题: " + message);

                    // 执行think阶段（think方法内部会自动发送THINK:消息）
                    boolean needsAction = hachimiManus.think();

                    ChatResponse response = hachimiManus.getToolCallChatResponse();

                    if (needsAction && response != null && response.getResult().getOutput().getToolCalls() != null) {
                        // 有工具调用
                        for (AssistantMessage.ToolCall toolCall : response.getResult().getOutput().getToolCalls()) {

                            // 发送工具开始信号
                            emitter.send("TOOL_START:" + toolCall.name());

                            // 发送工具参数
                            emitter.send("TOOL_ARGS:" + toolCall.arguments());

                            // 执行工具（act方法内部会自动发送思考过程）
                            String result = hachimiManus.act();

                            // 发送工具结果
                            emitter.send("TOOL_RESULT:" + result);
                        }
                    } else {
                        // 没有工具调用，获取AI的文本回复作为最终回复
                        if (response != null && response.getResult().getOutput().getText() != null) {
                            String textResponse = response.getResult().getOutput().getText();
                            if (!textResponse.trim().isEmpty()) {
                                emitter.send("FINAL_RESPONSE:" + textResponse);
                            }
                        }
                        break;
                    }

                    Thread.sleep(100);
                }

                // 如果循环结束但没有发送最终回复，发送默认完成消息
                if (streamingManager.shouldContinue(streamId)) {
                    emitter.send("FINAL_RESPONSE:任务处理完成");
                    emitter.send("[DONE]");
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

        setupEmitterCallbacks(emitter, streamId);
        return emitter;
    }

    public boolean stopStream(String streamId) {
        if (streamId == null || streamId.trim().isEmpty()) {
            throw new IllegalArgumentException("流ID不能为空");
        }
        return streamingManager.stopStream(streamId);
    }

    public String getChatId(String streamId) {
        return streamingManager.getChatId(streamId);
    }

    public int getActiveStreamCount() {
        return streamingManager.getActiveStreamCount();
    }

    public void stopAllStreams() {
        streamingManager.stopAllStreams();
    }

    public Map<String, Object> getActiveStreamsInfo() {
        int activeCount = getActiveStreamCount();
        return Map.of(
                "activeCount", activeCount,
                "timestamp", System.currentTimeMillis()
        );
    }

    private void setupEmitterCallbacks(SseEmitter emitter, String streamId) {
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
    }
}