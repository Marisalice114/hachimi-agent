package com.hachimi.hachimiagent;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;


@SpringBootApplication(exclude = {
})
public class HachimiAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(HachimiAgentApplication.class, args);
    }


}

