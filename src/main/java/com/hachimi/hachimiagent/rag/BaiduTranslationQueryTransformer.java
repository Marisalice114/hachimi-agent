/*
 * Copyright 2023-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.ai.rag.preretrieval.query.transformation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.rag.Query;
import org.springframework.http.*;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * 使用百度翻译API将任意语言的用户查询翻译为中文的查询转换器。
 * <p>
 * 该转换器利用百度翻译API的自动语言检测功能，无需手动指定源语言类型，
 * 可以自动检测输入文本的语言并将其翻译为中文，从而提高中文向量数据库的查询效果。
 *
 * @author Your Name
 * @since 1.0.0
 */
public class BaiduTranslationQueryTransformer implements QueryTransformer {

    private static final Logger logger = LoggerFactory.getLogger(BaiduTranslationQueryTransformer.class);

    private static final String DEFAULT_API_URL = "http://api.fanyi.baidu.com/api/trans/vip/translate";
    private static final String DEFAULT_FROM_LANG = "auto"; // 自动检测源语言
    private static final String DEFAULT_TO_LANG = "zh"; // 翻译为中文

    private final String appId;
    private final String appSecret;
    private final String apiUrl;
    private final String fromLang;
    private final String toLang;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final SecureRandom random;

    public BaiduTranslationQueryTransformer(String appId, String appSecret, @Nullable String apiUrl,
                                            @Nullable String fromLang, @Nullable String toLang, @Nullable RestTemplate restTemplate) {
        Assert.hasText(appId, "appId cannot be null or empty");
        Assert.hasText(appSecret, "appSecret cannot be null or empty");

        this.appId = appId;
        this.appSecret = appSecret;
        this.apiUrl = apiUrl != null ? apiUrl : DEFAULT_API_URL;
        this.fromLang = fromLang != null ? fromLang : DEFAULT_FROM_LANG;
        this.toLang = toLang != null ? toLang : DEFAULT_TO_LANG;
        this.restTemplate = restTemplate != null ? restTemplate : new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.random = new SecureRandom();
    }

    @Override
    public Query transform(Query query) {
        Assert.notNull(query, "query cannot be null");

        String originalText = query.text();
        if (!StringUtils.hasText(originalText)) {
            logger.warn("Query text is null or empty. Returning the input query unchanged.");
            return query;
        }

        logger.debug("Translating query from {} to {}: {}", this.fromLang, this.toLang, originalText);

        try {
            String translatedText = translateText(originalText);
            if (!StringUtils.hasText(translatedText)) {
                logger.warn("Translation result is null/empty. Returning the input query unchanged.");
                return query;
            }

            // 如果翻译结果与原文相同，可能原文就是中文，直接返回
            if (originalText.equals(translatedText)) {
                logger.debug("Translation result is the same as original text. Likely already in target language.");
                return query;
            }

            logger.debug("Translation completed. Original: {} -> Translated: {}", originalText, translatedText);
            return query.mutate().text(translatedText).build();

        } catch (Exception e) {
            logger.error("Translation failed for query: {}. Returning original query. Error: {}",
                    originalText, e.getMessage(), e);
            return query;
        }
    }

    /**
     * 调用百度翻译API进行文本翻译
     */
    private String translateText(String text) throws Exception {
        // 生成随机盐值
        String salt = String.valueOf(random.nextInt(65536 - 32768) + 32768);

        // 生成签名: MD5(appid + query + salt + secret)
        String signStr = appId + text + salt + appSecret;
        String sign = DigestUtils.md5DigestAsHex(signStr.getBytes(StandardCharsets.UTF_8));

        // 构建请求URL
        String requestUrl = UriComponentsBuilder.fromHttpUrl(apiUrl)
                .queryParam("q", text)
                .queryParam("from", fromLang)
                .queryParam("to", toLang)
                .queryParam("appid", appId)
                .queryParam("salt", salt)
                .queryParam("sign", sign)
                .build()
//                .encode(StandardCharsets.UTF_8)
                .toUriString();

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        // 发送请求
        ResponseEntity<String> response = restTemplate.exchange(requestUrl, HttpMethod.GET, entity, String.class);

        if (response.getStatusCode() == HttpStatus.OK && StringUtils.hasText(response.getBody())) {
            return parseTranslationResult(response.getBody());
        } else {
            throw new RuntimeException("Translation API request failed with status: " + response.getStatusCode());
        }
    }

    /**
     * 解析百度翻译API的响应结果
     */
    private String parseTranslationResult(String responseBody) throws Exception {
        JsonNode rootNode = objectMapper.readTree(responseBody);

        // 检查是否有错误
        if (rootNode.has("error_code")) {
            String errorCode = rootNode.get("error_code").asText();
            String errorMsg = rootNode.has("error_msg") ? rootNode.get("error_msg").asText() : "Unknown error";
            throw new RuntimeException("Translation API error: " + errorCode + " - " + errorMsg);
        }

        // 提取翻译结果
        if (rootNode.has("trans_result") && rootNode.get("trans_result").isArray()) {
            JsonNode transResult = rootNode.get("trans_result").get(0);
            if (transResult != null && transResult.has("dst")) {
                return transResult.get("dst").asText();
            }
        }

        throw new RuntimeException("Invalid translation response format");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String appId;
        private String appSecret;
        @Nullable
        private String apiUrl;
        @Nullable
        private String fromLang;
        @Nullable
        private String toLang;
        @Nullable
        private RestTemplate restTemplate;

        private Builder() {
        }

        public Builder appId(String appId) {
            this.appId = appId;
            return this;
        }

        public Builder appSecret(String appSecret) {
            this.appSecret = appSecret;
            return this;
        }

        public Builder apiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
            return this;
        }

        public Builder fromLang(String fromLang) {
            this.fromLang = fromLang;
            return this;
        }

        public Builder toLang(String toLang) {
            this.toLang = toLang;
            return this;
        }

        public Builder restTemplate(RestTemplate restTemplate) {
            this.restTemplate = restTemplate;
            return this;
        }

        public BaiduTranslationQueryTransformer build() {
            return new BaiduTranslationQueryTransformer(this.appId, this.appSecret, this.apiUrl,
                    this.fromLang, this.toLang, this.restTemplate);
        }
    }
}