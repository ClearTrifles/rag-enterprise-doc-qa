package com.enterprise.rag.infrastructure.vector;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * 清理所有1536维度的旧集合
 */
public class CleanupOldCollectionsTest {
    
    private static final String BASE_URL = "http://localhost:8000";
    private static final String TENANT = "default_tenant";
    private static final String DATABASE = "default_database";
    
    @Test
    void cleanupOldCollections() throws Exception {
        HttpClient httpClient = HttpClient.newBuilder().build();
        
        // 1. 获取集合列表
        String listUrl = String.format("%s/api/v2/tenants/%s/databases/%s/collections", 
                BASE_URL, TENANT, DATABASE);
        
        HttpRequest listRequest = HttpRequest.newBuilder()
                .uri(URI.create(listUrl))
                .GET()
                .build();
        
        HttpResponse<String> listResponse = httpClient.send(listRequest, HttpResponse.BodyHandlers.ofString());
        System.out.println("获取集合列表: " + listResponse.statusCode());
        
        String responseBody = listResponse.body();
        
        // 解析所有集合，找到维度为1536的集合
        int start = 0;
        int deletedCount = 0;
        
        while (true) {
            // 查找下一个集合的维度信息
            int dimStart = responseBody.indexOf("\"dimension\":1536", start);
            if (dimStart == -1) {
                break;
            }
            
            // 向前查找这个集合的id
            int idStart = responseBody.lastIndexOf("\"id\":", dimStart);
            if (idStart == -1) {
                start = dimStart + 14; // 跳过 "dimension":1536
                continue;
            }
            
            idStart += 6; // 跳过 "id":
            int idEnd = responseBody.indexOf("\"", idStart);
            if (idEnd == -1) {
                start = dimStart + 14;
                continue;
            }
            
            String collectionId = responseBody.substring(idStart, idEnd);
            
            // 查找集合名称
            int nameStart = responseBody.indexOf("\"name\":", idEnd);
            String collectionName = "unknown";
            if (nameStart != -1) {
                nameStart += 8; // 跳过 "name":
                int nameEnd = responseBody.indexOf("\"", nameStart);
                if (nameEnd != -1) {
                    collectionName = responseBody.substring(nameStart, nameEnd);
                }
            }
            
            System.out.println("找到1536维度集合: " + collectionName + " (" + collectionId + ")");
            
            // 删除这个集合
            String deleteUrl = String.format("%s/api/v2/tenants/%s/databases/%s/collections/%s", 
                    BASE_URL, TENANT, DATABASE, collectionId);
            
            HttpRequest deleteRequest = HttpRequest.newBuilder()
                    .uri(URI.create(deleteUrl))
                    .DELETE()
                    .build();
            
            HttpResponse<String> deleteResponse = httpClient.send(deleteRequest, HttpResponse.BodyHandlers.ofString());
            
            if (deleteResponse.statusCode() == 200 || deleteResponse.statusCode() == 204) {
                System.out.println("✓ 删除成功");
                deletedCount++;
            } else {
                System.out.println("✗ 删除失败: " + deleteResponse.statusCode());
            }
            
            start = dimStart + 14;
        }
        
        System.out.println("\n=== 清理完成 ===");
        System.out.println("共删除 " + deletedCount + " 个1536维度的集合");
    }
}