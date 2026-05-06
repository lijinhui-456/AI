package com.sqnu.ai.Entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.springframework.stereotype.Component;
@Component
@TableName("login")
@Data
public class loginEntity {
    @TableId(type = IdType.AUTO)
    Integer id;
    long zhanghao;
    String password;
}
