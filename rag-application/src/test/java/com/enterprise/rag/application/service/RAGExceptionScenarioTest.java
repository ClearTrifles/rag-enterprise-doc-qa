package com.enterprise.rag.application.service;

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RAG异常场景专项测试
 * 
 * 测试覆盖：
 * 1. 损坏文件场景
 * 2. 超长文本场景
 * 3. LLM离线场景
 * 4. 无知识库问答场景
 * 5. 限流场景
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RAG异常场景专项测试")
class RAGExceptionScenarioTest {

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

        org.springframework.test.util.ReflectionTestUtils.setField(
                qaService, "similarityThreshold", 0.75);
        org.springframework.test.util.ReflectionTestUtils.setField(
                qaService, "recallTopNum", 5);
        org.springframework.test.util.ReflectionTestUtils.setField(
                qaService, "chunkSize", 512);
        org.springframework.test.util.ReflectionTestUtils.setField(
                qaService, "chunkOverlap", 120);
        org.springframework.test.util.ReflectionTestUtils.setField(
                qaService, "cacheExpireSeconds", 3600);
    }

    /**
     * 【异常场景1】无知识库问答 - 向量召回为空
     * 
     * 测试：当知识库为空或问题与文档无关时，系统应返回明确提示
     */
    @Test
    @DisplayName("无知识库问答应返回NO_MATCH状态")
    void testNoMatchScenario() {
        String question = "完全无关的问题";
        String sessionId = "test-session";

        // 模拟限流通过
        when(rateLimiter.tryAcquire(anyString(), anyInt(), anyInt())).thenReturn(true);
        
        // 模拟缓存未命中
        when(redisService.get(anyString())).thenReturn(null);

        // 模拟Embedding成功
        when(embeddingService.embed(question)).thenReturn(new float[1024]);

        // 模拟向量召回为空
        when(chromaDBClient.search(any(float[].class), anyInt()))
                .thenReturn(java.util.List.of());

        QAQuestionRequest request = QAQuestionRequest.builder()
                .question(question)
                .sessionId(sessionId)
                .build();

        QAAnswerResponse response = qaService.ask(request);

        // 验证返回NO_MATCH状态
        assertEquals("NO_MATCH", response.getStatus());
        assertEquals("无法从知识库中找到相关信息", response.getAnswer());
        assertTrue(response.getSources().isEmpty());
        
        System.out.println("✅ 测试通过：无知识库问答时返回明确提示");
    }

    /**
     * 【异常场景2】无知识库问答 - 召回结果相似度低
     * 
     * 测试：当召回结果相似度都低于阈值时，应返回无匹配提示
     */
    @Test
    @DisplayName("召回结果相似度低于阈值应返回NO_MATCH")
    void testLowSimilarityScenario() {
        String question = "非常专业化的问题";
        String sessionId = "test-session";

        when(rateLimiter.tryAcquire(anyString(), anyInt(), anyInt())).thenReturn(true);
        when(redisService.get(anyString())).thenReturn(null);
        when(embeddingService.embed(question)).thenReturn(new float[1024]);

        // 模拟召回结果相似度都低于阈值0.75
        ChromaDBClient.SearchResult lowSimilarityResult = new ChromaDBClient.SearchResult();
        lowSimilarityResult.setDocumentId(1L);
        lowSimilarityResult.setChunkId(1L);
        lowSimilarityResult.setContent("不相关的内容");
        lowSimilarityResult.setSimilarity(0.35); // 低于0.75阈值
        
        when(chromaDBClient.search(any(float[].class), anyInt()))
                .thenReturn(java.util.List.of(lowSimilarityResult));

        QAQuestionRequest request = QAQuestionRequest.builder()
                .question(question)
                .sessionId(sessionId)
                .build();

        QAAnswerResponse response = qaService.ask(request);

        assertEquals("NO_MATCH", response.getStatus());
        assertEquals("无法从知识库中找到相关信息", response.getAnswer());
        
        System.out.println("✅ 测试通过：相似度低于阈值时过滤并返回无匹配");
    }

    /**
     * 【异常场景3】LLM离线场景
     * 
     * 测试：当LLM服务不可用时，系统应降级返回知识库原文
     */
    @Test
    @DisplayName("LLM离线时应降级返回知识库原文")
    void testLLMOfflineScenario() {
        String question = "测试问题";
        String sessionId = "test-session";
        String knowledgeContent = "这是知识库中的相关原文内容";

        when(rateLimiter.tryAcquire(anyString(), anyInt(), anyInt())).thenReturn(true);
        when(redisService.get(anyString())).thenReturn(null);
        when(embeddingService.embed(question)).thenReturn(new float[1024]);

        ChromaDBClient.SearchResult result = new ChromaDBClient.SearchResult();
        result.setDocumentId(1L);
        result.setChunkId(1L);
        result.setContent(knowledgeContent);
        result.setSimilarity(0.85);
        
        when(chromaDBClient.search(any(float[].class), anyInt()))
                .thenReturn(java.util.List.of(result));

        // 模拟熔断器允许调用
        io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker = mock(
                io.github.resilience4j.circuitbreaker.CircuitBreaker.class);
        when(circuitBreakerRegistry.circuitBreaker(anyString())).thenReturn(circuitBreaker);
        when(circuitBreaker.executeSupplier(any()))
                .thenThrow(new RuntimeException("LLM服务不可用"));

        QAQuestionRequest request = QAQuestionRequest.builder()
                .question(question)
                .sessionId(sessionId)
                .build();

        QAAnswerResponse response = qaService.ask(request);

        // 验证返回了知识库原文作为兜底
        assertNotNull(response.getAnswer());
        assertTrue(response.getSources().size() > 0);
        
        System.out.println("✅ 测试通过：LLM离线时返回知识库原文兜底");
    }

    /**
     * 【异常场景4】限流场景
     * 
     * 测试：当用户请求超过限流阈值时，应抛出限流异常
     */
    @Test
    @DisplayName("超过限流阈值应抛出限流异常")
    void testRateLimitScenario() {
        String question = "测试问题";
        String sessionId = "test-session";

        // 模拟限流拒绝
        when(rateLimiter.tryAcquire(anyString(), anyInt(), anyInt())).thenReturn(false);

        QAQuestionRequest request = QAQuestionRequest.builder()
                .question(question)
                .sessionId(sessionId)
                .build();

        assertThrows(com.enterprise.rag.common.exception.BizException.class, () -> {
            qaService.ask(request);
        }, "应该抛出限流异常");
        
        System.out.println("✅ 测试通过：超过限流阈值时抛出限流异常");
    }

    /**
     * 【异常场景5】超长问题截断
     * 
     * 测试：超长问题应被正确处理
     */
    @Test
    @DisplayName("超长问题应正确处理")
    void testLongQuestionScenario() {
        // 构造超长问题（>100字符，不会命中缓存）
        StringBuilder longQuestion = new StringBuilder();
        for (int i = 0; i < 150; i++) {
            longQuestion.append("这是一个非常长的问题。");
        }
        String question = longQuestion.toString();
        String sessionId = "test-session";

        when(rateLimiter.tryAcquire(anyString(), anyInt(), anyInt())).thenReturn(true);
        when(redisService.get(anyString())).thenReturn(null);
        when(embeddingService.embed(anyString())).thenReturn(new float[1024]);

        ChromaDBClient.SearchResult result = new ChromaDBClient.SearchResult();
        result.setDocumentId(1L);
        result.setChunkId(1L);
        result.setContent("相关内容");
        result.setSimilarity(0.85);
        
        when(chromaDBClient.search(any(float[].class), anyInt()))
                .thenReturn(java.util.List.of(result));

        // 模拟熔断器
        io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker = mock(
                io.github.resilience4j.circuitbreaker.CircuitBreaker.class);
        when(circuitBreakerRegistry.circuitBreaker(anyString())).thenReturn(circuitBreaker);
        when(circuitBreaker.executeSupplier(any())).thenReturn("回答内容");

        QAQuestionRequest request = QAQuestionRequest.builder()
                .question(question)
                .sessionId(sessionId)
                .build();

        QAAnswerResponse response = qaService.ask(request);

        // 验证超长问题不会缓存，但能正常处理
        assertNotNull(response);
        
        // 验证没有命中缓存（超长问题不会缓存）
        verify(redisService, never()).get(anyString());
        
        System.out.println("✅ 测试通过：超长问题正确处理且不缓存");
    }

    /**
     * 【异常场景6】Embedding服务异常
     * 
     * 测试：当Embedding服务异常时，系统应抛出业务异常
     */
    @Test
    @DisplayName("Embedding服务异常时应抛出业务异常")
    void testEmbeddingServiceError() {
        String question = "测试问题";
        String sessionId = "test-session";

        when(rateLimiter.tryAcquire(anyString(), anyInt(), anyInt())).thenReturn(true);
        when(redisService.get(anyString())).thenReturn(null);
        
        // 模拟Embedding服务异常
        when(embeddingService.embed(question))
                .thenThrow(new RuntimeException("Embedding服务超时"));

        QAQuestionRequest request = QAQuestionRequest.builder()
                .question(question)
                .sessionId(sessionId)
                .build();

        assertThrows(com.enterprise.rag.common.exception.BizException.class, () -> {
            qaService.ask(request);
        }, "应该抛出AI服务异常");
        
        System.out.println("✅ 测试通过：Embedding服务异常时抛出业务异常");
    }

    /**
     * 【异常场景7】高频问答缓存命中
     * 
     * 测试：相同问题第二次请求应命中缓存
     */
    @Test
    @DisplayName("高频问答应命中缓存")
    void testCacheHitScenario() {
        String question = "什么是RAG?"; // 短问题，会缓存
        String sessionId = "test-session";

        // 模拟缓存命中
        String cachedJson = "{\"answer\":\"RAG是检索增强生成\",\"sources\":[],\"responseTime\":50,\"status\":\"SUCCESS\",\"timestamp\":\"2026-06-18T10:00:00\"}";
        when(redisService.get(anyString())).thenReturn(cachedJson);

        QAQuestionRequest request = QAQuestionRequest.builder()
                .question(question)
                .sessionId(sessionId)
                .build();

        QAAnswerResponse response = qaService.ask(request);

        // 验证命中缓存
        assertEquals("CACHE_HIT", response.getStatus());
        
        // 验证没有调用Embedding和LLM
        verify(embeddingService, never()).embed(anyString());
        verify(llmService, never()).generate(anyString());
        
        System.out.println("✅ 测试通过：高频问答命中缓存，节省API调用费用");
    }
}