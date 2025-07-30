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

    public SseEmitter doChatWithManus(String message) {
        HachimiManus hachimiManus = new HachimiManus(allTools, dashscopeChatModel, manusPrompt);
        return hachimiManus.runStream(message);
    }
}
