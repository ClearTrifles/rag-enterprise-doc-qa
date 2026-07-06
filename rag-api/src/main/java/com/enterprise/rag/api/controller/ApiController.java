package com.enterprise.rag.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * API根路径控制器
 * 提供API服务信息和可用端点列表
 */
@RestController
@RequestMapping("/api")
@Tag(name = "API入口", description = "API服务信息")
public class ApiController {

    @GetMapping
    @Operation(summary = "API信息", description = "获取API服务信息和可用端点")
    public ResponseEntity<Map<String, Object>> apiInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("name", "RAG企业文档问答系统");
        info.put("version", "1.0.0");
        info.put("description", "企业级本地RAG文档智能问答系统API");
        
        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("健康检查", "GET /api/health");
        endpoints.put("文档列表", "GET /api/documents");
        endpoints.put("上传文档", "POST /api/documents");
        endpoints.put("文档详情", "GET /api/documents/{id}");
        endpoints.put("更新文档状态", "PUT /api/documents/{id}/status");
        endpoints.put("删除文档", "DELETE /api/documents/{id}");
        endpoints.put("智能问答", "POST /api/qa/ask");
        endpoints.put("问答历史", "GET /api/qa/history");
        
        info.put("endpoints", endpoints);
        info.put("swagger-ui", "http://localhost:8080/swagger-ui.html");
        info.put("knife4j", "http://localhost:8080/doc.html");
        
        return ResponseEntity.ok(info);
    }
}