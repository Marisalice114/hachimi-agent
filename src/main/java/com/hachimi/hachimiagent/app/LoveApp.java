package com.hachimi.hachimiagent.app;

import com.hachimi.hachimiagent.advisor.BanWordAdvisor;
import com.hachimi.hachimiagent.advisor.SelfLogAdvisor;
import com.hachimi.hachimiagent.chatmemory.MysqlBasedChatMemoryRepository;
import com.hachimi.hachimiagent.rag.QueryTransformer;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;


@Component
@Slf4j
public class LoveApp {

    // 客户端和所有依赖都声明为 final
    private final ChatClient normalChatClient;
    private final ChatClient ragChatClient;
    private final ChatClient cloudRagClient;
    private final ChatMemory dbChatMemory;
    private final SelfLogAdvisor selfLogAdvisor;
    private final ChatMemory baseChatMemory;
    private final BanWordAdvisor banWordAdvisor;
    private final QueryTransformer queryTransformer;

        private static final String SYSTEM_PROMPT = "扮演资深写作导师和文学创作专家，拥有丰富的写作指导经验。开场向用户表明身份，告知用户可分享写作困惑和创作难题。" +
                "根据用户写作目标进行针对性提问：" +
                "【创意写作类】针对小说、散文、诗歌等文学创作，询问题材选择、人物塑造、情节构思、文笔风格等方面的困扰；" +
                "【实用写作类】针对论文、报告、商务文案、自媒体内容等应用写作，询问结构组织、逻辑表达、受众定位、效果达成等问题；" +
                "【技能提升类】针对写作基础薄弱者，询问语言表达、素材积累、写作习惯、灵感获取等基础能力提升需求。" +
                "深入了解用户的写作背景、目标读者、创作动机、遇到的具体障碍，以及已尝试的解决方法，" +
                "结合用户提供的作品片段或创作计划，给出个性化的写作指导建议和实操方案。";

    /**
     * 使用构造函数注入所有依赖。
     * Spring 在调用此构造函数之前，会确保所有参数（Bean）都已创建完毕。
     * @param dashscopeChatModel 核心聊天模型
     * @param mysqlBasedChatMemoryRepository 数据库聊天记录仓库
     * @param loveAppRagCloudAdvisor 阿里云知识库顾问 (使用 @Qualifier 精确指定Bean名称)
     */
    public LoveApp(ChatModel dashscopeChatModel,
                   MysqlBasedChatMemoryRepository mysqlBasedChatMemoryRepository,
//                   VectorStore loveAppVectorStore, // 暂时移除
                   VectorStore pgVectorVectorStore,
                   @Qualifier("loveAppRagCloudAdvisor") Advisor loveAppRagCloudAdvisor, QueryTransformer queryTransformer) {

        // 1. 初始化内部组件
        this.dbChatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(mysqlBasedChatMemoryRepository)
                .maxMessages(20)
                .build();
        this.queryTransformer = queryTransformer;
        this.selfLogAdvisor = new SelfLogAdvisor();
        this.banWordAdvisor = new BanWordAdvisor();

        InMemoryChatMemoryRepository chatMemoryRepository = new InMemoryChatMemoryRepository();
        this.baseChatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(20)
                .build();

        // 2. 初始化所有 ChatClient
        // 在构造函数内部，所有注入的依赖都是可用的
        this.normalChatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(dbChatMemory).build(),
                        selfLogAdvisor
                ).build();

