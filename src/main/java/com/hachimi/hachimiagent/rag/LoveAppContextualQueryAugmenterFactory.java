package com.hachimi.hachimiagent.rag;


import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.generation.augmentation.QueryAugmenter;

/**
 * 创建上下文查询增强器方法
 */
public class LoveAppContextualQueryAugmenterFactory {

    public static QueryAugmenter createLoveAppContextualQueryAugmenter() {
        PromptTemplate emptyContextPromptTemplate = new PromptTemplate("""
                你应该输出这样的内容:
                抱歉，我只能回答恋爱相关的内容，别的没有办法回答你哦
                哈基米哈基米(*^▽^*)
                """);
        return ContextualQueryAugmenter.builder()
                .allowEmptyContext(false) //RAG检索不到相关文档的情况，回答模板中的内容
                .emptyContextPromptTemplate(emptyContextPromptTemplate)
                .build();
    }
}
