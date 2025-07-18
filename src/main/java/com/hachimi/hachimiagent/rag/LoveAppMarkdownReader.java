package com.hachimi.hachimiagent.rag;


import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import org.springframework.ai.document.Document;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class LoveAppMarkdownReader {

    private final ResourcePatternResolver resourcePatternResolver;

    public LoveAppMarkdownReader(ResourcePatternResolver resourcePatternResolver) {
        this.resourcePatternResolver = resourcePatternResolver;
    }


    /**
     * markdown文档加载
     * @param
     * @return
     * @throws IOException
     */
    public List<Document> LoadMarkdownDocuments() {
        List<Document> allDocuments = new ArrayList<>();

        //加载多篇markdown文档
        try {
            Resource[] resources = resourcePatternResolver.getResources("classpath:document/*.md");
            for (Resource resource : resources) {
                // 文件名
                String filename = resource.getFilename();
                MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                        .withHorizontalRuleCreateDocument(true)
                        .withIncludeCodeBlock(false)
                        .withIncludeBlockquote(false)
                        .withAdditionalMetadata("filename", filename) //每篇文档的额外信息
                        .build();
                MarkdownDocumentReader reader = new MarkdownDocumentReader(resource, config);
                allDocuments.addAll(reader.get());
            }
        } catch (IOException e) {
            log.error("Error loading markdown documents.");
            throw new RuntimeException(e);

        }
        return allDocuments;
    }
}