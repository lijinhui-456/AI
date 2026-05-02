package com.sqnu.ai.Aionfig;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class AiConfig {

    @Value("${aliyun.api-key}")
    private String apiKey;

    @Value("${aliyun.model}")
    private String model;
    @Bean
    public StreamingChatModel streamingChatLanguageModel() {
            return OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .modelName(model)
                .build();
    }

    @Bean(name = "imgchatmodel")
    public StreamingChatModel streamingChatModel() {
        return OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .modelName("qwen-vl-plus")
                .build();
    }

}