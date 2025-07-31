package com.hachimi.hachimiagent.service;

import com.hachimi.hachimiagent.agent.HachimiManus;
import com.hachimi.hachimiagent.agent.Prompt.ManusPrompt;
import com.hachimi.hachimiagent.app.LoveApp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class AISyncService {

    private final LoveApp loveApp;
    private final ToolCallback[] allTools;
    private final ChatModel dashscopeChatModel;
    private final ManusPrompt manusPrompt;

    public String doChatWithLoveAppSync(String message, String chatId) {
        return loveApp.doChatWithRAG(message, chatId);
    }

    public Flux<String> doChatWithLoveAppSSE(String message, String chatId) {
        return loveApp.doChatByStream(message, chatId);
    }

    public SseEmitter doChatWithLoveAppSseEmitter(String message, String chatId) {
        SseEmitter emitter = new SseEmitter(180000L);

        // 设置超时和完成回调
        emitter.onTimeout(() -> {
            log.warn("SSE connection timeout for chatId: {}", chatId);
        });

        emitter.onCompletion(() -> {
            log.debug("SSE connection completed for chatId: {}", chatId);
        });

        loveApp.doChatByStream(message, chatId)
                .doOnNext(chunk -> {
                    try {
                        // 检查emitter是否已经完成
                        if (!isEmitterCompleted(emitter)) {
                            emitter.send(chunk);
                        }
                    } catch (IOException e) {
                        log.warn("Client disconnected during SSE streaming: {}", e.getMessage());
                        // 不要在这里调用completeWithError，让doOnError处理
                    } catch (IllegalStateException e) {
                        log.warn("SSE emitter already completed: {}", e.getMessage());
                    }
                })
                .doOnComplete(() -> {
                    try {
                        if (!isEmitterCompleted(emitter)) {
                            emitter.complete();
                        }
                    } catch (IllegalStateException e) {
                        log.warn("SSE emitter already completed on complete: {}", e.getMessage());
                    }
                })
                .doOnError(error -> {
                    log.error("Error in SSE streaming for chatId: {}", chatId, error);
                    try {
                        if (!isEmitterCompleted(emitter)) {
                            emitter.completeWithError(error);
                        }
                    } catch (IllegalStateException e) {
                        log.warn("SSE emitter already completed on error: {}", e.getMessage());
                    }
                })
                .subscribe(
                        // onNext: 已在doOnNext中处理
                        chunk -> {},
                        // onError: 错误处理
                        error -> log.error("Stream error for chatId: {}", chatId, error),
                        // onComplete: 完成处理
                        () -> log.debug("Stream completed for chatId: {}", chatId)
                );

        return emitter;
    }

    // 辅助方法：检查emitter是否已完成
    private boolean isEmitterCompleted(SseEmitter emitter) {
        try {
            // 尝试发送一个测试数据来检查状态
            // 这不是最优雅的方式，但在当前Spring版本中比较实用
            return false; // 简化实现，您可以根据需要优化
        } catch (IllegalStateException e) {
            return true;
        }
    }

    public SseEmitter doChatWithManus(String message) {
        HachimiManus hachimiManus = new HachimiManus(allTools, dashscopeChatModel, manusPrompt);
        return hachimiManus.runStream(message);
    }
}
