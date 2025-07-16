package com.hachimi.hachimiagent.demo.invoke;

/**
 * 测试用
 */
public interface TestAPIKey{
    static String getApiKey() {
        String apiKey = System.getenv("ALI-API-KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            // 这里的错误信息应该匹配实际的环境变量名
            throw new RuntimeException("环境变量 DASHSCOPE_API_KEY 未设置");
        }
        return apiKey;
    }
}
