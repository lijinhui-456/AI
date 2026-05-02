package com.sqnu.ai.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sqnu.ai.Entity.AiChatEntity;
import com.sqnu.ai.Mapper.AiChatMemoryMapper;
import org.springframework.stereotype.Service;

@Service
public class AiChatMemoryServicrImpl extends ServiceImpl<AiChatMemoryMapper, AiChatEntity> implements AiChatMemoryService {
}
