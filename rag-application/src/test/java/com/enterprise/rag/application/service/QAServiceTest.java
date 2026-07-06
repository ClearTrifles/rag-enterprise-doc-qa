package com.enterprise.rag.application.service;

import com.alibaba.fastjson2.JSON;
import com.enterprise.rag.application.dto.QAAnswerResponse;
import com.enterprise.rag.application.dto.QAQuestionRequest;
import com.enterprise.rag.application.service.impl.QAServiceImpl;
import com.enterprise.rag.infrastructure.ai.QwenEmbeddingService;
import com.enterprise.rag.infrastructure.ai.QwenLLMService;
import com.enterprise.rag.infrastructure.mapper.RagChatRecordMapper;
import com.enterprise.rag.infrastructure.redis.RateLimiter;
import com.enterprise.rag.infrastructure.redis.RedisService;
import com.enterprise.rag.infrastructure.vector.ChromaDBClient;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * QAService单元测试
 * 
 * 测试重点：
 * 1. 高频问答Redis缓存生效，减少API调用费用
 * 2. LLM调用失败有兜底，服务不雪崩
 */
@ExtendWith(MockitoExtension.class)
class QAServiceTest {

    @Mock
    private QwenEmbeddingService embeddingService;

    @Mock
    private QwenLLMService llmService;

    @Mock
    private ChromaDBClient chromaDBClient;

    @Mock
    private RateLimiter rateLimiter;

    @Mock
    private RedisService redisService;

    @Mock
    private RagChatRecordMapper chatRecordMapper;

    @Mock
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private QAServiceImpl qaService;

    @BeforeEach
    void setUp() {
        qaService = new QAServiceImpl(
                embeddingService,
                llmService,
                chromaDBClient,
                rateLimiter,
                redisService,
                chatRecordMapper,
                circuitBreakerRegistry
        );

        ReflectionTestUtils.setField(qaService, "similarityThreshold", 0.75);
        ReflectionTestUtils.setField(qaService, "recallTopNum", 5);
        ReflectionTestUtils.setField(qaService, "chunkSize", 512);
        ReflectionTestUtils.setField(qaService, "chunkOverlap", 120);
        ReflectionTestUtils.setField(qaService, "cacheExpireSeconds", 3600);
    }

    /**
     * 【测试点1】高频问答Redis缓存生效，减少API调用费用
     * 
     * 测试场景：
     * - 第一次提问：调用Embedding和LLM，结果存入Redis
     * - 第二次提问相同问题：直接从Redis返回，不调用Embedding和LLM
     */
    @Test
    void testHighFrequencyQuestionCache() {
        String question = "什么是RAG?";
        String sessionId = "test-session-001";
        String cacheKey = "rag:qa:cache:" + DigestUtils.md5DigestAsHex(question.getBytes());

        QAAnswerResponse cachedResponse = QAAnswerResponse.builder()
                .answer("RAG是检索增强生成技术...")
                .sources(List.of())
                .responseTime(100)
                .status("SUCCESS")
                .timestamp(LocalDateTime.now())
                .build();

        // 模拟Redis缓存命中
        when(redisService.get(cacheKey)).thenReturn(JSON.toJSONString(cachedResponse));

        QAQuestionRequest request = QAQuestionRequest.builder()
                .question(question)
                .sessionId(sessionId)
                .build();

        QAAnswerResponse response = qaService.ask(request);

        // 验证缓存命中
        assertEquals("CACHE_HIT", response.getStatus());
        assertEquals("RAG是检索增强生成技术...", response.getAnswer());

        // 验证没有调用Embedding和LLM（节省API费用）
        verify(embeddingService, never()).embed(anyString());
        verify(llmService, never()).generate(anyString());

        System.out.println("✅ 测试通过：高频问答Redis缓存生效，未调用Embedding和LLM，节省API费用");
    }

    /**
     * 【测试点2】LLM调用失败有兜底，服务不雪崩
     * 
     * 测试场景：
     * - LLM调用抛出异常
     * - 服务返回知识库原文兜底，而不是崩溃
     */
    @Test
    void testLLMCallFailureFallback() {
        String question = "测试问题";
        String sessionId = "test-session-002";

        // 模拟限流通过
        when(rateLimiter.tryAcquire(anyString(), anyInt(), anyInt())).thenReturn(true);

        // 模拟Redis缓存未命中
        when(redisService.get(anyString())).thenReturn(null);

        // 模拟Embedding成功
        when(embeddingService.embed(question)).thenReturn(new float[1024]);

        // 模拟向量召回成功
        ChromaDBClient.SearchResult searchResult = new ChromaDBClient.SearchResult();
        searchResult.setDocumentId(1L);
        searchResult.setChunkId(1L);
        searchResult.setContent("这是知识库原文内容");
        searchResult.setSimilarity(0.85);
        when(chromaDBClient.search(any(float[].class), anyInt()))
                .thenReturn(List.of(searchResult));

        // 模拟LLM调用失败
        when(llmService.generate(anyString())).thenThrow(new RuntimeException("LLM调用超时"));

        // 模拟熔断器允许调用
        io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker = mock(io.github.resilience4j.circuitbreaker.CircuitBreaker.class);
        when(circuitBreakerRegistry.circuitBreaker(anyString())).thenReturn(circuitBreaker);
        when(circuitBreaker.executeSupplier(any())).thenThrow(new RuntimeException("熔断器触发"));

        QAQuestionRequest request = QAQuestionRequest.builder()
                .question(question)
                .sessionId(sessionId)
                .build();

        QAAnswerResponse response = qaService.ask(request);

        // 验证服务没有崩溃，返回了兜底结果
        assertNotNull(response);
        assertNotNull(response.getAnswer());

        System.out.println("✅ 测试通过：LLM调用失败后服务正常返回兜底结果，未发生雪崩");
    }

    /**
     * 【测试点3】向量召回无匹配时返回兜底提示
     */
    @Test
    void testNoMatchFallback() {
        String question = "无关问题";
        String sessionId = "test-session-003";

        when(rateLimiter.tryAcquire(anyString(), anyInt(), anyInt())).thenReturn(true);
        when(redisService.get(anyString())).thenReturn(null);
        when(embeddingService.embed(question)).thenReturn(new float[1024]);

        // 模拟向量召回无匹配（相似度低于阈值）
        ChromaDBClient.SearchResult lowSimilarityResult = new ChromaDBClient.SearchResult();
        lowSimilarityResult.setSimilarity(0.3); // 低于阈值0.75
        when(chromaDBClient.search(any(float[].class), anyInt()))
                .thenReturn(List.of(lowSimilarityResult));

        QAQuestionRequest request = QAQuestionRequest.builder()
                .question(question)
                .sessionId(sessionId)
                .build();

        QAAnswerResponse response = qaService.ask(request);

        // 验证返回无匹配提示
        assertEquals("NO_MATCH", response.getStatus());
        assertEquals("无法从知识库中找到相关信息", response.getAnswer());

        System.out.println("✅ 测试通过：向量召回无匹配时返回兜底提示");
    }
}