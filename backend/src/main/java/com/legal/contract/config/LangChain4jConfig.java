package com.legal.contract.config;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiTokenizer;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Data
@Configuration
@ConfigurationProperties(prefix = "langchain4j.open-ai.chat-model")
public class LangChain4jConfig {

    private String baseUrl;
    private String apiKey;
    private String modelName;
    private Double temperature;
    private Integer maxTokens;
    private Boolean logRequests;
    private Boolean logResponses;

    @Bean
    public OpenAiChatModel openAiChatModel() {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .timeout(Duration.ofSeconds(60))
                .build();
    }


    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return openAiChatModel();
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        return OpenAiEmbeddingModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName("text-embedding-v1")
                .timeout(Duration.ofSeconds(60))
                .build();
    }

    /**
     * 聊天记忆提供者
     * <p>
     * 使用 TokenWindowChatMemory，最多保留10个token的对话历史
     */
    @Bean
    public ChatMemoryProvider chatMemoryProvider() {
        return chatId -> TokenWindowChatMemory.builder()
                .maxTokens(10,new OpenAiTokenizer())
                .id(chatId)
                .build();
    }
}