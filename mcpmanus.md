现在我会提供给你一系列的信息，你需要参照着这些信息来帮我完成一个基于spring ai的manus构建

在我让你生成代码之前，你先不要生成，只需要思考并了解我提供的代码实现逻辑即可

首先是我目前的实现是参照着openmanus来实现的

openmanus主要实现的流程是

baseagent->reactagent->toolcallagent->manus

而openmanus还提供了一种实现方法，这种实现方法建立的manus可以进行mcp的调用

baseagent->reactagent->toolcallagent->mcpagent

我目前已经完成了baseagent->reactagent->toolcallagent->manus这种实现方式，具体的代码如下
```java
package com.hachimi.hachimiagent.agent;

import com.hachimi.hachimiagent.advisor.SelfLogAdvisor;
import com.hachimi.hachimiagent.agent.Prompt.ManusPrompt;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;


//springboot启动的时候自动注入chatclient 所以这里也让其作为component
@Component
public class HachimiManus extends ToolCallAgent {

    public HachimiManus(ToolCallback[] allTools, ChatModel dashscopeChatModel, ManusPrompt manusPrompt) {
        super(allTools);
        // 设置基本信息
        this.setName("HachimiManus");
        this.setDescription("OpenManus AI Assistant capable of handling complex tasks");

        // 使用 ManusPrompt 配置提示词
        // 项目根目录
        String workingDirectory = System.getProperty("user.dir");
        this.setSystemPrompt(manusPrompt.buildSystemPrompt(workingDirectory));
        this.setNextStepPrompt(manusPrompt.getNextStepPrompt());
        this.setMaxSteps(10);
        // 初始化客户端
        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(new SelfLogAdvisor())
                .build();
        this.setChatClient(chatClient);

    }
}

```

```java
package com.hachimi.hachimiagent.agent;

/**
 * 处理工具调用的基础代理类，具体实现了 think 和 act 方法，可以用作创建实例的父类
 */
import cn.hutool.core.collection.CollUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.hachimi.hachimiagent.agent.model.AgentState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ToolCallAgent extends ReactAgent{

    //可用工具list
    private final ToolCallback[] avaliableToollist;
    //对话回复，方便提取信息
    private ChatResponse toolCallChatResponse;
    //工具调用管理
    private final ToolCallingManager toolCallingManager;
    //对话选项，配置工具调用相关选项
    private final ChatOptions chatOptions;
    //调用体
    public ToolCallAgent (ToolCallback[] avaliableToollist) {
        super();
        this.avaliableToollist = avaliableToollist;
        this.toolCallingManager = ToolCallingManager.builder().build();
        this.chatOptions = DashScopeChatOptions.builder()
                .withInternalToolExecutionEnabled(false)
                .build();
    }

    /**
     * 处理状态，判断下一步的执行
     * @return
     */
    @Override
    public boolean think() {
        //1.判断输入内容
        if(getNextStepPrompt()!=null && !getNextStepPrompt().isEmpty()){
            UserMessage userMessage = new UserMessage(getNextStepPrompt());
            //添加消息到会话中
            getMessagesList().add(userMessage);
            // 清空nextStepPrompt，避免重复添加
            setNextStepPrompt(null);
        }
        List<Message> messageList = getMessagesList();
        //对话设置需要再prompt中指定
        Prompt prompt = new Prompt(messageList,chatOptions);


        //2.工具调用
        try {
            //获取聊天客户端
            ChatResponse chatResponse = getChatClient().prompt(prompt)
                    .messages(messageList)  // 使用messages而不是重复的system
                    .toolCallbacks(avaliableToollist)
                    .call()
                    .chatResponse();
            //获取工具调用结果，记录为响应
            this.toolCallChatResponse = chatResponse;
            AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
            //提示输出信息
            String result = assistantMessage.getText();
            //最终选择使用的工具
            List<AssistantMessage.ToolCall> toolCallList = assistantMessage.getToolCalls();
            //输出信息和选择使用的工具的info
            log.info(getName() + " - ToolCallAgent think result: " + result);
            log.info(getName() + " - ToolCallAgent think nums of tool calls to use: " + toolCallList.size());
            //返回工具调用信息 名称和参数
            String toolCallInfo = toolCallList.stream()
                    .map(toolCall -> "Tool: " + toolCall.name() + ", Arguments: " + toolCall.arguments())
                    .reduce((a, b) -> a + "\n" + b)
                    .orElse("No tools called");
            log.info(toolCallInfo);
            if (toolCallList.isEmpty()){
                // 不需要调用工具
                // 参照为继续对话
                messageList.add(assistantMessage);
                return false;
            }else {
                // 需要调用工具
                // 自动记录
                return true;
            }
        } catch (Exception e) {
            log.error(getName() + " - ToolCallAgent think failed: " + e.getMessage(), e);
            getMessagesList().add(new AssistantMessage("Error during tool call: " + e.getMessage()));
            return false;
        }
    }

    /**
     * 执行工具调用，并返回结果
     * @return
     */

    @Override
    public String act() {
        if (!toolCallChatResponse.hasToolCalls()){
            return "Tool calls not found in chat response, no action taken.";
        }
        //调用会话记录
        Prompt prompt = new Prompt(getMessagesList(), chatOptions);
        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, toolCallChatResponse);
        //将调用情况记录到messageList中
        setMessagesList(toolExecutionResult.conversationHistory());
        //获取执行结果
        //conversationHistory是整个会话记录，只需要取最后一条
        //ToolResponseMessage继承自 Message
        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) CollUtil.getLast(toolExecutionResult.conversationHistory());
        //判断是否用了终止调用工具
        boolean isTerminateToolUsed = toolResponseMessage.getResponses().stream()
                //判断调用的工具名
                .anyMatch(response -> response.name().equals("doTerminate"));
        if(isTerminateToolUsed){
            setState(AgentState.FINISHED);
        }

        String result = toolResponseMessage.getResponses().stream()
                .map(response -> "Tool: " + response.name() + ", Result: " + response.responseData())
                .collect(Collectors.joining("\n"));
        log.info(result);
        return result;
    }
}
```

