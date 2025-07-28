package com.hachimi.hachimiagent.controller;

import com.hachimi.hachimiagent.entity.ChatMessage;
import com.hachimi.hachimiagent.service.ChatHistoryService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;

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
