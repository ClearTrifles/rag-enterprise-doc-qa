package com.enterprise.rag.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文档列表响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentListResponse {

    private List<DocumentItem> list;

    private Long total;

    private Integer pageNum;

    private Integer pageSize;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentItem {
        private Long id;
        private String documentName;
        private String originalFilename;
        private String fileType;
        private Long fileSize;
        private Integer chunkCount;
        private Integer status;
        private String statusDesc;
        private String vectorStatus;
        private String uploadTime;
    }
}