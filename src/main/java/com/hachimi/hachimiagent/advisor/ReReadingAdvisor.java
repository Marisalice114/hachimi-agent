package com.hachimi.hachimiagent.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.Ordered;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ReReadingAdvisor implements CallAdvisor, StreamAdvisor {

    private static final String RE_READING_TEMPLATE = """
            %s
            Read the question again:%s
            """;

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        // 应用 re-reading
        ChatClientRequest enhancedRequest = applyReReading(chatClientRequest);

        // 调用下一个 advisor
        ChatClientResponse response = callAdvisorChain.nextCall(enhancedRequest);

        return response;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        ChatClientRequest enhancedRequest = applyReReading(chatClientRequest);

        return streamAdvisorChain.nextStream(enhancedRequest)
                .doOnNext(response -> {
                    log.debug("Stream response context: {}", response.context());
                });
    }

    private ChatClientRequest applyReReading(ChatClientRequest request) {
        try {
            // 检查是否已经应用过 re-reading，避免重复处理
            if (request.context().containsKey("re_reading_applied")) {
                log.debug("Re-reading already applied, skipping");
                return request;
            }

            // 1. 提取用户的最后一条消息
            String lastUserMessage = extractLastUserMessage(request);

            if (lastUserMessage == null || lastUserMessage.trim().isEmpty()) {
                log.warn("No user message found, skipping re-reading");
                return request;
            }

            // 2. 创建 re-reading 的增强提示
            String enhancedMessage = String.format(RE_READING_TEMPLATE,
                    lastUserMessage, lastUserMessage);

            // 3. 构建新的 Prompt
            Prompt newPrompt = buildEnhancedPrompt(request.prompt(), enhancedMessage);

            // 4. 创建新的 ChatClientRequest
            return request.mutate()
                    .prompt(newPrompt)
                    .context("re_reading_applied", true)
                    .context("original_user_message", lastUserMessage)
                    .context("enhancement_timestamp", System.currentTimeMillis())
                    .build();

        } catch (Exception e) {
            log.error("Failed to apply re-reading: {}", e.getMessage(), e);
            return request;
        }
    }

    private String extractLastUserMessage(ChatClientRequest request) {
        try {
            return request.prompt().getInstructions().stream()
                    .filter(msg -> msg.getMessageType() == MessageType.USER)
                    .reduce((first, second) -> second) // 获取最后一条用户消息
                    .map(Message::getText)
                    .orElse(null);
        } catch (Exception e) {
            log.error("Error extracting user message: {}", e.getMessage(), e);
            return null;
        }
    }

    private Prompt buildEnhancedPrompt(Prompt originalPrompt, String enhancedMessage) {
        try {
            List<Message> messages = new ArrayList<>(originalPrompt.getInstructions());

            // 找到最后一条用户消息并替换
            for (int i = messages.size() - 1; i >= 0; i--) {
                if (messages.get(i).getMessageType() == MessageType.USER) {
                    messages.set(i, new UserMessage(enhancedMessage));
                    break;
                }
            }

            return new Prompt(messages, originalPrompt.getOptions());
        } catch (Exception e) {
            log.error("Error building enhanced prompt: {}", e.getMessage(), e);
            return originalPrompt; // 出错时返回原始 prompt
        }
    }

    @Override
    public String getName() {
        return "ReReadingAdvisor";
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
