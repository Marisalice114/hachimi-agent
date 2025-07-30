package com.hachimi.hachimiagent.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class StreamStatusResponse {

    /**
     * 流式会话ID
     */
    private String streamId;

    /**
     * 是否活跃
     */
    private Boolean active;

    /**
     * 是否已停止
     */
    private Boolean stopped;

    /**
     * 对话ID
     */
    private String chatId;

    /**
     * 当前用户消息
     */
    private String currentMessage;

    /**
     * 创建时间戳
     */
    private Long createTime;

    /**
     * 运行时长（毫秒）
     */
    private Long duration;
}
