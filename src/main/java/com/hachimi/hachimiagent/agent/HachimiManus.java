package com.hachimi.hachimiagent.agent;

import com.hachimi.hachimiagent.advisor.SelfLogAdvisor;
import com.hachimi.hachimiagent.agent.Prompt.ManusPrompt;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;


//springboot启动的时候自动注入chatclient 所以这里也让其作为component
//此处的manus是一个单例模式
@Component
public class HachimiManus extends ToolCallAgent {

    public HachimiManus(ToolCallback[] allTools, ChatModel dashscopeChatModel, ManusPrompt manusPrompt) {
        super(allTools);
        // 设置基本信息
        this.setName("HachimiManus");
        this.setDescription("OpenManus AI Assistant capable of handling complex tasks");

        // 使用 ManusPrompt 配置提示词
        // 项目根目录
        String workingDirectory = System.getProperty("user.dir");
        this.setSystemPrompt(manusPrompt.buildSystemPrompt(workingDirectory));
        this.setNextStepPrompt(manusPrompt.getNextStepPrompt());
        this.setMaxSteps(10);
        // 初始化客户端
        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(new SelfLogAdvisor())
                .build();
        this.setChatClient(chatClient);

    }
}
