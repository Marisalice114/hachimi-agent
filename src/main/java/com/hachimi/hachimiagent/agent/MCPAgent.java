package com.hachimi.hachimiagent.agent;

import com.hachimi.hachimiagent.agent.Prompt.MCPPrompt;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
@Slf4j
@Component
public class MCPAgent extends ToolCallAgent {

    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    protected ToolCallbackProvider getToolCallbackProvider() {
        return this.toolCallbackProvider;
    }

    private int toolRefreshCounter = 0;
    private final int refreshToolsInterval = 5;
    // 用于跟踪当前已知的工具名称
    private Set<String> knownToolNames = new HashSet<>();

    @Autowired
    public MCPAgent(MCPPrompt mcpPrompt) {
        super(new ToolCallback[0]);
        this.setSystemPrompt(mcpPrompt.getSystemPrompt());
        this.setNextStepPrompt(mcpPrompt.getNextStepPrompt());
        this.setMaxSteps(10);
    }

    @Override
    public boolean think() {
        refreshToolsIfNeeded();

        if (getNextStepPrompt() != null && !getNextStepPrompt().isEmpty()) {
            getMessagesList().add(new UserMessage(getNextStepPrompt()));
            setNextStepPrompt(null);
        }

        List<Message> messageList = getMessagesList();

        try {
            List<ToolCallback> mcpTools = Arrays.asList(toolCallbackProvider.getToolCallbacks());
            if (mcpTools.isEmpty()) {
                log.warn("No MCP tools available from provider. Agent might not function as expected.");
            }

            ChatResponse chatResponse = getChatClient().prompt()
                    .messages(messageList)
                    .toolCallbacks(mcpTools)
                    .call()
                    .chatResponse();

            setToolCallChatResponse(chatResponse);
            AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
            log.info("{} - Think result: {}", getName(), assistantMessage.getText());

            if (!chatResponse.hasToolCalls()) {
                messageList.add(assistantMessage);
                return false;
            } else {
                log.info("{} - Found {} tool calls.", getName(), assistantMessage.getToolCalls().size());
                return true;
            }
        } catch (Exception e) {
            log.error("{} - Think failed: {}", getName(), e.getMessage(), e);
            getMessagesList().add(new AssistantMessage("Error during tool call planning: " + e.getMessage()));
            return false;
        }
    }

    /**
     * 检查并刷新工具列表，与 OpenManus 逻辑对齐。
     * 检测工具的新增和移除，并将变更作为系统消息通知模型。
     */
    void refreshToolsIfNeeded() {
        toolRefreshCounter++;
        if (toolRefreshCounter % refreshToolsInterval == 0) {
            log.info("Step {}: Checking for MCP tool updates...", getCurrentStep());

            // 1. 获取当前可用的工具
            Set<String> currentToolNames = Arrays.stream(toolCallbackProvider.getToolCallbacks())
                    .map(toolCallback -> toolCallback.getToolDefinition().name())
                    .collect(Collectors.toSet());

            // 首次运行时，初始化 knownToolNames
            if (knownToolNames.isEmpty()) {
                log.info("Initializing MCP tools. Found: {}", currentToolNames);
                knownToolNames.addAll(currentToolNames);
                return;
            }

            // 2. 比较新旧工具列表，找出差异
            Set<String> addedTools = new HashSet<>(currentToolNames);
            addedTools.removeAll(knownToolNames);

            Set<String> removedTools = new HashSet<>(knownToolNames);
            removedTools.removeAll(currentToolNames);

            // 3. 如果有变更，则更新对话历史并记录日志
            if (!addedTools.isEmpty()) {
                log.info("New MCP tools detected: {}", addedTools);
                String message = "System update: New tools are now available: " + String.join(", ", addedTools);
                getMessagesList().add(new SystemMessage(message));
            }

            if (!removedTools.isEmpty()) {
                log.info("MCP tools removed: {}", removedTools);
                String message = "System update: The following tools are no longer available: " + String.join(", ", removedTools);
                getMessagesList().add(new SystemMessage(message));
            }

            if (addedTools.isEmpty() && removedTools.isEmpty()) {
                log.info("MCP tool list is up to date.");
            }

            // 4. 更新已知工具列表
            this.knownToolNames = currentToolNames;
        }
    }
}