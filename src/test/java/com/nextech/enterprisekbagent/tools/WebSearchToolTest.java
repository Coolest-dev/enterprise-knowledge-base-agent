package com.nextech.enterprisekbagent.tools;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class WebSearchToolTest {

    @Resource
    private WebSearchTool webSearchTool;

    @Test
    void searchWeb() {
        String query = "NexTech Solutions 企业知识库 Git Flow";
        String result = webSearchTool.searchWeb(query);
        Assertions.assertNotNull(result);
    }
}
