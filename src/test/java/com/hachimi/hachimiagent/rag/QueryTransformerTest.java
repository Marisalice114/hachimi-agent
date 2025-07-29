package com.hachimi.hachimiagent.rag;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Repository;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

//@SpringBootTest
@Slf4j
class QueryTransformerTest {

    private final QueryTransformer queryTransformer;

    // ✅ 构造函数注入
    QueryTransformerTest(@Autowired QueryTransformer queryTransformer) {
        this.queryTransformer = queryTransformer;
    }

    @Test
    void testQueryRewrite() {
        String original = "怎么追女孩";
        String rewritten = queryTransformer.doQueryRewrite(original);

        log.info("原始: {}", original);
        log.info("重写: {}", rewritten);

        // ✅ 基本验证
        assertThat(rewritten).isNotNull();
        assertThat(rewritten).isNotEmpty();

        // 记录是否发生了重写
        boolean wasRewritten = !original.equals(rewritten);
        log.info("查询是否被重写: {}", wasRewritten);
    }
}