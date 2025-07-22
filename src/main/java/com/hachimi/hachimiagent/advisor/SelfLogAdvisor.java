package com.hachimi.hachimiagent.advisor;


import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import reactor.core.publisher.Flux;


@Slf4j
public class SelfLogAdvisor implements CallAdvisor, StreamAdvisor {


    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        log.info("🔍 [SelfLogAdvisor] 开始处理请求");
        log.info("🔍 [SelfLogAdvisor] 当前Instructions数量: {}", chatClientRequest.prompt().getInstructions().size());

        // 显示当前所有instruction
        for (int i = 0; i < chatClientRequest.prompt().getInstructions().size(); i++) {
            Message msg = chatClientRequest.prompt().getInstructions().get(i);
            log.info("   [{}] {}: {} 字符", i, msg.getClass().getSimpleName(), msg.getText().length());
        }

        log.info("User: {}", extractLastUserMessage(chatClientRequest));

        // ✅ 调用下一个advisor前后对比
        log.info("🔄 [SelfLogAdvisor] 调用下一个advisor...");
        ChatClientResponse response = callAdvisorChain.nextCall(chatClientRequest);
        log.info("🔄 [SelfLogAdvisor] advisor链调用完成");

        log.info("AI: {}", extractAIContent(response));
        return response;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        log.info("User: {}", extractLastUserMessage(chatClientRequest));

        return streamAdvisorChain.nextStream(chatClientRequest)
                .doOnNext(response -> {
                    String content = extractAIContent(response);
                    if (content != null && !content.trim().isEmpty()) {
                        log.info("Stream: {}", content);
                    }
                });
    }

    private String extractLastUserMessage(ChatClientRequest request) {
        try {
            // 简单的字符串提取，寻找最后一个UserMessage
            String fullRequest = request.toString();
            int lastUserIndex = fullRequest.lastIndexOf("UserMessage{content='");
            if (lastUserIndex != -1) {
                int startIndex = lastUserIndex + "UserMessage{content='".length();
                int endIndex = fullRequest.indexOf("'", startIndex);
                if (endIndex != -1) {
                    return fullRequest.substring(startIndex, endIndex);
                }
            }
            return "Unable to extract user message";
        } catch (Exception e) {
            return "Error extracting user message";
        }
    }

    private String extractAIContent(ChatClientResponse response) {
        try {
            if (response.chatResponse() != null) {
                //从字符串中精确提取 textContent
                String responseStr = response.chatResponse().toString();
                log.debug("Full response string: {}", responseStr);

                // 查找 textContent=
                String pattern = "textContent=";
                int textContentIndex = responseStr.indexOf(pattern);
                if (textContentIndex != -1) {
                    int startIndex = textContentIndex + pattern.length();
                    int endIndex = responseStr.indexOf(", metadata=", startIndex);
                    if (endIndex != -1) {
                        return responseStr.substring(startIndex, endIndex);
                    }
                }

                return "Unable to extract textContent from response";
            }
            return null;
        } catch (Exception e) {
            log.warn("Error extracting AI content: {}", e.getMessage());
            return "Error extracting AI content: " + e.getMessage();
        }
    }


    @Override
    public String getName() {
        return "SelfLogAdvisor";
    }

    @Override
    public int getOrder() {
        return 1000;
    }
}