package com.hachimi.hachimiagent.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class SSEEventData {

    /**
     * 事件类型
     */
    private String eventType;

    /**
     * 事件数据
     */
    private Object data;

    /**
     * 时间戳
     */
    private Long timestamp;

    /**
     * 流ID
     */
    private String streamId;

    public static SSEEventData of(String eventType, Object data, String streamId) {
        return new SSEEventData()
                .setEventType(eventType)
                .setData(data)
                .setStreamId(streamId)
                .setTimestamp(System.currentTimeMillis());
    }
}