而我现在想参照着openmanus的mcpagent的实现，来实现一个基于springai的agent,openmanus的mcpagent实现是这样的

```python
from typing import Any, Dict, List, Optional, Tuple

from pydantic import Field

from app.agent.toolcall import ToolCallAgent
from app.logger import logger
from app.prompt.mcp import MULTIMEDIA_RESPONSE_PROMPT, NEXT_STEP_PROMPT, SYSTEM_PROMPT
from app.schema import AgentState, Message
from app.tool.base import ToolResult
from app.tool.mcp import MCPClients


class MCPAgent(ToolCallAgent):
    """Agent for interacting with MCP (Model Context Protocol) servers.

    This agent connects to an MCP server using either SSE or stdio transport
    and makes the server's tools available through the agent's tool interface.
    """

    name: str = "mcp_agent"
    description: str = "An agent that connects to an MCP server and uses its tools."

    system_prompt: str = SYSTEM_PROMPT
    next_step_prompt: str = NEXT_STEP_PROMPT

    # Initialize MCP tool collection
    mcp_clients: MCPClients = Field(default_factory=MCPClients)
    available_tools: MCPClients = None  # Will be set in initialize()

    max_steps: int = 20
    connection_type: str = "stdio"  # "stdio" or "sse"

    # Track tool schemas to detect changes
    tool_schemas: Dict[str, Dict[str, Any]] = Field(default_factory=dict)
    _refresh_tools_interval: int = 5  # Refresh tools every N steps

    # Special tool names that should trigger termination
    special_tool_names: List[str] = Field(default_factory=lambda: ["terminate"])

    # 初始化
    async def initialize(
        self,
        connection_type: Optional[str] = None,
        server_url: Optional[str] = None,
        command: Optional[str] = None,
        args: Optional[List[str]] = None,
    ) -> None:
        """Initialize the MCP connection.

        Args:
            connection_type: Type of connection to use ("stdio" or "sse")
            server_url: URL of the MCP server (for SSE connection)
            command: Command to run (for stdio connection)
            args: Arguments for the command (for stdio connection)
        """
        if connection_type:
            self.connection_type = connection_type

        # Connect to the MCP server based on connection type
        # 以sse和本地stdio两种方式连接MCP服务器
        if self.connection_type == "sse":
            if not server_url:
                raise ValueError("Server URL is required for SSE connection")
            await self.mcp_clients.connect_sse(server_url=server_url)
        elif self.connection_type == "stdio":
            if not command:
                raise ValueError("Command is required for stdio connection")
            await self.mcp_clients.connect_stdio(command=command, args=args or [])
        else:
            raise ValueError(f"Unsupported connection type: {self.connection_type}")

        # Set available_tools to our MCP instance
        self.available_tools = self.mcp_clients

        # Store initial tool schemas
        # 存储初始化的工具模式
        await self._refresh_tools()

        # Add system message about available tools
        tool_names = list(self.mcp_clients.tool_map.keys())
        tools_info = ", ".join(tool_names)

        # Add system prompt and available tools information
        self.memory.add_message(
            Message.system_message(
                f"{self.system_prompt}\n\nAvailable MCP tools: {tools_info}"
            )
        )

    async def _refresh_tools(self) -> Tuple[List[str], List[str]]:
        """Refresh the list of available tools from the MCP server.

        Returns:
            A tuple of (added_tools, removed_tools)
        """
        if not self.mcp_clients.sessions:
            return [], []

        # Get current tool schemas directly from the server
        response = await self.mcp_clients.list_tools()
        current_tools = {tool.name: tool.inputSchema for tool in response.tools}

        # ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
        # │   AI Agent      │    │  MCP Client     │    │  MCP Server     │
        # │                 │    │  (工具管理器)    │    │  (工具提供者)    │
        # ├─────────────────┤    ├─────────────────┤    ├─────────────────┤
        # │ self.llm        │───▶│ 调用大模型API    │    │                 │
        # │ (LLM客户端)      │    │                 │    │                 │
        # │                 │    │                 │    │                 │
        # │ self.available_ │◀───┤ self.mcp_clients│◀──▶│ 提供工具列表     │
        # │ tools           │    │ (MCP客户端)      │    │ 执行工具调用     │
        # │ (工具管理器)     │    │                 │    │                 │
        # └─────────────────┘    └─────────────────┘    └─────────────────┘

        # Determine added, removed, and changed tools
        current_names = set(current_tools.keys())
        previous_names = set(self.tool_schemas.keys())

        added_tools = list(current_names - previous_names)
        removed_tools = list(previous_names - current_names)

        # Check for schema changes in existing tools
        changed_tools = []
        for name in current_names.intersection(previous_names):
            if current_tools[name] != self.tool_schemas.get(name):
                changed_tools.append(name)

        # Update stored schemas
        self.tool_schemas = current_tools

        # Log and notify about changes
        if added_tools:
            logger.info(f"Added MCP tools: {added_tools}")
            self.memory.add_message(
                Message.system_message(f"New tools available: {', '.join(added_tools)}")
            )
        if removed_tools:
            logger.info(f"Removed MCP tools: {removed_tools}")
            self.memory.add_message(
                Message.system_message(
                    f"Tools no longer available: {', '.join(removed_tools)}"
                )
            )
        if changed_tools:
            logger.info(f"Changed MCP tools: {changed_tools}")

        return added_tools, removed_tools

    async def think(self) -> bool:
        """Process current state and decide next action."""
        # Check MCP session and tools availability
        # 如果MCP客户端断开连接或工具列表为空，则结束交互
        if not self.mcp_clients.sessions or not self.mcp_clients.tool_map:
            logger.info("MCP service is no longer available, ending interaction")
            self.state = AgentState.FINISHED
            return False

        # Refresh tools periodically

        # 设置多少步来刷新工具
        if self.current_step % self._refresh_tools_interval == 0:
            await self._refresh_tools()
            # 刷新下工作列表
            # All tools removed indicates shutdown
            if not self.mcp_clients.tool_map:
                logger.info("MCP service has shut down, ending interaction")
                self.state = AgentState.FINISHED
                return False

        # Use the parent class's think method
        return await super().think()

    async def _handle_special_tool(self, name: str, result: Any, **kwargs) -> None:
        """Handle special tool execution and state changes"""
        # First process with parent handler
        await super()._handle_special_tool(name, result, **kwargs)

        # Handle multimedia responses
        if isinstance(result, ToolResult) and result.base64_image:
            self.memory.add_message(
                Message.system_message(
                    MULTIMEDIA_RESPONSE_PROMPT.format(tool_name=name)
                )
            )

    def _should_finish_execution(self, name: str, **kwargs) -> bool:
        """Determine if tool execution should finish the agent"""
        # Terminate if the tool name is 'terminate'
        return name.lower() == "terminate"

    async def cleanup(self) -> None:
        """Clean up MCP connection when done."""
        if self.mcp_clients.sessions:
            await self.mcp_clients.disconnect()
            logger.info("MCP connection closed")

    async def run(self, request: Optional[str] = None) -> str:
        """Run the agent with cleanup when done."""
        try:
            result = await super().run(request)
            return result
        finally:
            # Ensure cleanup happens even if there's an error
            await self.cleanup()
```

