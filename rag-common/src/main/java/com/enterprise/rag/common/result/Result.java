package com.enterprise.rag.common.result;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 全局统一返回体
 * <p>
 * 用于统一封装API接口的返回结果，包含状态码、消息和数据。
 * 所有Controller层接口应使用此类进行返回值封装。
 * </p>
 *
 * @param <T> 返回数据的类型
 * @author Enterprise RAG Team
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "全局统一返回体")
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 成功状态码
     */
    public static final int SUCCESS_CODE = 200;

    /**
     * 失败状态码
     */
    public static final int ERROR_CODE = 500;

    /**
     * 状态码
     */
    @Schema(description = "状态码", example = "200")
    private Integer code;

    /**
     * 返回消息
     */
    @Schema(description = "返回消息", example = "操作成功")
    private String message;

    /**
     * 返回数据
     */
    @Schema(description = "返回数据")
    private T data;

    /**
     * 时间戳
     */
    @Schema(description = "时间戳", example = "1704067200000")
    private Long timestamp;

    /**
     * 成功返回结果（无数据）
     *
     * @param <T> 数据类型
     * @return 成功结果
     */
    public static <T> Result<T> success() {
        return success(null);
    }

    /**
     * 成功返回结果（有数据）
     *
     * @param data 返回数据
     * @param <T>  数据类型
     * @return 成功结果
     */
    public static <T> Result<T> success(T data) {
        return success("操作成功", data);
    }

    /**
     * 成功返回结果（自定义消息和数据）
     *
     * @param message 返回消息
     * @param data    返回数据
     * @param <T>     数据类型
     * @return 成功结果
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(SUCCESS_CODE, message, data, System.currentTimeMillis());
    }

    /**
     * 失败返回结果（默认消息）
     *
     * @param <T> 数据类型
     * @return 失败结果
     */
    public static <T> Result<T> error() {
        return error("操作失败");
    }

    /**
     * 失败返回结果（自定义消息）
     *
     * @param message 错误消息
     * @param <T>     数据类型
     * @return 失败结果
     */
    public static <T> Result<T> error(String message) {
        return error(ERROR_CODE, message);
    }

    /**
     * 失败返回结果（自定义状态码和消息）
     *
     * @param code    错误状态码
     * @param message 错误消息
     * @param <T>     数据类型
     * @return 失败结果
     */
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null, System.currentTimeMillis());
    }

    /**
     * 分页成功返回结果
     *
     * @param list     数据列表
     * @param total    总记录数
     * @param pageNum  当前页码
     * @param pageSize 每页大小
     * @param <T>      数据类型
     * @return 分页结果
     */
    public static <T> Result<PageResult<T>> successPage(java.util.List<T> list, long total, int pageNum, int pageSize) {
        PageResult<T> pageResult = new PageResult<>(list, total, pageNum, pageSize);
        return success(pageResult);
    }

    /**
     * 判断是否成功
     *
     * @return 是否成功
     */
    public boolean isSuccess() {
        return SUCCESS_CODE == this.code;
    }
}