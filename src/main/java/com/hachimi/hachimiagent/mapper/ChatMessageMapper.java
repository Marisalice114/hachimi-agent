package com.hachimi.hachimiagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hachimi.hachimiagent.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 聊天消息Mapper
 *
 * @author hachimi
 * @since 2025-01-17
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    /**
     * 根据对话ID查询消息列表（按顺序排序）
     *
     * @param conversationId 对话ID
     * @return 消息列表
     */
    @Select("SELECT * FROM chat_message WHERE conversation_id = #{conversationId} AND deleted = 0 ORDER BY message_order")
    List<ChatMessage> findByConversationIdOrderByOrder(@Param("conversationId") String conversationId);

    /**
     * 获取对话中的最大消息顺序号
     *
     * @param conversationId 对话ID
     * @return 最大顺序号
     */
    @Select("SELECT COALESCE(MAX(message_order), -1) FROM chat_message WHERE conversation_id = #{conversationId} AND deleted = 0")
    Integer getMaxMessageOrder(@Param("conversationId") String conversationId);
}
