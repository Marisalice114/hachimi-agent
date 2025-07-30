package com.hachimi.hachimiagent.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ActiveSessionsResponse {

    /**
     * 活跃会话数量
     */
    private Integer activeCount;

    /**
     * 总会话数量
     */
    private Integer totalCount;
}
