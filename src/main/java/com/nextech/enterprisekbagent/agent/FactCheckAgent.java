package com.nextech.enterprisekbagent.agent;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 事实核查 Agent — 显式实现 ReAct 推理闭环。
 * <p>
 * 对给定的回答逐条验证其中的陈述是否被知识库支持。
 * 每轮迭代明确分工：think() → act() → observe()，三轮一组构成闭环。
 */
@Slf4j
@Component
public class FactCheckAgent {

    @Resource
    private ChatModel dashscopeChatModel;

    @Resource
    private VectorStore knowledgeBaseVectorStore;

    private final ChatOptions chatOptions;

    // ===== ReAct 循环状态 =====
    private String answerToVerify;
    private int currentStep = 0;
    private static final int MAX_STEPS = 10;
    private volatile boolean finished = false;

    /** 累积的"观察"上下文，每轮 observe() 写入，供下一轮 think() 读取 */
    private final List<String> observationHistory = new ArrayList<>();

    /** 最新一次 think 的决策文本 */
    private String lastDecision = "";

    /** 最新一次 act 的执行结果文本 */
    private String lastActionResult = "";

    public FactCheckAgent() {
        this.chatOptions = DashScopeChatOptions.builder()
                .withInternalToolExecutionEnabled(false)
                .build();
    }

    // =================================================================
    //  ReAct 闭环入口
    // =================================================================

    /**
     * 执行完整的事实核查 ReAct 循环。
     * <p>
     * 流程：think() → act() → observe() 重复，直到 LLM 决定 finished。
     */
    public String execute(String answerToVerify) {
        this.answerToVerify = answerToVerify;
        this.currentStep = 0;
        this.finished = false;
        this.observationHistory.clear();

        StringBuilder fullLog = new StringBuilder();
        fullLog.append("══════ ReAct 事实核查闭环开始 ══════\n\n");
        fullLog.append("【待核查的回答】\n").append(answerToVerify).append("\n\n");
        fullLog.append("──────────────────────────────────\n\n");

        while (!finished && currentStep < MAX_STEPS) {
            int round = currentStep + 1;
            fullLog.append("=== 第 ").append(round).append(" 轮 ReAct ===\n");

            // ------ 1. THINK ------
            fullLog.append("【THINK】\n");
            think();
            fullLog.append("决策: ").append(lastDecision).append("\n\n");

            // 如果 think 决定结束，跳出循环
            if (finished) break;

            // ------ 2. ACT ------
            fullLog.append("【ACT】\n");
            act();
            fullLog.append("执行结果: ").append(lastActionResult).append("\n\n");

            // ------ 3. OBSERVE ------
            fullLog.append("【OBSERVE】\n");
            observe();
            fullLog.append("已累积 ").append(observationHistory.size()).append(" 条观察\n\n");

            currentStep++;
        }

        if (currentStep >= MAX_STEPS) {
            fullLog.append("【循环结束】达到最大步数 (").append(MAX_STEPS).append(")\n");
        }

        fullLog.append("══════ ReAct 闭环结束 ══════\n");
        return fullLog.toString().replace("\n", System.lineSeparator());
    }

    // =================================================================
    //  ReAct 三阶段
    // =================================================================

    /**
     * THINK 阶段：根据当前上下文，让 LLM 决定下一步动作。
     * <p>
     * LLM 输出结构化决策格式：
     * <pre>
     * ACTION: search_kb | evaluate | finish
     * CLAIM: …
     * REASON: …
     * </pre>
     */
    private void think() {
        String systemPrompt = """
                你是 FactCheck Agent 的"思考"模块。你的任务是对给定的回答进行事实核查。
                
                你的工作方式是逐轮决策，每轮决定一个动作。可用的动作：
                
                1. ACTION: search_kb
                   选择一个需要验证的陈述，描述要用什么关键词检索知识库。
                   输出格式：
                   ACTION: search_kb
                   CLAIM: 要验证的陈述原文
                   SEARCH_KEYWORDS: 检索知识库用的关键词
                   REASON: 为什么需要验证这个
                
                2. ACTION: evaluate
                   基于检索到的知识库内容，对某个陈述做出判断。
                   输出格式：
                   ACTION: evaluate
                   VERDICT: supported | unsupported | partial
                   EVIDENCE: 知识库中的依据原文
                   EXPLANATION: 判断理由
                
                3. ACTION: finish
                   所有陈述验证完毕，结束流程。
                   输出格式：
                   ACTION: finish
                   SUMMARY: 核查总结
                
                当前需要核查的回答：
                """ + answerToVerify;

        // 构建本轮上下文
        String observationContext = observationHistory.isEmpty()
                ? "尚未检索任何内容。"
                : String.join("\n---\n", observationHistory);

        String userPrompt = """
                这是目前已累积的观察结果：
                %s
                
                请做出本轮决策。
                """.formatted(observationContext);

        Prompt prompt = new Prompt(List.of(
                new UserMessage(userPrompt)
        ), chatOptions);

        String decision = org.springframework.ai.chat.client.ChatClient.builder(dashscopeChatModel)
                .build()
                .prompt(prompt)
                .system(systemPrompt)
                .call()
                .content();

        this.lastDecision = (decision != null) ? decision.trim() : "LLM 返回为空";

        // 如果决策是 finish，标记结束
        if (lastDecision.toUpperCase().contains("ACTION: FINISH")) {
            this.finished = true;
        }

        log.info("[FactCheckAgent] THINK 第 {} 轮 决策: {}", currentStep + 1, lastDecision);
    }

