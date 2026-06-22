package com.legal.contract.service.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 合同分析Agent - 负责分析合同风险
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContractAnalysisAgent {

    private final ChatLanguageModel chatLanguageModel;
    private final ObjectMapper objectMapper = new ObjectMapper();


    public AnalysisResult analyze(String contractTitle, String contractType, String content) {
        log.info("[ContractAnalysisAgent] 分析合同: title={}", contractTitle);

        String prompt = buildAnalysisPrompt(contractTitle, contractType, content);

        String response = chatLanguageModel.generate(prompt);
        log.info("[ContractAnalysisAgent] LLM返回长度: {}", response.length());

        return parseAnalysisResult(response);
    }

    private String buildAnalysisPrompt(String contractTitle, String contractType, String content) {
        return """
            你是一个专业的法律合同风险分析专家。请分析以下合同的风险。

            合同标题：%s
            合同类型：%s
            合同内容：
            %s

            请仔细分析合同中的风险条款，并返回JSON格式的分析结果：
            {
                "riskLevel": "低/中/高/严重",
                "riskScore": 1-100的数字评分,
                "risks": [
                    {
                        "clause": "具体条款内容",
                        "riskType": "风险类型如：霸王条款/模糊条款/缺失条款/高风险条款",
                        "riskLevel": "低/中/高/严重",
                        "description": "风险描述",
                        "suggestion": "修改建议"
                    }
                ]
            }

            请直接返回JSON，不要有其他文字。
            """.formatted(contractTitle, contractType, content);
    }

    private AnalysisResult parseAnalysisResult(String response) {
        AnalysisResult result = new AnalysisResult();
        result.setRawResponse(response);

        try {
            // 提取JSON
            String jsonStr = extractJson(response);
            if (jsonStr != null && !jsonStr.isEmpty()) {
                JsonNode node = objectMapper.readTree(jsonStr);

                result.setRiskLevel(node.has("riskLevel") ? node.get("riskLevel").asText() : "中");
                result.setRiskScore(node.has("riskScore") ? node.get("riskScore").asInt() : 50);

                if (node.has("risks") && node.get("risks").isArray()) {
                    List<RiskItem> risks = new ArrayList<>();
                    for (JsonNode riskNode : node.get("risks")) {
                        RiskItem risk = new RiskItem();
                        risk.setClause(riskNode.has("clause") ? riskNode.get("clause").asText() : "");
                        risk.setRiskType(riskNode.has("riskType") ? riskNode.get("riskType").asText() : "");
                        risk.setRiskLevel(riskNode.has("riskLevel") ? riskNode.get("riskLevel").asText() : "中");
                        risk.setDescription(riskNode.has("description") ? riskNode.get("description").asText() : "");
                        risk.setSuggestion(riskNode.has("suggestion") ? riskNode.get("suggestion").asText() : "");
                        risks.add(risk);
                    }
                    result.setRisks(risks);
                }
            }
        } catch (Exception e) {
            log.error("[ContractAnalysisAgent] 解析结果异常", e);
            // 解析失败时返回默认结果
            result.setRiskLevel("中");
            result.setRiskScore(50);
            result.setRisks(new ArrayList<>());

            // 尝试从原始文本中提取风险信息
            extractRisksFromText(response, result);
        }

        return result;
    }

    private String extractJson(String text) {
        int start = text.indexOf("{");
        int end = text.lastIndexOf("}");
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return null;
    }

    private void extractRisksFromText(String text, AnalysisResult result) {
        // 简单的文本解析作为后备
        if (text.contains("高风险") || text.contains("严重")) {
            result.setRiskLevel("高");
            result.setRiskScore(80);
        } else if (text.contains("中风险")) {
            result.setRiskLevel("中");
            result.setRiskScore(50);
        } else {
            result.setRiskLevel("低");
            result.setRiskScore(20);
        }
    }

    /**
     * 分析结果封装类
     */
    @lombok.Data
    public static class AnalysisResult {
        private String riskLevel;
        private Integer riskScore;
        private List<RiskItem> risks;
        private String rawResponse;
    }

    /**
     * 风险项
     */
    @lombok.Data
    public static class RiskItem {
        private String clause;
        private String riskType;
        private String riskLevel;
        private String description;
        private String suggestion;
    }
}
