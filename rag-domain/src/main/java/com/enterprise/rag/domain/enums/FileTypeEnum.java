package com.enterprise.rag.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 文件类型枚举
 * 用于限定支持的文档格式，杜绝硬编码文件类型
 * 
 * 设计考虑：预留多模态扩展槽位，支持后续扩展语音、图片等类型
 * 
 * @author Enterprise RAG Team
 * @since 2026-06-18
 */
@Getter
@AllArgsConstructor
public enum FileTypeEnum {

    /**
     * ==================== 文本类文档（当前支持） ====================
     */
    
    /**
     * PDF文档
     */
    PDF("PDF", "PDF文档", ".pdf", FileCategory.TEXT),

    /**
     * 纯文本
     */
    TXT("TXT", "纯文本", ".txt", FileCategory.TEXT),

    /**
     * Word文档
     */
    DOCX("DOCX", "Word文档", ".docx", FileCategory.TEXT),

    /**
     * ==================== 语音类文档（预留扩展） ====================
     */
    
    /**
     * MP3音频
     */
    MP3("MP3", "MP3音频", ".mp3", FileCategory.AUDIO),

    /**
     * WAV音频
     */
    WAV("WAV", "WAV音频", ".wav", FileCategory.AUDIO),

    /**
     * ==================== 图片类文档（预留扩展） ====================
     */
    
    /**
     * JPEG图片
     */
    JPG("JPG", "JPEG图片", ".jpg", FileCategory.IMAGE),

    /**
     * PNG图片
     */
    PNG("PNG", "PNG图片", ".png", FileCategory.IMAGE),

    /**
     * ==================== 视频类文档（预留扩展） ====================
     */
    
    /**
     * MP4视频
     */
    MP4("MP4", "MP4视频", ".mp4", FileCategory.VIDEO),

    /**
     * ==================== 其他类（预留扩展） ====================
     */
    
    /**
     * 其他格式
     */
    OTHER("OTHER", "其他格式", ".other", FileCategory.OTHER);

    /**
     * 类型编码
     */
    private final String code;

    /**
     * 类型描述
     */
    private final String desc;

    /**
     * 文件扩展名
     */
    private final String extension;

    /**
     * 文件分类（用于多模态处理路由）
     */
    private final FileCategory category;

    /**
     * 根据编码获取枚举
     *
     * @param code 类型编码
     * @return 枚举对象，不存在则返回null
     */
    public static FileTypeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (FileTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 根据文件扩展名获取枚举
     *
     * @param extension 文件扩展名
     * @return 枚举对象，不存在则返回null
     */
    public static FileTypeEnum getByExtension(String extension) {
        if (extension == null) {
            return null;
        }
        String ext = extension.toLowerCase();
        for (FileTypeEnum type : values()) {
            if (type.getExtension().equalsIgnoreCase(ext)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 判断是否为支持的文件类型
     *
     * @param extension 文件扩展名
     * @return true-支持，false-不支持
     */
    public static boolean isSupported(String extension) {
        return getByExtension(extension) != null;
    }
}