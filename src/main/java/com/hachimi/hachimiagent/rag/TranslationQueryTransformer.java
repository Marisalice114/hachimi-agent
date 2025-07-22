package com.hachimi.hachimiagent.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.BaiduTranslationQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * 将原本的查询翻译为中文查询
 */
@Component
@Slf4j
public class TranslationQueryTransformer {

    private final BaiduTranslationQueryTransformer baiduTranslationQueryTransformer;

    public TranslationQueryTransformer(
            @Value("${spring.ai.baidu.translation.app-id}") String appId,
            @Value("${spring.ai.baidu.translation.app-secret}") String appSecret) {
        log.info("读取到的配置 - AppId: {}, AppSecret: {}", appId, appSecret);
        this.baiduTranslationQueryTransformer = BaiduTranslationQueryTransformer.builder()
                .appId(appId)
                .appSecret(appSecret)
                .build();
    }

    public String doQueryTranslation(String prompt) {
        log.info("🔄 [QueryTransformer] 原始查询: '{}'", prompt);

        Query query = Query.builder()
                .text(prompt)
                .build();
        Query transformedQuery = baiduTranslationQueryTransformer.transform(query);

        log.info("🔄 [QueryTransformer] 重写后查询: '{}'", transformedQuery.text());

        return transformedQuery.text();
    }
}