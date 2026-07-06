package com.enterprise.rag.common.exception;

import com.enterprise.rag.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常拦截器
 * <p>
 * 统一处理系统中抛出的各类异常，返回标准化的错误响应。
 * 包括业务异常、参数校验异常、系统异常等。
 * </p>
 *
 * @author Enterprise RAG Team
 * @version 1.0.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     *
     * @param e       业务异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException e, HttpServletRequest request) {
        log.warn("业务异常 - URI: {}, 错误码: {}, 错误消息: {}",
                request.getRequestURI(), e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数校验异常（@Valid注解）
     *
     * @param e       参数校验异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e, HttpServletRequest request) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数校验失败 - URI: {}, 错误消息: {}", request.getRequestURI(), errorMessage);
        return Result.error(ExceptionEnum.PARAM_VALID_ERROR.getCode(), errorMessage);
    }

    /**
     * 处理参数绑定异常
     *
     * @param e       参数绑定异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBindException(BindException e, HttpServletRequest request) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数绑定失败 - URI: {}, 错误消息: {}", request.getRequestURI(), errorMessage);
        return Result.error(ExceptionEnum.PARAM_VALID_ERROR.getCode(), errorMessage);
    }

    /**
     * 处理约束违规异常
     *
     * @param e       约束违规异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleConstraintViolationException(
            ConstraintViolationException e, HttpServletRequest request) {
        String errorMessage = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        log.warn("约束校验失败 - URI: {}, 错误消息: {}", request.getRequestURI(), errorMessage);
        return Result.error(ExceptionEnum.PARAM_VALID_ERROR.getCode(), errorMessage);
    }

    /**
     * 处理缺少请求参数异常
     *
     * @param e       缺少请求参数异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException e, HttpServletRequest request) {
        String errorMessage = "缺少必需参数: " + e.getParameterName();
        log.warn("缺少请求参数 - URI: {}, 参数名: {}", request.getRequestURI(), e.getParameterName());
        return Result.error(ExceptionEnum.PARAM_VALID_ERROR.getCode(), errorMessage);
    }

    /**
     * 处理参数类型不匹配异常
     *
     * @param e       参数类型不匹配异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        String errorMessage = "参数类型错误: " + e.getName();
        log.warn("参数类型不匹配 - URI: {}, 参数名: {}, 需要类型: {}",
                request.getRequestURI(), e.getName(), e.getRequiredType());
        return Result.error(ExceptionEnum.PARAM_VALID_ERROR.getCode(), errorMessage);
    }

    /**
     * 处理请求体解析异常
     *
     * @param e       请求体解析异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e, HttpServletRequest request) {
        log.warn("请求体解析失败 - URI: {}, 错误消息: {}", request.getRequestURI(), e.getMessage());
        return Result.error(ExceptionEnum.PARAM_VALID_ERROR.getCode(), "请求体格式错误");
    }

    /**
     * 处理请求方法不支持异常
     *
     * @param e       请求方法不支持异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<Void> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        log.warn("请求方法不支持 - URI: {}, 方法: {}", request.getRequestURI(), e.getMethod());
        return Result.error(ExceptionEnum.METHOD_NOT_SUPPORTED.getCode(),
                ExceptionEnum.METHOD_NOT_SUPPORTED.getMessage());
    }

    /**
     * 处理404异常
     *
     * @param e       404异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNoHandlerFoundException(
            NoHandlerFoundException e, HttpServletRequest request) {
        log.warn("请求路径不存在 - URI: {}", request.getRequestURI());
        return Result.error(ExceptionEnum.PATH_NOT_FOUND.getCode(),
                ExceptionEnum.PATH_NOT_FOUND.getMessage());
    }

    /**
     * 处理静态资源不存在异常
     *
     * @param e       静态资源不存在异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNoResourceFoundException(
            NoResourceFoundException e, HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri.equals("/favicon.ico")) {
            log.debug("静态资源不存在 - URI: {}", uri);
        } else {
            log.warn("静态资源不存在 - URI: {}", uri);
        }
        return Result.error(ExceptionEnum.PATH_NOT_FOUND.getCode(), "资源不存在");
    }

    /**
     * 处理空指针异常
     *
     * @param e       空指针异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(NullPointerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleNullPointerException(
            NullPointerException e, HttpServletRequest request) {
        log.error("空指针异常 - URI: {}", request.getRequestURI(), e);
        return Result.error(ExceptionEnum.SYSTEM_ERROR.getCode(), "系统内部错误");
    }

    /**
     * 处理非法参数异常
     *
     * @param e       非法参数异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleIllegalArgumentException(
            IllegalArgumentException e, HttpServletRequest request) {
        log.warn("非法参数 - URI: {}, 错误消息: {}", request.getRequestURI(), e.getMessage());
        return Result.error(ExceptionEnum.PARAM_VALID_ERROR.getCode(), e.getMessage());
    }

    /**
     * 处理运行时异常
     *
     * @param e       运行时异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        log.error("运行时异常 - URI: {}", request.getRequestURI(), e);
        return Result.error(ExceptionEnum.SYSTEM_ERROR.getCode(), "系统内部错误，请稍后重试");
    }

    /**
     * 处理所有未捕获的异常
     *
     * @param e       异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("未知异常 - URI: {}", request.getRequestURI(), e);
        return Result.error(ExceptionEnum.SYSTEM_ERROR.getCode(), "系统异常，请稍后重试");
    }
}