package com.hachimi.hachimiagent.common;

import com.hachimi.hachimiagent.entity.ChatMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StructuredOutputConverter 测试类
 * 测试结构化输出转换器的各种功能
 */
//@SpringBootTest
@DisplayName("结构化输出转换器测试")
class StructuredOutputConverterTest {

    private StructuredOutputConverter converter;

    @BeforeEach
    void setUp() {
        converter = new StructuredOutputConverter();
    }

    @Test
    @DisplayName("测试获取格式说明")
    void testGetFormat() {
        // 获取格式说明
        String format = converter.getFormat();

        // 验证格式说明不为空
        assertNotNull(format);
        assertFalse(format.trim().isEmpty());

        // 输出格式说明，便于查看
        System.out.println("=== JSON Schema 格式说明 ===");
        System.out.println(format);

        // 验证包含关键字段
        assertTrue(format.contains("content"), "格式说明应该包含 content 字段");
        assertTrue(format.contains("messageType"), "格式说明应该包含 messageType 字段");
    }

    @Test
    @DisplayName("测试获取格式化指令")
    void testGetFormatInstructions() {
        String instructions = converter.getFormatInstructions();

        assertNotNull(instructions);
        assertFalse(instructions.trim().isEmpty());

        System.out.println("=== 格式化指令 ===");
        System.out.println(instructions);

        // 验证包含指导性文本
        assertTrue(instructions.contains("JSON格式"), "指令应该提到JSON格式");
        assertTrue(instructions.contains("注意"), "指令应该包含注意事项");
    }

    @Test
    @DisplayName("测试获取示例格式")
    void testGetExampleFormat() {
        String example = converter.getExampleFormat();

        assertNotNull(example);
        assertFalse(example.trim().isEmpty());

        System.out.println("=== 示例格式 ===");
        System.out.println(example);

        // 验证是有效的JSON格式
        assertTrue(example.contains("{"), "示例应该是JSON格式");
        assertTrue(example.contains("}"), "示例应该是JSON格式");
        assertTrue(example.contains("content"), "示例应该包含content字段");
        assertTrue(example.contains("messageType"), "示例应该包含messageType字段");
    }

    @Test
    @DisplayName("测试JSON转换为ChatMessage - 完整数据")
    void testConvertToChatMessage_CompleteData() {
        // 准备测试数据
        String jsonText = """
            {
                "content": "你好，我是AI助手，很高兴为你提供恋爱建议！",
                "messageType": "ASSISTANT"
            }
            """;

        // 执行转换
        ChatMessage result = converter.convertToChatMessage(jsonText);

        // 验证结果
        assertNotNull(result, "转换结果不应该为null");
        assertEquals("你好，我是AI助手，很高兴为你提供恋爱建议！", result.getContent());
        assertEquals("ASSISTANT", result.getMessageType());

        System.out.println("=== 转换结果 ===");
        System.out.println("Content: " + result.getContent());
        System.out.println("MessageType: " + result.getMessageType());
    }

    @Test
    @DisplayName("测试JSON转换为ChatMessage - 最小数据")
    void testConvertToChatMessage_MinimalData() {
        String jsonText = """
            {
                "content": "简单回复"
            }
            """;

        ChatMessage result = converter.convertToChatMessage(jsonText);

        assertNotNull(result);
        assertEquals("简单回复", result.getContent());
        // messageType可能为null，这是正常的

        System.out.println("=== 最小数据转换结果 ===");
        System.out.println("Content: " + result.getContent());
        System.out.println("MessageType: " + result.getMessageType());
    }

    @Test
    @DisplayName("测试增强元数据功能")
    void testEnhanceWithMetadata() {
        // 创建基础ChatMessage
        ChatMessage baseMessage = new ChatMessage();
        baseMessage.setContent("测试消息内容");
        baseMessage.setMessageType("ASSISTANT");

        // 增强元数据
        String conversationId = "test-conversation-123";
        Integer messageOrder = 5;

        ChatMessage enhanced = converter.enhanceWithMetadata(baseMessage, conversationId, messageOrder);

        // 验证增强结果
        assertNotNull(enhanced);
        assertEquals("测试消息内容", enhanced.getContent());
        assertEquals("ASSISTANT", enhanced.getMessageType());
        assertEquals(conversationId, enhanced.getConversationId());
        assertEquals(messageOrder, enhanced.getMessageOrder());

        System.out.println("=== 增强后的消息 ===");
        System.out.println("Content: " + enhanced.getContent());
        System.out.println("MessageType: " + enhanced.getMessageType());
        System.out.println("ConversationId: " + enhanced.getConversationId());
        System.out.println("MessageOrder: " + enhanced.getMessageOrder());
    }

