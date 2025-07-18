package com.hachimi.hachimiagent.configuration;


import com.hachimi.hachimiagent.rag.LoveAppMarkdownReader;
import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


import java.util.List;

@Configuration
public class LoveAppVectorStoreConfig {

    @Resource
    private LoveAppMarkdownReader loveAppMarkdownReader;

    @Bean("loveAppVectorStore")
    public VectorStore loveAppVectorStore(@Qualifier("ollamaEmbeddingModel") EmbeddingModel ollamaEmbeddingModel) {
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(ollamaEmbeddingModel).build();
        List<Document> documents = loveAppMarkdownReader.LoadMarkdownDocuments();
        simpleVectorStore.add(documents);
        return simpleVectorStore;
    }


}
