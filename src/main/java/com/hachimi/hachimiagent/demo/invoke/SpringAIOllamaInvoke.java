package com.hachimi.hachimiagent.demo.invoke;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;


/*
SpringAI框架
 */
//@Component
public class SpringAIOllamaInvoke implements CommandLineRunner {

    @Resource
    private ChatModel ollamaChatModel;


    @Override
    public void run(String... args) throws Exception {
        String output = ollamaChatModel.call(new Prompt("你好，我是哈基米"))
                .getResult()
                .getOutput()
                .getText();  // 注意这里是 getText()，不是 getContent()
        System.out.println("AI回复: " + output);
    }
}
