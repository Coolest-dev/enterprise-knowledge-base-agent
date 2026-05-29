package com.nextech.enterprisekbagent.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

@SpringBootTest
class KnowledgeBaseAppTest {

    @Resource
    private KnowledgeBaseApp knowledgeBaseApp;

    @Test
    void testChat() {
        String chatId = UUID.randomUUID().toString();
        String message = "你好，我是新员工张三";
        String answer = knowledgeBaseApp.doChat(message, chatId);
        message = "公司的 Git 分支管理规范是什么？";
        answer = knowledgeBaseApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
        message = "我刚才说我叫什么来着？帮我回忆一下";
        answer = knowledgeBaseApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void doChatWithReport() {
        String chatId = UUID.randomUUID().toString();
        String message = "你好，我是新员工，想了解入职第一周的注意事项";
        KnowledgeBaseApp.KnowledgeBaseReport knowledgeBaseReport = knowledgeBaseApp.doChatWithReport(message, chatId);
        Assertions.assertNotNull(knowledgeBaseReport);
    }

    @Test
    void doChatWithRag() {
        String chatId = UUID.randomUUID().toString();
        String message = "公司远程办公政策是怎样的？每周可以远程几天？";
        String answer = knowledgeBaseApp.doChatWithRag(message, chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void doChatWithTools() {
        testMessage("帮我搜索企业 Git Flow 分支管理规范");
        testMessage("查看公司技术文档网站上的架构说明摘要");
        testMessage("下载一张适合用作企业内训封面的科技感图片为文件");
        testMessage("执行 Python3 脚本统计本周工单数据");
        testMessage("保存我的新员工入职检查清单为文件");
        testMessage("生成一份《新员工入职指南》PDF，包含第一周任务与常用系统说明");
    }

    private void testMessage(String message) {
        String chatId = UUID.randomUUID().toString();
        String answer = knowledgeBaseApp.doChatWithTools(message, chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void doChatWithMcp() {
        String chatId = UUID.randomUUID().toString();
        String message = "帮我搜索一些适合企业内训 PPT 使用的科技感配图";
        String answer = knowledgeBaseApp.doChatWithMcp(message, chatId);
        Assertions.assertNotNull(answer);
    }
}
