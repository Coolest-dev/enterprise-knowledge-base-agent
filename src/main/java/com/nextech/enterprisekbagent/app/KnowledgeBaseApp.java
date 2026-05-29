package com.nextech.enterprisekbagent.app;

import com.nextech.enterprisekbagent.advisor.MyLoggerAdvisor;
import com.nextech.enterprisekbagent.rag.KnowledgeBaseRagCustomAdvisorFactory;
import com.nextech.enterprisekbagent.rag.QueryRewriter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

@Component
@Slf4j
public class KnowledgeBaseApp {

    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT = "你是 NexTech Solutions 企业内部知识库智能助手。" +
            "开场向用户表明身份，告知用户可咨询企业文化、组织架构、技术规范、信息安全、办公行政、入职培训、应急响应等企业内部相关问题。" +
            "引导用户说明具体场景与需求，优先依据企业知识库内容作答；若知识库无相关信息，应明确说明并建议联系对应部门。";

    /**
     * 初始化 ChatClient
     *
     * @param dashscopeChatModel
     */
    public KnowledgeBaseApp(ChatModel dashscopeChatModel) {
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();
        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        new MyLoggerAdvisor()
                )
                .build();
    }

    /**
     * AI 基础对话（支持多轮对话记忆）
     *
     * @param message
     * @param chatId
     * @return
     */
    public String doChat(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    /**
     * AI 基础对话（支持多轮对话记忆，SSE 流式传输）
     *
     * @param message
     * @param chatId
     * @return
     */
    public Flux<String> doChatByStream(String message, String chatId) {
        return chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream()
                .content();
    }

    record KnowledgeBaseReport(String title, List<String> suggestions) {

    }

    /**
     * 企业知识库咨询报告（实战结构化输出）
     *
     * @param message
     * @param chatId
     * @return
     */
    public KnowledgeBaseReport doChatWithReport(String message, String chatId) {
        KnowledgeBaseReport knowledgeBaseReport = chatClient
                .prompt()
                .system(SYSTEM_PROMPT + "每次对话后都要生成咨询结果，标题为{用户名}的企业知识库咨询报告，内容为建议列表")
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .entity(KnowledgeBaseReport.class);
        log.info("knowledgeBaseReport: {}", knowledgeBaseReport);
        return knowledgeBaseReport;
    }

    @Resource
    private VectorStore knowledgeBaseVectorStore;

    @Resource
    private Advisor knowledgeBaseRagCloudAdvisor;

    @Resource
    private VectorStore pgVectorVectorStore;

    @Resource
    private QueryRewriter queryRewriter;

    /**
     * 和 RAG 知识库进行对话
     *
     * @param message
     * @param chatId
     * @return
     */
    public String doChatWithRag(String message, String chatId) {
        String rewrittenMessage = queryRewriter.doQueryRewrite(message);
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(rewrittenMessage)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(new MyLoggerAdvisor())
                .advisors(new QuestionAnswerAdvisor(knowledgeBaseVectorStore))
//                .advisors(knowledgeBaseRagCloudAdvisor)
//                .advisors(new QuestionAnswerAdvisor(pgVectorVectorStore))
//                .advisors(
//                        KnowledgeBaseRagCustomAdvisorFactory.createKnowledgeBaseRagCustomAdvisor(
//                                knowledgeBaseVectorStore, "企业"
//                        )
//                )
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    @Resource
    private ToolCallback[] allTools;

    /**
     * 企业知识库问答（支持调用工具）
     *
     * @param message
     * @param chatId
     * @return
     */
    public String doChatWithTools(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(new MyLoggerAdvisor())
                .toolCallbacks(allTools)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    /**
     * 企业知识库问答（调用 MCP 服务）
     *
     * @param message
     * @param chatId
     * @return
     */
    public String doChatWithMcp(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(new MyLoggerAdvisor())
                .toolCallbacks(toolCallbackProvider)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }
}
