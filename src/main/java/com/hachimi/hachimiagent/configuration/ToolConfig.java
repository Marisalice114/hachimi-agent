package com.hachimi.hachimiagent.configuration;

import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.IOException;

/**
 * 自定义工具执行异常处理配置
 * 处理网络错误和安全异常
 */
@Configuration
public class ToolConfig {

    @Bean
    @Primary // 确保这是默认的异常处理器
    public ToolExecutionExceptionProcessor customExceptionProcessor() {
        return exception -> {
            if (exception.getCause() instanceof IOException) {
                // 网络错误返回友好消息给模型
                return "Unable to access external resource. Please try a different approach.";
            } else if (exception.getCause() instanceof SecurityException) {
                // 安全异常直接抛出
                throw exception;
            }
            // 其他异常返回详细信息
            return "Error executing tool: " + exception.getMessage();
        };
    }
}
