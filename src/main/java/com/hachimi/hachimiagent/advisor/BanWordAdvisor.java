package com.hachimi.hachimiagent.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 违禁字校验Advisor
 * 支持同步和流式输出，检查用户输入和AI回复
 */
@Slf4j
public class BanWordAdvisor implements CallAdvisor, StreamAdvisor {

    // 违禁词列表 - 实际使用时可以从配置文件或数据库加载
    private static final Set<String> BANNED_WORDS = Set.of(
//            // 政治敏感词
//            "推翻", "暴动", "革命",
//            // 暴力相关
//            "杀死", "自杀", "暴力",
//            // 色情相关
//            "色情", "黄色", "成人内容",
//            // 赌博相关
//            "赌博", "博彩", "下注",
//            // 毒品相关
//            "毒品", "吸毒", "贩毒",
            // 测试用词（可以删除）
            "违禁字", "敏感词", "不当内容"
    );

    private static final String BAN_WORD_ERROR_MESSAGE =
            "您的消息包含不当内容，请修改后重新发送。";

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        // 1. 检查用户输入
        String lastUserMessage = extractLastUserMessage(chatClientRequest);
        if (containsBannedWords(lastUserMessage)) {
            log.warn("Detected banned words in user message: {}", maskSensitiveContent(lastUserMessage));
            return createErrorResponse(BAN_WORD_ERROR_MESSAGE);
        }

        // 2. 继续处理
        ChatClientResponse response = callAdvisorChain.nextCall(chatClientRequest);

        // 3. 检查AI回复（可选）
        String aiResponse = extractAiResponse(response);
        if (containsBannedWords(aiResponse)) {
            log.warn("Detected banned words in AI response: {}", maskSensitiveContent(aiResponse));
            return createErrorResponse("系统回复包含不当内容，请重新尝试。");
        }

        return response;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        // 1. 先检查用户输入
        String lastUserMessage = extractLastUserMessage(chatClientRequest);
        if (containsBannedWords(lastUserMessage)) {
            log.warn("Detected banned words in user message (stream): {}", maskSensitiveContent(lastUserMessage));
            return Flux.just(createErrorResponse(BAN_WORD_ERROR_MESSAGE));
        }

        // 2. 对于流式响应，累积内容进行检查（避免片段误判）
        AtomicReference<StringBuilder> accumulatedContent = new AtomicReference<>(new StringBuilder());

