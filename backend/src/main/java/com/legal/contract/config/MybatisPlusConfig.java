package com.legal.contract.config;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Configuration
@MapperScan("com.legal.contract.mapper")
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return interceptor;
    }

    @Bean
    public com.baomidou.mybatisplus.core.handlers.MetaObjectHandler metaObjectHandler() {
        return new com.baomidou.mybatisplus.core.handlers.MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                this.strictInsertFill(metaObject, "createdTime", LocalDateTime::now, LocalDateTime.class);
                this.strictInsertFill(metaObject, "updatedTime", LocalDateTime::now, LocalDateTime.class);
                this.strictInsertFill(metaObject, "deleted", () -> 0, Integer.class);
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                this.strictUpdateFill(metaObject, "updatedTime", LocalDateTime::now, LocalDateTime.class);
            }
        };
    }
}