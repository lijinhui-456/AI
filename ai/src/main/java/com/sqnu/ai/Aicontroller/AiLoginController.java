package com.sqnu.ai.Aicontroller;
import com.sqnu.ai.Entity.loginEntity;
import com.sqnu.ai.service.AiLoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;
@RestController
public class AiLoginController {
    @Autowired
    private AiLoginService loservice;
    @PostMapping("/login")
    public Map login(@RequestBody loginEntity loginEntity){
        Map<String,Object> map=new HashMap<>();
       long zhanghao= loginEntity.getZhanghao();
       String password=loginEntity.getPassword();
       com.sqnu.ai.Entity.loginEntity login =loservice.chatLogin(zhanghao,password);
        if(login!=null){
             map.put("code","200");
             map.put("id",login.getId());
             return map;
        }else {
            map.put("code","400");
            return map;
        }
    }
    @PostMapping("/zhuce")
    public Map zhuce(@RequestBody loginEntity loginEntity){
        Map<String,Object> map=new HashMap<>();
        long zhanghao= loginEntity.getZhanghao();
        String password=loginEntity.getPassword();
        String zhu=loservice.zhuce(zhanghao,password);
        if(zhu.contains("成功")){
            map.put("code","200");
        }
        else{
            map.put("code","400");
        }
        return map;
    }
}
