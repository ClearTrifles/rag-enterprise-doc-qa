package com.enterprise.rag.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 文件分类枚举
 * 用于多模态处理路由，将文件类型归类到不同处理管道
 * 
 * 设计考虑：
 * - 文本类：直接提取文本内容
 * - 音频类：需要语音转文字服务
 * - 图片类：需要OCR识别服务
 * - 视频类：需要视频转文字服务
 * - 其他类：预留扩展
 * 
 * @author Enterprise RAG Team
 * @since 2026-06-18
 */
@Getter
@AllArgsConstructor
public enum FileCategory {

    /**
     * 文本类文档
     * 处理方式：直接提取文本内容
     * 当前支持：PDF、TXT、DOCX
     */
    TEXT("TEXT", "文本类", "直接提取文本内容"),

    /**
     * 音频类文档（预留扩展）
     * 处理方式：语音转文字（ASR）
     * 预留支持：MP3、WAV
     */
    AUDIO("AUDIO", "音频类", "语音转文字（ASR）"),

    /**
     * 图片类文档（预留扩展）
     * 处理方式：OCR文字识别
     * 预留支持：JPG、PNG
     */
    IMAGE("IMAGE", "图片类", "OCR文字识别"),

    /**
     * 视频类文档（预留扩展）
     * 处理方式：视频转文字（语音识别+OCR）
     * 预留支持：MP4
     */
    VIDEO("VIDEO", "视频类", "视频转文字"),

    /**
     * 其他类（预留扩展）
     * 处理方式：自定义处理
     */
    OTHER("OTHER", "其他类", "自定义处理");

    /**
     * 分类编码
     */
    private final String code;

    /**
     * 分类名称
     */
    private final String name;

    /**
     * 处理方式描述
     */
    private final String processMethod;

    /**
     * 判断是否为当前支持的分类（仅文本类）
     *
     * @return true-当前支持，false-预留扩展
     */
    public boolean isSupportedNow() {
        return this == TEXT;
    }

    /**
     * 判断是否为多模态分类（需要特殊处理）
     *
     * @return true-需要特殊处理，false-普通文本处理
     */
    public boolean isMultimodal() {
        return this == AUDIO || this == IMAGE || this == VIDEO;
    }
}