package com.sqnu.ai.Aionfig;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class AiMemoryConfig {
    @Autowired
    private AiMysqlChatStore aiMysqlChatStore;
   @Bean
    public ChatMemoryStore chatMemoryStore(){
        return aiMysqlChatStore;
  }
    private final Map<String, MessageWindowChatMemory> userMemoryMap = new ConcurrentHashMap<>();

    @Bean
    public ChatMemoryProvider chatMemoryProvider() {
        return memoryId -> {
            // 同一个用户 ID → 永远返回同一个记忆！！！
            return userMemoryMap.computeIfAbsent((String) memoryId, id ->
                    MessageWindowChatMemory.builder()
                            .id(id)
                            .maxMessages(3)
                            .chatMemoryStore( chatMemoryStore())
                            .build()
            );
        };
    }
}