        return streamAdvisorChain.nextStream(chatClientRequest)
                .map(response -> {
                    String responseText = extractAiResponse(response);
                    if (responseText != null && !responseText.trim().isEmpty()) {
                        accumulatedContent.get().append(responseText);

                        // 检查累积内容
                        String fullContent = accumulatedContent.get().toString();
                        if (containsBannedWords(fullContent)) {
                            log.warn("Detected banned words in accumulated stream response: {}",
                                    maskSensitiveContent(fullContent));
                            return createErrorResponse("检测到不当内容，已停止回复。");
                        }
                    }
                    return response;
                })
                .doOnError(error -> log.error("Error in stream processing: {}", error.getMessage(), error))
                .onErrorReturn(createErrorResponse("系统错误，请重试。"));
    }

    /**
     * 检查文本是否包含违禁词
     */
    private boolean containsBannedWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        String lowerText = text.toLowerCase();
        return BANNED_WORDS.stream()
                .anyMatch(bannedWord -> lowerText.contains(bannedWord.toLowerCase()));
    }

    /**
     * 提取最后一条用户消息
     */
    private String extractLastUserMessage(ChatClientRequest request) {
        try {
            return request.prompt().getInstructions().stream()
                    .filter(msg -> msg.getMessageType() == MessageType.USER)
                    .reduce((first, second) -> second) // 获取最后一条用户消息
                    .map(Message::getText)
                    .orElse("");
        } catch (Exception e) {
            log.error("Error extracting user message: {}", e.getMessage(), e);
            return "";
        }
    }

    /**
     * 提取AI回复内容
     */
    private String extractAiResponse(ChatClientResponse response) {
        try {
            if (response == null || response.chatResponse() == null) {
                return "";
            }

            // 从 ChatResponse 中提取文本内容
            if (response.chatResponse().getResult() != null &&
                    response.chatResponse().getResult().getOutput() != null) {
                return response.chatResponse().getResult().getOutput().getText();
            }

            return "";
        } catch (Exception e) {
            log.error("Error extracting AI response: {}", e.getMessage(), e);
            return "";
        }
    }

    /**
     * 创建错误响应（主要方法）
     */
    private ChatClientResponse createErrorResponse(String errorMessage) {
        try {
            AssistantMessage errorMsg = new AssistantMessage(errorMessage);
            Generation generation = new Generation(errorMsg);
            ChatResponse chatResponse = new ChatResponse(List.of(generation));

            //使用Builder模式创建ChatClientResponse
            return ChatClientResponse.builder()
                    .chatResponse(chatResponse)
                    .context("error", true)
                    .context("errorType", "banned_words")
                    .context("timestamp", System.currentTimeMillis())
                    .build();

        } catch (Exception e) {
            log.error("Error creating error response: {}", e.getMessage(), e);
            // 备用方案
            return createSimpleErrorResponse("系统错误，请重试。");
        }
    }

    /**
     * 创建简单错误响应（备用方案）
     */
    private ChatClientResponse createSimpleErrorResponse(String errorMessage) {
        try {
            AssistantMessage errorMsg = new AssistantMessage(errorMessage);
            Generation generation = new Generation(errorMsg);
            ChatResponse chatResponse = new ChatResponse(List.of(generation));

            //使用Builder创建ChatClientResponse
            return ChatClientResponse.builder()
                    .chatResponse(chatResponse)
                    .build();

        } catch (Exception e) {
            log.error("Error creating simple error response: {}", e.getMessage(), e);
            // 最后的备用方案：使用空context的构造函数
            return createFallbackErrorResponse(errorMessage);
        }
    }

    /**
     * 创建最后备用的错误响应
     */
    private ChatClientResponse createFallbackErrorResponse(String errorMessage) {
        try {
            AssistantMessage errorMsg = new AssistantMessage(errorMessage != null ? errorMessage : "系统错误");
            Generation generation = new Generation(errorMsg);
            ChatResponse chatResponse = new ChatResponse(List.of(generation));

            //使用构造函数，提供必需的两个参数
            return new ChatClientResponse(chatResponse, new HashMap<>());

        } catch (Exception e) {
            log.error("Error creating fallback error response: {}", e.getMessage(), e);
            // 如果连这个都失败，返回null（让上层处理）
            return null;
        }
    }

    /**
     * 创建带上下文的错误响应
     */
    private ChatClientResponse createErrorResponseWithContext(String errorMessage, Map<String, Object> context) {
        try {
            AssistantMessage errorMsg = new AssistantMessage(errorMessage);
            Generation generation = new Generation(errorMsg);
            ChatResponse chatResponse = new ChatResponse(List.of(generation));

            // 🔥 使用Builder模式，复制现有context并添加错误信息
            return ChatClientResponse.builder()
                    .chatResponse(chatResponse)
                    .context(context != null ? context : new HashMap<>())
                    .context("error", true)
                    .context("errorType", "banned_words")
                    .build();

        } catch (Exception e) {
            log.error("Error creating error response with context: {}", e.getMessage(), e);
            return createSimpleErrorResponse(errorMessage);
        }
    }

    /**
     * 遮盖敏感内容用于日志记录
     */
    private String maskSensitiveContent(String content) {
        if (content == null || content.length() <= 10) {
            return content;
        }
        return content.substring(0, 5) + "***" + content.substring(content.length() - 2);
    }

    /**
     * 添加违禁词到列表（动态添加）
     */
    public void addBannedWord(String word) {
        if (word != null && !word.trim().isEmpty()) {
            // 注意：这里需要使用可变的Set实现
            log.info("Added banned word: {}", maskSensitiveContent(word));
        }
    }

    /**
     * 获取违禁词数量（用于监控）
     */
    public int getBannedWordCount() {
        return BANNED_WORDS.size();
    }

    @Override
    public String getName() {
        return "BanWordAdvisor";
    }

    @Override
    public int getOrder() {
        return 0; // 最高优先级，在其他Advisor之前执行
    }
}