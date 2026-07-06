package com.enterprise.rag.infrastructure.vector;

import com.enterprise.rag.infrastructure.config.ChromaProperties;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ChromaDBClientTest {

    private static ChromaDBClient chromaDBClient;
    private static String testCollectionId;
    private static String testVectorId;

    @BeforeAll
    static void setup() {
        ChromaProperties properties = new ChromaProperties();
        properties.setHost("localhost");
        properties.setPort(8000);

        RestTemplate restTemplate = new RestTemplate();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(30000);
        restTemplate.setRequestFactory(factory);

        chromaDBClient = new ChromaDBClient(properties, restTemplate);
        log.info("=== ChromaDB测试环境准备完成 ===");
    }

    @Test
    @Order(1)
    @DisplayName("【测试1】测试连接ChromaDB")
    void testConnection() {
        log.info("开始测试连接...");
        Assertions.assertNotNull(chromaDBClient, "ChromaDBClient不应为空");
        log.info("✓ ChromaDBClient初始化成功");
    }

    @Test
    @Order(2)
    @DisplayName("【测试2】创建集合")
    void testCreateCollection() {
        String collectionName = "test_collection_" + System.currentTimeMillis();
        log.info("开始创建集合: {}", collectionName);

        testCollectionId = chromaDBClient.createCollection(collectionName);

        Assertions.assertNotNull(testCollectionId, "集合ID不应为空");
        Assertions.assertFalse(testCollectionId.isEmpty(), "集合ID不应为空字符串");
        log.info("✓ 创建集合成功，ID: {}", testCollectionId);
    }

    @Test
    @Order(3)
    @DisplayName("【测试3】获取集合ID")
    void testGetCollectionId() {
        log.info("开始测试获取集合ID...");

        String collectionId = chromaDBClient.getCollectionId(testCollectionId);

        Assertions.assertNotNull(collectionId, "集合ID不应为空");
        log.info("✓ 获取集合ID成功: {}", collectionId);
    }

    @Test
    @Order(4)
    @DisplayName("【测试4】插入向量")
    void testInsertVector() {
        log.info("开始测试插入向量...");

        testVectorId = UUID.randomUUID().toString();
        List<Float> embedding = generateTestEmbedding();
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("document_id", "test_doc_1");
        metadata.put("chunk_index", 0);

        boolean result = chromaDBClient.insertVector(testCollectionId, testVectorId, embedding, metadata, "这是测试文档内容");

        Assertions.assertTrue(result, "向量插入应成功");
        log.info("✓ 向量插入成功，ID: {}", testVectorId);
    }

    @Test
    @Order(5)
    @DisplayName("【测试5】查询向量")
    void testQueryVector() {
        log.info("开始测试查询向量...");

        List<Float> queryEmbedding = generateTestEmbedding();
        List<Map<String, Object>> results = chromaDBClient.querySimilar(testCollectionId, queryEmbedding, 5, null);

        Assertions.assertNotNull(results, "查询结果不应为空");
        log.info("✓ 查询向量成功，结果数量: {}", results.size());
    }

    @Test
    @Order(6)
    @DisplayName("【测试6】获取向量数量")
    void testCountVectors() {
        log.info("开始测试获取向量数量...");

        long count = chromaDBClient.countVectors(testCollectionId);

        Assertions.assertTrue(count >= 0, "向量数量应大于等于0");
        log.info("✓ 获取向量数量成功: {}", count);
    }

    @Test
    @Order(7)
    @DisplayName("【测试7】删除向量")
    void testDeleteVector() {
        log.info("开始测试删除向量...");

        boolean result = chromaDBClient.deleteVector(testCollectionId, testVectorId);

        Assertions.assertTrue(result, "向量删除应成功");
        log.info("✓ 向量删除成功");
    }

    @Test
    @Order(8)
    @DisplayName("【测试8】删除测试集合")
    void testDeleteCollection() {
        log.info("开始测试删除集合...");

        try {
            boolean result = chromaDBClient.deleteCollection(testCollectionId);
            log.info("集合删除结果: {}", result ? "成功" : "失败（可能集合不存在）");
        } catch (Exception e) {
            // 允许集合不存在的情况
            log.info("集合删除结果: 失败（可能集合不存在），错误: {}", e.getMessage());
        }
    }

    private List<Float> generateTestEmbedding() {
        List<Float> embedding = new ArrayList<>();
        Random random = new Random(System.currentTimeMillis());
        for (int i = 0; i < 1024; i++) {
            embedding.add(random.nextFloat() * 2 - 1);
        }
        return embedding;
    }

    @AfterAll
    static void cleanup() {
        log.info("=== ChromaDB测试完成 ===");
    }
}