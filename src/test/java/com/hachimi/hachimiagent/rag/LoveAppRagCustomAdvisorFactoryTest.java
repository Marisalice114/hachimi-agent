package com.hachimi.hachimiagent.rag;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


//@SpringBootTest
@Slf4j
class LoveAppRagCustomAdvisorFactoryTest {

    @Resource
    private VectorStore pgVectorVectorStore;
    @Test
    void testDocumentRetrievalForNonLoveQueries(){
        log.info("🧪 直接测试文档检索行为...");

        // ✅ 在测试中直接创建DocumentRetriever
        VectorStoreDocumentRetriever retriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(pgVectorVectorStore)  // 使用注入的pgVectorVectorStore
                .filterExpression(new FilterExpressionBuilder().eq("status", "单身").build())
                .similarityThreshold(0.5)
                .topK(5)
                .build();

        String[] testQueries = {
                "如何学习编程？",           // 完全不相关
                "今天天气怎么样？",         // 完全不相关
                "单身的人如何脱单？",       // 应该有结果
                "如何克服恋爱焦虑？",       // 应该有结果
                "怎么做红烧肉？"           // 完全不相关
        };

        for (String query : testQueries) {
            List<Document> results = retriever.retrieve(new Query(query));
            log.info("🔍 查询: '{}' -> {} 个结果", query, results.size());

            if (results.isEmpty()) {
                log.info("   ✅ 空结果，应该触发emptyContextPromptTemplate");
            } else {
                results.forEach(doc -> {
                    String status = (String) doc.getMetadata().get("status");
                    String content = doc.getText().substring(0, Math.min(50, doc.getText().length()));
                    log.info("   📄 找到文档: status='{}', 内容='{}'", status, content);
                });
            }
        }
    }

    @Test
    void testDifferentThresholds(){
        log.info("🧪 测试不同相似度阈值的效果...");

        String[] testQueries = {
                "今天天气怎么样？",      // 应该0个结果
                "怎么做红烧肉？",        // 应该0个结果
                "如何学习编程？",        // 应该0个结果
                "单身如何脱单？",        // 应该有结果
                "恋爱焦虑怎么办？"       // 应该有结果
        };

        double[] thresholds = {0.3, 0.5, 0.7, 0.8};

        for (double threshold : thresholds) {
            log.info("\n🔍 测试阈值: {}", threshold);

            VectorStoreDocumentRetriever retriever = VectorStoreDocumentRetriever.builder()
                    .vectorStore(pgVectorVectorStore)
                    .filterExpression(new FilterExpressionBuilder().eq("status", "单身").build())
                    .similarityThreshold(threshold)
                    .topK(5)
                    .build();

            for (String query : testQueries) {
                List<Document> results = retriever.retrieve(new Query(query));
                log.info("   查询: '{}' -> {} 个结果", query, results.size());
            }
        }
    }

}