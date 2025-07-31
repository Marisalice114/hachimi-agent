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
                // 去掉 .md 后缀
                String filenameWithoutExtension = filename.substring(0, filename.lastIndexOf('.'));
                // 提取倒数第二个和第三个字作为状态
                String status = "";
                int dashIndex = filenameWithoutExtension.indexOf(" - ");
                if (dashIndex != -1 && dashIndex < filenameWithoutExtension.length() - 3) {
                    String afterDash = filenameWithoutExtension.substring(dashIndex + 3); // +3 是因为" - "长度为3
                    if (afterDash.length() > 1) {
                        status = afterDash.substring(0, afterDash.length() - 1); // 去掉最后一个字符
                    }
                }
                MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                        .withHorizontalRuleCreateDocument(true)
                        .withIncludeCodeBlock(false)
                        .withIncludeBlockquote(false)
                        .withAdditionalMetadata("filename", filename) //每篇文档的metadata
                        .withAdditionalMetadata("status", status) //每篇文档的状态
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