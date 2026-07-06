package com.enterprise.rag.infrastructure.vector;

import com.enterprise.rag.infrastructure.config.ChromaProperties;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * ChromaDB向量数据库客户端封装类
 * <p>
 * 使用HTTP REST API直接调用ChromaDB服务，支持向量查询功能。
 * 内置熔断、重试等容错机制，确保服务稳定性。
 * </p>
 *
 * <h3>功能特性：</h3>
 * <ul>
 *     <li>支持向量数据的增删改查</li>
 *     <li>支持批量向量插入</li>
 *     <li>支持向量相似度检索</li>
 *     <li>内置Resilience4j熔断器，防止级联故障</li>
 *     <li>内置重试机制，自动重试失败请求</li>
 *     <li>支持元数据过滤查询</li>
 * </ul>
 *
 * @author Enterprise RAG Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChromaDBClient {

    private final ChromaProperties chromaProperties;
    private final RestTemplate restTemplate;

    private static final String CIRCUIT_BREAKER_NAME = "chromaDb";
    
    /**
     * 默认集合名称
     */
    private static final String DEFAULT_COLLECTION = "rag_documents";
    
    /**
     * 默认租户
     */
    private static final String DEFAULT_TENANT = "default_tenant";
    
    /**
     * 默认数据库
     */
    private static final String DEFAULT_DATABASE = "default_database";

    /**
     * 搜索结果封装类
     */
    @Data
    @AllArgsConstructor
    public static class SearchResult {
        private Long documentId;
        private Long chunkId;
        private String content;
        private double similarity;
        private String vectorId;
    }

    /**
     * 获取集合ID (直接返回collection_id，不使用CRN格式)
     */
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getCollectionIdFallback")
    @Retry(name = CIRCUIT_BREAKER_NAME)
    public String getCollectionId(String collectionName) {
        // 使用v2 API: /api/v2/tenants/{tenant}/databases/{database}/collections
        String url = String.format("%s/api/v2/tenants/%s/databases/%s/collections",
                chromaProperties.getServerUrl(), DEFAULT_TENANT, DEFAULT_DATABASE);
        
        try {
            ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                for (Object collection : response.getBody()) {
                    Map<String, Object> collectionMap = (Map<String, Object>) collection;
                    if (collectionMap.get("name").equals(collectionName)) {
                        // 检查维度是否匹配
                        Object dimensionObj = collectionMap.get("dimension");
                        if (dimensionObj != null) {
                            int existingDimension = ((Number) dimensionObj).intValue();
                            int expectedDimension = chromaProperties.getEmbeddingDimension();
                            
                            if (existingDimension != expectedDimension) {
                                // 维度不匹配，删除旧集合并创建新集合
                                String oldCollectionId = (String) collectionMap.get("id");
                                log.warn("发现维度不匹配的集合 {}，当前维度: {}, 期望维度: {}，将删除旧集合并创建新集合", 
                                        collectionName, existingDimension, expectedDimension);
                                
                                // 尝试删除旧集合
                                try {
                                    String deleteUrl = String.format("%s/api/v2/tenants/%s/databases/%s/collections/%s",
                                            chromaProperties.getServerUrl(), DEFAULT_TENANT, DEFAULT_DATABASE, oldCollectionId);
                                    restTemplate.delete(deleteUrl);
                                    log.info("旧集合删除成功");
                                } catch (Exception deleteEx) {
                                    log.warn("删除旧集合失败，可能已被删除: {}", deleteEx.getMessage());
                                }
                                
                                // 创建新集合
                                return createCollection(collectionName);
                            }
                        }
                        return (String) collectionMap.get("id");
                    }
                }
            }
            
            // 集合不存在，创建新集合
            return createCollection(collectionName);
        } catch (Exception e) {
            log.error("获取集合ID失败: {}", e.getMessage());
            throw new RuntimeException("获取集合ID失败", e);
        }
    }

    /**
     * 获取集合ID熔断降级方法
     */
    private String getCollectionIdFallback(String collectionName, Throwable t) {
        log.error("ChromaDB获取集合ID熔断降级 - 集合: {}, 错误: {}", collectionName, t.getMessage());
        throw new RuntimeException("ChromaDB服务不可用，请稍后重试", t);
    }

    /**
     * 创建集合
     */
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "createCollectionFallback")
    @Retry(name = CIRCUIT_BREAKER_NAME)
    public String createCollection(String collectionName) {
        // 使用v2 API: POST /api/v2/tenants/{tenant}/databases/{database}/collections
        String url = String.format("%s/api/v2/tenants/%s/databases/%s/collections",
                chromaProperties.getServerUrl(), DEFAULT_TENANT, DEFAULT_DATABASE);
        
        Map<String, Object> request = new HashMap<>();
        request.put("name", collectionName);
        
        // 指定向量维度，与Embedding模型输出维度一致
        request.put("dimension", chromaProperties.getEmbeddingDimension());
        
        // 指定HNSW配置
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("hnsw:space", "cosine");  // 使用余弦相似度
        request.put("metadata", metadata);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
        
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            if (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED) {
                if (response.getBody() != null) {
                    return (String) response.getBody().get("id");
                }
            }
            throw new RuntimeException("创建集合失败");
        } catch (Exception e) {
            log.error("创建集合失败: {}", e.getMessage());
            throw new RuntimeException("创建集合失败", e);
        }
    }

    /**
     * 创建集合熔断降级方法
     */
    private String createCollectionFallback(String collectionName, Throwable t) {
        log.error("ChromaDB创建集合熔断降级 - 集合: {}, 错误: {}", collectionName, t.getMessage());
        throw new RuntimeException("ChromaDB服务不可用，无法创建集合", t);
    }

    /**
     * 插入单个向量
     */
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "insertVectorFallback")
    @Retry(name = CIRCUIT_BREAKER_NAME)
    public boolean insertVector(String collectionName, String id, List<Float> embedding,
            Map<String, Object> metadata, String document) {
        log.debug("插入向量 - 集合: {}, ID: {}", collectionName, id);

        String collectionId = getCollectionId(collectionName);
        // 使用v2 API: POST /api/v2/tenants/{tenant}/databases/{database}/collections/{collection_id}/add
        String url = String.format("%s/api/v2/tenants/%s/databases/%s/collections/%s/add", 
                chromaProperties.getServerUrl(), DEFAULT_TENANT, DEFAULT_DATABASE, collectionId);

        Map<String, Object> request = new HashMap<>();
        request.put("ids", Collections.singletonList(id));
        request.put("embeddings", Collections.singletonList(embedding));
        
        // 转换元数据类型
        Map<String, String> stringMetadata = new HashMap<>();
        if (metadata != null) {
            metadata.forEach((k, v) -> stringMetadata.put(k, v != null ? v.toString() : ""));
        }
        request.put("metadatas", Collections.singletonList(stringMetadata));
        
        if (document != null) {
            request.put("documents", Collections.singletonList(document));
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            if (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED) {
                log.debug("向量插入成功 - ID: {}", id);
                return true;
            }
            throw new RuntimeException("向量插入失败");
        } catch (Exception e) {
            log.error("向量插入失败 - ID: {}, 错误: {}", id, e.getMessage());
            throw new RuntimeException("向量插入失败", e);
        }
    }

    /**
     * 批量插入向量
     */
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "batchInsertVectorsFallback")
    @Retry(name = CIRCUIT_BREAKER_NAME)
    public boolean batchInsertVectors(String collectionName, List<String> ids,
            List<List<Float>> embeddings, List<Map<String, Object>> metadatas, List<String> documents) {
        log.debug("批量插入向量 - 集合: {}, 数量: {}", collectionName, ids.size());

        String collectionId = getCollectionId(collectionName);
        // 使用v2 API: POST /api/v2/tenants/{tenant}/databases/{database}/collections/{collection_id}/add
        String url = String.format("%s/api/v2/tenants/%s/databases/%s/collections/%s/add", 
                chromaProperties.getServerUrl(), DEFAULT_TENANT, DEFAULT_DATABASE, collectionId);

        Map<String, Object> request = new HashMap<>();
        request.put("ids", ids);
        request.put("embeddings", embeddings);
        
        // 转换元数据类型
        List<Map<String, String>> processedMetadatas = new ArrayList<>();
        if (metadatas != null) {
            for (Map<String, Object> meta : metadatas) {
                Map<String, String> stringMeta = new HashMap<>();
                if (meta != null) {
                    meta.forEach((k, v) -> stringMeta.put(k, v != null ? v.toString() : ""));
                }
                processedMetadatas.add(stringMeta);
            }
        } else {
            for (int i = 0; i < ids.size(); i++) {
                processedMetadatas.add(new HashMap<>());
            }
        }
        request.put("metadatas", processedMetadatas);
        
        if (documents != null) {
            request.put("documents", documents);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            if (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED) {
                log.debug("批量向量插入成功 - 数量: {}", ids.size());
                return true;
            }
            throw new RuntimeException("批量向量插入失败");
        } catch (Exception e) {
            log.error("批量向量插入失败 - 数量: {}, 错误: {}", ids.size(), e.getMessage());
            throw new RuntimeException("批量向量插入失败", e);
        }
    }

    /**
     * 向量相似度检索
     */
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "querySimilarFallback")
    @Retry(name = CIRCUIT_BREAKER_NAME)
    public List<Map<String, Object>> querySimilar(String collectionName, List<Float> queryEmbedding,
            int topK, Map<String, Object> whereClause) {
        log.debug("相似度检索 - 集合: {}, topK: {}", collectionName, topK);

        String collectionId = getCollectionId(collectionName);
        // 使用v2 API: POST /api/v2/tenants/{tenant}/databases/{database}/collections/{collection_id}/query
        String url = String.format("%s/api/v2/tenants/%s/databases/%s/collections/%s/query", 
                chromaProperties.getServerUrl(), DEFAULT_TENANT, DEFAULT_DATABASE, collectionId);

        Map<String, Object> request = new HashMap<>();
        request.put("query_embeddings", Collections.singletonList(queryEmbedding));
        request.put("n_results", topK);
        request.put("include", Arrays.asList("documents", "metadatas", "distances"));
        
        // 转换where参数类型
        if (whereClause != null) {
            Map<String, String> stringWhere = new HashMap<>();
            whereClause.forEach((k, v) -> stringWhere.put(k, v != null ? v.toString() : ""));
            request.put("where", stringWhere);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> results = parseQueryResponse(response.getBody());
                log.debug("相似度检索成功 - 结果数量: {}", results.size());
                return results;
            }
            throw new RuntimeException("相似度检索失败");
        } catch (Exception e) {
            log.error("相似度检索失败: {}", e.getMessage());
            throw new RuntimeException("相似度检索失败", e);
        }
    }

    /**
     * 解析查询响应结果
     */
    private List<Map<String, Object>> parseQueryResponse(Map<String, Object> response) {
        List<Map<String, Object>> results = new ArrayList<>();

        if (response == null) {
            return results;
        }

        List<List<String>> ids = (List<List<String>>) response.get("ids");
        List<List<Double>> distances = (List<List<Double>>) response.get("distances");
        List<List<Map<String, Object>>> metadatas = (List<List<Map<String, Object>>>) response.get("metadatas");
        List<List<String>> documents = (List<List<String>>) response.get("documents");

        if (ids == null || ids.isEmpty()) {
            return results;
        }

        // 处理第一个查询的结果
        List<String> firstIds = ids.get(0);
        List<Double> firstDistances = distances != null && !distances.isEmpty() 
                ? distances.get(0) 
                : Collections.emptyList();

        for (int i = 0; i < firstIds.size(); i++) {
            Map<String, Object> result = new HashMap<>();
            result.put("id", firstIds.get(i));
            
            if (i < firstDistances.size()) {
                result.put("distance", firstDistances.get(i));
            }
            
            if (metadatas != null && !metadatas.isEmpty() && metadatas.get(0) != null) {
                List<Map<String, Object>> firstMetadatas = metadatas.get(0);
                if (i < firstMetadatas.size()) {
                    result.put("metadata", firstMetadatas.get(i));
                }
            }
            
            if (documents != null && !documents.isEmpty() && documents.get(0) != null) {
                List<String> firstDocuments = documents.get(0);
                if (i < firstDocuments.size()) {
                    result.put("document", firstDocuments.get(i));
                }
            }
            
            results.add(result);
        }

        return results;
    }

    /**
     * 根据ID删除向量
     */
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "deleteVectorFallback")
    @Retry(name = CIRCUIT_BREAKER_NAME)
    public boolean deleteVector(String collectionName, String id) {
        log.debug("删除向量 - 集合: {}, ID: {}", collectionName, id);

        String collectionId = getCollectionId(collectionName);
        // 使用v2 API: POST /api/v2/tenants/{tenant}/databases/{database}/collections/{collection_id}/delete
        String url = String.format("%s/api/v2/tenants/%s/databases/%s/collections/%s/delete", 
                chromaProperties.getServerUrl(), DEFAULT_TENANT, DEFAULT_DATABASE, collectionId);

        Map<String, Object> request = new HashMap<>();
        request.put("ids", Collections.singletonList(id));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            if (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.NO_CONTENT) {
                log.debug("向量删除成功 - ID: {}", id);
                return true;
            }
            throw new RuntimeException("向量删除失败");
        } catch (Exception e) {
            log.error("向量删除失败 - ID: {}, 错误: {}", id, e.getMessage());
            throw new RuntimeException("向量删除失败", e);
        }
    }

    /**
     * 根据元数据删除向量
     */
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "deleteByMetadataFallback")
    @Retry(name = CIRCUIT_BREAKER_NAME)
    public boolean deleteByMetadata(String collectionName, Map<String, Object> whereClause) {
        log.debug("根据元数据删除向量 - 集合: {}, 条件: {}", collectionName, whereClause);

        String collectionId = getCollectionId(collectionName);
        // 使用v2 API: POST /api/v2/tenants/{tenant}/databases/{database}/collections/{collection_id}/delete
        String url = String.format("%s/api/v2/tenants/%s/databases/%s/collections/%s/delete", 
                chromaProperties.getServerUrl(), DEFAULT_TENANT, DEFAULT_DATABASE, collectionId);

        Map<String, Object> request = new HashMap<>();
        
        // 转换where参数类型
        Map<String, String> stringWhere = new HashMap<>();
        if (whereClause != null) {
            whereClause.forEach((k, v) -> stringWhere.put(k, v != null ? v.toString() : ""));
        }
        request.put("where", stringWhere);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            if (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.NO_CONTENT) {
                log.debug("根据元数据删除向量成功");
                return true;
            }
            throw new RuntimeException("根据元数据删除向量失败");
        } catch (Exception e) {
            log.error("根据元数据删除向量失败: {}", e.getMessage());
            throw new RuntimeException("根据元数据删除向量失败", e);
        }
    }

    /**
     * 获取集合中的向量数量
     */
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "countVectorsFallback")
    @Retry(name = CIRCUIT_BREAKER_NAME)
    public long countVectors(String collectionName) {
        log.debug("获取向量数量 - 集合: {}", collectionName);

        String collectionId = getCollectionId(collectionName);
        // 使用v2 API: GET /api/v2/tenants/{tenant}/databases/{database}/collections/{collection_id}/count
        String url = String.format("%s/api/v2/tenants/%s/databases/%s/collections/%s/count", 
                chromaProperties.getServerUrl(), DEFAULT_TENANT, DEFAULT_DATABASE, collectionId);

        try {
            ResponseEntity<Integer> response = restTemplate.getForEntity(url, Integer.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody().longValue();
            }
            return 0;
        } catch (Exception e) {
            log.error("获取向量数量失败: {}", e.getMessage());
            throw new RuntimeException("获取向量数量失败", e);
        }
    }

    /**
     * 删除集合
     */
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "deleteCollectionFallback")
    @Retry(name = CIRCUIT_BREAKER_NAME)
    public boolean deleteCollection(String collectionName) {
        log.debug("删除集合: {}", collectionName);

        String collectionId = getCollectionId(collectionName);
        // 使用v2 API: DELETE /api/v2/tenants/{tenant}/databases/{database}/collections/{collection_id}
        String url = String.format("%s/api/v2/tenants/%s/databases/%s/collections/%s", 
                chromaProperties.getServerUrl(), DEFAULT_TENANT, DEFAULT_DATABASE, collectionId);

        try {
            restTemplate.delete(url);
            log.debug("集合删除成功: {}", collectionName);
            return true;
        } catch (Exception e) {
            log.error("集合删除失败: {}", e.getMessage());
            throw new RuntimeException("集合删除失败", e);
        }
    }

    // ==================== 熔断降级方法 ====================

    private boolean insertVectorFallback(String collectionName, String id, List<Float> embedding,
            Map<String, Object> metadata, String document, Throwable t) {
        log.error("ChromaDB插入向量熔断降级 - ID: {}, 错误: {}", id, t.getMessage());
        return false;
    }

    private boolean batchInsertVectorsFallback(String collectionName, List<String> ids,
            List<List<Float>> embeddings, List<Map<String, Object>> metadatas, 
            List<String> documents, Throwable t) {
        log.error("ChromaDB批量插入向量熔断降级 - 数量: {}, 错误: {}", ids.size(), t.getMessage());
        return false;
    }

    private List<Map<String, Object>> querySimilarFallback(String collectionName, 
            List<Float> queryEmbedding, int topK, Map<String, Object> whereClause, Throwable t) {
        log.error("ChromaDB相似度检索熔断降级 - 错误: {}", t.getMessage());
        return Collections.emptyList();
    }

    private boolean deleteVectorFallback(String collectionName, String id, Throwable t) {
        log.error("ChromaDB删除向量熔断降级 - ID: {}, 错误: {}", id, t.getMessage());
        return false;
    }

    private boolean deleteByMetadataFallback(String collectionName, 
            Map<String, Object> whereClause, Throwable t) {
        log.error("ChromaDB根据元数据删除熔断降级 - 错误: {}", t.getMessage());
        return false;
    }

    private long countVectorsFallback(String collectionName, Throwable t) {
        log.error("ChromaDB获取向量数量熔断降级 - 错误: {}", t.getMessage());
        return 0;
    }

    private boolean deleteCollectionFallback(String collectionName, Throwable t) {
        log.error("ChromaDB删除集合熔断降级 - 错误: {}", t.getMessage());
        return false;
    }

    // ==================== 业务友好封装方法 ====================

    /**
     * 添加向量（业务简化方法）
     * 
     * @param documentId 文档ID
     * @param content 文本内容
     * @param embedding 向量
     * @return 向量ID
     */
    public String add(Long documentId, String content, List<Float> embedding) {
        String vectorId = UUID.randomUUID().toString();
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("document_id", documentId);
        metadata.put("chunk_index", 0);
        
        boolean success = insertVector(DEFAULT_COLLECTION, vectorId, embedding, metadata, content);
        
        if (success) {
            return vectorId;
        }
        throw new RuntimeException("向量添加失败");
    }

    /**
     * 搜索相似向量（业务简化方法）
     * 
     * @param embedding 查询向量
     * @param topK 返回数量
     * @return 搜索结果列表
     */
    public List<SearchResult> search(List<Float> embedding, int topK) {
        List<Map<String, Object>> results = querySimilar(DEFAULT_COLLECTION, embedding, topK, null);
        
        List<SearchResult> searchResults = new ArrayList<>();
        for (Map<String, Object> result : results) {
            String id = (String) result.get("id");
            String document = (String) result.get("document");
            Double distance = (Double) result.get("distance");
            Map<String, Object> metadata = (Map<String, Object>) result.get("metadata");
            
            Long documentId = null;
            Long chunkId = null;
            if (metadata != null) {
                Object docIdObj = metadata.get("document_id");
                if (docIdObj instanceof Number) {
                    documentId = ((Number) docIdObj).longValue();
                }
                Object chunkIdObj = metadata.get("chunk_id");
                if (chunkIdObj instanceof Number) {
                    chunkId = ((Number) chunkIdObj).longValue();
                }
            }
            
            // 将距离转换为相似度（距离越小，相似度越高）
            double similarity = 1.0 - (distance != null ? distance : 0.0);
            
            searchResults.add(new SearchResult(documentId, chunkId, document, similarity, id));
        }
        
        return searchResults;
    }

    /**
     * 根据文档ID删除向量
     * 
     * @param documentId 文档ID
     */
    public void deleteByDocumentId(Long documentId) {
        Map<String, Object> whereClause = new HashMap<>();
        whereClause.put("document_id", documentId);
        
        deleteByMetadata(DEFAULT_COLLECTION, whereClause);
        log.info("已删除文档 {} 的所有向量数据", documentId);
    }
}