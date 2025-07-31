package com.hachimi.hachimiagent.rag;

import com.hachimi.hachimiagent.app.LoveApp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

import java.util.List;

import static com.hachimi.hachimiagent.rag.LoveAppContextualQueryAugmenterFactory.createLoveAppContextualQueryAugmenter;


@Slf4j
public class LoveAppRagCustomAdvisorFactory {

    /**
     * Advisor工厂
     * @return
     */
    public static Advisor createLoveAppRagCustomAdvisor(VectorStore vectorStore,String status) {
        log.info("🔧 [RAG工厂] 开始创建RAG Advisor，过滤条件: status={}", status);

        // 测试向量存储是否有数据
        try {
            List<Document> testDocs = vectorStore.similaritySearch("测试");
            log.info("🔍 [RAG工厂] 向量存储测试查询结果: {} 个文档", testDocs.size());
        } catch (Exception e) {
            log.error("❌ [RAG工厂] 向量存储测试失败", e);
        }

        Filter.Expression filterStatus = new FilterExpressionBuilder()
                .eq("status", status)
                .build();
        log.info("🔧 [RAG工厂] 创建过滤表达式: {}", filterStatus);

        DocumentRetriever retriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .similarityThreshold(0.5)
                .topK(3)
                .filterExpression(filterStatus)
                .build();
        log.info("🔧 [RAG工厂] 创建文档检索器完成");

        Advisor advisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(retriever)
                .queryAugmenter(createLoveAppContextualQueryAugmenter())
                .build();
        log.info("✅ [RAG工厂] RAG Advisor创建完成: {}", advisor.getClass().getSimpleName());

        return advisor;
    }


}
