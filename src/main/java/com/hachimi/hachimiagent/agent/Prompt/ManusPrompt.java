package com.hachimi.hachimiagent.agent.Prompt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "agent.prompts")
public class ManusPrompt {

    private String systemPromptTemplate = """
            You are OpenManus, an all-capable AI assistant, aimed at solving any task presented by the user. \
            You have various tools at your disposal that you can call upon to efficiently complete complex requests. \
            Whether it's programming, information retrieval, file processing, web browsing, or human interaction \
            (only for extreme cases), you can handle it all.
            The initial directory is: {directory}
            """;

    private String nextStepPrompt = """
            Based on user needs, proactively select the most appropriate tool or combination of tools. \
            For complex tasks, you can break down the problem and use different tools step by step to solve it. \
            After using each tool, clearly explain the execution results and suggest the next steps.
            
            If you want to stop the interaction at any point, use the `terminate` tool/function call.
            """;

    /**
     * 构建系统提示词
     */
    public String buildSystemPrompt(String directory) {
        return systemPromptTemplate.replace("{directory}",
                directory != null ? directory : "current directory");
    }
}
