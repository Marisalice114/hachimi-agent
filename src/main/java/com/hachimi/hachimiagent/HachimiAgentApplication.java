package com.hachimi.hachimiagent;


import com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeEmbeddingAutoConfiguration;
import org.springframework.ai.model.ollama.autoconfigure.OllamaEmbeddingAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;



@SpringBootApplication(scanBasePackages = "com.hachimi.hachimiagent",exclude = {DashScopeEmbeddingAutoConfiguration.class, OllamaEmbeddingAutoConfiguration.class})
public class HachimiAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(HachimiAgentApplication.class, args);
    }


}

