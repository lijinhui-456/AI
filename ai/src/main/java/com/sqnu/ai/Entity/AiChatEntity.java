package com.sqnu.ai.Entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
@Component
@TableName("chat_history")
@Data
public class AiChatEntity {
    @TableId(type = IdType.AUTO)
   private Integer id;
   private String userId;
   private String messageType;
   private String message;
   private LocalDateTime createTime;
}
