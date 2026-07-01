package com.nextech.enterprisekbagent.tools;

import cn.hutool.core.util.StrUtil;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * 企业文档片段摘要生成工具。
 */
public class DocumentSummaryTool {

    private static final int DEFAULT_MAX_WORDS = 200;
    private static final int MIN_MAX_WORDS = 50;
    private static final int MAX_MAX_WORDS = 800;
    private static final int MAX_SECTION_LENGTH = 20000;

    private final ChatClient chatClient;

    public DocumentSummaryTool(ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem("""
                        你是企业知识库摘要助手。
                        只基于用户提供的企业文档片段生成摘要，不补充外部信息。
                        输出必须准确、简洁、结构清晰，适合企业员工快速理解原文。
                        """)
                .build();
    }

    @Tool(description = "Generate a concise summary for a selected enterprise document section")
    public String summarizeDocumentSection(
            @ToolParam(description = "Selected enterprise document section text") String sectionText,
            @ToolParam(description = "Summary style: brief, bullet, action_items") String style,
            @ToolParam(description = "Maximum Chinese words in the summary") Integer maxWords) {
        if (StrUtil.isBlank(sectionText)) {
            return "请提供需要摘要的文档片段。";
        }

        String normalizedStyle = normalizeStyle(style);
        int normalizedMaxWords = normalizeMaxWords(maxWords);
        String normalizedSectionText = normalizeSectionText(sectionText);

        return chatClient.prompt()
                .user("""
                        请为以下企业文档片段生成摘要。

                        摘要风格：%s
                        字数上限：%d 字

                        要求：
                        1. 只基于原文内容总结，不编造原文没有的信息。
                        2. 保留关键制度、流程、责任主体、时间要求、风险提示。
                        3. 如果原文包含行动项或待办事项，请单独列出“行动项”。
                        4. 如果原文信息不足，请明确说明“原文未提供”。

                        文档片段：
                        %s
                        """.formatted(normalizedStyle, normalizedMaxWords, normalizedSectionText))
                .call()
                .content();
    }

    private String normalizeStyle(String style) {
        if (StrUtil.isBlank(style)) {
            return "brief";
        }
        return switch (style.trim()) {
            case "bullet", "action_items" -> style.trim();
            default -> "brief";
        };
    }

    private int normalizeMaxWords(Integer maxWords) {
        if (maxWords == null) {
            return DEFAULT_MAX_WORDS;
        }
        return Math.min(Math.max(maxWords, MIN_MAX_WORDS), MAX_MAX_WORDS);
    }

    private String normalizeSectionText(String sectionText) {
        String text = sectionText.trim();
        if (text.length() <= MAX_SECTION_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_SECTION_LENGTH)
                + "\n\n[系统提示：原文片段过长，已截取前 " + MAX_SECTION_LENGTH + " 个字符用于摘要。]";
    }
}
