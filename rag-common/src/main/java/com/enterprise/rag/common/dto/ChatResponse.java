package com.enterprise.rag.common.dto;

import com.enterprise.rag.common.enums.ChatReplyTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 问答响应DTO
 * <p>
 * 用于封装RAG问答的三种应答类型返回值。
 * </p>
 *
 * @author Enterprise RAG Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "问答响应DTO")
public class ChatResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 应答类型：RAG_ANSWER / NEED_CONFIRM_GENERAL / GENERAL_ANSWER
     */
    @Schema(description = "应答类型", example = "RAG_ANSWER")
    private ChatReplyTypeEnum replyType;

    /**
     * 原始提问内容
     */
    @Schema(description = "原始提问", example = "公司年假是怎么计算的？")
    private String question;

    /**
     * 回答内容
     * <p>
     * - RAG_ANSWER: 基于文档生成的回答
     * - NEED_CONFIRM_GENERAL: 空字符串
     * - GENERAL_ANSWER: 通用大模型回答
     * </p>
     */
    @Schema(description = "回答内容", example = "根据公司制度...")
    private String answer;

    /**
     * 切片溯源引用列表
     * <p>
     * 仅 RAG_ANSWER 类型时有效，包含文档ID、文档名称、切片内容等信息。
     * </p>
     */
    @Schema(description = "溯源引用列表")
    private List<ReferenceItem> referenceList;

    /**
     * 提示文案
     * <p>
     * 仅 NEED_CONFIRM_GENERAL 类型时填充，用于前端弹窗询问用户是否开启通用问答。
     * </p>
     */
    @Schema(description = "提示文案", example = "未在内部私有知识库查询到该问题相关资料，请问您是否需要AI基于通用公开知识为您解答？")
    private String promptTip;

    /**
     * 创建问答响应 - RAG模式（命中知识库）
     *
     * @param question      原始问题
     * @param answer        回答内容
     * @param referenceList 溯源列表
     * @return ChatResponse
     */
    public static ChatResponse ragAnswer(String question, String answer, List<ReferenceItem> referenceList) {
        return ChatResponse.builder()
                .replyType(ChatReplyTypeEnum.RAG_ANSWER)
                .question(question)
                .answer(answer)
                .referenceList(referenceList)
                .promptTip("")
                .build();
    }

    /**
     * 创建问答响应 - 需要确认模式（未命中知识库）
     *
     * @param question 原始问题
     * @param promptTip 提示文案
     * @return ChatResponse
     */
    public static ChatResponse needConfirmGeneral(String question, String promptTip) {
        return ChatResponse.builder()
                .replyType(ChatReplyTypeEnum.NEED_CONFIRM_GENERAL)
                .question(question)
                .answer("")
                .referenceList(List.of())
                .promptTip(promptTip)
                .build();
    }

    /**
     * 创建问答响应 - 通用问答模式
     *
     * @param question 原始问题
     * @param answer   回答内容
     * @return ChatResponse
     */
    public static ChatResponse generalAnswer(String question, String answer) {
        return ChatResponse.builder()
                .replyType(ChatReplyTypeEnum.GENERAL_ANSWER)
                .question(question)
                .answer(answer)
                .referenceList(List.of())
                .promptTip("")
                .build();
    }
}
