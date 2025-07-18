package com.hachimi.hachimiagent.app;

import com.hachimi.hachimiagent.chatmemory.MysqlBasedChatMemoryRepository;
import com.hachimi.hachimiagent.entity.ChatConversation;
import com.hachimi.hachimiagent.entity.ChatMessage;
import com.hachimi.hachimiagent.mapper.ChatConversationMapper;
import com.hachimi.hachimiagent.mapper.ChatMessageMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@Rollback(false)  // 禁用事务回滚，让数据真正保存到数据库
class LoveAppTest {
    @Resource
    private LoveApp loveApp;

    @Resource
    private MysqlBasedChatMemoryRepository chatMemoryRepository;

    @Resource
    private ChatConversationMapper conversationMapper;

    @Resource
    private ChatMessageMapper messageMapper;
    @Autowired
    private MysqlBasedChatMemoryRepository mysqlBasedChatMemoryRepository;

    @Test
    @Rollback(false)  // 🔥 加这个注解
    void doChatTest() {
        String chatId = UUID.randomUUID().toString();
        System.out.println("Test Chat ID: " + chatId);

        // 只测试2轮对话，确保不超过maxMessages=5
        String response1 = loveApp.doChat("你好，我是哈基米。我的幸运数字是1919810", chatId);
        System.out.println("First response: " + response1);
        assertNotNull(response1);

        String response2 = loveApp.doChat("你还记得我的幸运数字吗？", chatId);
        System.out.println("Second response: " + response2);
        assertNotNull(response2);

        // 🔥 验证AI是否真的记住了
        assertTrue(response2.contains("1919810") ||
                        response2.toLowerCase().contains("记得"),
                "AI应该记住幸运数字，但回复：" + response2);

        // 验证数据库：应该有4条消息
        long messageCount = mysqlBasedChatMemoryRepository.getMessageCount(chatId);
        assertEquals(4, messageCount, "应该有4条消息（2轮对话）");

        verifyDatabaseData(chatId);
    }

    @Test
    void doChatWithReport() {
        String chatId = UUID.randomUUID().toString();
        //第一轮
        String userMessage = "你好，我是哈基米.我想让我的女友(叮咚鸡)更加的爱我，但是我不知道该怎么做";
        LoveApp.LoveReport loveReport = loveApp.doChatWithReport(userMessage, chatId);
        assertNotNull(loveReport);
    }

    @Test
    void verifyDatabaseConnection() {
        // 测试数据库连接和基本查询
        System.out.println("=== 数据库连接测试 ===");

        // 查询所有对话
        List<String> conversationIds = conversationMapper.findAllConversationIds();
        System.out.println("数据库中总对话数: " + conversationIds.size());

        // 显示最近的几个对话ID
        conversationIds.stream().limit(5).forEach(id ->
            System.out.println("对话ID: " + id));

        // 查询消息总数
        List<ChatMessage> allMessages = messageMapper.selectList(null);
        System.out.println("数据库中总消息数: " + allMessages.size());

        assertTrue(true, "数据库连接正常");
    }

    private void verifyDatabaseData(String chatId) {
        System.out.println("\n=== 验证数据库中的对话数据 ===");

        // 1. 检查对话记录
        ChatConversation conversation = conversationMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ChatConversation>()
                .eq(ChatConversation::getConversationId, chatId)
                .eq(ChatConversation::getDeleted, 0)
        );

        if (conversation != null) {
            System.out.println("✅ 找到对话记录: " + conversation.getConversationId());
            System.out.println("   创建时间: " + conversation.getCreateTime());
        } else {
            System.out.println("❌ 未找到对话记录");
        }

        // 2. 检查消息记录
        List<ChatMessage> messages = messageMapper.findByConversationIdOrderByOrder(chatId);
        System.out.println("📝 该对话的消息数量: " + messages.size());

        for (int i = 0; i < messages.size(); i++) {
            ChatMessage msg = messages.get(i);
            System.out.println("   消息 " + (i+1) + ": [" + msg.getMessageType() + "] " +
                             (msg.getContent().length() > 50 ?
                              msg.getContent().substring(0, 50) + "..." :
                              msg.getContent()));
        }

        // 3. 使用 ChatMemoryRepository 查询
        List<Message> memoryMessages = mysqlBasedChatMemoryRepository.findByConversationId(chatId);
        System.out.println("🧠 ChatMemoryRepository 查询到的消息数: " + memoryMessages.size());

        // 断言验证
        assertNotNull(conversation, "对话记录应该存在");
        assertTrue(messages.size() >= 2, "应该至少有2条消息（用户消息和AI回复）");
        assertEquals(messages.size(), memoryMessages.size(), "两种查询方式的结果应该一致");
    }

    @Test
    void doChatWithRAG(){
        String chatId = UUID.randomUUID().toString();
        //第一轮
        String userMessage = "我已经结婚了，但是婚后关系不太亲密，我该如何妥善解决";
        String result = loveApp.doChatWithRAG(userMessage, chatId);
        System.out.println(result);
    }
}
