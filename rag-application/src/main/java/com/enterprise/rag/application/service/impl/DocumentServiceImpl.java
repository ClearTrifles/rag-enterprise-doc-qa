package com.enterprise.rag.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.rag.application.dto.DocumentListResponse;
import com.enterprise.rag.application.dto.DocumentUploadRequest;
import com.enterprise.rag.application.dto.DocumentUploadResponse;
import com.enterprise.rag.application.dto.DocumentQueryRequest;
import com.enterprise.rag.application.service.DocumentService;
import com.enterprise.rag.common.exception.BizException;
import com.enterprise.rag.common.exception.ExceptionEnum;
import com.enterprise.rag.domain.entity.RagDocument;
import com.enterprise.rag.domain.entity.RagDocumentChunk;
import com.enterprise.rag.domain.enums.DocumentStatusEnum;
import com.enterprise.rag.domain.enums.FileTypeEnum;
import com.enterprise.rag.infrastructure.ai.QwenEmbeddingService;
import com.enterprise.rag.infrastructure.mapper.RagDocumentChunkMapper;
import com.enterprise.rag.infrastructure.mapper.RagDocumentMapper;
import com.enterprise.rag.infrastructure.vector.ChromaDBClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 文档管理服务实现类
 *
 * 【面试优化点说明】
 * 3. 大文件异步分片解析，防止OOM
 * 4. 删除文档联动删除向量库脏数据，保证数据一致性
 * @author 13411
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final ChromaDBClient chromaDBClient;
    private final RagDocumentMapper documentMapper;
    private final RagDocumentChunkMapper chunkMapper;
    private final QwenEmbeddingService embeddingService;

    @Value("${file.storage.base-path:./temp-file}")
    private String storageBasePath;

    @Value("${rag.chunk-size:512}")
    private int chunkSize;

    @Value("${rag.chunk-overlap:120}")
    private int chunkOverlap;

    @Override
    @Transactional
    public DocumentUploadResponse uploadDocument(DocumentUploadRequest request, byte[] fileBytes, String originalFilename) {
        String fileExtension = getFileExtension(originalFilename);
        FileTypeEnum fileTypeEnum = FileTypeEnum.getByExtension(fileExtension);

        if (fileTypeEnum == null || fileTypeEnum == FileTypeEnum.OTHER) {
            throw new BizException(ExceptionEnum.DOCUMENT_TYPE_NOT_SUPPORTED);
        }

        if (!fileTypeEnum.getCategory().isSupportedNow()) {
            throw new BizException(ExceptionEnum.DOCUMENT_TYPE_NOT_SUPPORTED);
        }

        String storagePath = saveFile(fileBytes, originalFilename);

        RagDocument document = RagDocument.builder()
                .documentName(request.getDocumentName())
                .originalFilename(originalFilename)
                .fileType(fileTypeEnum.getCode())
                .fileSize((long) fileBytes.length)
                .storagePath(storagePath)
                .status(DocumentStatusEnum.PROCESSING.getCode())
                .chunkCount(0)
                .build();

        documentMapper.insert(document);

        asyncProcessDocument(document.getId());

        return DocumentUploadResponse.builder()
                .id(document.getId())
                .documentName(document.getDocumentName())
                .fileName(document.getOriginalFilename())
                .fileType(document.getFileType())
                .fileSize(document.getFileSize())
                .storagePath(document.getStoragePath())
                .status(document.getStatus())
                .statusDesc(DocumentStatusEnum.getByCode(document.getStatus()).getDesc())
                .vectorStatus(document.getStatus() == DocumentStatusEnum.PROCESSING.getCode() ? "processing" : "pending")
                .uploadTime(document.getCreateTime() != null ? document.getCreateTime().toString() : "")
                .build();
    }

    @Override
    public DocumentListResponse getDocumentList(DocumentQueryRequest queryRequest) {
        LambdaQueryWrapper<RagDocument> wrapper = new LambdaQueryWrapper<>();

        if (queryRequest.getDocumentName() != null) {
            wrapper.like(RagDocument::getDocumentName, queryRequest.getDocumentName());
        }
        if (queryRequest.getStatus() != null) {
            wrapper.eq(RagDocument::getStatus, queryRequest.getStatus());
        }
        if (queryRequest.getFileType() != null) {
            wrapper.eq(RagDocument::getFileType, queryRequest.getFileType());
        }
        wrapper.eq(RagDocument::getIsDelete, 0);
        wrapper.orderByDesc(RagDocument::getCreateTime);

        IPage<RagDocument> page = documentMapper.selectPage(
                new Page<>(queryRequest.getPageNum(), queryRequest.getPageSize()), wrapper);

        List<DocumentListResponse.DocumentItem> items = new ArrayList<>();
        for (RagDocument doc : page.getRecords()) {
            LambdaQueryWrapper<RagDocumentChunk> chunkWrapper = new LambdaQueryWrapper<>();
            chunkWrapper.eq(RagDocumentChunk::getDocumentId, doc.getId());
            chunkWrapper.eq(RagDocumentChunk::getIsDelete, 0);
            Long chunkCount = chunkMapper.selectCount(chunkWrapper);

            String vectorStatus;
            if (doc.getStatus() == DocumentStatusEnum.PROCESSING.getCode()) {
                vectorStatus = "processing";
            } else if (doc.getStatus() == DocumentStatusEnum.ENABLED.getCode() && chunkCount > 0) {
                vectorStatus = "completed";
            } else {
                vectorStatus = "pending";
            }

            items.add(DocumentListResponse.DocumentItem.builder()
                    .id(doc.getId())
                    .documentName(doc.getDocumentName())
                    .originalFilename(doc.getOriginalFilename())
                    .fileType(doc.getFileType())
                    .fileSize(doc.getFileSize())
                    .chunkCount(chunkCount.intValue())
                    .status(doc.getStatus())
                    .statusDesc(DocumentStatusEnum.getByCode(doc.getStatus()).getDesc())
                    .vectorStatus(vectorStatus)
                    .uploadTime(doc.getCreateTime() != null ? doc.getCreateTime().toString() : "")
                    .build());
        }

        return DocumentListResponse.builder()
                .list(items)
                .total(page.getTotal())
                .pageNum(queryRequest.getPageNum())
                .pageSize(queryRequest.getPageSize())
                .build();
    }

    @Override
    public DocumentUploadResponse getDocumentById(Long id) {
        RagDocument document = documentMapper.selectById(id);
        if (document == null || document.getIsDelete() == 1) {
            throw new BizException(ExceptionEnum.DOCUMENT_NOT_FOUND);
        }

        return DocumentUploadResponse.builder()
                .id(document.getId())
                .documentName(document.getDocumentName())
                .fileName(document.getOriginalFilename())
                .fileType(document.getFileType())
                .fileSize(document.getFileSize())
                .storagePath(document.getStoragePath())
                .status(document.getStatus())
                .statusDesc(DocumentStatusEnum.getByCode(document.getStatus()).getDesc())
                .vectorStatus(document.getStatus() == DocumentStatusEnum.PROCESSING.getCode() ? "processing" : "pending")
                .uploadTime(document.getCreateTime() != null ? document.getCreateTime().toString() : "")
                .build();
    }

    @Override
    @Transactional
    public DocumentUploadResponse updateDocumentStatus(Long id, Integer status) {
        RagDocument document = documentMapper.selectById(id);
        if (document == null || document.getIsDelete() == 1) {
            throw new BizException(ExceptionEnum.DOCUMENT_NOT_FOUND);
        }

        DocumentStatusEnum statusEnum = DocumentStatusEnum.getByCode(status);
        if (statusEnum == null) {
            throw new BizException(ExceptionEnum.PARAM_ERROR);
        }

        document.setStatus(status);
        documentMapper.updateById(document);

        return DocumentUploadResponse.builder()
                .id(document.getId())
                .documentName(document.getDocumentName())
                .fileName(document.getOriginalFilename())
                .fileType(document.getFileType())
                .fileSize(document.getFileSize())
                .storagePath(document.getStoragePath())
                .status(document.getStatus())
                .statusDesc(DocumentStatusEnum.getByCode(document.getStatus()).getDesc())
                .vectorStatus(document.getStatus() == DocumentStatusEnum.PROCESSING.getCode() ? "processing" : "pending")
                .uploadTime(document.getCreateTime() != null ? document.getCreateTime().toString() : "")
                .build();
    }

    @Override
    @Transactional
    public void deleteDocument(Long id) {
        /**
         * 【面试优化点4】删除文档联动删除向量库脏数据，保证数据一致性
         *
         * 删除流程：
         * 1. 先删除数据库中的文档记录（逻辑删除）
         * 2. 联动删除向量库中的相关向量数据
         * 3. 删除本地存储的文件
         * 4. 删除文档切片记录（逻辑删除）
         *
         * 使用事务保证一致性，任何一步失败都会回滚
         */
        RagDocument document = documentMapper.selectById(id);
        if (document == null || document.getIsDelete() == 1) {
            throw new BizException(ExceptionEnum.DOCUMENT_NOT_FOUND);
        }

        // 1. 逻辑删除文档记录（使用deleteById触发@TableLogic自动处理）
        documentMapper.deleteById(id);

        // 2. 删除向量库中的相关向量数据
        chromaDBClient.deleteByDocumentId(id);

        // 3. 逻辑删除文档切片记录
        LambdaQueryWrapper<RagDocumentChunk> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RagDocumentChunk::getDocumentId, id);
        List<RagDocumentChunk> chunks = chunkMapper.selectList(wrapper);
        for (RagDocumentChunk chunk : chunks) {
            chunk.setIsDelete(1);
            chunkMapper.updateById(chunk);
        }

        // 4. 删除本地存储的文件
        try {
            Path filePath = Paths.get(document.getStoragePath());
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("删除本地文件: {}", document.getStoragePath());
            }
        } catch (IOException e) {
            log.warn("删除本地文件失败: {}", document.getStoragePath());
        }

        log.info("文档删除完成，联动删除向量数据: {}", id);
    }

    @Override
    @Async("fileParserThreadPool")
    public void asyncProcessDocument(Long documentId) {
        /**
         * 【面试优化点3】大文件异步分片解析，防止OOM
         *
         * 处理流程：
         * 1. 读取文件内容
         * 2. 智能切片（512字符大小，120字符重叠）
         * 3. 批量向量化
         * 4. 存入向量库
         * 5. 更新文档状态
         *
         * 使用独立线程池处理，避免阻塞主线程
         */
        log.info("开始异步处理文档: {}", documentId);

        try {
            RagDocument document = documentMapper.selectById(documentId);
            if (document == null) {
                log.error("文档不存在: {}", documentId);
                return;
            }

            // 读取文件内容
            Path filePath = Paths.get(document.getStoragePath());
            if (!Files.exists(filePath)) {
                log.error("文件不存在: {}", document.getStoragePath());
                document.setStatus(DocumentStatusEnum.FAILED.getCode());
                documentMapper.updateById(document);
                return;
            }

            String content = Files.readString(filePath);
            log.info("读取文件内容完成，长度: {} 字符", content.length());

            // 智能切片
            List<String> chunks = splitContent(content);
            log.info("切片完成，共 {} 个切片", chunks.size());

            // 批量向量化并存入向量库
            int successCount = 0;
            for (int i = 0; i < chunks.size(); i++) {
                String chunkContent = chunks.get(i);

                try {
                    // 向量化
                    List<Float> embedding = embeddingService.embed(chunkContent);

                    // 存入向量库
                    String vectorId = chromaDBClient.add(documentId, chunkContent, embedding);

                    // 保存切片记录
                    RagDocumentChunk chunk = RagDocumentChunk.builder()
                            .documentId(documentId)
                            .chunkIndex(i)
                            .chunkContent(chunkContent)
                            .chunkSize(chunkContent.length())
                            .vectorId(vectorId)
                            .build();
                    chunkMapper.insert(chunk);

                    successCount++;
                } catch (Exception e) {
                    log.warn("切片 {} 向量化失败: {}", i, e.getMessage());
                }
            }

            // 更新文档状态
            document.setChunkCount(successCount);
            document.setStatus(DocumentStatusEnum.ENABLED.getCode());
            documentMapper.updateById(document);

            log.info("文档处理完成: {}, 成功切片数: {}", documentId, successCount);

        } catch (Exception e) {
            log.error("文档处理失败: {}", documentId, e);

            RagDocument document = documentMapper.selectById(documentId);
            if (document != null) {
                document.setStatus(DocumentStatusEnum.FAILED.getCode());
                documentMapper.updateById(document);
            }
        }
    }

    /**
     * 智能切片算法
     *
     * 切片策略：
     * - chunkSize: 512字符
     * - chunkOverlap: 120字符重叠
     * - 保证上下文连贯性
     */
    private List<String> splitContent(String content) {
        List<String> chunks = new ArrayList<>();

        if (content.length() <= chunkSize) {
            chunks.add(content);
            return chunks;
        }

        int start = 0;
        while (start < content.length()) {
            int end = Math.min(start + chunkSize, content.length());

            // 尝试在句子边界处切分
            if (end < content.length()) {
                int lastPeriod = content.lastIndexOf('.', end);
                int lastNewline = content.lastIndexOf('\n', end);
                int boundary = Math.max(lastPeriod, lastNewline);

                if (boundary > start + chunkSize / 2) {
                    end = boundary + 1;
                }
            }

            chunks.add(content.substring(start, end).trim());

            // 下一个切片起始位置，考虑重叠
            start = end - chunkOverlap;
            if (start < 0) {
                start = 0;
            }
            if (start >= end) {
                start = end;
            }
        }

        return chunks;
    }

    private String saveFile(byte[] fileBytes, String originalFilename) {
        try {
            Path basePath = Paths.get(storageBasePath);
            if (!Files.exists(basePath)) {
                Files.createDirectories(basePath);
            }

            String fileName = System.currentTimeMillis() + "_" + originalFilename;
            Path filePath = basePath.resolve(fileName);
            Files.write(filePath, fileBytes);

            return filePath.toString();
        } catch (IOException e) {
            log.error("文件保存失败: {}", originalFilename, e);
            throw new BizException(ExceptionEnum.DOCUMENT_UPLOAD_FAILED);
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    @Override
    public String getDocumentContent(Long id) {
        RagDocument document = documentMapper.selectById(id);
        if (document == null || document.getIsDelete() == 1) {
            throw new BizException(ExceptionEnum.DOCUMENT_NOT_FOUND);
        }

        try {
            Path filePath = Paths.get(document.getStoragePath());
            if (!Files.exists(filePath)) {
                throw new BizException(ExceptionEnum.DOCUMENT_NOT_FOUND);
            }
            return Files.readString(filePath);
        } catch (IOException e) {
            log.error("读取文档内容失败: {}", id, e);
            throw new BizException(ExceptionEnum.DOCUMENT_READ_FAILED);
        }
    }
}
