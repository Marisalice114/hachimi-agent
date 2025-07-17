package com.hachimi.hachimiagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hachimi.hachimiagent.entity.ChatConversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 聊天对话Mapper
 *
 * @author hachimi
 * @since 2025-01-17
 */
@Mapper
public interface ChatConversationMapper extends BaseMapper<ChatConversation> {

    /**
     * 查询所有对话ID
     *
     * @return 对话ID列表
     */
    @Select("SELECT DISTINCT conversation_id FROM chat_conversation WHERE deleted = 0")
    List<String> findAllConversationIds();
}