        this.ragChatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
//                        //基于内存存储的向量存储
//                        QuestionAnswerAdvisor.builder(loveAppVectorStore).build(),
                        //基于pgVector的向量存储
                        QuestionAnswerAdvisor.builder(pgVectorVectorStore).build(),
                        //基于自建工厂的rag
//                        createLoveAppRagCustomAdvisor(pgVectorVectorStore, "单身"),
                        MessageChatMemoryAdvisor.builder(dbChatMemory).build(),
                        banWordAdvisor,
                        selfLogAdvisor
                ).build();

        this.cloudRagClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        loveAppRagCloudAdvisor,
                        MessageChatMemoryAdvisor.builder(dbChatMemory).build(),
                        selfLogAdvisor
                ).build();
    }

    // ... (doChat, doChatWithRAG 等其他方法保持不变)
    /**
     * 支持多轮对话
     * @param userMessage 用户输入的消息
     * @param chatId 对话ID，用于标识不同的对话会话
     * @return 助手回复的消息内容
     */
    public String doChat(String userMessage, String chatId) {
        // 调用chatClient进行对话，使用官方文档推荐的方式传递对话ID
        ChatResponse chatResponse = normalChatClient.prompt()
                .user(userMessage)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .chatResponse();

        // 添加空值检查
        if (chatResponse == null || chatResponse.getResult() == null) {
            log.error("Chat response is null for chatId: {}", chatId);
            return "抱歉，系统暂时无法回复，请稍后再试。";
        }

        String chatResult = chatResponse.getResult().getOutput().getText();
//        log.info("Chat result: {}", chatResult);
        return chatResult;
    }

    //快速生成一个类
    record LoveReport(String title, List<String> suggestions) {
    }

    public LoveReport doChatWithReport(String userMessage, String chatId) {
        // 调用chatClient进行对话，使用官方文档推荐的方式传递对话ID
        LoveReport loveReport = normalChatClient.prompt()
                .system(SYSTEM_PROMPT + "每次对话后都要生成写作结果，标题为{用户名}的写作报告，内容为建议列表")
                .user(userMessage)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .entity(LoveReport.class);// 使用实体类接收结果;
        //分析实体类结构：检查LoveReport record的字段定义
        //自动生成JSON Schema：基于Java类结构创建对应的JSON Schema
        //添加系统提示：自动在prompt中添加格式化指令，要求AI返回符合schema的JSON
        //反序列化响应：将AI返回的JSON自动映射为Java对象

        log.info("loveReport: {}", loveReport);
        return loveReport;
    }

    /**
     * 知识库问答功能 - 使用RAG、会话记忆和自定义日志
     * @param message 用户消息
     * @param chatId 对话ID，用于标识不同的对话会话
     * @return 助手回复的消息内容
     */
    public String doChatWithRAG(String message, String chatId) {
        //查询重写
//        log.info("Original message: {}", message);
        String rewriteMessage = queryTransformer.doQueryRewrite(message);
        log.info("Rewritten message: {}", rewriteMessage);

        // 使用预配置的RAG专用客户端，所有advisors已经配置好
        ChatResponse chatResponse = ragChatClient.prompt()
                .user(rewriteMessage)
                //告诉 MessageChatMemoryAdvisor 使用哪个对话ID来存取历史记录
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .chatResponse();

        // 添加空值检查
        if (chatResponse == null || chatResponse.getResult() == null) {
            log.error("RAG Chat response is null for chatId: {}", chatId);
            return "抱歉，系统暂时无法回复，请稍后再试。";
        }

        String chatResult = chatResponse.getResult().getOutput().getText();
        log.info("ragAnswer: {}", chatResult);
        return chatResult;
    }

    // 可选：简化版本，直接返回内容而不是ChatResponse
    public String doChatWithRAGSimple(String message, String chatId) {
        String result = ragChatClient.prompt()
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .content();

        log.info("ragAnswer: {}", result);
        return result;
    }

    //基于百炼平台云知识库的rag调用
    public String doChatWithCloudRAG(String message, String chatId) {
        // 使用预配置的RAG专用客户端，所有advisors已经配置好
        ChatResponse chatResponse = cloudRagClient.prompt()
                .user(message)
                //告诉 MessageChatMemoryAdvisor 使用哪个对话ID来存取历史记录
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .chatResponse();

        // 添加空值检查
        if (chatResponse == null || chatResponse.getResult() == null) {
            log.error("Cloud RAG Chat response is null for chatId: {}", chatId);
            return "抱歉，系统暂时无法回复，请稍后再试。";
        }

        String chatResult = chatResponse.getResult().getOutput().getText();
        log.info("cloudRagAnswer: {}", chatResult);
        return chatResult;
    }



    //ai工具调用
    @Resource
    private ToolCallback[] allTools;


    public String doChatWithTools(String userMessage, String chatId) {
        // 调用chatClient进行对话，使用官方文档推荐的方式传递对话ID
        ChatResponse chatResponse = ragChatClient.prompt()
                .user(userMessage)
                //告诉 MessageChatMemoryAdvisor 使用哪个对话ID来存取历史记录
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                .toolCallbacks(allTools)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("Toolscontent: {}", content );
        return content ;
    }

    //AI调用MCP
    @Resource
    private ToolCallbackProvider toolCallbackProvider;


    public String doChatWithMCP(String userMessage, String chatId) {
        // 调用chatClient进行对话，使用官方文档推荐的方式传递对话ID
        ChatResponse chatResponse = ragChatClient.prompt()
                .user(userMessage)
                //告诉 MessageChatMemoryAdvisor 使用哪个对话ID来存取历史记录
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                .toolCallbacks(toolCallbackProvider.getToolCallbacks())
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("MCPcontent: {}", content );
        return content ;
    }


    public Flux<String> doChatByStream(String userMessage, String chatId) {
        // 调用chatClient进行对话，使用官方文档推荐的方式传递对话ID

        return ragChatClient.prompt()
                .user(userMessage)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream()
//                .chatResponse() // 使用流式响应的时候，可以不需要反应响应的全部信息
                .content();
    }
}
