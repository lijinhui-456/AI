package com.legal.contract.service;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.legal.contract.common.BusinessException;
import com.legal.contract.common.JwtUtil;
import com.legal.contract.dto.LoginDto;
import com.legal.contract.dto.RegisterDto;
import com.legal.contract.entity.User;
import com.legal.contract.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    /**
     * 用户登录
     */
    public Map<String, Object> login(LoginDto dto) {
        log.info("用户登录: {}", dto.getUsername());

        // 查询用户
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, dto.getUsername());
        User user = userMapper.selectOne(queryWrapper);

        if (user == null) {
            throw BusinessException.badRequest("用户名或密码错误");
        }

        // 验证密码
        String salt = user.getPassword().split("\\$")[0];
        String encryptedPassword = encryptPassword(dto.getPassword(), salt);
        if (!encryptedPassword.equals(user.getPassword())) {
            throw BusinessException.badRequest("用户名或密码错误");
        }

        // 检查用户状态
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw BusinessException.badRequest("账户已被禁用");
        }

        // 生成JWT Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        // 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("tokenType", "Bearer");

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("nickname", user.getNickname());
        userInfo.put("email", user.getEmail());
        userInfo.put("avatar", user.getAvatar());
        userInfo.put("role", user.getRole());
        result.put("userInfo", userInfo);

        return result;
    }

    /**
     * 用户注册
     */
    public void register(RegisterDto dto) {
        log.info("用户注册: {}", dto.getUsername());

        // 检查用户名唯一性
        LambdaQueryWrapper<User> usernameQuery = new LambdaQueryWrapper<>();
        usernameQuery.eq(User::getUsername, dto.getUsername());
        Long count = userMapper.selectCount(usernameQuery);
        if (count > 0) {
            throw BusinessException.badRequest("用户名已存在");
        }

        // 检查邮箱唯一性
        if (dto.getEmail() != null && !dto.getEmail().isEmpty()) {
            LambdaQueryWrapper<User> emailQuery = new LambdaQueryWrapper<>();
            emailQuery.eq(User::getEmail, dto.getEmail());
            Long emailCount = userMapper.selectCount(emailQuery);
            if (emailCount > 0) {
                throw BusinessException.badRequest("邮箱已被注册");
            }
        }

        // 创建用户
        User user = new User();
        user.setUsername(dto.getUsername());
        // 使用SHA-256加盐加密密码
        String salt = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        user.setPassword(encryptPassword(dto.getPassword(), salt));
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setRole("USER");
        user.setStatus(1);

        userMapper.insert(user);
        log.info("用户注册成功: {}", dto.getUsername());
    }

    /**
     * 获取用户信息
     */
    public Map<String, Object> getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.badRequest("用户不存在");
        }

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("nickname", user.getNickname());
        userInfo.put("email", user.getEmail());
        userInfo.put("avatar", user.getAvatar());
        userInfo.put("role", user.getRole());
        userInfo.put("status", user.getStatus());
        userInfo.put("createdTime", user.getCreatedTime());

        return userInfo;
    }

    /**
     * 使用SHA-256加盐加密密码
     * 格式: salt$hashedPassword
     */
    private String encryptPassword(String password, String salt) {
        String saltedPassword = salt + password;
        String hash = DigestUtil.sha256Hex(saltedPassword);
        return salt + "$" + hash;
    }
}