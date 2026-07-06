package com.enterprise.rag.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.enterprise.rag.domain.enums.DocumentStatusEnum;
import com.enterprise.rag.domain.enums.FileTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文档实体类
 * 对应表：rag_document
 * 存储文档基础信息、状态、存储路径
 *
 * @author Enterprise RAG Team
 * @since 2026-06-18
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("rag_document")
public class RagDocument {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 文档名称
     */
    @TableField("document_name")
    private String documentName;

    /**
     * 原始文件名
     */
    @TableField("original_filename")
    private String originalFilename;

    /**
     * 文件类型（PDF/TXT/DOCX）
     */
    @TableField("file_type")
    private String fileType;

    /**
     * 文件大小（字节）
     */
    @TableField("file_size")
    private Long fileSize;

    /**
     * 本地存储路径
     */
    @TableField("storage_path")
    private String storagePath;

    /**
     * 切片数量
     */
    @TableField("chunk_count")
    private Integer chunkCount;

    /**
     * 文档状态（枚举）
     */
    @TableField("status")
    private Integer status;

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
     * 获取文档状态枚举
     *
     * @return 文档状态枚举对象
     */
    public DocumentStatusEnum getStatusEnum() {
        return DocumentStatusEnum.getByCode(this.status);
    }

    /**
     * 设置文档状态枚举
     *
     * @param statusEnum 文档状态枚举对象
     */
    public void setStatusEnum(DocumentStatusEnum statusEnum) {
        if (statusEnum != null) {
            this.status = statusEnum.getCode();
        }
    }

    /**
     * 获取文件类型枚举
     *
     * @return 文件类型枚举对象
     */
    public FileTypeEnum getFileTypeEnum() {
        return FileTypeEnum.getByCode(this.fileType);
    }

    /**
     * 设置文件类型枚举
     *
     * @param fileTypeEnum 文件类型枚举对象
     */
    public void setFileTypeEnum(FileTypeEnum fileTypeEnum) {
        if (fileTypeEnum != null) {
            this.fileType = fileTypeEnum.getCode();
        }
    }

    /**
     * 判断文档是否可参与召回
     *
     * @return true-可参与召回，false-不可参与召回
     */
    public boolean isAvailable() {
        DocumentStatusEnum statusEnum = getStatusEnum();
        return statusEnum != null && statusEnum.isAvailable();
    }
}