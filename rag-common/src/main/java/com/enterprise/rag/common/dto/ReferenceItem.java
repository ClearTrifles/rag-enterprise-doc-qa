package com.enterprise.rag.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 溯源引用项DTO
 * <p>
 * 用于封装问答返回结果中的文档引用信息。
 * </p>
 *
 * @author Enterprise RAG Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "溯源引用项")
public class ReferenceItem implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 文档ID
     */
    @Schema(description = "文档ID", example = "1")
    private Long documentId;

    /**
     * 文档名称
     */
    @Schema(description = "文档名称", example = "员工手册.pdf")
    private String documentName;

    /**
     * 页码
     */
    @Schema(description = "页码", example = "1")
    private Integer pageNumber;

    /**
     * 切片内容
     */
    @Schema(description = "切片内容", example = "员工年假计算方法...")
    private String content;

    /**
     * 相似度分数
     */
    @Schema(description = "相似度分数", example = "0.85")
    private Double score;
}
