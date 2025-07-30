package com.hachimi.hachimiagent.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class StreamStopResponse {

    /**
     * 流式会话ID
     */
    private String streamId;

    /**
     * 是否成功停止
     */
    private Boolean stopped;

    /**
     * 停止时间戳
     */
    private Long stopTime;

    /**
     * 相关的对话ID
     */
    private String chatId;
}
