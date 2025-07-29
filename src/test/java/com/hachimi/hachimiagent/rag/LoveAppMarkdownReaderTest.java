package com.hachimi.hachimiagent.rag;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;


//@SpringBootTest
class LoveAppMarkdownReaderTest {


    @Resource
    private LoveAppMarkdownReader loveAppMarkdownReader;


    @Test
    void loadMarkdownDocuments() {
        loveAppMarkdownReader.LoadMarkdownDocuments();
    }
}