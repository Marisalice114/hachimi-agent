package com.hachimi.hachimiagent.rag;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.ai.rag.Query;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


//@SpringBootTest
class MyMultiQueryExpanderTest {


    @Resource
    private MyMultiQueryExpander myMultiQueryExpander;
    @Test
    void expand() {
        Query query = Query.builder()
                .text("你知道哈基米吗")
                .build();
        List<Query> expand = myMultiQueryExpander.expand(query);
        System.out.println(expand);
    }
}