package com.hachimi.hachimiagent.configuration;


import com.hachimi.hachimiagent.ETL.MyKeyWordEnricher;
import com.hachimi.hachimiagent.ETL.MyTokenTextSplitter;
import com.hachimi.hachimiagent.rag.LoveAppMarkdownReader;
import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;


import java.util.List;

@Configuration
public class LoveAppVectorStoreConfig {

    @Resource
    private LoveAppMarkdownReader loveAppMarkdownReader;

    @Resource
    private MyTokenTextSplitter myTokenTextSplitter;

    @Resource
    private MyKeyWordEnricher myKeyWordEnricher;


    @Bean("loveAppVectorStore")
    @Lazy
    public VectorStore loveAppVectorStore(EmbeddingModel embeddingModel) {
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(embeddingModel).build();
        List<Document> documents = loveAppMarkdownReader.LoadMarkdownDocuments();
        //自定义文档切分
        List<Document> splitDocuments = myTokenTextSplitter.splitCustomized(documents);
        //自动补充文档的metadata
        List<Document> enrichDocuments = myKeyWordEnricher.enrichDocuments(splitDocuments);
        simpleVectorStore.add(enrichDocuments);
        return simpleVectorStore;
    }


}
