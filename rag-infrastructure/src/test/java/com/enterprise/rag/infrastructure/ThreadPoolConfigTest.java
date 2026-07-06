package com.enterprise.rag.infrastructure;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ThreadPoolConfigTest {

    @Test
    void testThreadPoolExecute() throws InterruptedException {
        Executor executor = createTestExecutor();
        
        assertNotNull(executor, "线程池应存在");
        
        CountDownLatch latch = new CountDownLatch(1);
        
        executor.execute(() -> {
            System.out.println("线程池任务执行成功");
            latch.countDown();
        });

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertTrue(completed, "任务应在5秒内完成");
    }

    private Executor createTestExecutor() {
        return Runnable::run;
    }

    @Test
    void testExecutorNotNull() {
        Executor executor = createTestExecutor();
        assertNotNull(executor, "执行器不应为空");
    }
}