package com.legal.contract.service.agent;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * RAG检索Agent - 从Milvus向量数据库检索相关合同/知识
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RAGAgent {

    private final MilvusService milvusService;
    private final ChatLanguageModel chatLanguageModel;


    public String retrieve(String query, int topK) {
        log.info("[RAGAgent] 检索知识: query={}", query);

        try {
            List<Map<String, Object>> results = milvusService.searchSimilar(query, topK);
            if (results == null || results.isEmpty()) {
                return "";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("【相关知识检索结果】\n\n");
            for (int i = 0; i < results.size(); i++) {
                Map<String, Object> r = results.get(i);
                sb.append("第").append(i + 1).append("条：\n");
                sb.append("标题：").append(r.get("title")).append("\n");
                sb.append("内容：").append(r.get("content")).append("\n");
                if (r.get("tags") != null) {
                    sb.append("标签：").append(r.get("tags")).append("\n");
                }
                sb.append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("[RAGAgent] 检索异常", e);
            return "";
        }
    }


    public String generateWithContext(String userQuery, String retrievedContent) {
        log.info("[RAGAgent] 结合上下文生成回答");

        PromptTemplate template = PromptTemplate.from(
            "你是一个专业的法律合同助手。请根据以下检索到的相关知识，回答用户的问题。\n\n" +
            "【检索到的知识】\n{{retrievedContent}}\n\n" +
            "【用户问题】\n{{userQuery}}\n\n" +
            "【回答】"
        );

        Prompt prompt = template.apply(Map.of(
            "retrievedContent", retrievedContent.isEmpty() ? "没有找到相关知识，请基于你的法律知识回答。" : retrievedContent,
            "userQuery", userQuery
        ));

        String response = chatLanguageModel.generate(prompt.text());
        return response;
    }
}
