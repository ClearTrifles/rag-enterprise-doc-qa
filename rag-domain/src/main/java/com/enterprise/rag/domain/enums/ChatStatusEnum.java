package com.enterprise.rag.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 问答状态枚举
 * 用于管理问答记录状态，杜绝硬编码状态值
 *
 * @author Enterprise RAG Team
 * @since 2026-06-18
 */
@Getter
@AllArgsConstructor
public enum ChatStatusEnum {

    /**
     * 待处理 - 问题已提交，等待处理
     */
    PENDING(0, "待处理"),

    /**
     * 成功 - 问答成功完成
     */
    SUCCESS(1, "成功"),

    /**
     * 无匹配 - 知识库无匹配结果
     */
    NO_MATCH(2, "无匹配"),

    /**
     * 已过滤 - 敏感词过滤拒绝
     */
    FILTERED(3, "已过滤"),

    /**
     * 限流 - 超过限流阈值拒绝
     */
    RATE_LIMITED(4, "限流"),

    /**
     * 失败 - 问答处理异常
     */
    FAILED(5, "失败");

    /**
     * 状态编码
     */
    private final Integer code;

    /**
     * 状态描述
     */
    private final String desc;

    /**
     * 根据编码获取枚举
     *
     * @param code 状态编码
     * @return 枚举对象，不存在则返回null
     */
    public static ChatStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ChatStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 判断是否为成功状态
     *
     * @return true-成功，false-非成功
     */
    public boolean isSuccess() {
        return this == SUCCESS;
    }

    /**
     * 判断是否为拒绝状态（被过滤或限流）
     *
     * @return true-拒绝，false-非拒绝
     */
    public boolean isRejected() {
        return this == FILTERED || this == RATE_LIMITED;
    }

    /**
     * 判断是否为异常状态
     *
     * @return true-异常，false-正常
     */
    public boolean isAbnormal() {
        return this == NO_MATCH || this == FAILED;
    }
}