package com.nextech.enterprisekbagent.rag;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

@SpringBootTest
class PgVectorVectorStoreConfigTest {

    @Autowired(required = false)
    private VectorStore pgVectorVectorStore;

    @Test
    void test() {
        if (pgVectorVectorStore == null) {
            return;
        }
        List<Document> documents = List.of(
                new Document("企业知识库有什么用？解答内部制度与流程问题", Map.of("meta1", "meta1")),
                new Document("NexTech Solutions 远程办公政策说明"),
                new Document("新员工入职 30 天翱翔计划", Map.of("meta2", "meta2")));
        pgVectorVectorStore.add(documents);
    }
}