我只想实现其中的部分功能，能够保证项目的mvp(最小可行性实现)，我认为只需要实现和think方法重写和刷新工具

注意，prompt相关的实现可以单独建一个类来实现，比如这样

```java
package com.hachimi.hachimiagent.agent.Prompt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "agent.prompts")
public class ManusPrompt {

    private String systemPromptTemplate = """
            You are OpenManus, an all-capable AI assistant, aimed at solving any task presented by the user. \
            You have various tools at your disposal that you can call upon to efficiently complete complex requests. \
            Whether it's programming, information retrieval, file processing, web browsing, or human interaction \
            (only for extreme cases), you can handle it all.
            The initial directory is: {directory}
            """;

    private String nextStepPrompt = """
            Based on user needs, proactively select the most appropriate tool or combination of tools. \
            For complex tasks, you can break down the problem and use different tools step by step to solve it. \
            After using each tool, clearly explain the execution results and suggest the next steps.
            
            If you want to stop the interaction at any point, use the `terminate` tool/function call.
            """;

    /**
     * 构建系统提示词
     */
    public String buildSystemPrompt(String directory) {
        return systemPromptTemplate.replace("{directory}",
                directory != null ? directory : "current directory");
    }
}

```




对于spring ai来说，不用像python这样单独创建一个函数来管理与mcp的连接

