package com.enterprise.rag.api;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 企业级RAG文档问答系统 - SpringBoot启动类
 *
 * @author RAG-Enterprise-Team
 * @version 1.0.0
 */
@EnableAsync
@EnableScheduling
@EnableTransactionManagement
@MapperScan("com.enterprise.rag.infrastructure.mapper")
@SpringBootApplication(scanBasePackages = "com.enterprise.rag")
public class RagApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(RagApiApplication.class, args);
    }
}
