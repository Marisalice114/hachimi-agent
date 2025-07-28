package com.hachimi.hachimiagent.agent;

import com.hachimi.hachimiagent.advisor.SelfLogAdvisor;
import com.hachimi.hachimiagent.agent.Prompt.MCPPrompt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
// 继承 MCPAgent 来复用其 MCP 工具刷新逻辑
public class HachimiMCPManus extends MCPAgent {

    // 用于存储本地工具
    private final List<ToolCallback> localTools;

    /**
     * 构造函数，接收本地工具、ChatModel 和 MCP 提示。
     * @param allTools 本地工具数组，由 Spring 自动注入。
     * @param dashscopeChatModel 用于构建 ChatClient 的模型。
     * @param mcpPrompt 用于配置 Agent 的 MCP 相关提示。
     */
    @Autowired
    public HachimiMCPManus(ToolCallback[] allTools, ChatModel dashscopeChatModel, MCPPrompt mcpPrompt) {
        // 1. 调用父类 MCPAgent 的构造函数，完成 MCP 相关配置
        super(mcpPrompt);
        // 2. 存储本地工具
        this.localTools = Arrays.asList(allTools);

        // --- 重新配置 Agent 的基本信息 ---
        this.setName("HachimiMCPManus");
        this.setDescription("A powerful Manus agent that uses both local and remote MCP tools.");
        this.setMaxSteps(10); // 可以根据需要调整

        // --- 初始化 ChatClient (这部分逻辑与您之前的 HachimiManus 一致) ---
        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(new SelfLogAdvisor())
                .build();
        this.setChatClient(chatClient);

        log.info("HachimiMCPManus initialized with {} local tools.", this.localTools.size());
    }

    /**
     * 重写 think 方法，以合并本地和 MCP 工具。
     */
    @Override
    public boolean think() {
        // 1. 直接调用父类的刷新逻辑，实现代码复用
        super.refreshToolsIfNeeded();

        // 2. 处理用户输入 (与父类逻辑一致)
        if (getNextStepPrompt() != null && !getNextStepPrompt().isEmpty()) {
            getMessagesList().add(new UserMessage(getNextStepPrompt()));
            setNextStepPrompt(null);
        }

        List<Message> messageList = getMessagesList();

        try {
            // 3. 合并本地和远程 MCP 工具
            List<ToolCallback> mcpTools = Arrays.asList(getToolCallbackProvider().getToolCallbacks());
            List<ToolCallback> allAvailableTools = new ArrayList<>(this.localTools);
            allAvailableTools.addAll(mcpTools);

            log.info("Thinking with {} total tools ({} local, {} MCP).",
                    allAvailableTools.size(), localTools.size(), mcpTools.size());

            // 4. 使用合并后的完整工具列表调用模型
            ChatResponse chatResponse = getChatClient().prompt()
                    .messages(messageList)
                    .toolCallbacks(allAvailableTools) // 关键：传入合并后的工具列表
                    .call()
                    .chatResponse();

            setToolCallChatResponse(chatResponse);
            AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
            log.info("{} - Think result: {}", getName(), assistantMessage.getText());

            if (!chatResponse.hasToolCalls()) {
                messageList.add(assistantMessage);
                return false; // 无需执行 act
            } else {
                log.info("{} - Found {} tool calls.", getName(), assistantMessage.getToolCalls().size());
                return true; // 需要执行 act
            }
        } catch (Exception e) {
            log.error("{} - Think failed: {}", getName(), e.getMessage(), e);
            getMessagesList().add(new AssistantMessage("Error during tool call planning: " + e.getMessage()));
            return false;
        }
    }

    // act() 方法会从 ToolCallAgent 继承而来，无需重写，除非有特殊逻辑。
}