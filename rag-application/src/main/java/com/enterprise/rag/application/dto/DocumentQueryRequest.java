package com.enterprise.rag.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档查询请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentQueryRequest {

    private String documentName;

    private Integer status;

    private String fileType;

    @Min(value = 1, message = "页码最小为1")
    @Builder.Default
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页数量最小为1")
    @Max(value = 100, message = "每页数量最大为100")
    @Builder.Default
    private Integer pageSize = 10;
}