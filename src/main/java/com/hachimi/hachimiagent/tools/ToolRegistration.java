package com.hachimi.hachimiagent.tools;


import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * 集中工具注册
 */
@Configuration
public class ToolRegistration {

    @Value("${search-api.api-key}")
    private String searchApiKey;

    @Bean
    public ToolCallback[] allTools() {
        FileOperationTool fileOperationTool = new FileOperationTool();
        WebScrapingTool webScrapingTool = new WebScrapingTool();
        ResourceDownloadTool resourceDownloadTool = new ResourceDownloadTool();
        TerminalOperationTool terminalOperationTool = new TerminalOperationTool();
        TerminateTool terminateTool = new TerminateTool();
        PDFGenerationTool pdfGenerationTool = new PDFGenerationTool();

        // 只有当API密钥可用时才创建WebSearchTool
        if (StringUtils.hasText(searchApiKey)) {
            WebSearchTool webSearchTool = new WebSearchTool(searchApiKey);
            return ToolCallbacks.from(
                    fileOperationTool,
                    webSearchTool,
                    webScrapingTool,
                    resourceDownloadTool,
                    terminalOperationTool,
                    terminateTool,
                    pdfGenerationTool
            );
        } else {
            // 如果没有API密钥，就不包含WebSearchTool
            return ToolCallbacks.from(
                    fileOperationTool,
                    webScrapingTool,
                    resourceDownloadTool,
                    terminalOperationTool,
                    terminateTool,
                    pdfGenerationTool
            );
        }
    }
}
