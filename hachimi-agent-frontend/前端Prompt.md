你是一位专业的前端开发，请帮我根据下列信息来生成对应的前端项目代码。

## 需求

1）主页：用于切换不同的应用

2）页面 1：AI 恋爱大师应用。页面风格为聊天室，上方是聊天记录（用户信息在右边，AI 信息在左边），下方是输入框，进入页面后自动生成一个聊天室 id，用于区分不同的会话。通过 SSE 的方式调用 doChatWithLoveAppSseEmitter 接口，实时显示对话内容。

3）页面 2：AI 超级智能体应用。页面风格同页面 1，但是调用 doChatWithManus 接口，也是实时显示对话内容。

4）在ai恋爱大师应用页面的侧边栏会显示历史会话记录，点击历史会话记录可以加载对应的聊天内容。用户可以在当前界面自行修改历史会话记录的名字，这个名字是不需要保存到后端的，只在前端做保留，可以设置为cookie

5）在AI 超级智能体的页面可以不设置页边的会话记录调取，因为manus没有相应的会话记录保存功能

## 技术选型

1. Vue3 项目
2. Axios 请求库

## 后端接口信息

接口地址前缀：http://localhost:8123/api

## SpringBoot 后端接口代码

```java

/**
 * 聊天历史管理控制器
 * 基于现有的数据库ChatMemory体系，直接使用Entity
 */
@RestController
@RequestMapping("/chat")
//@CrossOrigin(origins = "*")
public class ChatHistoryController {

    @Resource
    private ChatHistoryService chatHistoryService;

    /**
     * 获取所有会话列表
     * 返回会话摘要信息的Map列表
     */
    @GetMapping("/sessions")
    public List<Map<String, Object>> getAllSessions() {
        return chatHistoryService.getAllSessions();
    }

    /**
     * 获取指定会话的消息历史
     * 直接返回ChatMessage实体列表
     */
    @GetMapping("/sessions/{sessionId}/messages")
    public List<ChatMessage> getSessionMessages(@PathVariable String sessionId) {
        return chatHistoryService.getSessionMessages(sessionId);
    }

    /**
     * 获取会话信息
     * 返回会话摘要Map
     */
    @GetMapping("/sessions/{sessionId}")
    public Map<String, Object> getSession(@PathVariable String sessionId) {
        return chatHistoryService.getSession(sessionId);
    }

    /**
     * 删除会话（使用现有的ChatMemory体系）
     */
    @DeleteMapping("/sessions/{sessionId}")
    public void deleteSession(@PathVariable String sessionId) {
        chatHistoryService.deleteSession(sessionId);
    }

    /**
     * 注意：自定义会话名称功能由前端Cookie管理，不需要后端接口
     * 因为您的需求是"只在前端做保留，可以设置为cookie"
     */
}

```

```java

@RestController
@RequestMapping("/ai")
public class AIController {

    @Resource
    private LoveApp loveApp;

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private ChatModel dashscopeChatModel;

    @Resource
    private ManusPrompt manusPrompt;

    @GetMapping("/love_app/chat/sync")
    public String doChatWithLoveAppSync(String message, String chatId) {
        // LoveApp内部的dbChatMemory会自动保存用户消息和AI回复
        return loveApp.doChatWithRAG(message, chatId);
    }

    @GetMapping(value = "/love_app/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithLoveAppSSE(String message, String chatId) {
        // LoveApp内部的dbChatMemory会自动保存用户消息和AI回复
        return loveApp.doChatByStream(message, chatId);
    }

    @GetMapping("/love_app/chat/sse/emitter")
    public SseEmitter doChatWithLoveAppSseEmitter(String message, String chatId) {
        SseEmitter emitter = new SseEmitter(180000L); // 3分钟超时

        loveApp.doChatByStream(message, chatId)
                .doOnNext(chunk -> {
                    try {
                        emitter.send(chunk);
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                })
                .doOnComplete(emitter::complete)
                .doOnError(emitter::completeWithError)
                .subscribe();

        return emitter;
    }

    /**
     * 流式调用manus
     */
    @GetMapping("/manus/chat")
    public SseEmitter doChatWithManus(String message) {
        HachimiManus hachimiManus = new HachimiManus(allTools, dashscopeChatModel, manusPrompt);
        return hachimiManus.runStream(message);
    }

    @GetMapping("/manus/chat/sse/emitter")
    public SseEmitter doChatWithManusSSE(String message, String chatId) {
        // 注意：HachimiManus目前没有集成dbChatMemory，消息不会自动保存
        // 这是一个设计决策 - manus作为工具型智能体，可能不需要持久化对话历史

        HachimiManus hachimiManus = new HachimiManus(allTools, dashscopeChatModel, manusPrompt);
        return hachimiManus.runStream(message);
    }
}

```

