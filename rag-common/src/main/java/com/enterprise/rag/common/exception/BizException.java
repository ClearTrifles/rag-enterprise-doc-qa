package com.enterprise.rag.common.exception;

import lombok.Getter;

import java.io.Serial;

/**
 * 全局自定义业务异常
 * <p>
 * 用于封装业务逻辑中抛出的异常信息，包含错误码和错误消息。
 * 所有业务层抛出的异常应使用此类，以便全局异常处理器统一处理。
 * </p>
 *
 * @author Enterprise RAG Team
 * @version 1.0.0
 */
@Getter
public class BizException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 错误消息
     */
    private final String message;

    /**
     * 构造业务异常（使用异常枚举）
     *
     * @param exceptionEnum 异常枚举
     */
    public BizException(ExceptionEnum exceptionEnum) {
        super(exceptionEnum.getMessage());
        this.code = exceptionEnum.getCode();
        this.message = exceptionEnum.getMessage();
    }

    /**
     * 构造业务异常（使用异常枚举和自定义消息）
     *
     * @param exceptionEnum 异常枚举
     * @param message       自定义错误消息
     */
    public BizException(ExceptionEnum exceptionEnum, String message) {
        super(message);
        this.code = exceptionEnum.getCode();
        this.message = message;
    }

    /**
     * 构造业务异常（使用异常枚举和原因异常）
     *
     * @param exceptionEnum 异常枚举
     * @param cause         原因异常
     */
    public BizException(ExceptionEnum exceptionEnum, Throwable cause) {
        super(exceptionEnum.getMessage(), cause);
        this.code = exceptionEnum.getCode();
        this.message = exceptionEnum.getMessage();
    }

    /**
     * 构造业务异常（使用异常枚举、自定义消息和原因异常）
     *
     * @param exceptionEnum 异常枚举
     * @param message       自定义错误消息
     * @param cause         原因异常
     */
    public BizException(ExceptionEnum exceptionEnum, String message, Throwable cause) {
        super(message, cause);
        this.code = exceptionEnum.getCode();
        this.message = message;
    }

    /**
     * 构造业务异常（自定义错误码和消息）
     *
     * @param code    错误码
     * @param message 错误消息
     */
    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    /**
     * 构造业务异常（自定义错误码、消息和原因异常）
     *
     * @param code    错误码
     * @param message 错误消息
     * @param cause   原因异常
     */
    public BizException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
    }

    /**
     * 构造业务异常（仅使用消息，默认错误码）
     *
     * @param message 错误消息
     */
    public BizException(String message) {
        super(message);
        this.code = ExceptionEnum.BIZ_ERROR.getCode();
        this.message = message;
    }

    /**
     * 构造业务异常（仅使用消息和原因异常，默认错误码）
     *
     * @param message 错误消息
     * @param cause   原因异常
     */
    public BizException(String message, Throwable cause) {
        super(message, cause);
        this.code = ExceptionEnum.BIZ_ERROR.getCode();
        this.message = message;
    }

    @Override
    public String toString() {
        return "BizException{" +
                "code=" + code +
                ", message='" + message + '\'' +
                '}';
    }
}