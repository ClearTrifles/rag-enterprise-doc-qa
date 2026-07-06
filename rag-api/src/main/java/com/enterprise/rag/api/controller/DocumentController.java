package com.enterprise.rag.api.controller;

import com.enterprise.rag.application.dto.DocumentListResponse;
import com.enterprise.rag.application.dto.DocumentUploadRequest;
import com.enterprise.rag.application.dto.DocumentUploadResponse;
import com.enterprise.rag.application.dto.DocumentQueryRequest;
import com.enterprise.rag.application.service.DocumentService;
import com.enterprise.rag.common.exception.BizException;
import com.enterprise.rag.common.exception.ExceptionEnum;
import com.enterprise.rag.common.result.Result;
import com.enterprise.rag.domain.entity.RagDocument;
import com.enterprise.rag.domain.enums.DocumentStatusEnum;
import com.enterprise.rag.infrastructure.mapper.RagDocumentMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文档管理Controller
 * 
 * RESTful接口规范：
 * - POST /api/documents - 上传文档
 * - GET /api/documents - 文档列表
 * - GET /api/documents/{id} - 文档详情
 * - PUT /api/documents/{id} - 更新文档状态
 * - DELETE /api/documents/{id} - 删除文档
 */
@Slf4j
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(name = "文档管理", description = "文档上传、查询、更新、删除接口")
public class DocumentController {

    private final DocumentService documentService;
    private final RagDocumentMapper documentMapper;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传文档", description = "上传PDF/TXT/DOCX文档，自动进行切片和向量化处理")
    public Result<DocumentUploadResponse> upload(
            @Parameter(description = "文档名称") @RequestParam("documentName") String documentName,
            @Parameter(description = "文件") @RequestParam("file") MultipartFile file) throws Exception {
        
        log.info("接收到文档上传请求: {}, 文件大小: {} bytes", documentName, file.getSize());
        
        DocumentUploadRequest request = DocumentUploadRequest.builder()
                .documentName(documentName)
                .build();
        
        DocumentUploadResponse response = documentService.uploadDocument(
                request, 
                file.getBytes(), 
                file.getOriginalFilename()
        );
        
        return Result.success(response);
    }

    @GetMapping
    @Operation(summary = "文档列表", description = "分页查询文档列表，支持按名称、状态、类型筛选")
    public Result<DocumentListResponse> list(
            @Parameter(description = "文档名称") @RequestParam(required = false) String documentName,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "文件类型") @RequestParam(required = false) String fileType,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize) {
        
        DocumentQueryRequest request = DocumentQueryRequest.builder()
                .documentName(documentName)
                .status(status)
                .fileType(fileType)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .build();
        
        DocumentListResponse response = documentService.getDocumentList(request);
        return Result.success(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "文档详情", description = "根据ID获取文档详情")
    public Result<DocumentUploadResponse> getById(
            @Parameter(description = "文档ID") @PathVariable Long id) {
        
        DocumentUploadResponse response = documentService.getDocumentById(id);
        return Result.success(response);
    }

    @GetMapping("/{id}/content")
    @Operation(summary = "文档内容", description = "根据ID获取文档内容")
    public Result<String> getDocumentContent(
            @Parameter(description = "文档ID") @PathVariable Long id) {
        
        log.info("接收到文档内容请求: {}", id);
        String content = documentService.getDocumentContent(id);
        return Result.success(content);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "更新文档状态", description = "更新文档启用/禁用状态")
    public Result<DocumentUploadResponse> updateStatus(
            @Parameter(description = "文档ID") @PathVariable Long id,
            @Parameter(description = "状态") @RequestParam Integer status) {
        
        DocumentUploadResponse response = documentService.updateDocumentStatus(id, status);
        return Result.success(response);
    }

    @PutMapping("/{id}/retry-vector")
    @Operation(summary = "重新向量化", description = "重新触发文档的切片和向量化处理")
    public Result<Void> retryVector(
            @Parameter(description = "文档ID") @PathVariable Long id) {
        
        log.info("接收到重新向量化请求: {}", id);
        
        RagDocument document = documentMapper.selectById(id);
        if (document == null || document.getIsDelete() == 1) {
            throw new BizException(ExceptionEnum.DOCUMENT_NOT_FOUND);
        }
        
        document.setStatus(DocumentStatusEnum.PROCESSING.getCode());
        documentMapper.updateById(document);
        
        documentService.asyncProcessDocument(id);
        
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除文档", description = "删除文档及其关联的向量数据")
    public Result<Void> delete(
            @Parameter(description = "文档ID") @PathVariable Long id) {
        
        documentService.deleteDocument(id);
        return Result.success();
    }
}