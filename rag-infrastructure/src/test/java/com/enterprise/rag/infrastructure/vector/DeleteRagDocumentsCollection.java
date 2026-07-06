package com.enterprise.rag.infrastructure.vector;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * 删除rag_documents集合的工具类
 * 用于解决维度不匹配问题
 */
public class DeleteRagDocumentsCollection {
    
    private static final String BASE_URL = "http://localhost:8000";
    private static final String TENANT = "default_tenant";
    private static final String DATABASE = "default_database";
    private static final String COLLECTION_NAME = "rag_documents";
    
    public static void main(String[] args) throws Exception {
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
        System.out.println("响应: " + listResponse.body());
        
        // 2. 查找rag_documents集合的ID
        String collectionId = null;
        String responseBody = listResponse.body();
        
        // 简单解析JSON，找到rag_documents的ID
        int nameIndex = responseBody.indexOf("\"name\":\"" + COLLECTION_NAME + "\"");
        if (nameIndex != -1) {
            // 向前查找id字段
            int idStart = responseBody.lastIndexOf("\"id\":", nameIndex);
            if (idStart != -1) {
                idStart += 6; // 跳过"id":
                int idEnd = responseBody.indexOf("\"", idStart);
                if (idEnd != -1) {
                    collectionId = responseBody.substring(idStart, idEnd);
                }
            }
        }
        
        if (collectionId == null) {
            System.out.println("未找到集合: " + COLLECTION_NAME);
            return;
        }
        
        System.out.println("找到集合ID: " + collectionId);
        
        // 3. 删除集合
        String deleteUrl = String.format("%s/api/v2/tenants/%s/databases/%s/collections/%s", 
                BASE_URL, TENANT, DATABASE, collectionId);
        
        HttpRequest deleteRequest = HttpRequest.newBuilder()
                .uri(URI.create(deleteUrl))
                .DELETE()
                .build();
        
        HttpResponse<String> deleteResponse = httpClient.send(deleteRequest, HttpResponse.BodyHandlers.ofString());
        System.out.println("删除集合: " + deleteResponse.statusCode());
        System.out.println("响应: " + deleteResponse.body());
        
        if (deleteResponse.statusCode() == 200 || deleteResponse.statusCode() == 204) {
            System.out.println("✓ 集合删除成功！");
        } else {
            System.out.println("✗ 集合删除失败");
        }
    }
}