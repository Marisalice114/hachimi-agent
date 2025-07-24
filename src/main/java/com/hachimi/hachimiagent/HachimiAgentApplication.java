package com.hachimi.hachimiagent;

import org.springframework.ai.autoconfigure.mcp.server.MpcServerAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication(exclude = {
        MpcServerAutoConfiguration.class
})
public class HachimiAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(HachimiAgentApplication.class, args);
    }

}

