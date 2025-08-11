package com.hachimi.hachimiagent.agent;

import cn.hutool.core.util.StrUtil;
import com.hachimi.hachimiagent.agent.model.AgentState;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;


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

    /**
     * 同步输出
     * @param userPrompt
     * @return
     */
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


    /**
     * 流式输出 使用sseemiter来进行处理
     * @param userPrompt
     * @return
     */
    public SseEmitter runStream(String userPrompt) {
        //定义SseEmitter
        SseEmitter emitter = new SseEmitter(180000L); // 3分钟超时
        //因为这里要进行现成异步处理
        //如果不使用异步，那么emitter就需要一直等待后才能返回出去
        CompletableFuture.runAsync(()->{
            //传入输入
            this.nextStepPrompt = userPrompt;
            //判断当前状态
            try {
                if (state != AgentState.IDLE) {
                    emitter.completeWithError( new IllegalStateException("Agent is not in IDLE state."));
                    return ;
                }
                //传入内容为空
                if (StrUtil.isBlank(userPrompt)) {
                    emitter.completeWithError(new IllegalArgumentException("User prompt cannot be empty."));
                    return ;
                }
            } catch (Exception e) {
                emitter.completeWithError(e);
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
                    //每完成一步都需要输出当前结果到sse
                    emitter.send(result);
                }
                if (currentStep >= maxSteps) {
                    state = AgentState.FINISHED;
                    results.add("Reached maximum steps without finishing.");
                    emitter.send("Reached maximum steps without finishing.");
                }
                //最后一定要完成一次响应
                emitter.complete();;
            } catch (Exception e) {
                state = AgentState.ERROR;
                log.error("An error occurred during agent execution: {}", e.getMessage(), e);
                //每一次发送都有可能出错
                try {
                    emitter.send("error:"+e.getMessage());
                    emitter.complete();
                } catch (IOException ex) {
                    emitter.completeWithError(ex);
                }
            } finally {
                //清理资源
                this.cleanup();
                //重置状态
                state = AgentState.IDLE;
                currentStep = 0;
                log.info("Agent run completed. State reset to IDLE.");
            }
        });

        // 设置超时回调,注意要清理资源，而且判断完成状态是需要从运行状态转变为完成状态来判断
        emitter.onTimeout(() -> {
            state = AgentState.ERROR;
            this.cleanup();
            log.warn("SseEmitter timed out after 3 minutes.");
            emitter.complete();
        });
        emitter.onCompletion(() -> {
            if(state == AgentState.RUNNING) {
                state = AgentState.FINISHED;
            }
            this.cleanup();
            log.warn("SseEmitter timed out after 3 minutes.");
            emitter.complete();
        });
        return emitter;
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
    if (message == null) {
      return false;
    }

    // 方法1: 使用Spring AI的MessageType枚举判断（首选）
    try {
      var messageType = message.getMessageType();
      if (messageType != null) {
        return "ASSISTANT".equals(messageType.name());
      }
    } catch (Exception e) {
      // 兼容性处理，继续尝试其他方法
      log.debug("Failed to get message type via getMessageType(): {}", e.getMessage());
    }

    // 方法2: 使用instanceof判断（备用方案）
    try {
      // 检查是否为AssistantMessage类型
      String className = message.getClass().getSimpleName();
      if ("AssistantMessage".equals(className)) {
        return true;
      }

      // 也可以使用反射检查包名（更精确）
      String fullClassName = message.getClass().getName();
      if (fullClassName.contains("AssistantMessage")) {
        return true;
      }
    } catch (Exception e) {
      log.debug("Failed to check message type via class name: {}", e.getMessage());
    }

    // 默认返回false（只有确认是助手消息才返回true）
    return false;
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

