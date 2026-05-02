package com.sqnu.ai.Aicontroller;
import com.sqnu.ai.Entity.AiEntity;
import com.sqnu.ai.service.aiService;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.logging.Logger;
@RestController
@RequiredArgsConstructor
public class aicontroller {
    private static final Logger log = Logger.getLogger(aicontroller.class.getName());
    private final aiService aiservice;
    private final Executor sseExecutor;
    private final ChatMemoryProvider chatMemoryProvider;
    @PostMapping(value = "/ai/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter aiChat(@RequestBody AiEntity entity)
    {
        String currentId =entity.getId();
        var chatmemory= chatMemoryProvider.get(currentId);
        UserMessage userMessage=UserMessage.from(entity.getMessage());
        SseEmitter emitter = new SseEmitter(60000L);

        emitter.onTimeout(() -> emitter.completeWithError(new RuntimeException("超时")));
        emitter.onError(e -> emitter.completeWithError(e));
        sseExecutor.execute(() -> {
            aiservice.chat(chatmemory,userMessage,emitter);
        });
        return emitter;
    }

    @PostMapping(value = "/ai/chat-with-image", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatWithImage(@RequestParam("id") String id,@RequestParam("image") MultipartFile file,@RequestParam("message") String question) {
        SseEmitter emitter = new SseEmitter(60000L);
        emitter.onTimeout(() -> emitter.completeWithError(new RuntimeException("超时")));
        emitter.onError(e -> emitter.completeWithError(e));
        sseExecutor.execute(() -> {
            try {
                aiservice.chatWithImageStream(id,file,question,emitter);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        return emitter;
    }
}