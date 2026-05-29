package com.nextech.enterprisekbagent.rag;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class KnowledgeBaseDocumentLoaderTest {

    @Resource
    private KnowledgeBaseDocumentLoader knowledgeBaseDocumentLoader;

    @Test
    void loadMarkdowns() {
        knowledgeBaseDocumentLoader.loadMarkdowns();
    }
}
