package com.hachimi.hachimiagent.agent;

import cn.hutool.core.util.StrUtil;
import com.hachimi.hachimiagent.agent.model.AgentState;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;

import java.util.ArrayList;
import java.util.List;


@Data
@Slf4j
public abstract class BaseAgent {

    private String name;
    private String description;
    private String systemPrompt;
    private String userPrompt;
    private String stuckPrompt;
    private String nextStepPrompt;
    //llm
    private ChatClient chatClient;
    //对话记忆
    private List<Message> messagesList = new ArrayList<>();
    //步数
    private int currentStep = 0;
    private int maxSteps = 10;
    //状态
    private AgentState state = AgentState.IDLE;
    //重复值
    private int duplicateThreshold = 2;

    public String run(String userPrompt) {
        //传入输入
        this.nextStepPrompt = userPrompt;
        //判断当前状态
        if (state != AgentState.IDLE) {
            throw new IllegalStateException("Agent is not in IDLE state.");
        }
        //传入内容为空
        if (StrUtil.isBlank(userPrompt)) {
            throw new IllegalArgumentException("User prompt cannot be empty.");
        }
        //更改状态
        this.state = AgentState.RUNNING;
        //保存结果
        List<String> results = new ArrayList<>();


        try {
            while (currentStep < maxSteps && state != AgentState.FINISHED) {
                currentStep++;
                //记录执行步数
                log.info("Executing step: {}/{}", currentStep, maxSteps);
                //运行一步
                String stepResult = step();
                String result = "Step " + currentStep + ": " + stepResult;
                //判断是否循环
                if (is_stuck()) {
                    handle_stuck_step();
                    result = "Step " + currentStep + ": Stuck detected, handling stuck step.";
                    log.warn("Stuck detected at step {}", currentStep);
                    results.add(result);
                    break;
                }
                results.add(result);
            }
            if (currentStep >= maxSteps) {
                state = AgentState.FINISHED;
                log.warn("Reached maximum steps without finishing.");
                results.add("Reached maximum steps without finishing.");
            }

            return String.join("\n", results);
        } catch (Exception e) {
            state = AgentState.ERROR;
            log.error("An error occurred during agent execution: {}", e.getMessage(), e);
            return "error: " + e.getMessage();
        } finally {
            //清理资源
            this.cleanup();
            //重置状态
            state = AgentState.IDLE;
            currentStep = 0;
            log.info("Agent run completed. State reset to IDLE.");
        }
    }


    public abstract String step();


    /**
     * 判断循环
     */
    protected boolean is_stuck() {
        if (messagesList.size() < 2) {
            return false;
        }

        Message lastMessage = messagesList.get(messagesList.size() - 1);
        if (lastMessage.getText() == null || lastMessage.getText().trim().isEmpty()) {
            return false;
        }

        // 计算相同内容的出现次数
        long duplicateCount = messagesList.stream()
                .limit(messagesList.size() - 1) // 排除最后一条消息
                .filter(msg -> isAssistantMessage(msg)) // 只检查助手消息
                .filter(msg -> msg.getText() != null)
                .mapToLong(msg -> msg.getText().equals(lastMessage.getText()) ? 1 : 0)
                .sum();

        return duplicateCount >= duplicateThreshold;
    }


    private boolean isAssistantMessage(Message message) {
        // 需要根据你的Message类的实际结构来实现
        // 可能是：
        // return "assistant".equals(message.getRole());
        // 或者：
        // return message instanceof AssistantMessage;

        // 临时实现，你需要根据实际情况调整
        return true; // 或者具体的判断逻辑
    }
    /**
     * 防止循环
     */
    public void handle_stuck_step() {
        nextStepPrompt += stuckPrompt;
        log.info("Handling stuck step with prompt: {}", stuckPrompt);

    }

    /**
     * 清理
     */
    protected void cleanup() {

    }

}

