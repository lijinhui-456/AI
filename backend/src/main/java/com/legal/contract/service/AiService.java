package com.legal.contract.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.legal.contract.dto.AiChatDto;
import com.legal.contract.dto.ContractGenerateDto;
import com.legal.contract.entity.ChatMessage;
import com.legal.contract.entity.Contract;
import com.legal.contract.entity.ContractRisk;
import com.legal.contract.mapper.ChatMessageMapper;
import com.legal.contract.mapper.ContractMapper;
import com.legal.contract.service.agent.ContractAnalysisAgent;
import com.legal.contract.service.agent.ContractAnalysisAgent.AnalysisResult;
import com.legal.contract.service.agent.ConversationMemoryService;
import com.legal.contract.service.agent.RAGAgent;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final RAGAgent ragAgent;
    private final ContractAnalysisAgent contractAnalysisAgent;
    private final ConversationMemoryService conversationMemoryService;
    private final ContractService contractService;
    private final ContractRiskService contractRiskService;
    private final ChatMessageMapper chatMessageMapper;
    private final ContractMapper contractMapper;
    private final ChatLanguageModel chatLanguageModel;

    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    /**
     * AI聊天（同步方式）
     */
    public String chat(AiChatDto dto, Long userId) {
        log.info("AI聊天: sessionId={}, userId={}", dto.getSessionId(), userId);
        saveChatMessage(userId, dto.getSessionId(), "user", dto.getContent());

        // RAG检索
        String retrieved = ragAgent.retrieve(dto.getContent(), 3);
        // 生成回答
        String response = ragAgent.generateWithContext(dto.getContent(), retrieved);

        saveChatMessage(userId, dto.getSessionId(), "assistant", response);
        return response;
    }

    /**
     * AI聊天（流式方式 - SSE）
     */
    public SseEmitter chatStream(AiChatDto dto, Long userId) {
        SseEmitter emitter = new SseEmitter(1800000L);

        executorService.execute(() -> {
            try {
                emitter.send(SseEmitter.event().name("start").data("{\"status\":\"started\"}"));
                conversationMemoryService.addMessage(dto.getSessionId(), "user", dto.getContent());
                saveChatMessage(userId, dto.getSessionId(), "user", dto.getContent());

                emitter.send(SseEmitter.event().name("thinking").data("{\"status\":\"thinking\"}"));

                // RAG检索
                String retrieved = ragAgent.retrieve(dto.getContent(), 3);
                // 生成回答
                String response = ragAgent.generateWithContext(dto.getContent(), retrieved);

                if (response == null || response.isBlank()) {
                    response = "您好，我是法务合同智能助手。请问有什么法律问题或合同相关的事情我可以帮助您？";
                }
                log.info("[AiService] SSE发送内容, 长度={}", response.length());

                List<String> semanticChunks = splitByPunctuation(response);
                if (semanticChunks.size() < 5) {
                    semanticChunks.clear();
                    int charSize = 3;
                    for (int i = 0; i < response.length(); i += charSize) {
                        semanticChunks.add(response.substring(i, Math.min(i + charSize, response.length())));
                    }
                }

                for (String piece : semanticChunks) {
                    emitter.send(SseEmitter.event().name("content").data(piece));
                    long sleepMs = 30L + ((long) piece.length() * 8L);
                    Thread.sleep(Math.min(sleepMs, 120L));
                }

                conversationMemoryService.addMessage(dto.getSessionId(), "assistant", response);
                saveChatMessage(userId, dto.getSessionId(), "assistant", response);

                emitter.send(SseEmitter.event().name("end").data("{\"status\":\"completed\"}"));
                emitter.complete();

            } catch (Exception e) {
                log.error("AI流式聊天异常", e);
                try {
                    emitter.send(SseEmitter.event().name("error")
                            .data("{\"error\":\"" + e.getMessage() + "\"}"));
                } catch (Exception ex) {
                    log.warn("发送错误事件失败", ex);
                }
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    /**
     * 按中文标点符号切句子，让输出更自然
     */
    private List<String> splitByPunctuation(String text) {
        List<String> result = new java.util.ArrayList<>();
        if (text == null || text.isEmpty()) return result;

        int len = text.length();
        int start = 0;
        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            if (c == '。' || c == '！' || c == '？' || c == '，' ||
                    c == '；' || c == '：' || c == '\n' || c == '、' ||
                    c == '）' || c == '》' || c == '」' || c == '“' || c == '”') {
                if (i > start) {
                    result.add(text.substring(start, i + 1));
                } else {
                    result.add(String.valueOf(c));
                }
                start = i + 1;
            }
        }
        if (start < len) {
            result.add(text.substring(start));
        }
        List<String> finalResult = new java.util.ArrayList<>();
        for (String s : result) {
            if (s.length() > 12) {
                for (int i = 0; i < s.length(); i += 6) {
                    finalResult.add(s.substring(i, Math.min(i + 6, s.length())));
                }
            } else {
                finalResult.add(s);
            }
        }
        return finalResult;
    }

    /**
     * 生成合同
     */
    public String generateContract(ContractGenerateDto dto, Long userId) {
        log.info("生成合同: userId={}, contractType={}", userId, dto.getContractType());

        StringBuilder prompt = new StringBuilder();
        prompt.append("请生成一份").append(dto.getContractType()).append("合同。\n");
        prompt.append("合同方信息：\n");
        if (dto.getParties() != null) {
            for (ContractGenerateDto.PartyInfo party : dto.getParties()) {
                prompt.append("- ").append(party.getRole()).append(": ").append(party.getName()).append("\n");
            }
        }
        prompt.append("标的物：").append(dto.getSubjectMatter()).append("\n");
        if (dto.getDuration() != null && !dto.getDuration().isEmpty()) {
            prompt.append("合同期限：").append(dto.getDuration()).append("\n");
        }
        if (dto.getAmount() != null && !dto.getAmount().isEmpty()) {
            prompt.append("合同金额：").append(dto.getAmount()).append("\n");
        }
        if (dto.getSpecialClauses() != null && !dto.getSpecialClauses().isEmpty()) {
            prompt.append("特殊条款：").append(dto.getSpecialClauses()).append("\n");
        }

        String response = chatLanguageModel.generate(prompt.toString());
        return response;
    }

    /**
     * 分析合同风险
     */
    public AnalysisResult analyzeContract(Long contractId, Long userId) {
        log.info("分析合同风险: contractId={}, userId={}", contractId, userId);
        Contract contract = contractService.getById(contractId);

        // 获取合同内容，如果为空或占位符则从磁盘文件重新解析
        String contentText = contract.getContentText();
        boolean needsReParse = contentText == null || contentText.trim().isEmpty()
                || contentText.contains("待解析") || contentText.contains("<<BLOB>>")
                || contentText.startsWith("[提示");
        if (needsReParse) {
            log.info("contentText为空/占位符，从磁盘文件重新解析: {}", contract.getFileUrl());
            contentText = contractService.extractTextFromFileUrl(contract.getFileUrl());
        }

        // 调用合同分析Agent
        AnalysisResult result = contractAnalysisAgent.analyze(
                contract.getTitle(), contract.getType(), contentText);

        // 保存风险结果
        if (result.getRisks() != null && !result.getRisks().isEmpty()) {
            List<ContractRisk> risks = result.getRisks().stream()
                    .map(rr -> {
                        ContractRisk risk = new ContractRisk();
                        risk.setContractId(contractId);
                        risk.setClause(rr.getClause());
                        risk.setRiskType(rr.getRiskType());
                        risk.setRiskLevel(rr.getRiskLevel());
                        risk.setDescription(rr.getDescription());
                        risk.setSuggestion(rr.getSuggestion());
                        return risk;
                    })
                    .toList();
            contractRiskService.deleteByContractId(contractId);
            contractRiskService.saveRisks(contractId, risks);
        }

        // 更新合同状态
        LambdaUpdateWrapper<Contract> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(Contract::getStatus, "ANALYZED");
        if (result.getRiskLevel() != null) {
            updateWrapper.set(Contract::getRiskLevel, result.getRiskLevel());
        }
        if (result.getRiskScore() != null) {
            updateWrapper.set(Contract::getRiskScore, result.getRiskScore());
        }
        updateWrapper.eq(Contract::getId, contractId);
        contractMapper.update(null, updateWrapper);

        return result;
    }

    /**
     * 获取聊天历史
     */
    public List<ChatMessage> getChatHistory(String sessionId) {
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getSessionId, sessionId);
        wrapper.orderByAsc(ChatMessage::getCreatedTime);
        return chatMessageMapper.selectList(wrapper);
    }

    private void saveChatMessage(Long userId, String sessionId, String role, String content) {
        ChatMessage message = new ChatMessage();
        message.setUserId(userId);
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setMessageType("text");
        chatMessageMapper.insert(message);
    }
}
