package com.enterprise.rag.application.service;

import com.enterprise.rag.application.dto.DocumentListResponse;
import com.enterprise.rag.application.dto.DocumentUploadRequest;
import com.enterprise.rag.application.dto.DocumentUploadResponse;
import com.enterprise.rag.application.dto.DocumentQueryRequest;

/**
 * 文档管理服务接口
 */
public interface DocumentService {

    /**
     * 上传文档
     * 
     * @param request 上传请求
     * @param fileBytes 文件字节数组
     * @param originalFilename 原始文件名
     * @return 上传响应
     */
    DocumentUploadResponse uploadDocument(DocumentUploadRequest request, byte[] fileBytes, String originalFilename);

    /**
     * 获取文档列表
     * 
     * @param queryRequest 查询条件
     * @return 文档列表
     */
    DocumentListResponse getDocumentList(DocumentQueryRequest queryRequest);

    /**
     * 获取文档详情
     * 
     * @param id 文档ID
     * @return 文档详情
     */
    DocumentUploadResponse getDocumentById(Long id);

    /**
     * 更新文档状态
     * 
     * @param id 文档ID
     * @param status 状态
     * @return 更新后的文档
     */
    DocumentUploadResponse updateDocumentStatus(Long id, Integer status);

    /**
     * 删除文档（联动删除向量）
     * 
     * @param id 文档ID
     */
    void deleteDocument(Long id);

    /**
     * 异步处理文档（切片+向量化）
     * 
     * @param documentId 文档ID
     */
    void asyncProcessDocument(Long documentId);

    /**
     * 获取文档内容
     * 
     * @param id 文档ID
     * @return 文档内容
     */
    String getDocumentContent(Long id);
}