package com.enterprise.rag.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 问答响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QAAnswerResponse {

    private String answer;

    private List<ReferenceSource> sources;

    private Integer tokenCount;

    private Integer responseTime;

    private String status;

    private LocalDateTime timestamp;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReferenceSource {
        private Long documentId;
        private String documentName;
        private Long chunkId;
        private String content;
        private Double similarity;
    }
}