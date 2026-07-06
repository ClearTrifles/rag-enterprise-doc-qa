package com.enterprise.rag.application.service;

import com.enterprise.rag.application.dto.DocumentListResponse;
import com.enterprise.rag.application.dto.DocumentQueryRequest;
import com.enterprise.rag.application.dto.DocumentUploadRequest;
import com.enterprise.rag.application.dto.DocumentUploadResponse;
import com.enterprise.rag.application.service.impl.DocumentServiceImpl;
import com.enterprise.rag.common.exception.BizException;
import com.enterprise.rag.common.exception.ExceptionEnum;
import com.enterprise.rag.domain.entity.RagDocument;
import com.enterprise.rag.domain.enums.DocumentStatusEnum;
import com.enterprise.rag.infrastructure.ai.QwenEmbeddingService;
import com.enterprise.rag.infrastructure.mapper.RagDocumentChunkMapper;
import com.enterprise.rag.infrastructure.mapper.RagDocumentMapper;
import com.enterprise.rag.infrastructure.vector.ChromaDBClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DocumentService单元测试
 * 
 * 测试覆盖：
 * 1. 文档上传流程
 * 2. 文档列表查询
 * 3. 文档状态更新
 * 4. 文档删除联动
 * 5. 异常场景测试
 */
@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private ChromaDBClient chromaDBClient;

    @Mock
    private RagDocumentMapper documentMapper;

    @Mock
    private RagDocumentChunkMapper chunkMapper;

    @Mock
    private QwenEmbeddingService embeddingService;

    private DocumentServiceImpl documentService;

    @BeforeEach
    void setUp() {
        documentService = new DocumentServiceImpl(
                chromaDBClient,
                documentMapper,
                chunkMapper,
                embeddingService
        );
        
        org.springframework.test.util.ReflectionTestUtils.setField(
                documentService, "storageBasePath", "./temp-file");
        org.springframework.test.util.ReflectionTestUtils.setField(
                documentService, "chunkSize", 512);
        org.springframework.test.util.ReflectionTestUtils.setField(
                documentService, "chunkOverlap", 120);
    }

    /**
     * 【测试1】文档上传 - 正常流程
     */
    @Test
    void testUploadDocumentSuccess() {
        String fileName = "test.txt";
        byte[] fileBytes = "Hello World".getBytes();
        
        DocumentUploadRequest request = DocumentUploadRequest.builder()
                .documentName("测试文档")
                .build();

        when(documentMapper.insert(any(RagDocument.class))).thenAnswer(invocation -> {
            RagDocument doc = invocation.getArgument(0);
            doc.setId(1L);
            return 1;
        });

        DocumentUploadResponse response = documentService.uploadDocument(
                request, fileBytes, fileName);

        assertNotNull(response);
        assertEquals("测试文档", response.getDocumentName());
        assertEquals("test.txt", response.getOriginalFilename());
        assertEquals("TXT", response.getFileType());
        
        verify(documentMapper).insert(any(RagDocument.class));
    }

    /**
     * 【测试2】文档上传 - 不支持的文件类型
     */
    @Test
    void testUploadDocumentUnsupportedType() {
        String fileName = "test.exe";
        byte[] fileBytes = "Fake exe content".getBytes();
        
        DocumentUploadRequest request = DocumentUploadRequest.builder()
                .documentName("非法文档")
                .build();

        assertThrows(BizException.class, () -> {
            documentService.uploadDocument(request, fileBytes, fileName);
        }, "应该抛出不支持文件类型的异常");
    }

    /**
     * 【测试3】文档列表查询 - 空列表
     */
    @Test
    void testGetDocumentListEmpty() {
        DocumentQueryRequest queryRequest = DocumentQueryRequest.builder()
                .pageNum(1)
                .pageSize(10)
                .build();

        when(documentMapper.selectPage(any(), any())).thenReturn(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10));

        DocumentListResponse response = documentService.getDocumentList(queryRequest);

        assertNotNull(response);
        assertEquals(0L, response.getTotal());
        assertTrue(response.getList().isEmpty());
    }

    /**
     * 【测试4】文档详情查询 - 文档不存在
     */
    @Test
    void testGetDocumentByIdNotFound() {
        when(documentMapper.selectById(999L)).thenReturn(null);

        assertThrows(BizException.class, () -> {
            documentService.getDocumentById(999L);
        }, "应该抛出文档不存在的异常");
    }

    /**
     * 【测试5】文档删除 - 联动删除向量数据
     */
    @Test
    void testDeleteDocumentWithVectorCleanup() {
        RagDocument document = RagDocument.builder()
                .id(1L)
                .documentName("待删除文档")
                .storagePath("./temp-file/test.txt")
                .status(DocumentStatusEnum.ENABLED.getCode())
                .isDelete(0)
                .build();

        when(documentMapper.selectById(1L)).thenReturn(document);
        when(chunkMapper.selectList(any())).thenReturn(java.util.List.of());
        doNothing().when(chromaDBClient).deleteByDocumentId(1L);

        documentService.deleteDocument(1L);

        // 验证逻辑删除
        assertEquals(1, document.getIsDelete());
        verify(documentMapper).updateById(document);
        
        // 验证联动删除向量库
        verify(chromaDBClient).deleteByDocumentId(1L);
        
        System.out.println("✅ 测试通过：文档删除时联动清理向量库数据");
    }

    /**
     * 【测试6】文档删除 - 文档不存在
     */
    @Test
    void testDeleteDocumentNotFound() {
        when(documentMapper.selectById(999L)).thenReturn(null);

        assertThrows(BizException.class, () -> {
            documentService.deleteDocument(999L);
        }, "应该抛出文档不存在的异常");
    }

    /**
     * 【测试7】更新文档状态 - 正常更新
     */
    @Test
    void testUpdateDocumentStatusSuccess() {
        RagDocument document = RagDocument.builder()
                .id(1L)
                .documentName("测试文档")
                .originalFilename("test.txt")
                .fileType("TXT")
                .fileSize(100L)
                .storagePath("./temp-file/test.txt")
                .status(DocumentStatusEnum.PROCESSING.getCode())
                .isDelete(0)
                .build();

        when(documentMapper.selectById(1L)).thenReturn(document);

        DocumentUploadResponse response = documentService.updateDocumentStatus(
                1L, DocumentStatusEnum.ENABLED.getCode());

        assertEquals(DocumentStatusEnum.ENABLED.getCode(), response.getStatus());
        assertEquals(DocumentStatusEnum.ENABLED.getDesc(), response.getStatusDesc());
        
        verify(documentMapper).updateById(document);
    }

    /**
     * 【测试8】更新文档状态 - 无效状态值
     */
    @Test
    void testUpdateDocumentStatusInvalid() {
        RagDocument document = RagDocument.builder()
                .id(1L)
                .documentName("测试文档")
                .isDelete(0)
                .build();

        when(documentMapper.selectById(1L)).thenReturn(document);

        assertThrows(BizException.class, () -> {
            documentService.updateDocumentStatus(1L, 999);
        }, "应该抛出参数错误的异常");
    }
}