package com.nextech.enterprisekbagent.rag;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 企业知识库向量数据库配置（初始化基于内存的向量数据库 Bean）
 */
@Configuration
@Slf4j
public class KnowledgeBaseVectorStoreConfig {

    @Resource
    private KnowledgeBaseDocumentLoader knowledgeBaseDocumentLoader;

    @Resource
    private MyKeywordEnricher myKeywordEnricher;

    @Bean
    VectorStore knowledgeBaseVectorStore(EmbeddingModel dashscopeEmbeddingModel) {
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(dashscopeEmbeddingModel).build();
        try {
            List<Document> documentList = knowledgeBaseDocumentLoader.loadMarkdowns();
            List<Document> enrichedDocuments = myKeywordEnricher.enrichDocuments(documentList);
            simpleVectorStore.add(enrichedDocuments);
            log.info("文档关键词增强完成，已添加到向量库");
        } catch (Exception e) {
            log.warn("向量库初始化跳过（可稍后重试）: {}", e.getMessage());
        }
        return simpleVectorStore;
    }
}
