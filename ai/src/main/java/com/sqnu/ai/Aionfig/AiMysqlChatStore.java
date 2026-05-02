package com.sqnu.ai.Aionfig;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sqnu.ai.Entity.AiChatEntity;
import com.sqnu.ai.Mapper.AiChatMemoryMapper;
import com.sqnu.ai.service.AiChatMemoryService;
import dev.langchain4j.data.message.*;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
@Component
public class AiMysqlChatStore implements ChatMemoryStore {
    @Autowired
    private AiChatMemoryService aiChatMemoryService;
    @Autowired
    private AiChatMemoryMapper aiChatMemoryMapper;
    //获取mysql中指定用户id的会话记忆
    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String memId = (String) memoryId;
        QueryWrapper<AiChatEntity> wrapper=new QueryWrapper<>();
        wrapper.eq("user_Id",memId);
        // 从数据库查询该用户的所有历史消息
        List<AiChatEntity> records = aiChatMemoryMapper.selectList(wrapper);
        List<ChatMessage> messages=new ArrayList<>();
        for(AiChatEntity record:records){
           String type=record.getMessageType();
           String message=record.getMessage();
           if("ai".equals(type)){
               messages.add(AiMessage.from(message));
           }else if("user".equals(type)){
               messages.add(UserMessage.from(message));
           }
        }
        return messages;
    }
    // 把当前会话的所有消息存到MySQL
    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {

        String memId = (String) memoryId;
        // 先清空旧消息，再批量插入新消息
        aiChatMemoryMapper.delete(new QueryWrapper<AiChatEntity>().eq("user_Id",memId));
        if(messages==null||messages.isEmpty()){
            return;
        }
        List<AiChatEntity> records=new ArrayList<>();
        for(ChatMessage msg:messages){
          if(!(msg instanceof AiMessage) && !(msg instanceof UserMessage)){
              continue;
          }
          AiChatEntity en=new AiChatEntity();
          en.setUserId(memId);
          if(msg instanceof AiMessage){
              en.setMessageType("ai");
              en.setMessage(((AiMessage) msg).text());
          }else if(msg instanceof UserMessage){
              en.setMessageType("user");
            UserMessage userMessage=(UserMessage) msg;
            StringBuilder sb=new StringBuilder();
            for(var item:userMessage.contents()){
                if(item instanceof TextContent){
                    sb.append(((TextContent) item).text()).append('\n');
                } else if (item instanceof ImageContent) {
                    sb.append(((ImageContent) item).image()).append('\n');
                }
            }
            en.setMessage(sb.toString());
          }
          records.add(en);
        }
     if(!(records.isEmpty())){
         aiChatMemoryService.saveBatch(records);
     }
    }

    //   清空某个会话的所有消息
    @Override
    public void deleteMessages(Object memoryId) {
        aiChatMemoryMapper.deleteById((String)memoryId);
    }
}
