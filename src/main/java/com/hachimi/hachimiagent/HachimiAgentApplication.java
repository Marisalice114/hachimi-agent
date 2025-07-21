package com.hachimi.hachimiagent;

import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication(exclude = PgVectorStoreAutoConfiguration.class)
public class HachimiAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(HachimiAgentApplication.class, args);
    }

}

