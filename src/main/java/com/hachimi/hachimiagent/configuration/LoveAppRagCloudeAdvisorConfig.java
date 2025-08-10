package com.hachimi.hachimiagent.configuration;


import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetriever;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetrieverOptions;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * 基于百炼平台知识库的rag配置类
 */
@Configuration
public class LoveAppRagCloudeAdvisorConfig {


    @Value("${spring.ai.dashscope.api-key}")
    private String dashscopeApiKey;

    @Bean("loveAppRagCloudAdvisor")
    public Advisor loveAppRagCloudAdvisor() {
        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(dashscopeApiKey)
                .build();
        final String KNOWLEDGE_BASE_ID = "写作大师";

        DocumentRetriever retriver = new DashScopeDocumentRetriever(dashScopeApi,
                new DashScopeDocumentRetrieverOptions().builder()
                        .withIndexName(KNOWLEDGE_BASE_ID)
                        .build()
                );
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(retriver)
                .build();

    }
}
