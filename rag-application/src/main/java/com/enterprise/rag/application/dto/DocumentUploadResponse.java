package com.enterprise.rag.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文档上传响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUploadResponse {

    private Long id;

    private String documentName;

    private String fileName;

    private String fileType;

    private Long fileSize;

    private String storagePath;

    private Integer status;

    private String statusDesc;

    private String vectorStatus;

    private String uploadTime;
}