springai可以这样实现与mcp的连接，这是工具调用和使用mcp的对比
```java
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
```

只需要在本地建立一个json文件

```json
{
  "mcpServers": {
    "baidu-map": {
      "command": "npx.cmd",
      "args": [
        "-y",
        "@baidumap/mcp-server-baidu-map"
      ],
      "env": {
        "BAIDU_MAP_API_KEY": "xxxxxxxxxxxx"
      }
    },
    "hachimi-image-search-mcp-server": {
      "command": "java",
      "args": [
        "-Dspring.ai.mcp.server.stdio=true",
        "-Dspring.main.web-application-type=none",
        "-Dlogging.pattern.console=",
        "-jar",
        "hachimi-image-search-mcp-server/target/hachimi-image-search-mcp-server-0.0.1-SNAPSHOT.jar"
      ],
      "env": {
      }
    }
  }
}
```

然后在配置文件中设置其连接方式即可

```yml
spring:
  application:
    name: hachimi-agent
  profiles:
    active: local

  # 主数据源 - MySQL (保持原有配置路径，对话存储)
  datasource:
    url: jdbc:mysql://localhost:3306/hachimi_agent?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: root
    password: 123456
    # Druid 连接池配置
    initial-size: 5
    min-idle: 5
    max-active: 20
    max-wait: 60000
    time-between-eviction-runs-millis: 60000
    min-evictable-idle-time-millis: 300000
    validation-query: SELECT 1
    test-while-idle: true
    test-on-borrow: false
    test-on-return: false
    pool-prepared-statements: true
    max-pool-prepared-statement-per-connection-size: 20

  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        model: qwen3:0.6b
      embedding:
        model: nomic-embed-text:latest
    vectorstore:
      pgvector:
        index-type: HNSW                    # 向量索引类型
        distance-type: COSINE_DISTANCE     # 距离计算方式
        max-document-batch-size: 10000     # 批处理大小

    mcp:
      client:
        request-timeout: 30s
        type: SYNC  # or ASYNC for reactive applications
        stdio:
          servers-configuration: classpath:mcp-servers.json
#        sse:
#          connections:
#            server1:
#              url: http://localhost:910
```