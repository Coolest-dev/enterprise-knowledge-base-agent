package com.nextech.enterprisekbagent.rag;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 混合检索器：向量检索（语义相似度）+ BM25 关键词检索，通过 RRF 融合排序
 */
@Slf4j
@Component
public class HybridDocumentRetriever implements DocumentRetriever {

    @Resource
    private VectorStore knowledgeBaseVectorStore;

    @Resource
    private KeywordIndexer keywordIndexer;

    private static final int TOP_K = 5;
    private static final int RRF_K = 60;

    @Override
    public List<Document> retrieve(Query query) {
        String queryText = query.text();

        // 1. 向量语义检索
        List<Document> vectorDocs = knowledgeBaseVectorStore.similaritySearch(
                SearchRequest.builder().query(queryText).topK(TOP_K).build()
        );

        // 2. BM25 关键词检索
        List<String> bm25Ids = keywordIndexer.search(queryText, TOP_K);

        // 3. RRF 融合
        return mergeResults(vectorDocs, bm25Ids);
    }

    private List<Document> mergeResults(List<Document> vectorDocs, List<String> bm25Ids) {
        if (bm25Ids.isEmpty()) {
            return vectorDocs;
        }
        if (vectorDocs.isEmpty()) {
            return vectorDocs;
        }

        // 构建排名映射
        Map<String, Integer> vectorRank = new HashMap<>();
        for (int i = 0; i < vectorDocs.size(); i++) {
            vectorRank.put(vectorDocs.get(i).getId(), i + 1);
        }

        Map<String, Integer> bm25Rank = new HashMap<>();
        for (int i = 0; i < bm25Ids.size(); i++) {
            bm25Rank.put(bm25Ids.get(i), i + 1);
        }

        // 收集所有文档 ID
        Set<String> allIds = new LinkedHashSet<>();
        for (Document doc : vectorDocs) allIds.add(doc.getId());
        allIds.addAll(bm25Ids);

        // RRF 评分
        List<ScoredDoc> scored = new ArrayList<>();
        for (String id : allIds) {
            double score = 0;
            if (vectorRank.containsKey(id)) {
                score += 1.0 / (RRF_K + vectorRank.get(id));
            }
            if (bm25Rank.containsKey(id)) {
                score += 1.0 / (RRF_K + bm25Rank.get(id));
            }
            scored.add(new ScoredDoc(id, score));
        }

        // 按 RRF 分值降序排序
        scored.sort((a, b) -> Double.compare(b.score, a.score));

        // 映射回完整的 Document 对象
        Map<String, Document> docMap = new HashMap<>();
        for (Document doc : vectorDocs) docMap.put(doc.getId(), doc);

        List<Document> result = new ArrayList<>();
        for (ScoredDoc sd : scored) {
            Document doc = docMap.get(sd.id);
            if (doc != null) {
                result.add(doc);
            }
        }

        log.debug("混合检索结果: 向量 {} 条, BM25 {} 条, 融合后 {} 条",
                vectorDocs.size(), bm25Ids.size(), result.size());
        return result;
    }

    private record ScoredDoc(String id, double score) {}
}
