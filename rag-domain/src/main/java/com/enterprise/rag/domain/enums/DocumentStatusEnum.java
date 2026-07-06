package com.enterprise.rag.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 文档状态枚举
 * 用于管理文档生命周期状态，杜绝硬编码状态值
 *
 * @author Enterprise RAG Team
 * @since 2026-06-18
 */
@Getter
@AllArgsConstructor
public enum DocumentStatusEnum {

    /**
     * 上传中 - 文档正在上传
     */
    UPLOADING(0, "上传中"),

    /**
     * 处理中 - 文档正在解析切片
     */
    PROCESSING(1, "处理中"),

    /**
     * 已启用 - 文档可用，参与召回
     */
    ENABLED(2, "已启用"),

    /**
     * 已禁用 - 文档禁用，不参与召回
     */
    DISABLED(3, "已禁用"),

    /**
     * 处理失败 - 文档处理异常
     */
    FAILED(4, "处理失败");

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
    public static DocumentStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (DocumentStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 判断是否为有效状态（可参与召回）
     *
     * @return true-可参与召回，false-不可参与召回
     */
    public boolean isAvailable() {
        return this == ENABLED;
    }

    /**
     * 判断是否为处理中状态
     *
     * @return true-处理中，false-非处理中
     */
    public boolean isProcessing() {
        return this == UPLOADING || this == PROCESSING;
    }
}