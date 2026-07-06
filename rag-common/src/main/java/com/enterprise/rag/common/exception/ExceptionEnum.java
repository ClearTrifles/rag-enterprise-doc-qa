package com.enterprise.rag.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 异常枚举
 * <p>
 * 统一定义系统中的异常类型，包含错误码和错误消息。
 * 所有业务异常应使用此枚举进行定义，确保异常信息的统一管理。
 * </p>
 *
 * @author Enterprise RAG Team
 * @version 1.0.0
 */
@Getter
@AllArgsConstructor
public enum ExceptionEnum {

    // ==================== 系统级异常 1xxx ====================
    
    /**
     * 系统异常
     */
    SYSTEM_ERROR(1000, "系统异常，请稍后重试"),
    
    /**
     * 参数校验失败
     */
    PARAM_VALID_ERROR(1001, "参数校验失败"),
    
    /**
     * 请求方式不支持
     */
    METHOD_NOT_SUPPORTED(1002, "请求方式不支持"),
    
    /**
     * 请求路径不存在
     */
    PATH_NOT_FOUND(1003, "请求路径不存在"),
    
    /**
     * 请求超时
     */
    REQUEST_TIMEOUT(1004, "请求超时"),
    
    /**
     * 服务不可用
     */
    SERVICE_UNAVAILABLE(1005, "服务暂时不可用"),

    // ==================== 业务级异常 2xxx ====================
    
    /**
     * 业务异常
     */
    BIZ_ERROR(2000, "业务处理失败"),
    
    /**
     * 数据不存在
     */
    DATA_NOT_FOUND(2001, "数据不存在"),
    
    /**
     * 数据已存在
     */
    DATA_ALREADY_EXISTS(2002, "数据已存在"),
    
    /**
     * 数据状态异常
     */
    DATA_STATUS_ERROR(2003, "数据状态异常"),
    
    /**
     * 操作不允许
     */
    OPERATION_NOT_ALLOWED(2004, "操作不允许"),
    
    /**
     * 权限不足
     */
    PERMISSION_DENIED(2005, "权限不足"),

    // ==================== 文档处理异常 3xxx ====================
    
    /**
     * 文件上传失败
     */
    FILE_UPLOAD_ERROR(3000, "文件上传失败"),
    
    /**
     * 文件不存在
     */
    FILE_NOT_FOUND(3001, "文件不存在"),
    
    /**
     * 文件格式不支持
     */
    FILE_FORMAT_NOT_SUPPORTED(3002, "文件格式不支持"),
    
    /**
     * 文件大小超限
     */
    FILE_SIZE_EXCEEDED(3003, "文件大小超过限制"),
    
    /**
     * 文件解析失败
     */
    FILE_PARSE_ERROR(3004, "文件解析失败"),
    
    /**
     * 文档切片失败
     */
    DOCUMENT_CHUNK_ERROR(3005, "文档切片失败"),
    
    /**
     * 文档向量化失败
     */
    DOCUMENT_VECTORIZE_ERROR(3006, "文档向量化失败"),
    
    /**
     * 文档类型不支持
     */
    DOCUMENT_TYPE_NOT_SUPPORTED(3007, "文档类型不支持"),
    
    /**
     * 文档不存在
     */
    DOCUMENT_NOT_FOUND(3008, "文档不存在"),
    
    /**
     * 文档上传失败
     */
    DOCUMENT_UPLOAD_FAILED(3009, "文档上传失败"),
    
    /**
     * 文档读取失败
     */
    DOCUMENT_READ_FAILED(3010, "文档读取失败"),
    
    /**
     * 参数错误
     */
    PARAM_ERROR(3011, "参数错误"),

    // ==================== AI服务异常 4xxx ====================
    
    /**
     * AI服务调用失败
     */
    AI_SERVICE_ERROR(4000, "AI服务调用失败"),
    
    /**
     * AI模型响应超时
     */
    AI_MODEL_TIMEOUT(4001, "AI模型响应超时"),
    
    /**
     * AI密钥配置错误
     */
    AI_API_KEY_ERROR(4002, "AI密钥配置错误"),
    
    /**
     * AI模型不可用
     */
    AI_MODEL_UNAVAILABLE(4003, "AI模型暂时不可用"),
    
    /**
     * Embedding服务异常
     */
    EMBEDDING_SERVICE_ERROR(4004, "向量嵌入服务异常"),
    
    /**
     * 向量数据库异常
     */
    VECTOR_DB_ERROR(4005, "向量数据库异常"),

    // ==================== 数据库异常 5xxx ====================
    
    /**
     * 数据库操作失败
     */
    DATABASE_ERROR(5000, "数据库操作失败"),
    
    /**
     * 数据库连接失败
     */
    DATABASE_CONNECTION_ERROR(5001, "数据库连接失败"),
    
    /**
     * 数据库查询超时
     */
    DATABASE_QUERY_TIMEOUT(5002, "数据库查询超时"),
    
    /**
     * 数据库死锁
     */
    DATABASE_DEADLOCK(5003, "数据库死锁"),
    
    /**
     * 唯一键冲突
     */
    DUPLICATE_KEY_ERROR(5004, "数据已存在，唯一键冲突"),

    // ==================== 缓存异常 6xxx ====================
    
    /**
     * 缓存操作失败
     */
    CACHE_ERROR(6000, "缓存操作失败"),
    
    /**
     * 缓存连接失败
     */
    CACHE_CONNECTION_ERROR(6001, "缓存连接失败"),
    
    /**
     * 分布式锁获取失败
     */
    LOCK_ACQUIRE_ERROR(6002, "系统繁忙，请稍后重试"),

    // ==================== 限流熔断异常 7xxx ====================
    
    /**
     * 请求限流
     */
    RATE_LIMIT_ERROR(7000, "请求过于频繁，请稍后重试"),
    
    /**
     * 限流超出
     */
    RATE_LIMIT_EXCEEDED(7001, "请求次数超过限制，请稍后重试"),
    
    /**
     * 服务熔断
     */
    CIRCUIT_BREAKER_OPEN(7002, "服务熔断中，请稍后重试"),
    
    /**
     * 并发数超限
     */
    CONCURRENT_LIMIT_ERROR(7003, "并发数超限，请稍后重试");

    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 错误消息
     */
    private final String message;

    /**
     * 根据错误码获取异常枚举
     *
     * @param code 错误码
     * @return 异常枚举，未找到则返回null
     */
    public static ExceptionEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ExceptionEnum exceptionEnum : values()) {
            if (exceptionEnum.getCode().equals(code)) {
                return exceptionEnum;
            }
        }
        return null;
    }
}