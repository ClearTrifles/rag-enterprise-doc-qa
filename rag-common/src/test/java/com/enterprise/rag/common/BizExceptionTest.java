package com.enterprise.rag.common;

import com.enterprise.rag.common.exception.BizException;
import com.enterprise.rag.common.exception.ExceptionEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BizException业务异常单元测试
 *
 * @author Enterprise RAG Team
 * @since 2026-06-18
 */
class BizExceptionTest {

    @Test
    void testCreateWithEnum() {
        BizException exception = new BizException(ExceptionEnum.PARAM_ERROR);
        assertEquals(ExceptionEnum.PARAM_ERROR.getCode(), exception.getCode());
        assertEquals(ExceptionEnum.PARAM_ERROR.getMessage(), exception.getMessage());
    }

    @Test
    void testCreateWithCodeAndMessage() {
        BizException exception = new BizException(1001, "自定义错误");
        assertEquals(1001, exception.getCode());
        assertEquals("自定义错误", exception.getMessage());
    }

    @Test
    void testCreateWithMessage() {
        BizException exception = new BizException("简单错误");
        assertEquals(ExceptionEnum.BIZ_ERROR.getCode(), exception.getCode());
        assertEquals("简单错误", exception.getMessage());
    }

    @Test
    void testThrowException() {
        assertThrows(BizException.class, () -> {
            throw new BizException(ExceptionEnum.SYSTEM_ERROR);
        });
    }
}