package com.hachimi.hachimiagent.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
class LoveAppTest {
    @Resource
    private LoveApp loveApp;

    @Test
    void doChatTest() {
        String chatId = UUID.randomUUID().toString();
        //第一轮
        String userMessage = "你好，我是哈基米。我现在告诉你我的幸运数字是1919810";
        String answer = loveApp.doChat(userMessage, chatId);
//        //第二轮
//        userMessage = "我单身，我的女友是诗歌剧。可以为我和我的女友提一些建议吗";
//        answer = loveApp.doChat(userMessage, chatId);
//        Assertions.assertNotNull(answer);
//        //第三轮
//        userMessage = "我想追求一个心仪的对象，但不知道该如何开始，你还记得我的女友叫什么吗。还有你还记得我的幸运数据吗？";
//        answer = loveApp.doChat(userMessage, chatId);
//        Assertions.assertNotNull(answer);
    }
    @Test
    void doChatWithReport() {
        String chatId = UUID.randomUUID().toString();
        //第一轮
        String userMessage = "你好，我是哈基米.我想让我的女友(叮咚鸡)更加的爱我，但是我不知道该怎么做";
        LoveApp.LoveReport loveReport = loveApp.doChatWithReport(userMessage, chatId);
        Assertions.assertNotNull(loveReport);


    }
}