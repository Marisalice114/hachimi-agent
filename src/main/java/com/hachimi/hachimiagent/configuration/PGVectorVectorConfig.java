package com.hachimi.hachimiagent.configuration;



import com.hachimi.hachimiagent.ETL.MyKeyWordEnricher;
import com.hachimi.hachimiagent.ETL.MyTokenTextSplitter;
import com.hachimi.hachimiagent.rag.LoveAppMarkdownReader;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType.COSINE_DISTANCE;
import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType.HNSW;
import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType.IVFFLAT;

@Configuration
@Slf4j
public class PGVectorVectorConfig {

    @Resource
    private LoveAppMarkdownReader loveAppMarkdownReader;

    @Resource
    private MyTokenTextSplitter myTokenTextSplitter;

    @Resource
    private MyKeyWordEnricher myKeyWordEnricher;


    @Bean
    public VectorStore pgVectorVectorStore(@Qualifier("vectorJdbcTemplate")JdbcTemplate vectorJdbcTemplate,
                                           EmbeddingModel embeddingModel) {
        log.info("🚀 使用的EmbeddingModel类型: {}", embeddingModel.getClass().getName());
        PgVectorStore pgVectorStore = PgVectorStore.builder(vectorJdbcTemplate, embeddingModel)
                .distanceType(COSINE_DISTANCE)
                .indexType(HNSW) //HNSW 或 IVFFLAT
                .initializeSchema(true)
                .schemaName("public")
                .vectorTableName("vector_store")
                .maxDocumentBatchSize(10000)
                .build();

        // ✅ 添加文档初始化逻辑
        initializePgVectorStore(pgVectorStore);

        return pgVectorStore;
    }

    private void initializePgVectorStore(PgVectorStore pgVectorStore) {
        try {
            // ✅ 直接使用简单的similaritySearch方法
            List<Document> existingDocs = pgVectorStore.similaritySearch("test");

            if (existingDocs.isEmpty()) {
                log.info("🔄 PgVectorStore为空，开始初始化文档...");

                List<Document> documents = loveAppMarkdownReader.LoadMarkdownDocuments();
                List<Document> splitDocuments = myTokenTextSplitter.splitCustomized(documents);
                List<Document> enrichDocuments = myKeyWordEnricher.enrichDocuments(splitDocuments);

                // 验证metadata
                enrichDocuments.forEach(doc -> {
                    String status = (String) doc.getMetadata().get("status");
                    String filename = (String) doc.getMetadata().get("filename");
                    log.info("📄 文档metadata - status: {}, filename: {}", status, filename);
                });

                pgVectorStore.add(enrichDocuments);
                log.info("✅ PgVectorStore初始化完成，共添加{}个文档", enrichDocuments.size());

                // ✅ 验证初始化结果
                List<Document> testDocs = pgVectorStore.similaritySearch("test");
                log.info("🔍 验证搜索结果数量: {}", testDocs.size());

            } else {
                log.info("✅ PgVectorStore已有数据，跳过初始化");
            }
        } catch (Exception e) {
            log.error("❌ PgVectorStore初始化失败", e);
        }
    }
}
