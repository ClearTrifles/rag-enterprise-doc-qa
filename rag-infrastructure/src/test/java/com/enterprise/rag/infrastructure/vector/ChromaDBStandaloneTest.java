package com.enterprise.rag.infrastructure.vector;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ChromaDBStandaloneTest {

    private static final String BASE_URL = "http://localhost:8000";
    private static final String TENANT = "default_tenant";
    private static final String DATABASE = "default_database";
    
    private static HttpClient httpClient;
    private static String testCollectionId;
    private static String testVectorId;

    @BeforeAll
    static void setup() {
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        log.info("=== ChromaDB独立测试环境准备完成 ===");
    }

    @Test
    @Order(1)
    @DisplayName("【测试1】服务连接测试 - 验证ChromaDB服务是否可用")
    void testHealthCheck() throws Exception {
        log.info("开始服务连接测试...");
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/v2/tenants/" + TENANT + "/databases/" + DATABASE + "/collections"))
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        Assertions.assertTrue(response.statusCode() == 200 || response.statusCode() == 201, 
                "服务连接失败，状态码: " + response.statusCode());
        
        log.info("✓ ChromaDB服务连接测试通过");
    }

    @Test
    @Order(2)
    @DisplayName("【测试2】创建集合 - 使用v2 API创建测试集合")
    void testCreateCollection() throws Exception {
        String collectionName = "test_collection_" + System.currentTimeMillis();
        testCollectionId = UUID.randomUUID().toString();
        log.info("开始创建集合: {}, ID: {}", collectionName, testCollectionId);

        String json = "{\"id\":\"" + testCollectionId + "\",\"name\":\"" + collectionName + "\",\"metadata\":{\"hnsw:space\":\"cosine\"}}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/v2/tenants/" + TENANT + "/databases/" + DATABASE + "/collections"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        String responseBody = response.body();
        
        log.info("创建集合响应: {}", responseBody);
        
        Assertions.assertTrue(response.statusCode() == 200 || response.statusCode() == 201, 
                "创建集合失败，状态码: " + response.statusCode() + ", 响应: " + responseBody);
        
        String actualId = extractIdFromResponse(responseBody);
        if (actualId != null && !actualId.isEmpty()) {
            testCollectionId = actualId;
        }
        
        Assertions.assertNotNull(testCollectionId, "集合ID不应为空");
        Assertions.assertFalse(testCollectionId.isEmpty(), "集合ID不应为空字符串");
        
        log.info("✓ 创建集合成功，ID: {}", testCollectionId);
    }

    @Test
    @Order(3)
    @DisplayName("【测试3】获取集合列表 - 验证集合是否已创建")
    void testGetCollections() throws Exception {
        log.info("开始获取集合列表...");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/v2/tenants/" + TENANT + "/databases/" + DATABASE + "/collections"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        Assertions.assertEquals(200, response.statusCode(), "获取集合列表失败");
        
        Assertions.assertTrue(response.body().contains("\"name\":"), "集合列表不应为空");
        
        log.info("✓ 获取集合列表成功");
    }

    @Test
    @Order(4)
    @DisplayName("【测试4】插入向量 - 验证向量数据能否正常插入")
    void testInsertVector() throws Exception {
        log.info("开始测试插入向量...");
        
        ensureCollectionExists();

        testVectorId = UUID.randomUUID().toString();
        String embeddingJson = generateEmbeddingJson(1024);
        
        String json = String.format(
            "{\"ids\":[\"%s\"],\"embeddings\":[%s],\"metadatas\":[{\"document_id\":1,\"chunk_index\":0}],\"documents\":[\"这是一段测试文本，用于验证ChromaDB向量插入功能\"]}",
            testVectorId, embeddingJson
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/v2/tenants/" + TENANT + "/databases/" + DATABASE + "/collections/" + testCollectionId + "/add"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        Assertions.assertTrue(response.statusCode() == 200 || response.statusCode() == 201, 
                "向量插入失败，状态码: " + response.statusCode() + ", 响应: " + response.body());
        
        log.info("✓ 向量插入成功，ID: {}", testVectorId);
    }

    @Test
    @Order(5)
    @DisplayName("【测试5】批量插入向量 - 验证批量插入功能")
    void testBatchInsertVectors() throws Exception {
        log.info("开始测试批量插入向量...");

        ensureCollectionExists();
        
        int batchSize = 3;
        StringBuilder idsJson = new StringBuilder("[");
        StringBuilder embeddingsJson = new StringBuilder("[");
        StringBuilder metadatasJson = new StringBuilder("[");
        StringBuilder documentsJson = new StringBuilder("[");

        Random random = new Random();
        for (int i = 0; i < batchSize; i++) {
            if (i > 0) {
                idsJson.append(",");
                embeddingsJson.append(",");
                metadatasJson.append(",");
                documentsJson.append(",");
            }
            idsJson.append("\"").append(UUID.randomUUID().toString()).append("\"");
            embeddingsJson.append(generateEmbeddingJson(1024));
            metadatasJson.append("{\"document_id\":2,\"chunk_index\":").append(i).append("}");
            documentsJson.append("\"批量测试文档内容 ").append(i).append("\"");
        }
        idsJson.append("]");
        embeddingsJson.append("]");
        metadatasJson.append("]");
        documentsJson.append("]");

        String json = String.format(
            "{\"ids\":%s,\"embeddings\":%s,\"metadatas\":%s,\"documents\":%s}",
            idsJson.toString(), embeddingsJson.toString(), metadatasJson.toString(), documentsJson.toString()
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/v2/tenants/" + TENANT + "/databases/" + DATABASE + "/collections/" + testCollectionId + "/add"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        Assertions.assertTrue(response.statusCode() == 200 || response.statusCode() == 201, 
                "批量插入失败，状态码: " + response.statusCode() + ", 响应: " + response.body());
        
        log.info("✓ 批量插入向量成功，数量: {}", batchSize);
    }

    @Test
    @Order(6)
    @DisplayName("【测试6】查询向量数量 - 验证向量计数功能")
    void testCountVectors() throws Exception {
        log.info("开始测试查询向量数量...");

        ensureCollectionExists();
        
        String embeddingJson = generateEmbeddingJson(1024);
        String json = String.format(
            "{\"ids\":[\"%s\",\"%s\",\"%s\"],\"embeddings\":[%s,%s,%s],\"metadatas\":[{\"test\":1},{\"test\":2},{\"test\":3}],\"documents\":[\"test1\",\"test2\",\"test3\"]}",
            UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString(),
            embeddingJson, embeddingJson, embeddingJson
        );

        HttpRequest insertRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/v2/tenants/" + TENANT + "/databases/" + DATABASE + "/collections/" + testCollectionId + "/add"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        httpClient.send(insertRequest, HttpResponse.BodyHandlers.ofString());
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/v2/tenants/" + TENANT + "/databases/" + DATABASE + "/collections/" + testCollectionId + "/count"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        Assertions.assertEquals(200, response.statusCode(), "查询向量数量失败");
        
        int count = Integer.parseInt(response.body());
        Assertions.assertTrue(count >= 3, "向量数量应至少为3，当前为: " + count);
        
        log.info("✓ 查询向量数量成功，共{}个向量", count);
    }

    @Test
    @Order(7)
    @DisplayName("【测试7】向量相似度查询 - 验证查询功能")
    void testQuerySimilar() throws Exception {
        log.info("开始测试向量相似度查询...");

        ensureCollectionExists();
        
        String queryEmbedding = generateEmbeddingJson(1024);
        
        String json = String.format(
            "{\"query_embeddings\":[%s],\"n_results\":3,\"include\":[\"documents\",\"metadatas\",\"distances\"]}",
            queryEmbedding
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/v2/tenants/" + TENANT + "/databases/" + DATABASE + "/collections/" + testCollectionId + "/query"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        Assertions.assertEquals(200, response.statusCode(), "向量查询失败，状态码: " + response.statusCode() + ", 响应: " + response.body());
        
        Assertions.assertTrue(response.body().contains("\"ids\":"), "查询结果ID不应为空");
        
        log.info("✓ 向量相似度查询成功");
    }

    @Test
    @Order(8)
    @DisplayName("【测试8】删除向量 - 验证单个向量删除功能")
    void testDeleteVector() throws Exception {
        log.info("开始测试删除向量...");

        ensureCollectionExists();
        
        String deleteId = testVectorId != null ? testVectorId : UUID.randomUUID().toString();
        String json = "{\"ids\":[\"" + deleteId + "\"]}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/v2/tenants/" + TENANT + "/databases/" + DATABASE + "/collections/" + testCollectionId + "/delete"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        Assertions.assertTrue(response.statusCode() == 200 || response.statusCode() == 204, 
                "删除向量失败，状态码: " + response.statusCode());
        
        log.info("✓ 向量删除成功，ID: {}", testVectorId);
    }

    @Test
    @Order(9)
    @DisplayName("【测试9】删除测试集合 - 清理测试数据")
    void testDeleteCollection() throws Exception {
        log.info("开始测试删除集合...");

        ensureCollectionExists();
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/v2/tenants/" + TENANT + "/databases/" + DATABASE + "/collections/" + testCollectionId))
                .DELETE()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        int statusCode = response.statusCode();
        Assertions.assertTrue(statusCode == 200 || statusCode == 204 || statusCode == 404, 
                "删除集合失败，状态码: " + statusCode);
        
        if (statusCode == 404) {
            log.info("集合可能已被删除或不存在，状态码: 404");
        } else {
            log.info("✓ 集合删除成功，ID: {}", testCollectionId);
        }
    }

    private String generateEmbeddingJson(int dimension) {
        StringBuilder sb = new StringBuilder("[");
        Random random = new Random(System.currentTimeMillis());
        for (int i = 0; i < dimension; i++) {
            if (i > 0) sb.append(",");
            sb.append(String.format("%.6f", random.nextDouble() * 2 - 1));
        }
        sb.append("]");
        return sb.toString();
    }

    private void ensureCollectionExists() throws Exception {
        if (testCollectionId != null && !testCollectionId.isEmpty()) {
            try {
                HttpRequest checkRequest = HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "/api/v2/tenants/" + TENANT + "/databases/" + DATABASE + "/collections/" + testCollectionId))
                        .GET()
                        .build();
                HttpResponse<String> checkResponse = httpClient.send(checkRequest, HttpResponse.BodyHandlers.ofString());
                if (checkResponse.statusCode() == 200) {
                    return;
                }
            } catch (Exception e) {
                log.info("集合不存在或检查失败，将创建新集合");
            }
        }
        
        String collectionName = "standalone_test_collection_" + System.currentTimeMillis();
        String json = "{\"name\":\"" + collectionName + "\",\"metadata\":{\"hnsw:space\":\"cosine\"}}";

        HttpRequest createRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/v2/tenants/" + TENANT + "/databases/" + DATABASE + "/collections"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> createResponse = httpClient.send(createRequest, HttpResponse.BodyHandlers.ofString());
        
        if (createResponse.statusCode() == 200 || createResponse.statusCode() == 201) {
            testCollectionId = extractIdFromResponse(createResponse.body());
            log.info("自动创建集合成功，ID: {}", testCollectionId);
        } else {
            throw new RuntimeException("无法创建集合，状态码: " + createResponse.statusCode() + ", 响应: " + createResponse.body());
        }
    }

    private String extractIdFromResponse(String responseBody) {
        int idStart = responseBody.indexOf("\"id\":") + 5;
        if (idStart < 5) return null;
        int idEnd = responseBody.indexOf(",", idStart);
        if (idEnd < 0) idEnd = responseBody.indexOf("}", idStart);
        if (idEnd < 0) idEnd = responseBody.length();
        return responseBody.substring(idStart, idEnd).replace("\"", "").trim();
    }

    @AfterAll
    static void cleanup() {
        log.info("=== ChromaDB独立测试完成 ===");
    }
}