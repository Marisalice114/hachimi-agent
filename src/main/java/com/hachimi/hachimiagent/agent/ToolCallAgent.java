package com.hachimi.hachimiagent.agent;

/**
 * 处理工具调用的基础代理类，具体实现了 think 和 act 方法，可以用作创建实例的父类
 */
import cn.hutool.core.collection.CollUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.hachimi.hachimiagent.agent.model.AgentState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ToolCallAgent extends ReactAgent{

    // 新增：SSE发送器，用于发送思考过程
    private SseEmitter sseEmitter;

    //可用工具list
    private final ToolCallback[] avaliableToollist;
    //对话回复，方便提取信息
    private ChatResponse toolCallChatResponse;

    //工具调用管理
    private final ToolCallingManager toolCallingManager;
    //对话选项，配置工具调用相关选项
    private final ChatOptions chatOptions;
    //调用体
    public ToolCallAgent (ToolCallback[] avaliableToollist) {
        super();
        this.avaliableToollist = avaliableToollist;
        this.toolCallingManager = ToolCallingManager.builder().build();
        this.chatOptions = DashScopeChatOptions.builder()
                .withInternalToolExecutionEnabled(false)
                .build();
    }

    /**
     * 处理状态，判断下一步的执行
     * @return
     */
    @Override
    public boolean think() {
        // 发送思考开始信号
        sendThinkingProcess("开始分析用户问题...");

        //1.判断输入内容
        if(getNextStepPrompt()!=null && !getNextStepPrompt().isEmpty()){
            UserMessage userMessage = new UserMessage(getNextStepPrompt());
            //添加消息到会话中
            getMessagesList().add(userMessage);
            // 清空nextStepPrompt，避免重复添加
            setNextStepPrompt(null);

            sendThinkingProcess("已接收用户输入: " + userMessage.getText());
        }

        List<Message> messageList = getMessagesList();
        //对话设置需要再prompt中指定
        Prompt prompt = new Prompt(messageList,chatOptions);

        sendThinkingProcess("正在调用AI模型进行推理...");

        //2.工具调用
        try {
            //获取聊天客户端
            ChatResponse chatResponse = getChatClient().prompt(prompt)
                    .messages(messageList)  // 使用messages而不是重复的system
                    .toolCallbacks(avaliableToollist)
                    .call()
                    .chatResponse();
            //获取工具调用结果，记录为响应
            this.toolCallChatResponse = chatResponse;
            AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
            //提示输出信息
            String result = assistantMessage.getText();
            //最终选择使用的工具
            List<AssistantMessage.ToolCall> toolCallList = assistantMessage.getToolCalls();
            //输出信息和选择使用的工具的info
            log.info(getName() + " - ToolCallAgent think result: " + result);
            log.info(getName() + " - ToolCallAgent think nums of tool calls to use: " + toolCallList.size());

            // 发送AI推理结果的思考过程
            if (result != null && !result.trim().isEmpty()) {
                sendThinkingProcess("AI推理结果: " + result);
            }

            //返回工具调用信息 名称和参数
            String toolCallInfo = toolCallList.stream()
                    .map(toolCall -> "Tool: " + toolCall.name() + ", Arguments: " + toolCall.arguments())
                    .reduce((a, b) -> a + "\n" + b)
                    .orElse("No tools called");
            log.info(toolCallInfo);

            if (toolCallList.isEmpty()){
                // 不需要调用工具
                sendThinkingProcess("AI决定直接回复，无需使用工具");
                messageList.add(assistantMessage);
                return false;
            }else {
                // 需要调用工具
                sendThinkingProcess("AI决定使用 " + toolCallList.size() + " 个工具来处理问题");
                // 发送具体工具信息
                for (AssistantMessage.ToolCall toolCall : toolCallList) {
                    sendThinkingProcess("准备调用工具: " + toolCall.name() + " 参数: " + toolCall.arguments());
                }
                return true;
            }
        } catch (Exception e) {
            log.error(getName() + " - ToolCallAgent think failed: " + e.getMessage(), e);
            sendThinkingProcess("思考过程中出现错误: " + e.getMessage());
            getMessagesList().add(new AssistantMessage("Error during tool call: " + e.getMessage()));
            return false;
        }
    }

    /**
     * 执行工具调用，并返回结果
     * @return
     */

    @Override
    public String act() {
        if (!toolCallChatResponse.hasToolCalls()){
            sendThinkingProcess("没有找到工具调用，跳过执行阶段");
            return "Tool calls not found in chat response, no action taken.";
        }

        sendThinkingProcess("开始执行工具调用...");

        //调用会话记录
        Prompt prompt = new Prompt(getMessagesList(), chatOptions);
        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, toolCallChatResponse);
        //将调用情况记录到messageList中
        setMessagesList(toolExecutionResult.conversationHistory());
        //获取执行结果
        //conversationHistory是整个会话记录，只需要取最后一条
        //ToolResponseMessage继承自 Message
        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) CollUtil.getLast(toolExecutionResult.conversationHistory());

        //判断是否用了终止调用工具
        boolean isTerminateToolUsed = toolResponseMessage.getResponses().stream()
                //判断调用的工具名
                .anyMatch(response -> response.name().equals("doTerminate"));
        if(isTerminateToolUsed){
            setState(AgentState.FINISHED);
            sendThinkingProcess("检测到终止工具调用，任务即将完成");
        }

        String result = toolResponseMessage.getResponses().stream()
                .map(response -> response.responseData().toString()) // 直接使用responseData
                .collect(Collectors.joining("\n"));

        // 发送工具执行结果的思考过程
        sendThinkingProcess("工具执行完成，结果: " + (result.length() > 100 ? result.substring(0, 100) + "..." : result));

        log.info(result);
        return result;
    }


    /**
     * 发送思考过程到前端
     */
    private void sendThinkingProcess(String content) {
        if (sseEmitter != null) {
            try {
                sseEmitter.send("THINK:" + content);
            } catch (IOException e) {
                log.error("发送思考过程失败: {}", e.getMessage());
            }
        }
    }
}

