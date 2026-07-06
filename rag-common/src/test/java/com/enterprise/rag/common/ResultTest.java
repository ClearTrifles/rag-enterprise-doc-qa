package com.enterprise.rag.common;

import com.enterprise.rag.common.result.Result;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Result统一返回体单元测试
 *
 * @author Enterprise RAG Team
 * @since 2026-06-18
 */
class ResultTest {

    @Test
    void testSuccess() {
        Result<String> result = Result.success("测试数据");
        assertTrue(result.isSuccess());
        assertEquals(200, result.getCode());
        assertEquals("测试数据", result.getData());
        assertEquals("操作成功", result.getMessage());
    }

    @Test
    void testSuccessWithoutData() {
        Result<Void> result = Result.success();
        assertTrue(result.isSuccess());
        assertEquals(200, result.getCode());
        assertNull(result.getData());
    }

    @Test
    void testError() {
        Result<Void> result = Result.error(500, "系统错误");
        assertFalse(result.isSuccess());
        assertEquals(500, result.getCode());
        assertEquals("系统错误", result.getMessage());
    }

    @Test
    void testErrorWithCode() {
        Result<Void> result = Result.error(400, "参数错误");
        assertFalse(result.isSuccess());
        assertEquals(400, result.getCode());
    }
}