```java
/**
 * 基于现有数据库ChatMemory体系的聊天历史服务
 * 直接使用Entity，基于您的实际数据库结构
 */
@Service
@Slf4j
public class ChatHistoryService {

    @Resource
    private MysqlBasedChatMemoryRepository chatMemoryRepository;

    @Resource
    private ChatMessageMapper messageMapper;

    @Resource
    private ChatConversationMapper conversationMapper;

    /**
     * 获取所有会话列表
     * 返回会话摘要信息的Map列表
     */
    public List<Map<String, Object>> getAllSessions() {
        try {
            // 使用现有的ChatMemoryRepository获取所有对话ID
            List<String> conversationIds = chatMemoryRepository.findConversationIds();

            return conversationIds.stream()
                    .map(this::buildSessionSummary)
                    .filter(session -> session != null)
                    .sorted((a, b) -> {
                        LocalDateTime timeA = (LocalDateTime) a.get("lastMessageTime");
                        LocalDateTime timeB = (LocalDateTime) b.get("lastMessageTime");
                        return timeB.compareTo(timeA);
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("获取会话列表失败", e);
            return List.of();
        }
    }

    /**
     * 获取指定会话的消息历史
     * 直接返回ChatMessage实体列表
     */
    public List<ChatMessage> getSessionMessages(String sessionId) {
        try {
            // 直接使用现有的mapper获取数据库中的完整消息信息
            List<ChatMessage> messages = messageMapper.findByConversationIdOrderByOrder(sessionId);

            log.debug("获取会话 {} 的消息 {} 条", sessionId, messages.size());
            return messages;

        } catch (Exception e) {
            log.error("获取会话消息失败, sessionId: {}", sessionId, e);
            return List.of();
        }
    }

    /**
     * 获取会话信息
     * 返回会话摘要Map
     */
    public Map<String, Object> getSession(String sessionId) {
        return buildSessionSummary(sessionId);
    }

    /**
     * 删除会话（使用现有的ChatMemoryRepository）
     */
    public void deleteSession(String sessionId) {
        try {
            chatMemoryRepository.deleteByConversationId(sessionId);
            log.info("已删除会话: {}", sessionId);
        } catch (Exception e) {
            log.error("删除会话失败, sessionId: {}", sessionId, e);
            throw new RuntimeException("删除会话失败", e);
        }
    }

    /**
     * 构建会话摘要信息
     * 基于您的实际Entity字段结构
     */
    private Map<String, Object> buildSessionSummary(String conversationId) {
        try {
            // 从数据库查询消息来构建会话信息
            List<ChatMessage> messages = messageMapper.findByConversationIdOrderByOrder(conversationId);

            if (messages.isEmpty()) {
                return null;
            }

            // 获取最后一条消息（按messageOrder排序后的最后一条）
            ChatMessage lastMessage = messages.get(messages.size() - 1);

            // 生成默认会话名称（基于第一条用户消息）
            String sessionName = generateDefaultSessionName(messages);

            // 构建会话摘要Map
            Map<String, Object> sessionSummary = new HashMap<>();
            sessionSummary.put("sessionId", conversationId);
            sessionSummary.put("sessionName", sessionName);
            sessionSummary.put("lastMessageTime", lastMessage.getCreateTime());
            sessionSummary.put("lastMessage", truncateContent(lastMessage.getContent()));
            sessionSummary.put("messageCount", messages.size());

            // 添加额外的元数据
            sessionSummary.put("lastMessageType", lastMessage.getMessageType());

            return sessionSummary;

        } catch (Exception e) {
            log.error("构建会话摘要失败, conversationId: {}", conversationId, e);
            return null;
        }
    }

    /**
     * 生成默认会话名称
     * 基于第一条用户消息的内容
     */
    private String generateDefaultSessionName(List<ChatMessage> messages) {
        // 查找第一条用户消息（messageType = "USER" 或 "user"）
        ChatMessage firstUserMessage = messages.stream()
                .filter(msg -> "USER".equalsIgnoreCase(msg.getMessageType()) || "user".equalsIgnoreCase(msg.getMessageType()))
                .findFirst()
                .orElse(null);

        if (firstUserMessage != null && firstUserMessage.getContent() != null) {
            String content = firstUserMessage.getContent().trim();
            if (content.length() <= 15) {
                return content;
            } else {
                return content.substring(0, 15) + "...";
            }
        }

        // 如果没有用户消息，使用第一条消息
        if (!messages.isEmpty()) {
            ChatMessage firstMessage = messages.get(0);
            if (firstMessage.getContent() != null) {
                String content = firstMessage.getContent().trim();
                if (content.length() <= 15) {
                    return content;
                } else {
                    return content.substring(0, 15) + "...";
                }
            }
        }

        return "新对话";
    }

    /**
     * 截断消息内容用于预览
     */
    private String truncateContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "...";
        }

        String trimmed = content.trim();
        if (trimmed.length() <= 50) {
            return trimmed;
        } else {
            return trimmed.substring(0, 50) + "...";
        }
    }
}

```




相关的页面配色风格和布局风格可以参考claude的会话页面



# 页面优化

现在我需要你帮我优化我的页面的样式，支持pc端平板和手机的响应式兼容，帮我优化每个页面的SEO，并且仿照这个页面[算法导航 - 交互式算法学习与可视化平台](https://algo.codefather.cn/)在底部增加版权信息，优化主页的样式，使用github上的高star风格，让人难忘.

可以参照着github上的高赞网页风格，为网页背景增加一些实时动画效果吗，注意配色和页面的主色调不要有过大的冲突，保证观赏性，同时在主页放一个可交互的对话示例

这个对话示例只需要，并且只能进行一次大模型调用，防止被滥用，相当于作为一个测试或者是体验例子，该对话也不需要保存到数据库，只是提供给用户进行一次体验，如果用户进行第二次输入时，禁用输入按钮。同时你现在提供的这个页面，在我输入内容的时候，前端页面显示的是"[object KeyboardEvent]"，修复这个问题。还有为网页的背景页面添加一些动态背景效果，目前的页面背景是全黑的，可以适当添加一些酷炫的动态特效

