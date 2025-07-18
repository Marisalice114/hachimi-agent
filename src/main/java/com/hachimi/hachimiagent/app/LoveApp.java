
package com.hachimi.hachimiagent.app;

import com.hachimi.hachimiagent.advisor.SelfLogAdvisor;
import com.hachimi.hachimiagent.chatmemory.MysqlBasedChatMemoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class LoveApp {

    // 普通对话专用客户端
    private final ChatClient normalChatClient;
    // RAG对话专用客户端  
    private final ChatClient ragChatClient;
    private final MysqlBasedChatMemoryRepository mysqlBasedChatMemoryRepository;
    private final ChatMemory dbChatMemory;
    private final SelfLogAdvisor selfLogAdvisor;

    private static final String SYSTEM_PROMPT = "扮演深耕恋爱心理领域的专家。开场向用户表明身份，告知用户可倾诉恋爱难题。" +
            "围绕单身、恋爱、已婚三种状态提问：单身状态询问社交圈拓展及追求心仪对象的困扰；" +
            "恋爱状态询问沟通、习惯差异引发的矛盾；已婚状态询问家庭责任与亲属关系处理的问题。" +
            "引导用户详述事情经过、对方反应及自身想法，以便给出专属解决方案。";

    private static final String MULTIMODAL_SYSTEM_PROMPT = SYSTEM_PROMPT +
            "\n\n作为恋爱心理专家，我还能够分析图片内容，帮助你理解照片中的情感表达、身体语言、场景氛围等，" +
            "并结合这些视觉信息给出更准确的恋爱建议。请详细描述你看到的内容，并分析其中的情感信息。";

    // 通过构造函数注入所有依赖，包括VectorStore
    public LoveApp(ChatModel dashscopeChatModel,
                   MysqlBasedChatMemoryRepository mysqlBasedChatMemoryRepository,
                   VectorStore loveAppVectorStore) {
        this.mysqlBasedChatMemoryRepository = mysqlBasedChatMemoryRepository;

//        // 使用内存聊天内存作为默认的聊天内存
//        ChatMemoryRepository repository = new InMemoryChatMemoryRepository();
//        ChatMemory chatMemory = MessageWindowChatMemory.builder()
//                .chatMemoryRepository(repository)
//                .maxMessages(10)  // 增加窗口大小，减少频繁保存
//                .build();

//        // 使用文件聊天内存作为默认的聊天内存
//        ChatMemoryRepository fileRepository = new FileBasedChatMemoryRepository();
//        ChatMemory fileChatMemory = MessageWindowChatMemory.builder()
//                .chatMemoryRepository(fileRepository)
//                .maxMessages(10)  // 增加窗口大小，减少频繁保存
//                .build();

        // 使用注入的 MysqlBasedChatMemoryRepository 实例
        this.dbChatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(mysqlBasedChatMemoryRepository)
                .maxMessages(20)
                .build();

        this.selfLogAdvisor = new SelfLogAdvisor();

        // 创建普通对话专用的 chatClient
        this.normalChatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(dbChatMemory).build(),
                        selfLogAdvisor
//                    new ReReadingAdvisor()
                ).build();

        // 创建RAG对话专用的 chatClient，预配置所有RAG相关的advisors
        this.ragChatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        QuestionAnswerAdvisor.builder(loveAppVectorStore).build(),  // RAG功能
                        MessageChatMemoryAdvisor.builder(dbChatMemory).build(),     // 会话记忆保存
                        selfLogAdvisor                                              // 自定义日志
                ).build();
    }

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
                .system(SYSTEM_PROMPT + "每次对话后都要生成恋爱结果，标题为{用户名}的恋爱报告，内容为建议列表")
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
        // 使用预配置的RAG专用客户端，所有advisors已经配置好
        ChatResponse chatResponse = ragChatClient.prompt()
                .user(message)
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
}