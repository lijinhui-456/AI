package com.sqnu.ai.service;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sqnu.ai.Entity.loginEntity;
import com.sqnu.ai.Mapper.AiLoginMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AiLoginServiceImpl extends ServiceImpl<AiLoginMapper, loginEntity> implements AiLoginService {
    @Autowired
    private AiLoginMapper aiLoginMapper;
    @Override
    public loginEntity chatLogin(long zhanghao, String password) {
        QueryWrapper<loginEntity> queryWrapper = new QueryWrapper<loginEntity>();
        queryWrapper.eq("zhanghao",zhanghao).eq("password",password);
        return aiLoginMapper.selectOne(queryWrapper);
    }
     @Override
    public String zhuce(long zhanghao,String password ) {
       QueryWrapper<loginEntity> queryWrapper=new QueryWrapper<>();
       queryWrapper.eq("zhanghao",zhanghao);
       boolean zh=aiLoginMapper.exists(queryWrapper);
       if(zh==true){
           return "账号已存在";
       }else{
         loginEntity lo=  new loginEntity();
         lo.setZhanghao(zhanghao);
         lo.setPassword(password);
        int result= aiLoginMapper.insert(lo);
         if(result>0){
             return "注册成功";
         }else {
             return "注册失败";
         }
       }
    }
}
