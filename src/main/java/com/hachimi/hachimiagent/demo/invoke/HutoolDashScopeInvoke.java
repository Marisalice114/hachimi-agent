package com.hachimi.hachimiagent.demo.invoke;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

/**
 * 使用Hutool调用阿里云DashScope兼容OpenAI接口,然后通过http调用
 */
public class HutoolDashScopeInvoke {

    // API端点
    private static final String API_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";

    /**
     * 调用DashScope Chat Completions API
     */
    public static String callChatCompletions(String question) {
        // 获取API Key
        String apiKey = System.getenv("ALI-API-KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("环境变量 ALI-API-KEY 未设置");
        }

        // 构建请求体
        JSONObject requestBody = buildRequestBody(question);

        // 发送HTTP POST请求
        HttpResponse response = HttpRequest.post(API_URL)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(requestBody.toString())
                .execute();

        // 检查响应状态
        if (!response.isOk()) {
            throw new RuntimeException("API调用失败，状态码: " + response.getStatus() + ", 响应: " + response.body());
        }

        return response.body();
    }

    /**
     * 构建请求体JSON
     */
    private static JSONObject buildRequestBody(String userMessage) {
        JSONObject requestBody = new JSONObject();

        // 设置模型
        requestBody.set("model", "qwen-plus");

        // 构建消息数组
        JSONArray messages = new JSONArray();

        // 系统消息
        JSONObject systemMessage = new JSONObject();
        systemMessage.set("role", "system");
        systemMessage.set("content", "You are a helpful assistant.");
        messages.add(systemMessage);

        // 用户消息
        JSONObject userMsg = new JSONObject();
        userMsg.set("role", "user");
        userMsg.set("content", userMessage);
        messages.add(userMsg);

        requestBody.set("messages", messages);

        return requestBody;
    }

    /**
     * 解析响应并提取AI回答
     */
    public static String extractAnswer(String responseBody) {
        try {
            JSONObject response = JSONUtil.parseObj(responseBody);
            JSONArray choices = response.getJSONArray("choices");
            if (choices != null && choices.size() > 0) {
                JSONObject firstChoice = choices.getJSONObject(0);
                JSONObject message = firstChoice.getJSONObject("message");
                return message.getStr("content");
            }
            return "未找到回答内容";
        } catch (Exception e) {
            return "解析响应失败: " + e.getMessage();
        }
    }

    /**
     * 完整的调用示例
     */
    public static String chatWithAI(String question) {
        try {
            // 调用API
            String responseBody = callChatCompletions(question);

            // 打印完整响应（调试用）
            System.out.println("完整API响应:");
            System.out.println(JSONUtil.formatJsonStr(responseBody));
            System.out.println("=".repeat(50));

            // 提取AI回答
            return extractAnswer(responseBody);

        } catch (Exception e) {
            return "调用失败: " + e.getMessage();
        }
    }

    /**
     * 主方法测试
     */
    public static void main(String[] args) {
        try {
            // 测试问题
            String question = "你是谁？";

            System.out.println("问题: " + question);
            System.out.println("回答: " + chatWithAI(question));

        } catch (Exception e) {
            System.err.println("执行出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
