package com.sqnu.ai.service;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sqnu.ai.Entity.loginEntity;
public interface AiLoginService extends IService<loginEntity> {
    public loginEntity chatLogin(long zhanghao, String psd);
    public String zhuce(long zhanghao,String psd );
}


