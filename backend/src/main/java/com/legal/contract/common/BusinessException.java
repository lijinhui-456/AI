package com.legal.contract.common;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 业务异常
 * <p>
 * 手动定义构造函数以正确设置 super(message)，确保 e.getMessage() 返回有效文本。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BusinessException extends RuntimeException {
    private int code;

    public BusinessException(String message) {
        super(message);
        this.code = 400;
    }


    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public static BusinessException of(int code, String message) {
        return new BusinessException(code, message);
    }

    public static BusinessException badRequest(String message) {
        return new BusinessException(400, message);
    }
}