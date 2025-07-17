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
import reactor.core.publisher.Flux;


@Slf4j
public class SelfLogAdvisor implements CallAdvisor, StreamAdvisor {


    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        // 记录用户输入
        log.info("User: {}", extractLastUserMessage(chatClientRequest));

        ChatClientResponse response = callAdvisorChain.nextCall(chatClientRequest);

        // 记录AI回复
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
        return 0;
    }
}