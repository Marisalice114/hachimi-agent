package com.hachimi.hachimiagent.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class StreamStartResponse {

    /**
     * 流式会话ID
     */
    private String streamId;

    /**
     * 对话ID
     */
    private String chatId;

    /**
     * 会话状态
     */
    private String status;

    /**
     * 创建时间戳
     */
    private Long createTime;

    /**
     * 用户消息
     */
    private String userMessage;
}
