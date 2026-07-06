package com.enterprise.rag.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.enterprise.rag.domain.enums.ChatStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 问答记录实体类
 * 对应表：rag_chat_record
 * 存储用户提问、AI回答、引用切片
 *
 * @author Enterprise RAG Team
 * @since 2026-06-18
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("rag_chat_record")
public class RagChatRecord {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 会话ID
     */
    @TableField("session_id")
    private String sessionId;

    /**
     * 用户提问内容
     */
    @TableField("user_question")
    private String userQuestion;

    /**
     * AI回答内容
     */
    @TableField("ai_answer")
    private String aiAnswer;

    /**
     * 引用切片ID列表（JSON）
     */
    @TableField("referenced_chunks")
    private String referencedChunks;

    /**
     * 引用文档ID列表（JSON）
     */
    @TableField("referenced_documents")
    private String referencedDocuments;

    /**
     * 消耗Token数量
     */
    @TableField("token_count")
    private Integer tokenCount;

    /**
     * 响应时间（毫秒）
     */
    @TableField("response_time")
    private Integer responseTime;

    /**
     * 问答状态（枚举）
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
     * 获取问答状态枚举
     *
     * @return 问答状态枚举对象
     */
    public ChatStatusEnum getStatusEnum() {
        return ChatStatusEnum.getByCode(this.status);
    }

    /**
     * 设置问答状态枚举
     *
     * @param statusEnum 问答状态枚举对象
     */
    public void setStatusEnum(ChatStatusEnum statusEnum) {
        if (statusEnum != null) {
            this.status = statusEnum.getCode();
        }
    }

    /**
     * 判断问答是否成功
     *
     * @return true-成功，false-非成功
     */
    public boolean isSuccess() {
        ChatStatusEnum statusEnum = getStatusEnum();
        return statusEnum != null && statusEnum.isSuccess();
    }

    /**
     * 判断问答是否被拒绝
     *
     * @return true-拒绝，false-非拒绝
     */
    public boolean isRejected() {
        ChatStatusEnum statusEnum = getStatusEnum();
        return statusEnum != null && statusEnum.isRejected();
    }
}