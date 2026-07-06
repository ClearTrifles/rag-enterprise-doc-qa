package com.enterprise.rag.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 文档切片实体类
 * 对应表：rag_document_chunk
 * 存储切片内容、向量关联、所属文档
 *
 * @author Enterprise RAG Team
 * @since 2026-06-18
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("rag_document_chunk")
public class RagDocumentChunk {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 所属文档ID（外键）
     */
    @TableField("document_id")
    private Long documentId;

    /**
     * 切片序号（从0开始）
     */
    @TableField("chunk_index")
    private Integer chunkIndex;

    /**
     * 切片内容
     */
    @TableField("chunk_content")
    private String chunkContent;

    /**
     * Chroma向量库ID
     */
    @TableField("vector_id")
    private String vectorId;

    /**
     * 切片长度（字符数）
     */
    @TableField("chunk_size")
    private Integer chunkSize;

    /**
     * 相似度分值
     */
    @TableField("similarity_score")
    private BigDecimal similarityScore;

    /**
     * 创建人
     */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private String createBy;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新人
     */
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记（0-正常 1-删除）
     */
    @TableLogic
    @TableField("is_delete")
    private Integer isDelete;

    /**
     * 判断切片是否已向量化
     *
     * @return true-已向量化，false-未向量化
     */
    public boolean isVectorized() {
        return this.vectorId != null && !this.vectorId.isEmpty();
    }

    /**
     * 判断相似度是否达标（>=0.75）
     *
     * @return true-达标，false-不达标
     */
    public boolean isSimilarityQualified() {
        if (this.similarityScore == null) {
            return false;
        }
        return this.similarityScore.compareTo(new BigDecimal("0.75")) >= 0;
    }
}