package com.hachimi.hachimiagent.configuration;

import com.hachimi.hachimiagent.rag.LoveAppMarkdownReader;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;


//@SpringBootTest
class PGVectorVectorConfigTest {

    @Resource
    private VectorStore pgVectorVectorStore;

    @Resource
    private LoveAppMarkdownReader loveAppMarkdownReader;

    @Test
    void pgVectorVectorStore() {
//        List<Document> documents = List.of(
//                new Document("Spring AI rocks!! Spring AI rocks!! Spring AI rocks!! Spring AI rocks!! Spring AI rocks!!", Map.of("meta1", "meta1")),
//                new Document("The World is Big and Salvation Lurks Around the Corner"),
//                new Document("You walk forward facing the past and you turn back toward the future.", Map.of("meta2", "meta2")));
//        // 添加文档
//        pgVectorVectorStore.add(documents);
        List<Document> documents = loveAppMarkdownReader.LoadMarkdownDocuments();
        pgVectorVectorStore.add(documents);

        // 相似度查询
        List<Document> results = pgVectorVectorStore.similaritySearch(SearchRequest.builder().query("Spring").topK(5).build());
        Assertions.assertNotNull(results);
    }

    @Value("${spring.ai.vectorstore.pgvector.dimensions:768}")
    private int configuredDimensions;

    @Test
    public void checkConfiguration() {
        System.out.println("配置的维度: " + configuredDimensions);
        // 这应该打印 768，如果还是 1536 说明配置没有生效
    }
}


