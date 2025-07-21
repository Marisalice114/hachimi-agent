package com.hachimi.hachimiagent.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import reactor.core.publisher.Flux;

@Slf4j
public class selflogoldapi implements CallAdvisor, StreamAdvisor {

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return 100;
    }

    // =========================
    // BEFORE 处理方法
    // =========================

    /**
     * 调用前处理 - 基于实际的ChatClientRequest结构
     */
    private ChatClientRequest before(ChatClientRequest request) {
        String userText = extractUserText(request);
        log.info("AI Request: {}", userText);

        // 可以在这里添加更多的前置处理逻辑
        analyzeRequest(request);

        return request; // 返回原始请求
    }

    // =========================
    // AFTER 处理方法
    // =========================

    /**
     * 调用后观察
     */
    private void observeAfter(ChatClientResponse response) {
        String responseText = extractResponseText(response);
        log.info("AI Response: {}", responseText);

        // 可以在这里添加更多的后置处理逻辑
        analyzeResponse(response);
    }

    // =========================
    // 主要接口实现
    // =========================

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        // BEFORE - 调用前处理
        chatClientRequest = this.before(chatClientRequest);

        // ACTUAL CALL - 实际AI调用
        ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(chatClientRequest);

        // AFTER - 调用后处理
        this.observeAfter(chatClientResponse);

        return chatClientResponse;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        // BEFORE - 流式调用前处理
        chatClientRequest = this.before(chatClientRequest);

        // ACTUAL STREAM CALL - 实际流式AI调用
        Flux<ChatClientResponse> responseFlux = streamAdvisorChain.nextStream(chatClientRequest);

        // AFTER - 流式调用后处理
        return aggregateStreamResponse(responseFlux);
    }

    // =========================
    // 辅助方法 - 基于实际ChatClientRequest结构
    // =========================

    /**
     * 从ChatClientRequest中提取用户文本
     * 基于实际的record结构：ChatClientRequest(Prompt prompt, Map<String, Object> context)
     */
    private String extractUserText(ChatClientRequest request) {
        try {
            // 方法1: 从Prompt中获取消息
            if (request.prompt() != null && request.prompt().getInstructions() != null) {
                var instructions = request.prompt().getInstructions();

                // 查找最后一个UserMessage
                for (int i = instructions.size() - 1; i >= 0; i--) {
                    Message message = instructions.get(i);
                    if (message instanceof UserMessage userMessage) {
                        return userMessage.getText();
                    }
                }

                // 如果没有找到UserMessage，返回最后一条消息
                if (!instructions.isEmpty()) {
                    Message lastMessage = instructions.get(instructions.size() - 1);
                    if (lastMessage.getText() instanceof String content) {
                        return content;
                    }
                    return lastMessage.toString();
                }
            }

            // 方法2: 从toString中提取（备用方案）
            String fullRequest = request.toString();
            int userIndex = fullRequest.lastIndexOf("UserMessage");
            if (userIndex != -1) {
                // 尝试提取UserMessage的内容
                String afterUser = fullRequest.substring(userIndex);
                int contentStart = afterUser.indexOf("content=");
                if (contentStart != -1) {
                    contentStart += "content=".length();
                    int contentEnd = afterUser.indexOf(",", contentStart);
                    if (contentEnd == -1) {
                        contentEnd = afterUser.indexOf(")", contentStart);
                    }
                    if (contentEnd != -1) {
                        return afterUser.substring(contentStart, contentEnd).trim();
                    }
                }
            }

            return "Unable to extract user text";
        } catch (Exception e) {
            log.debug("Error extracting user text: {}", e.getMessage());
            return "Error extracting user text: " + e.getMessage();
        }
    }

    /**
     * 从ChatClientResponse中提取响应文本
     */
    private String extractResponseText(ChatClientResponse response) {
        try {
            // 方法1: 直接访问标准路径
            if (response.chatResponse() != null &&
                    response.chatResponse().getResult() != null &&
                    response.chatResponse().getResult().getOutput() != null) {

                return response.chatResponse().getResult().getOutput().getText();
            }

            // 方法2: 从toString中提取（备用方案）
            String responseStr = response.toString();
            String pattern = "textContent=";
            int textContentIndex = responseStr.indexOf(pattern);
            if (textContentIndex != -1) {
                int startIndex = textContentIndex + pattern.length();
                int endIndex = responseStr.indexOf(", metadata=", startIndex);
                if (endIndex == -1) {
                    endIndex = responseStr.indexOf(")", startIndex);
                }
                if (endIndex != -1) {
                    return responseStr.substring(startIndex, endIndex);
                }
            }

            return "Unable to extract response text";
        } catch (Exception e) {
            log.debug("Error extracting response text: {}", e.getMessage());
            return "Error extracting response text: " + e.getMessage();
        }
    }

    /**
     * 分析请求内容（增强的调试功能）
     */
    private void analyzeRequest(ChatClientRequest request) {
        try {
            // 分析Prompt结构
            if (request.prompt() != null) {
                var instructions = request.prompt().getInstructions();
                if (instructions != null) {
                    log.info("📊 [BEFORE] 消息总数: {}", instructions.size());

                    // 统计消息类型
                    long systemMessages = instructions.stream()
                            .filter(msg -> msg.getClass().getSimpleName().contains("System"))
                            .count();
                    long userMessages = instructions.stream()
                            .filter(msg -> msg instanceof UserMessage)
                            .count();
                    long otherMessages = instructions.size() - systemMessages - userMessages;

                    log.info("📋 [BEFORE] 系统消息: {}, 用户消息: {}, 其他消息: {}",
                            systemMessages, userMessages, otherMessages);

                    // 检查是否包含RAG相关内容
                    String promptStr = request.prompt().toString();
                    if (promptStr.contains("Document{") ||
                            promptStr.contains("QuestionAnswerAdvisor") ||
                            promptStr.contains("context") ||
                            promptStr.contains("检索") ||
                            promptStr.contains("文档")) {
                        log.info("🔍 [BEFORE] 检测到RAG相关内容");

                        // 尝试统计文档数量
                        int docCount = countOccurrences(promptStr, "Document{");
                        if (docCount > 0) {
                            log.info("📚 [BEFORE] 检测到 {} 个文档", docCount);
                        }
                    }
                }
            }

            // 分析上下文
            if (request.context() != null && !request.context().isEmpty()) {
                log.info("🗃️ [BEFORE] 上下文数据: {} 项", request.context().size());
                request.context().keySet().forEach(key ->
                        log.debug("🔑 [BEFORE] 上下文键: {}", key));
            }

        } catch (Exception e) {
            log.debug("[BEFORE] 分析请求时出错: {}", e.getMessage());
        }
    }

    /**
     * 分析响应内容
     */
    private void analyzeResponse(ChatClientResponse response) {
        try {
            String responseText = extractResponseText(response);
            if (responseText != null) {
                log.info("📏 [AFTER] 响应长度: {} 字符", responseText.length());

                if (responseText.length() < 10) {
                    log.warn("⚠️ [AFTER] 响应内容较短");
                } else if (responseText.length() > 1000) {
                    log.info("📖 [AFTER] 响应内容详细");
                }

                // 检查响应质量指标
                if (responseText.contains("无法") || responseText.contains("不知道") ||
                        responseText.contains("抱歉")) {
                    log.info("🤔 [AFTER] 响应包含不确定性表达");
                }
            }
        } catch (Exception e) {
            log.debug("[AFTER] 分析响应时出错: {}", e.getMessage());
        }
    }

    /**
     * 聚合流式响应
     */
    private Flux<ChatClientResponse> aggregateStreamResponse(Flux<ChatClientResponse> responseFlux) {
        return responseFlux
                .doOnNext(response -> {
                    // 处理每个流式响应片段
                    String content = extractResponseText(response);
                    if (content != null && !content.trim().isEmpty()) {
                        log.info("🌊 [STREAM] 响应片段: {}",
                                content.length() > 50 ? content.substring(0, 50) + "..." : content);
                    }
                })
                .doOnComplete(() -> {
                    log.info("✅ [STREAM] 流式调用完成");
                })
                .doOnError(error -> {
                    log.error("❌ [STREAM] 流式调用出错: {}", error.getMessage());
                });
    }

    /**
     * 计算字符串出现次数
     */
    private int countOccurrences(String text, String pattern) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(pattern, index)) != -1) {
            count++;
            index += pattern.length();
        }
        return count;
    }
}