package com.hachimi.hachimiagent.rag;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

//@SpringBootTest
@Slf4j
class TranslationQueryTransformerTest {

    private final TranslationQueryTransformer translationQueryTransformer;

    // ✅ 构造函数注入
    TranslationQueryTransformerTest(@Autowired TranslationQueryTransformer translationQueryTransformer) {
        this.translationQueryTransformer = translationQueryTransformer;
    }

    @Test
    void testQueryTranslation() {
        String original = "What is artificial intelligence?";
        String translated = translationQueryTransformer.doQueryTranslation(original);

        log.info("原始: {}", original);
        log.info("翻译: {}", translated);

        // ✅ 基本验证
        assertThat(translated).isNotNull();
        assertThat(translated).isNotEmpty();

        // 记录是否发生了翻译
        boolean wasTranslated = !original.equals(translated);
        log.info("查询是否被翻译: {}", wasTranslated);
    }

    @Test
    void testChineseQuery() {
        String original = "什么是人工智能？";
        String translated = translationQueryTransformer.doQueryTranslation(original);

        log.info("原始: {}", original);
        log.info("翻译: {}", translated);

        // ✅ 基本验证
        assertThat(translated).isNotNull();
        assertThat(translated).isNotEmpty();

        // 中文查询应该保持不变
        boolean wasTranslated = !original.equals(translated);
        log.info("查询是否被翻译: {}", wasTranslated);
        log.info("中文查询应保持不变: {}", !wasTranslated);
    }

    @Test
    void testJapaneseQuery() {
        String original = "人工知能とは何ですか？";
        String translated = translationQueryTransformer.doQueryTranslation(original);

        log.info("原始: {}", original);
        log.info("翻译: {}", translated);

        // ✅ 基本验证
        assertThat(translated).isNotNull();
        assertThat(translated).isNotEmpty();

        // 记录是否发生了翻译
        boolean wasTranslated = !original.equals(translated);
        log.info("查询是否被翻译: {}", wasTranslated);
    }

    @Test
    void testEmptyQuery() {
        String original = "";
        String translated = translationQueryTransformer.doQueryTranslation(original);

        log.info("原始: '{}'", original);
        log.info("翻译: '{}'", translated);

        // ✅ 空查询应该返回空字符串
        assertThat(translated).isNotNull();
        assertThat(translated).isEmpty();

        boolean wasTranslated = !original.equals(translated);
        log.info("查询是否被翻译: {}", wasTranslated);
    }

    @Test
    void manualSignTest() {
        String appId = "20201128000628785";  // 替换为您真实的AppID
        String appSecret = "SId_xkccAlBR6D5an3vi";  // 替换为您真实的密钥
        String query = "hello";
        String salt = "1435660288";  // 固定盐值便于测试

        // 按百度官方规则生成签名
        String signStr = appId + query + salt + appSecret;
        String sign = DigestUtils.md5DigestAsHex(signStr.getBytes(StandardCharsets.UTF_8));

        System.out.println("AppID: " + appId);
        System.out.println("Query: " + query);
        System.out.println("Salt: " + salt);
        System.out.println("签名原串: " + signStr);
        System.out.println("MD5签名: " + sign);

        // 构建完整URL
        String url = "http://api.fanyi.baidu.com/api/trans/vip/translate?q=hello&from=auto&to=zh&appid="
                + appId + "&salt=" + salt + "&sign=" + sign;
        System.out.println("完整URL: " + url);
    }
}