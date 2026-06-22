package com.legal.contract.config;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Milvus 向量数据库配置
 * <p>
 * 从 application.yml 读取 milvus 配置并创建 MilvusClient 连接
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "milvus")
public class MilvusConfig {
    private  String token;
    private  String uri;

    /**
     * 集合名称
     */
    private String collectionName;


    private int dimension;

    @Bean
    public MilvusServiceClient milvusClient() {
        ConnectParam connectParam = ConnectParam.newBuilder()
                .withToken(token)
                .withUri(uri)
                .build();
        return new MilvusServiceClient(connectParam);
    }
}