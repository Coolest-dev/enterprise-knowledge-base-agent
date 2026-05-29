package com.nextech.enterprisekbagent.controller;

import com.nextech.enterprisekbagent.agent.EnterpriseKbSuperAgent;
import com.nextech.enterprisekbagent.app.KnowledgeBaseApp;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private KnowledgeBaseApp knowledgeBaseApp;

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private ChatModel dashscopeChatModel;

    /**
     * 同步调用企业知识库问答应用
     *
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping("/knowledge_base_app/chat/sync")
    public String doChatWithKnowledgeBaseAppSync(String message, String chatId) {
        return knowledgeBaseApp.doChat(message, chatId);
    }

    /**
     * SSE 流式调用企业知识库问答应用
     *
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping(value = "/knowledge_base_app/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithKnowledgeBaseAppSSE(String message, String chatId) {
        return knowledgeBaseApp.doChatByStream(message, chatId);
    }

    /**
     * SSE 流式调用企业知识库问答应用
     *
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping(value = "/knowledge_base_app/chat/server_sent_event")
    public Flux<ServerSentEvent<String>> doChatWithKnowledgeBaseAppServerSentEvent(String message, String chatId) {
        return knowledgeBaseApp.doChatByStream(message, chatId)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build());
    }

    /**
     * SSE 流式调用企业知识库问答应用
     *
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping(value = "/knowledge_base_app/chat/sse_emitter")
    public SseEmitter doChatWithKnowledgeBaseAppServerSseEmitter(String message, String chatId) {
        SseEmitter sseEmitter = new SseEmitter(180000L);
        knowledgeBaseApp.doChatByStream(message, chatId)
                .subscribe(chunk -> {
                    try {
                        if (chunk != null && !chunk.isEmpty()) {
                            sseEmitter.send(chunk);
                        }
                    } catch (IOException e) {
                        sseEmitter.completeWithError(e);
                    }
                }, error -> {
                    try {
                        sseEmitter.send("请求失败：" + error.getMessage());
                        sseEmitter.send("[DONE]");
                        sseEmitter.complete();
                    } catch (IOException e) {
                        sseEmitter.completeWithError(e);
                    }
                }, () -> {
                    try {
                        sseEmitter.send("[DONE]");
                        sseEmitter.complete();
                    } catch (IOException e) {
                        sseEmitter.completeWithError(e);
                    }
                });
        return sseEmitter;
    }

    /**
     * 流式调用 Manus 超级智能体
     *
     * @param message
     * @return
     */
    @GetMapping("/manus/chat")
    public SseEmitter doChatWithManus(String message) {
        EnterpriseKbSuperAgent enterpriseKbSuperAgent = new EnterpriseKbSuperAgent(allTools, dashscopeChatModel);
        return enterpriseKbSuperAgent.runStream(message);
    }
}
