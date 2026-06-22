package com.legal.contract.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 通用API响应封装
 *
 * @param <T> 数据类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    private int code;


    private String message;

    private T data;
    public static <T> Result<T> success() {
        return Result.<T>builder()
                .code(200)
                .message("success")
                .build();
    }

    public static <T> Result<T> success(T data) {
        return Result.<T>builder()
                .code(200)
                .message("success")
                .data(data)
                .build();
    }


    public static <T> Result<T> error(int code, String message) {
        return Result.<T>builder()
                .code(code)
                .message(message)
                .build();
    }


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Pager<T> {


        private int page;

        private int size;

        private long total;

        private List<T> records;
    }
}