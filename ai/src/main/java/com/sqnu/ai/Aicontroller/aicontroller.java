package com.sqnu.ai.Aicontroller;
import com.sqnu.ai.Entity.AiEntity;
import com.sqnu.ai.service.AiEmbeddingService;
import com.sqnu.ai.service.aiService;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
@RestController
@RequiredArgsConstructor
public class aicontroller {
    private final AiEmbeddingService aiEmbeddingService;
    private static final Logger log = Logger.getLogger(aicontroller.class.getName());
    private final aiService aiservice;
    private final Executor sseExecutor;
    private final ChatMemoryProvider chatMemoryProvider;
    @PostMapping(value = "/ai/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter aiChat(@RequestBody AiEntity entity) throws IOException {   String query=entity.getMessage();
        String currentId =entity.getId();
        aiEmbeddingService.add();
        var chatmemory= chatMemoryProvider.get(currentId);
        AtomicReference<String> finalPrompt = new AtomicReference<>();
        SseEmitter emitter = new SseEmitter(60000L);
       List<String> searchResult= aiEmbeddingService.search(query);
        emitter.onTimeout(() -> emitter.completeWithError(new RuntimeException("超时")));
        emitter.onError(e -> emitter.completeWithError(e));
        sseExecutor.execute(() -> {
            if(searchResult!=null&&!searchResult.isEmpty()){
                String content= String.join("\n",searchResult);
               finalPrompt.set("你是一个专业的AI助手，请根据已知信息，完整，连贯，自然的回答问题，如果信息中不包含答案，请根据你的常识回答:\n" +
                       "已知信息:\n" + content + "\n" + "用户问题:\n" + query);
            }else{
                finalPrompt.set("知识库中未找到相关信息\n"+"请根据你自己的通用知识和常识回答问题\n"+"如果无法回答，请礼貌告诉用户你不知道\n"+"用户信息:"+query);
            }
            UserMessage userMessage=UserMessage.from(finalPrompt.get());
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