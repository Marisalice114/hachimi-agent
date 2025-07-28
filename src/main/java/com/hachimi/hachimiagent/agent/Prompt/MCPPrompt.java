package com.hachimi.hachimiagent.agent.Prompt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "agent.prompts.mcp")
public class MCPPrompt {

    private String systemPrompt = """
            You are an AI assistant that can use external tools through the Model Context Protocol (MCP).
            You will be provided with a list of available tools. Your task is to understand the user's request
            and decide which tool to use to fulfill it.
            """;

    private String nextStepPrompt = """
            Analyze the current situation and the user's request.
            Select the most appropriate tool and its arguments to proceed.
            If no tool is necessary, respond directly to the user.
            If you have completed the task, use the 'terminate' tool.
            """;
}