    /**
     * ACT 阶段：执行 THINK 阶段做出的决策。
     * <p>
     * - search_kb: 用关键词检索向量库
     * - evaluate: 记录判断结果（无外部动作）
     * - finish: 无动作
     */
    private void act() {
        String decision = lastDecision.toUpperCase();

        if (decision.contains("ACTION: SEARCH_KB")) {
            // 从决策中提取检索关键词
            String keywords = extractField(lastDecision, "SEARCH_KEYWORDS");
            if (keywords.isEmpty()) {
                keywords = extractField(lastDecision, "CLAIM");
            }

            log.info("[FactCheckAgent] ACT 搜索知识库: {}", keywords);

            // 检索知识库
            try {
                List<Document> docs = knowledgeBaseVectorStore.similaritySearch(
                        SearchRequest.builder().query(keywords).topK(3).build()
                );
                if (docs.isEmpty()) {
                    lastActionResult = "【知识库检索无结果】关键词: " + keywords;
                } else {
                    lastActionResult = "【知识库检索结果】关键词: " + keywords + "\n"
                            + docs.stream()
                                    .map(d -> "  - " + d.getText().substring(0, Math.min(150, d.getText().length())))
                                    .collect(Collectors.joining("\n"));
                }
            } catch (Exception e) {
                lastActionResult = "【知识库检索失败】" + e.getMessage();
            }

        } else if (decision.contains("ACTION: EVALUATE")) {
            // evaluate 动作：记录判断，不需要外部调用
            String verdict = extractField(lastDecision, "VERDICT");
            String evidence = extractField(lastDecision, "EVIDENCE");
            String explanation = extractField(lastDecision, "EXPLANATION");
            lastActionResult = "【判断结果】裁决: " + verdict
                    + "\n  依据: " + evidence
                    + "\n  理由: " + explanation;

        } else if (decision.contains("ACTION: FINISH")) {
            lastActionResult = "【核查完成】" + extractField(lastDecision, "SUMMARY");

        } else {
            lastActionResult = "【未知动作】原始决策: " + lastDecision;
        }

        log.info("[FactCheckAgent] ACT 结果: {}", lastActionResult);
    }

    /**
     * OBSERVE 阶段：将本轮 act 的结果记录到观察历史中。
     * <p>
     * 这是 ReAct 闭环的关键——观察结果会在下一轮 THINK 时作为上下文传入。
     */
    private void observe() {
        String observation = "【第 " + (currentStep + 1) + " 轮】\n"
                + "  决策: " + lastDecision + "\n"
                + "  结果: " + lastActionResult;
        observationHistory.add(observation);
        log.info("[FactCheckAgent] OBSERVE 已记录第 {} 轮观察", currentStep + 1);
    }

    // =================================================================
    //  工具方法
    // =================================================================

    /**
     * 从 LLM 的结构化输出中提取指定字段的值。
     */
    private String extractField(String text, String fieldName) {
        if (text == null) return "";
        for (String line : text.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.toUpperCase().startsWith(fieldName + ":")) {
                return trimmed.substring(fieldName.length() + 1).trim();
            }
            if (trimmed.toUpperCase().startsWith(fieldName.toUpperCase() + ":")) {
                return trimmed.substring(fieldName.length() + 1).trim();
            }
        }
        return "";
    }
}
