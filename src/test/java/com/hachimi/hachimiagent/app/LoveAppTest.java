package com.hachimi.hachimiagent.app;

import com.hachimi.hachimiagent.chatmemory.MysqlBasedChatMemoryRepository;
import com.hachimi.hachimiagent.entity.ChatConversation;
import com.hachimi.hachimiagent.entity.ChatMessage;
import com.hachimi.hachimiagent.mapper.ChatConversationMapper;
import com.hachimi.hachimiagent.mapper.ChatMessageMapper;
import com.hachimi.hachimiagent.rag.QueryTransformer;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.rag.generation.augmentation.QueryAugmenter;
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
@Slf4j
class LoveAppTest {
    @Resource
    private LoveApp loveApp;

    @Resource
    private MysqlBasedChatMemoryRepository chatMemoryRepository;

    @Resource
    private ChatConversationMapper conversationMapper;

    @Resource
    private ChatMessageMapper messageMapper;

    @Resource
    private MysqlBasedChatMemoryRepository mysqlBasedChatMemoryRepository;

    @Resource
    private QueryTransformer queryTransformer;
    @Test
    @Rollback(false)
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
//        String userMessage = "";
        String result = loveApp.doChatWithRAG(userMessage, chatId);
        System.out.println(result);
    }

    @Test
    void doChatWithCloudRAG(){
        String chatId = UUID.randomUUID().toString();
        //第一轮
        String userMessage = "我已经结婚了，但是婚后关系不太亲密，我该如何妥善解决";
        String result = loveApp.doChatWithCloudRAG(userMessage, chatId);
        System.out.println(result);
    }

    @Test
    void testEmptyContextBehavior(){
        log.info("🧪 开始空上下文行为测试...");

        String chatId = UUID.randomUUID().toString();

        // ✅ 这些查询应该触发空上下文模板（检索不到文档）
        String[] emptyContextQueries = {
                "今天天气怎么样？",      // 完全无关
                "怎么做红烧肉？",        // 完全无关（如果阈值设置正确）
                "如何学习编程？",        // 完全无关
                "什么是人工智能？",      // 完全无关
                "北京有什么好玩的？"     // 完全无关
        };

        // ✅ 这些查询应该正常回答（检索到相关文档）
        String[] normalQueries = {
                "单身如何脱单？",        // 恋爱相关
                "恋爱焦虑怎么办？",      // 恋爱相关
                "相亲要注意什么？"       // 恋爱相关
        };

        log.info("📋 测试应该触发拒绝模板的查询：");
        for (String query : emptyContextQueries) {
            log.info("🔍 测试查询: {}", query);
            String result = loveApp.doChatWithRAG(query, chatId + "_empty_" + query.hashCode());

            // ✅ 检查是否使用了拒绝模板
            boolean usesTemplate = result.contains("抱歉，我只能回答恋爱相关的内容") &&
                    result.contains("哈基米哈基米");

            log.info("   -> 使用拒绝模板: {}", usesTemplate);

            if (usesTemplate) {
                log.info("   ✅ 正确！使用了空上下文模板");
            } else {
                log.warn("   ❌ 错误！应该使用拒绝模板，但实际回复: {}",
                        result.substring(0, Math.min(100, result.length())));
            }
        }

        log.info("\n📋 测试应该正常回答的查询：");
        for (String query : normalQueries) {
            log.info("🔍 测试查询: {}", query);
            String result = loveApp.doChatWithRAG(query, chatId + "_normal_" + query.hashCode());

            // ✅ 检查是否给出了正常回答（不是拒绝模板）
            boolean hasNormalAnswer = !result.contains("抱歉，我只能回答恋爱相关的内容");

            log.info("   -> 有正常回答: {}", hasNormalAnswer);

            if (hasNormalAnswer) {
                log.info("   ✅ 正确！给出了恋爱相关的建议");
            } else {
                log.warn("   ❌ 错误！不应该使用拒绝模板，实际回复: {}",
                        result.substring(0, Math.min(100, result.length())));
            }
        }
    }

    @Test
    void testEmptyContextWithDetailedLogging() {
        log.info("🧪 开始空上下文详细调试测试...");

        String chatId = UUID.randomUUID().toString();

        // 测试完全无关的查询
        String[] testQueries = {
                "今天天气怎么样？",
                "怎么做红烧肉？",
                "如何学习编程？",
                "单身如何脱单？"  // 这个应该有结果
        };

        for (String originalQuery : testQueries) {
            log.info("\n" + "=".repeat(50));
            log.info("🔍 测试原始查询: '{}'", originalQuery);

            // 1. 先测试查询重写结果
            String rewrittenQuery = queryTransformer.doQueryRewrite(originalQuery);
            log.info("📝 查询重写结果: '{}'", rewrittenQuery);

            // 2. 测试向量检索结果（你需要注入VectorStore来直接测试）
            // testDirectVectorSearch(rewrittenQuery);

            // 3. 测试完整的RAG调用
            String result = loveApp.doChatWithRAG(originalQuery, chatId + "_" + originalQuery.hashCode());

            // 4. 分析结果
            boolean isEmptyContextResponse = result.contains("抱歉，我只能回答恋爱相关的内容")
                    && result.contains("哈基米哈基米");

            log.info("📊 结果分析:");
            log.info("   - 原始查询: {}", originalQuery);
            log.info("   - 重写查询: {}", rewrittenQuery);
            log.info("   - 使用空上下文模板: {}", isEmptyContextResponse);
            log.info("   - 实际回复: {}", result.substring(0, Math.min(100, result.length())));

            if (originalQuery.contains("脱单")) {
                // 这个应该有正常回复
                assertFalse(isEmptyContextResponse, "恋爱相关查询不应该触发空上下文模板");
            } else {
                // 这些应该触发空上下文模板
                assertTrue(isEmptyContextResponse,
                        String.format("无关查询 '%s' 应该触发空上下文模板，但实际回复: %s",
                                originalQuery, result.substring(0, Math.min(50, result.length()))));
            }
        }
    }

    @Test
    void doChatWithTools() {
        // 测试联网搜索问题的答案
        testMessage("周末想带女朋友去上海约会，推荐几个适合情侣的小众打卡地？");

        // 测试网页抓取：恋爱案例分析
        testMessage("最近和对象吵架了，看看编程导航网站（codefather.cn）的其他情侣是怎么解决矛盾的？");

        // 测试资源下载：图片下载
        testMessage("直接下载一张适合做手机壁纸的星空情侣图片为文件");

        // 测试终端操作：执行代码
        testMessage("执行 Python3 脚本来生成数据分析报告");

        // 测试文件操作：保存用户档案
        testMessage("保存我的恋爱档案为文件");

        // 测试 PDF 生成
        testMessage("生成一份‘七夕约会计划’PDF，包含餐厅预订、活动流程和礼物清单");
    }

    private void testMessage(String message) {
        String chatId = UUID.randomUUID().toString();
        String answer = loveApp.doChatWithTools(message, chatId);
        Assertions.assertNotNull(answer);
    }

    //测试MCP
    @Test
    void doChatWithMCP() {
        String chatId = UUID.randomUUID().toString();
        //测试地图MCP
//        String userMessage = "我的另一半住在上海静安区，请帮我找到5公里内合适的约会地点";
//        String result = loveApp.doChatWithMCP(userMessage, chatId);
//        System.out.println(result);

        //测试图片搜索MCP
        String userMessage = "帮我搜索一些哄女朋友开心的图片，只需要一张即可";
        String result = loveApp.doChatWithMCP(userMessage, chatId);
        System.out.println(result);
    }
}
