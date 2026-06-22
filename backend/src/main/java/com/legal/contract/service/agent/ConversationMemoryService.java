package com.legal.contract.service.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 对话记忆服务 — 使用Redis存储会话历史
 * <p>
 * 采用StringRedisTemplate以列表形式存储对话消息
 */
@Slf4j
@Component
public class ConversationMemoryService {

    private static final String KEY_PREFIX = "chat:history:";
    private static final long SESSION_TTL_HOURS = 24;

    private final StringRedisTemplate stringRedisTemplate;

    public ConversationMemoryService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }


    public void addMessage(String sessionId, String role, String content) {
        log.debug("[ConversationMemoryService] 添加消息, sessionId={}, role={}", sessionId, role);

        if (sessionId == null || sessionId.isBlank()) {
            log.warn("[ConversationMemoryService] sessionId为空，跳过");
            return;
        }
        if (content == null || content.isBlank()) {
            log.warn("[ConversationMemoryService] 消息内容为空，跳过");
            return;
        }

        try {
            String key = buildKey(sessionId);
            String message = role + "::" + content;

            // 右侧推入列表
            stringRedisTemplate.opsForList().rightPush(key, message);

            // 设置过期时间
            stringRedisTemplate.expire(key, SESSION_TTL_HOURS, TimeUnit.HOURS);

        } catch (Exception e) {
            log.error("[ConversationMemoryService] 添加消息异常", e);
        }
    }


    public List<String> getHistory(String sessionId, int limit) {
        log.debug("[ConversationMemoryService] 获取历史, sessionId={}, limit={}", sessionId, limit);

        if (sessionId == null || sessionId.isBlank()) {
            return Collections.emptyList();
        }

        try {
            String key = buildKey(sessionId);

            // 获取列表长度
            Long size = stringRedisTemplate.opsForList().size(key);
            if (size == null || size == 0) {
                return Collections.emptyList();
            }

            // 计算起始索引（取最近limit条）
            int start = Math.max(0, (int) (size - limit));
            int end = (int) (size - 1);

            List<String> messages = stringRedisTemplate.opsForList().range(key, start, end);

            return messages != null ? messages : Collections.emptyList();

        } catch (Exception e) {
            log.error("[ConversationMemoryService] 获取历史异常", e);
            return Collections.emptyList();
        }
    }


    public String getFormattedHistory(String sessionId, int limit) {
        List<String> messages = getHistory(sessionId, limit);
        if (messages.isEmpty()) {
            return "";
        }

        return messages.stream()
                .map(msg -> {
                    int separatorIndex = msg.indexOf("::");
                    if (separatorIndex > 0) {
                        String role = msg.substring(0, separatorIndex);
                        String content = msg.substring(separatorIndex + 2);
                        return switch (role) {
                            case "user" -> "用户: " + content;
                            case "assistant" -> "助手: " + content;
                            case "system" -> "系统: " + content;
                            default -> msg;
                        };
                    }
                    return msg;
                })
                .collect(Collectors.joining("\n"));
    }


    public void clearSession(String sessionId) {
        log.info("[ConversationMemoryService] 清空会话, sessionId={}", sessionId);

        if (sessionId == null || sessionId.isBlank()) {
            return;
        }

        try {
            String key = buildKey(sessionId);
            stringRedisTemplate.delete(key);
            log.info("[ConversationMemoryService] 会话已清空: {}", sessionId);
        } catch (Exception e) {
            log.error("[ConversationMemoryService] 清空会话异常", e);
        }
    }


    public long getMessageCount(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return 0;
        }

        try {
            String key = buildKey(sessionId);
            Long size = stringRedisTemplate.opsForList().size(key);
            return size != null ? size : 0;
        } catch (Exception e) {
            log.error("[ConversationMemoryService] 获取消息数量异常", e);
            return 0;
        }
    }


    private String buildKey(String sessionId) {
        return KEY_PREFIX + sessionId;
    }
}