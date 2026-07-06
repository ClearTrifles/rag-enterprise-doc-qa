package com.enterprise.rag.infrastructure.ai;

import com.alibaba.dashscope.embeddings.TextEmbedding;
import com.alibaba.dashscope.embeddings.TextEmbeddingParam;
import com.alibaba.dashscope.embeddings.TextEmbeddingResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.enterprise.rag.infrastructure.config.QwenProperties;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 通义千问Embedding服务封装类
 * <p>
 * 封装阿里云通义千问文本向量化API调用，提供文本Embedding能力。
 * 内置熔断、重试等容错机制，确保服务稳定性。
 * </p>
 *
 * <h3>功能特性：</h3>
 * <ul>
 *     <li>支持单文本向量化</li>
 *     <li>支持批量文本向量化</li>
 *     <li>内置Resilience4j熔断器，防止级联故障</li>
 *     <li>内置重试机制，自动重试失败请求</li>
 *     <li>支持向量维度配置</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>
 * {@code
 * // 单文本向量化
 * List<Float> embedding = qwenEmbeddingService.embed("这是一段测试文本");
 *
 * // 批量文本向量化
 * List<String> texts = Arrays.asList("文本1", "文本2", "文本3");
 * List<List<Float>> embeddings = qwenEmbeddingService.batchEmbed(texts);
 * }
 * </pre>
 *
 * @author Enterprise RAG Team
 * @since 1.0.0
 * @see QwenProperties
 * @see TextEmbedding
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QwenEmbeddingService {

    /**
     * 通义千问配置属性
     */
    private final QwenProperties qwenProperties;

    /**
     * 熔断器名称
     */
    private static final String CIRCUIT_BREAKER_NAME = "qwenApi";

    /**
     * 批量处理最大数量
     */
    private static final int BATCH_SIZE = 20;

    /**
     * 单文本向量化
     * <p>
     * 将单个文本转换为向量表示。
     * </p>
     *
     * @param text 待向量化的文本
     * @return 向量列表（浮点数）
     * @throws RuntimeException 当API调用失败时抛出
     */
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "embedFallback")
    @Retry(name = CIRCUIT_BREAKER_NAME)
    public List<Float> embed(String text) {
        log.debug("开始单文本向量化 - 文本长度: {}", text.length());

        try {
            List<Float> result = embedBatch(Collections.singletonList(text));
            if (result != null && !result.isEmpty()) {
                log.debug("单文本向量化成功 - 向量维度: {}", result.size());
                return result;
            }
            return Collections.emptyList();

        } catch (Exception e) {
            log.error("单文本向量化失败: {}", e.getMessage());
            throw new RuntimeException("文本向量化失败", e);
        }
    }

    /**
     * 批量文本向量化
     * <p>
     * 将多个文本批量转换为向量表示。
     * 自动分批处理，避免单次请求数量过大。
     * </p>
     *
     * @param texts 待向量化的文本列表
     * @return 向量列表的列表（每个文本对应一个向量）
     * @throws RuntimeException 当API调用失败时抛出
     */
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "batchEmbedFallback")
    @Retry(name = CIRCUIT_BREAKER_NAME)
    public List<List<Float>> batchEmbed(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }

        log.debug("开始批量文本向量化 - 文本数量: {}", texts.size());

        try {
            List<List<Float>> allEmbeddings = new ArrayList<>();

            // 分批处理
            for (int i = 0; i < texts.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, texts.size());
                List<String> batch = texts.subList(i, end);
                List<Float> batchResult = embedBatch(batch);
                allEmbeddings.add(batchResult);
            }

            log.debug("批量文本向量化成功 - 总向量数: {}", allEmbeddings.size());
            return allEmbeddings;

        } catch (Exception e) {
            log.error("批量文本向量化失败: {}", e.getMessage());
            throw new RuntimeException("批量文本向量化失败", e);
        }
    }

    /**
     * 批量向量化内部实现
     * <p>
     * 调用通义千问Embedding API进行批量文本向量化。
     * 注意：此方法返回的是所有文本的向量合并结果（用于单文本场景）。
     * </p>
     *
     * @param texts 文本列表
     * @return 向量列表
     * @throws NoApiKeyException       API密钥异常
     * @throws InputRequiredException  输入参数异常
     * @throws ApiException            API调用异常
     */
    private List<Float> embedBatch(List<String> texts) 
            throws NoApiKeyException, InputRequiredException, ApiException {
        
        TextEmbeddingParam param = TextEmbeddingParam.builder()
                .model(qwenProperties.getEmbeddingModel())
                .texts(texts)
                .apiKey(qwenProperties.getApiKey())
                .build();

        TextEmbedding embedding = new TextEmbedding();
        TextEmbeddingResult result = embedding.call(param);

        return extractEmbedding(result);
    }

    /**
     * 从API响应中提取向量
     *
     * @param result API响应结果
     * @return 向量列表
     */
    private List<Float> extractEmbedding(TextEmbeddingResult result) {
        if (result != null && result.getOutput() != null 
                && result.getOutput().getEmbeddings() != null 
                && !result.getOutput().getEmbeddings().isEmpty()) {
            
            var embedding = result.getOutput().getEmbeddings().get(0);
            if (embedding != null && embedding.getEmbedding() != null) {
                // 通义千问API返回的是List<Double>，需要转换为List<Float>
                List<Double> doubleEmbedding = embedding.getEmbedding();
                List<Float> floatEmbedding = new ArrayList<>(doubleEmbedding.size());
                for (Double d : doubleEmbedding) {
                    floatEmbedding.add(d.floatValue());
                }
                return floatEmbedding;
            }
        }
        log.warn("通义千问Embedding API响应内容为空");
        return Collections.emptyList();
    }

    /**
     * 计算两个向量的余弦相似度
     * <p>
     * 用于计算文本相似度，值范围为[-1, 1]，值越大表示越相似。
     * </p>
     *
     * @param vector1 向量1
     * @param vector2 向量2
     * @return 余弦相似度
     */
    public double cosineSimilarity(List<Float> vector1, List<Float> vector2) {
        if (vector1 == null || vector2 == null || vector1.size() != vector2.size()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vector1.size(); i++) {
            double v1 = vector1.get(i);
            double v2 = vector2.get(i);
            dotProduct += v1 * v2;
            norm1 += v1 * v1;
            norm2 += v2 * v2;
        }

        if (norm1 == 0 || norm2 == 0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * 获取向量维度
     *
     * @return 向量维度
     */
    public int getEmbeddingDimension() {
        // text-embedding-v3 模型的向量维度
        return 1024;
    }

    // ==================== 熔断降级方法 ====================

    /**
     * 单文本向量化熔断降级方法
     */
    private List<Float> embedFallback(String text, Throwable t) {
        log.error("通义千问Embedding调用熔断降级 - 文本长度: {}, 错误: {}", 
                text.length(), t.getMessage());
        return Collections.emptyList();
    }

    /**
     * 批量文本向量化熔断降级方法
     */
    private List<List<Float>> batchEmbedFallback(List<String> texts, Throwable t) {
        log.error("通义千问Embedding批量调用熔断降级 - 文本数量: {}, 错误: {}", 
                texts.size(), t.getMessage());
        return Collections.emptyList();
    }
}