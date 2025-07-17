package com.hachimi.hachimiagent.app;

import com.hachimi.hachimiagent.advisor.ReReadingAdvisor;
import com.hachimi.hachimiagent.advisor.SelfLogAdvisor;
import com.hachimi.hachimiagent.chatmemory.FileBasedChatMemoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

import static org.springframework.ai.vectorstore.SearchRequest.DEFAULT_TOP_K;

@Component
@Slf4j
public class LoveApp {

    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT = "扮演深耕恋爱心理领域的专家。开场向用户表明身份，告知用户可倾诉恋爱难题。" +
            "围绕单身、恋爱、已婚三种状态提问：单身状态询问社交圈拓展及追求心仪对象的困扰；" +
            "恋爱状态询问沟通、习惯差异引发的矛盾；已婚状态询问家庭责任与亲属关系处理的问题。" +
            "引导用户详述事情经过、对方反应及自身想法，以便给出专属解决方案。";


    public LoveApp(ChatModel dashscopeChatModel) {  // 只注入 ChatModel
        // 初始化基于内存的对话记忆
        ChatMemoryRepository repository = new InMemoryChatMemoryRepository();
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(5)
                .build();

        // 初始化基于文件的对话记忆
        ChatMemoryRepository fileRepository = new FileBasedChatMemoryRepository("./tmp/chat_memory");
        ChatMemory fileChatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(fileRepository)
                .maxMessages(5)
                .build();

        // 创建配置好的 chatClient 并赋值给字段
        this.chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(fileChatMemory).build(),
                        new SelfLogAdvisor()
//                        new ReReadingAdvisor()
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
        ChatResponse chatResponse = chatClient.prompt()
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
        LoveReport loveReport = chatClient.prompt()
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
}
