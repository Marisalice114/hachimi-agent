package com.hachimi.hachimiagent.rag;


import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.stereotype.Component;


/**
 * 将原本的查询转换为新的查询
 */
@Component
@Slf4j
public class QueryTransformer {

    private final RewriteQueryTransformer rewriteQueryTransformer;

    public QueryTransformer(ChatModel dashscopeChatModel) {
        ChatClient.Builder builder = ChatClient.builder(dashscopeChatModel);
        //创建查询重写转换器
        rewriteQueryTransformer = RewriteQueryTransformer.builder()
                .chatClientBuilder(builder)
                .build();
    }

    /**
     * 执行查询重写
     * @param prompt
     * @return
     */
    public String doQueryRewrite(String prompt) {
        log.info("🔄 [QueryTransformer] 原始查询: '{}'", prompt);

        Query query = Query.builder()
                .text(prompt)
                .build();
        Query transformedQuery = rewriteQueryTransformer.transform(query);

        log.info("🔄 [QueryTransformer] 重写后查询: '{}'", transformedQuery.text());

        return transformedQuery.text();
    }
}
