package com.legal.contract.controller;

import com.legal.contract.common.Result;
import com.legal.contract.dto.AiChatDto;
import com.legal.contract.dto.ContractGenerateDto;
import com.legal.contract.entity.ChatMessage;
import com.legal.contract.service.AiService;
import com.legal.contract.service.agent.ContractAnalysisAgent.AnalysisResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    /**
     * AI聊天（同步）
     */
    @PostMapping("/chat")
    public Result<String> chat(@RequestBody AiChatDto dto,
                               @RequestAttribute Long userId) {
        String response = aiService.chat(dto, userId);
        return Result.success(response);
    }

    /**
     * AI聊天（流式SSE）
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody AiChatDto dto,
                                 @RequestAttribute Long userId) {
        return aiService.chatStream(dto, userId);
    }

    /**
     * 生成合同
     */
    @PostMapping("/generate-contract")
    public Result<String> generateContract(@Valid @RequestBody ContractGenerateDto dto,
                                           @RequestAttribute Long userId) {
        String response = aiService.generateContract(dto, userId);
        return Result.success(response);
    }

    /**
     * 分析合同风险
     */
    @PostMapping("/analyze/{contractId}")
    public Result<AnalysisResult> analyzeContract(@PathVariable Long contractId,
                                                  @RequestAttribute Long userId) {
        AnalysisResult result = aiService.analyzeContract(contractId, userId);
        return Result.success(result);
    }

    /**
     * 获取聊天历史
     */
    @GetMapping("/history/{sessionId}")
    public Result<List<ChatMessage>> getChatHistory(@PathVariable String sessionId) {
        List<ChatMessage> history = aiService.getChatHistory(sessionId);
        return Result.success(history);
    }
}
