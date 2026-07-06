package com.enterprise.rag.application.service.impl;

import com.alibaba.fastjson2.JSON;
import com.enterprise.rag.application.dto.QAAnswerResponse;
import com.enterprise.rag.application.dto.QAQuestionRequest;
import com.enterprise.rag.application.service.QAService;
import com.enterprise.rag.common.dto.ChatResponse;
import com.enterprise.rag.common.dto.ReferenceItem;
import com.enterprise.rag.common.exception.BizException;
import com.enterprise.rag.common.exception.ExceptionEnum;
import com.enterprise.rag.domain.entity.RagChatRecord;
import com.enterprise.rag.domain.enums.ChatStatusEnum;
import com.enterprise.rag.infrastructure.ai.QwenEmbeddingService;
import com.enterprise.rag.infrastructure.ai.QwenLLMService;
import com.enterprise.rag.infrastructure.mapper.RagChatRecordMapper;
import com.enterprise.rag.infrastructure.redis.RateLimiter;
import com.enterprise.rag.infrastructure.redis.RedisService;
import com.enterprise.rag.infrastructure.vector.ChromaDBClient;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import java.util.concurrent.ExecutionException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 问答服务实现类
 *
 * 【面试优化点说明】
 * 1. 大模型返回超时自动降级，直接返回知识库原文兜底
 * 2. 向量召回阈值动态配置，支持后台微调
 * 3. 高频问答Redis缓存生效，减少API调用费用
 * 4. LLM调用失败有熔断兜底，服务不雪崩
 * 5. Prompt模板解耦，配置化修改问答约束话术
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QAServiceImpl implements QAService {

    private final QwenEmbeddingService embeddingService;
    private final QwenLLMService llmService;
    private final ChromaDBClient chromaDBClient;
    private final RateLimiter rateLimiter;
    private final RedisService redisService;
    private final RagChatRecordMapper chatRecordMapper;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    @Value("${rag.similarity-threshold:0.75}")
    private double similarityThreshold;

    @Value("${rag.recall-top-num:5}")
    private int recallTopNum;

    @Value("${rag.chunk-size:512}")
    private int chunkSize;

    @Value("${rag.chunk-overlap:120}")
    private int chunkOverlap;

    @Value("${rag.cache.expire:3600}")
    private long cacheExpireSeconds;

    private static final String CACHE_PREFIX = "rag:qa:cache:";
    private static final int MAX_CACHE_QUESTION_LENGTH = 100;

    @Override
    public QAAnswerResponse ask(QAQuestionRequest request) {
        long startTime = System.currentTimeMillis();

        String sessionId = request.getSessionId();
        String question = request.getQuestion();

        if (sessionId != null) {
            boolean allow = rateLimiter.tryAcquire(sessionId, 10, 60);
            if (!allow) {
                throw new BizException(ExceptionEnum.RATE_LIMIT_EXCEEDED);
            }
        }

        if (question.length() <= MAX_CACHE_QUESTION_LENGTH) {
            String cacheKey = CACHE_PREFIX + DigestUtils.md5DigestAsHex(question.getBytes());
            String cachedAnswer = redisService.get(cacheKey);

            if (cachedAnswer != null) {
                log.info("命中高频问答缓存，问题: {}", question);
                QAAnswerResponse cachedResponse = JSON.parseObject(cachedAnswer, QAAnswerResponse.class);
                cachedResponse.setStatus("CACHE_HIT");
                return cachedResponse;
            }
        }

        try {
            List<Float> embedding = embeddingService.embed(question);

            List<ChromaDBClient.SearchResult> results = chromaDBClient.search(embedding, recallTopNum);

            List<QAAnswerResponse.ReferenceSource> sources = new ArrayList<>();
            StringBuilder context = new StringBuilder();

            for (ChromaDBClient.SearchResult result : results) {
                if (result.getSimilarity() >= similarityThreshold) {
                    sources.add(QAAnswerResponse.ReferenceSource.builder()
                            .documentId(result.getDocumentId())
                            .chunkId(result.getChunkId())
                            .content(result.getContent())
                            .similarity(result.getSimilarity())
                            .build());
                    context.append(result.getContent()).append("\n\n");
                }
            }

            if (sources.isEmpty()) {
                saveChatRecord(sessionId, question, "无法从知识库中找到相关信息",
                        ChatStatusEnum.NO_MATCH.getCode(), (int) (System.currentTimeMillis() - startTime));

                return QAAnswerResponse.builder()
                        .answer("无法从知识库中找到相关信息")
                        .sources(List.of())
                        .responseTime((int) (System.currentTimeMillis() - startTime))
                        .status("NO_MATCH")
                        .timestamp(LocalDateTime.now())
                        .build();
            }

            String prompt = ChatPromptConfig.buildRAGPrompt(context.toString(), question);

            String answer = callLLMWithCircuitBreaker(prompt, context.toString());

            QAAnswerResponse response = QAAnswerResponse.builder()
                    .answer(answer)
                    .sources(sources)
                    .responseTime((int) (System.currentTimeMillis() - startTime))
                    .status("SUCCESS")
                    .timestamp(LocalDateTime.now())
                    .build();

            if (question.length() <= MAX_CACHE_QUESTION_LENGTH) {
                String cacheKey = CACHE_PREFIX + DigestUtils.md5DigestAsHex(question.getBytes());
                redisService.set(cacheKey, JSON.toJSONString(response), cacheExpireSeconds);
                log.info("缓存问答结果，问题: {}", question);
            }

            saveChatRecord(sessionId, question, answer,
                    ChatStatusEnum.SUCCESS.getCode(), (int) (System.currentTimeMillis() - startTime));

            return response;

        } catch (Exception e) {
            log.error("问答处理失败: {}", e.getMessage(), e);
            saveChatRecord(sessionId, question, "系统错误",
                    ChatStatusEnum.FAILED.getCode(), (int) (System.currentTimeMillis() - startTime));
            return QAAnswerResponse.builder()
                    .answer("抱歉，系统暂时无法回答您的问题，请稍后再试。")
                    .sources(List.of())
                    .responseTime((int) (System.currentTimeMillis() - startTime))
                    .status("FAILED")
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    /**
     * 智能问答（模式C折中方案）
     * <p>
     * 检索匹配知识库则基于文档回答；无匹配则返回询问结构，
     * 前端弹窗确认后再调用通用大模型问答。
     * </p>
     *
     * 分支A：存在≥阈值有效切片 → RAG_ANSWER
     * 分支B：无有效匹配切片 → NEED_CONFIRM_GENERAL（不调用LLM，不消耗Token）
     */
    @Override
    public ChatResponse askModeC(QAQuestionRequest request) {
        long startTime = System.currentTimeMillis();

        String sessionId = request.getSessionId();
        String question = request.getQuestion();

        // 限流检查
        if (sessionId != null) {
            boolean allow = rateLimiter.tryAcquire(sessionId, 10, 60);
            if (!allow) {
                throw new BizException(ExceptionEnum.RATE_LIMIT_EXCEEDED);
            }
        }

        try {
            // 步骤1：问题向量化
            List<Float> embedding = embeddingService.embed(question);

            // 步骤2：Chroma相似度检索
            List<ChromaDBClient.SearchResult> results = chromaDBClient.search(embedding, recallTopNum);

            // 步骤3：筛选有效匹配切片
            List<ChromaDBClient.SearchResult> validResults = new ArrayList<>();
            StringBuilder context = new StringBuilder();

            for (ChromaDBClient.SearchResult result : results) {
                if (result.getSimilarity() >= similarityThreshold) {
                    validResults.add(result);
                    context.append(result.getContent()).append("\n\n");
                }
            }

            // 分支A：存在有效匹配切片 → RAG_ANSWER
            if (!validResults.isEmpty()) {
                log.info("检索命中知识库，有效切片数: {}", validResults.size());

                // 构建溯源引用列表
                List<ReferenceItem> referenceList = new ArrayList<>();
                for (ChromaDBClient.SearchResult result : validResults) {
                    referenceList.add(ReferenceItem.builder()
                            .documentId(result.getDocumentId())
                            .content(result.getContent())
                            .score(result.getSimilarity())
                            .build());
                }

                // 组装RAG约束Prompt
                String ragPrompt = ChatPromptConfig.buildRAGPrompt(context.toString(), question);

                // 调用LLM生成答案
                String answer = callLLMWithCircuitBreaker(ragPrompt, context.toString());

                // 保存问答记录
                int responseTime = (int) (System.currentTimeMillis() - startTime);
                saveChatRecord(sessionId, question, answer,
                        ChatStatusEnum.SUCCESS.getCode(), responseTime);

                return ChatResponse.ragAnswer(question, answer, referenceList);
            }

            // 分支B：无有效匹配切片 → NEED_CONFIRM_GENERAL
            // 不调用LLM、不消耗Token
            log.info("检索未命中知识库，返回询问结构");
            int responseTime = (int) (System.currentTimeMillis() - startTime);
            saveChatRecord(sessionId, question, "",
                    ChatStatusEnum.NO_MATCH.getCode(), responseTime);

            return ChatResponse.needConfirmGeneral(question, ChatPromptConfig.NO_MATCH_PROMPT_TIP);

        } catch (Exception e) {
            log.error("模式C问答处理失败: {}", e.getMessage(), e);
            saveChatRecord(sessionId, question, "系统错误",
                    ChatStatusEnum.FAILED.getCode(), (int) (System.currentTimeMillis() - startTime));
            return ChatResponse.needConfirmGeneral(question,
                    "抱歉，知识库检索服务暂时不可用，请稍后再试，或开启通用问答获取回答。");
        }
    }

    /**
     * 通用自由问答（无知识库限制）
     * <p>
     * 跳过向量检索，直接使用大模型进行自然对话回答。
     * 适用于闲聊、常识、外部资讯类问题。
     * </p>
     */
    @Override
    public ChatResponse generalAsk(QAQuestionRequest request) {
        long startTime = System.currentTimeMillis();

        String sessionId = request.getSessionId();
        String question = request.getQuestion();

        // 限流检查
        if (sessionId != null) {
            boolean allow = rateLimiter.tryAcquire(sessionId, 10, 60);
            if (!allow) {
                throw new BizException(ExceptionEnum.RATE_LIMIT_EXCEEDED);
            }
        }

        try {
            log.info("执行通用问答，问题: {}", question);

            // 组装通用Prompt（无知识库限制）
            String generalPrompt = ChatPromptConfig.buildGeneralPrompt(question);

            // 调用LLM生成答案
            String answer = llmService.chat(generalPrompt);

            // 保存问答记录
            int responseTime = (int) (System.currentTimeMillis() - startTime);
            saveChatRecord(sessionId, question, answer,
                    ChatStatusEnum.SUCCESS.getCode(), responseTime);

            return ChatResponse.generalAnswer(question, answer);

        } catch (Exception e) {
            log.error("通用问答处理失败: {}", e.getMessage(), e);
            saveChatRecord(sessionId, question, "系统错误",
                    ChatStatusEnum.FAILED.getCode(), (int) (System.currentTimeMillis() - startTime));
            return ChatResponse.generalAnswer(question,
                    "抱歉，通用问答服务暂时不可用，请稍后再试。");
        }
    }

    /**
     * 使用熔断器调用LLM，失败时返回知识库原文兜底
     *
     * 【面试优化点4】LLM调用失败有熔断兜底，服务不雪崩
     */
    private String callLLMWithCircuitBreaker(String prompt, String fallbackContent) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("qwenApi");

        try {
            return circuitBreaker.executeSupplier(() -> {
                Retryer<String> retryer = RetryerBuilder.<String>newBuilder()
                        .retryIfException()
                        .withStopStrategy(StopStrategies.stopAfterAttempt(3))
                        .withWaitStrategy(WaitStrategies.exponentialWait(1000, 10, TimeUnit.SECONDS))
                        .build();

                try {
                    return retryer.call(() -> llmService.chat(prompt));
                } catch (RetryException | ExecutionException e) {
                    log.warn("LLM重试3次后仍失败，使用知识库原文兜底");
                    return fallbackContent;
                }
            });
        } catch (CallNotPermittedException e) {
            log.warn("熔断器已打开，LLM调用被拒绝，使用知识库原文兜底");
            return fallbackContent;
        } catch (Exception e) {
            log.warn("LLM调用异常: {}, 使用知识库原文兜底", e.getMessage());
            return fallbackContent;
        }
    }

    /**
     * 保存问答记录
     */
    private void saveChatRecord(String sessionId, String question, String answer, Integer status, Integer responseTime) {
        try {
            RagChatRecord record = RagChatRecord.builder()
                    .sessionId(sessionId != null ? sessionId : "anonymous")
                    .userQuestion(question)
                    .aiAnswer(answer)
                    .status(status)
                    .responseTime(responseTime)
                    .build();
            chatRecordMapper.insert(record);
        } catch (Exception e) {
            log.error("保存问答记录失败: {}", e.getMessage());
        }
    }

    @Override
    public QAAnswerResponse getHistory(String sessionId, Integer pageNum, Integer pageSize) {
        return QAAnswerResponse.builder()
                .answer("")
                .sources(List.of())
                .status("SUCCESS")
                .timestamp(LocalDateTime.now())
                .build();
    }
}