    @Test
    @DisplayName("测试增强元数据 - 默认messageType")
    void testEnhanceWithMetadata_DefaultMessageType() {
        // 创建没有messageType的消息
        ChatMessage baseMessage = new ChatMessage();
        baseMessage.setContent("没有类型的消息");

        ChatMessage enhanced = converter.enhanceWithMetadata(baseMessage, "test-conv", 1);

        // 验证默认设置为ASSISTANT
        assertEquals("ASSISTANT", enhanced.getMessageType());

        System.out.println("=== 默认类型测试 ===");
        System.out.println("MessageType: " + enhanced.getMessageType());
    }

    @Test
    @DisplayName("测试增强元数据 - null输入")
    void testEnhanceWithMetadata_NullInput() {
        ChatMessage result = converter.enhanceWithMetadata(null, "test", 1);
        assertNull(result, "null输入应该返回null");
    }

    @Test
    @DisplayName("测试转换并增强功能")
    void testConvertAndEnhance() {
        String jsonText = """
            {
                "content": "这是一条完整的AI回复消息",
                "messageType": "ASSISTANT"
            }
            """;

        String conversationId = "conversation-456";
        Integer messageOrder = 10;

        ChatMessage result = converter.convertAndEnhance(jsonText, conversationId, messageOrder);

        // 验证所有字段
        assertNotNull(result);
        assertEquals("这是一条完整的AI回复消息", result.getContent());
        assertEquals("ASSISTANT", result.getMessageType());
        assertEquals(conversationId, result.getConversationId());
        assertEquals(messageOrder, result.getMessageOrder());

        System.out.println("=== 完整转换增强结果 ===");
        System.out.println("Content: " + result.getContent());
        System.out.println("MessageType: " + result.getMessageType());
        System.out.println("ConversationId: " + result.getConversationId());
        System.out.println("MessageOrder: " + result.getMessageOrder());
    }

    @Test
    @DisplayName("测试异常JSON格式")
    void testConvertToChatMessage_InvalidJson() {
        String invalidJson = "这不是一个有效的JSON";

        // 测试是否能正确处理异常
        assertThrows(Exception.class, () -> {
            converter.convertToChatMessage(invalidJson);
        }, "无效JSON应该抛出异常");
    }

    @Test
    @DisplayName("测试包含特殊字符的JSON")
    void testConvertToChatMessage_SpecialCharacters() {
        String jsonWithSpecialChars = """
            {
                "content": "包含特殊字符：\\n换行\\t制表符\\"引号",
                "messageType": "ASSISTANT"
            }
            """;

        ChatMessage result = converter.convertToChatMessage(jsonWithSpecialChars);

        assertNotNull(result);
        assertTrue(result.getContent().contains("特殊字符"));

        System.out.println("=== 特殊字符测试 ===");
        System.out.println("Content: " + result.getContent());
    }

    @Test
    @DisplayName("测试中文内容转换")
    void testConvertToChatMessage_ChineseContent() {
        String chineseJson = """
            {
                "content": "你好！我是你的恋爱心理专家。在感情的世界里，沟通是最重要的桥梁。",
                "messageType": "ASSISTANT"
            }
            """;

        ChatMessage result = converter.convertToChatMessage(chineseJson);

        assertNotNull(result);
        assertTrue(result.getContent().contains("恋爱心理专家"));
        assertEquals("ASSISTANT", result.getMessageType());

        System.out.println("=== 中文内容测试 ===");
        System.out.println("Content: " + result.getContent());
    }

    @Test
    @DisplayName("完整工作流程测试")
    void testCompleteWorkflow() {
        System.out.println("\n=== 完整工作流程测试 ===");

        // 1. 获取格式说明
        String format = converter.getFormat();
        System.out.println("1. 获取到的格式说明长度: " + format.length());

        // 2. 获取格式化指令
        String instructions = converter.getFormatInstructions();
        System.out.println("2. 格式化指令长度: " + instructions.length());

        // 3. 模拟AI返回的JSON
        String aiResponse = """
            {
                "content": "根据你的描述，我建议你主动创造一些共同话题，比如聊聊你们都喜欢的书籍类型。",
                "messageType": "ASSISTANT"
            }
            """;

        // 4. 转换并增强
        ChatMessage finalMessage = converter.convertAndEnhance(
            aiResponse,
            "love-advice-session-789",
            3
        );

        // 5. 验证最终结果
        assertNotNull(finalMessage);
        assertNotNull(finalMessage.getContent());
        assertNotNull(finalMessage.getMessageType());
        assertNotNull(finalMessage.getConversationId());
        assertNotNull(finalMessage.getMessageOrder());

        System.out.println("3. 最终消息内容: " + finalMessage.getContent());
        System.out.println("4. 消息类型: " + finalMessage.getMessageType());
        System.out.println("5. 对话ID: " + finalMessage.getConversationId());
        System.out.println("6. 消息顺序: " + finalMessage.getMessageOrder());

        System.out.println("✅ 完整工作流程测试通过！");
    }
}
