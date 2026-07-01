package com.nextech.enterprisekbagent.rag;

import jakarta.annotation.Resource;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class KeywordIndexer {

    @Resource
    private KnowledgeBaseDocumentLoader knowledgeBaseDocumentLoader;

    private final Directory directory = new ByteBuffersDirectory();
    private final Analyzer analyzer = new StandardAnalyzer();
    private volatile boolean indexed = false;

    @PostConstruct
    public void init() {
        try {
            List<Document> documents = knowledgeBaseDocumentLoader.loadMarkdowns();
            if (!documents.isEmpty()) {
                buildIndex(documents);
            }
        } catch (Exception e) {
            log.warn("关键词索引初始化跳过: {}", e.getMessage());
        }
    }

    public synchronized void buildIndex(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            log.warn("无可索引的文档");
            return;
        }
        IndexWriterConfig config = new IndexWriterConfig(analyzer)
                .setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        try (IndexWriter writer = new IndexWriter(directory, config)) {
            for (Document doc : documents) {
                org.apache.lucene.document.Document luceneDoc = new org.apache.lucene.document.Document();
                luceneDoc.add(new StringField("id", doc.getId(), Field.Store.YES));
                luceneDoc.add(new TextField("content", doc.getText(), Field.Store.NO));

                Object keywords = doc.getMetadata().get("keywords");
                if (keywords != null) {
                    luceneDoc.add(new TextField("keywords", keywords.toString(), Field.Store.NO));
                }

                writer.addDocument(luceneDoc);
            }
            writer.commit();
            indexed = true;
            log.info("BM25 关键词索引构建完成: {} 篇文档", documents.size());
        } catch (IOException e) {
            log.error("BM25 关键词索引构建失败", e);
        }
    }

    public List<String> search(String queryText, int topK) {
        if (!indexed) return List.of();
        DirectoryReader reader = null;
        try {
            reader = DirectoryReader.open(directory);
            IndexSearcher searcher = new IndexSearcher(reader);
            QueryParser parser = new QueryParser("content", analyzer);
            parser.setDefaultOperator(QueryParser.Operator.OR);
            org.apache.lucene.search.Query query = parser.parse(queryText);

            TopDocs results = searcher.search(query, topK);
            List<String> ids = new ArrayList<>();
            for (ScoreDoc scoreDoc : results.scoreDocs) {
                org.apache.lucene.document.Document doc = searcher.doc(scoreDoc.doc);
                ids.add(doc.get("id"));
            }
            log.debug("BM25 检索到 {} 条结果", ids.size());
            return ids;
        } catch (ParseException | IOException e) {
            log.warn("BM25 搜索失败: {}", e.getMessage());
            return List.of();
        } finally {
            if (reader != null) {
                try { reader.close(); } catch (IOException ignored) {}
            }
        }
    }
}