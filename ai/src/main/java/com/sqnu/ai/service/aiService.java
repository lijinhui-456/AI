package com.sqnu.ai.service;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.data.message.*;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.*;
@Service
public class aiService {
    private  final Logger log = LoggerFactory.getLogger(aiService.class);
    @Autowired
    private StreamingChatModel streamingChatLanguageModel;
    public void chat(ChatMemory chatMemory,UserMessage userMessage,SseEmitter emitter) {
                chatMemory.add(userMessage);
                streamingChatLanguageModel.chat(chatMemory.messages(), new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String s) {
                try {
                    emitter.send(SseEmitter.event().data(s));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }

            @Override
            public void onCompleteResponse(ChatResponse chatResponse) {
                try {
                    emitter.send(SseEmitter.event().data("[DONE]"));
                    chatMemory.add(chatResponse.aiMessage());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                emitter.complete();
            }

            @Override
            public void onError(Throwable throwable) {
                 emitter.completeWithError(throwable);
            }
        });

    }
    @Autowired
    private ChatMemoryProvider chatMemoryProvider;
    @Autowired
    @Qualifier("imgchatmodel")
    private StreamingChatModel imgchatmodel;
    public void chatWithImageStream(String id, MultipartFile file, String question,SseEmitter emitter) throws IOException {
          var chatmemory= chatMemoryProvider.get(id);
         Image base64= filetobase(file);
         UserMessage userMessage=UserMessage.from(
                          TextContent.from(question),
                 ImageContent.from(base64)
         );
         chatmemory.add(userMessage);
        imgchatmodel.chat(chatmemory.messages(), new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String s) {
                try {
                    emitter.send(SseEmitter.event().data(s));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onCompleteResponse(ChatResponse chatResponse) {
                try {
                    if(chatResponse==null){
                        System.out.println("ai返回的为空");
                    }
                    emitter.send(SseEmitter.event().data("[DONE]"));

                    chatmemory.add(chatResponse.aiMessage());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                emitter.complete();
            }

            @Override
            public void onError(Throwable throwable) {
                  emitter.completeWithError(throwable);
            }
        });
    }
    public Image filetobase(MultipartFile file) throws IOException {
        byte[] content=file.getBytes();
        String base64=Base64.getEncoder().encodeToString(content);
        String type=file.getContentType();
        if(type==null){
            type="image/png";
        }
        return Image.builder()
                .base64Data(base64)
                .mimeType(type)
                .build();
    }
}