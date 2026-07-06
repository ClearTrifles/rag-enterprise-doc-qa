package com.enterprise.rag.common.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 问答应答类型枚举
 * <p>
 * 用于区分RAG问答的三种应答模式：
 * 1. RAG_ANSWER: 检索命中知识库，基于文档回答
 * 2. NEED_CONFIRM_GENERAL: 未检索到知识库内容，需要前端弹窗询问是否开启通用问答
 * 3. GENERAL_ANSWER: 用户确认后，大模型通用自由问答
 * </p>
 *
 * @author Enterprise RAG Team
 * @version 1.0.0
 */
@Getter
@AllArgsConstructor
@Schema(description = "问答应答类型枚举")
public enum ChatReplyTypeEnum {

    /**
     * RAG应答：检索命中知识库，基于文档回答
     */
    RAG_ANSWER("RAG_ANSWER", "基于知识库文档回答"),

    /**
     * 需要确认通用问答：未检索到知识库内容，需要前端弹窗询问是否开启通用问答
     */
    NEED_CONFIRM_GENERAL("NEED_CONFIRM_GENERAL", "需要确认开启通用问答"),

    /**
     * 通用问答：用户确认后，大模型通用自由问答
     */
    GENERAL_ANSWER("GENERAL_ANSWER", "通用自由问答");

    /**
     * 枚举编码
     */
    @Schema(description = "枚举编码", example = "RAG_ANSWER")
    private final String code;

    /**
     * 枚举描述
     */
    @Schema(description = "枚举描述", example = "基于知识库文档回答")
    private final String description;
}
