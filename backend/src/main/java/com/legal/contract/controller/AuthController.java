package com.legal.contract.controller;

import com.legal.contract.common.Result;
import com.legal.contract.dto.LoginDto;
import com.legal.contract.dto.RegisterDto;
import com.legal.contract.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginDto dto) {
        Map<String, Object> result = authService.login(dto);
        return Result.success(result);
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDto dto) {
        authService.register(dto);
        return Result.success();
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/info")
    public Result<Map<String, Object>> getUserInfo(@RequestAttribute Long userId) {
        Map<String, Object> userInfo = authService.getUserInfo(userId);
        return Result.success(userInfo);
    }

    /**
     * 刷新Token
     */
    @GetMapping("/refresh")
    public Result<Map<String, Object>> refreshToken(@RequestAttribute Long userId,
                                                     @RequestAttribute String username,
                                                     @RequestAttribute String role) {
        Map<String, Object> result = authService.getUserInfo(userId);
        return Result.success(result);
    }
}