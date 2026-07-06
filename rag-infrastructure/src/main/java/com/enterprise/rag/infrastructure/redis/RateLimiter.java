package com.enterprise.rag.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Redis滑动窗口限流工具类
 * <p>
 * 基于Redis Lua脚本实现的滑动窗口限流器，支持精确的流量控制。
 * 使用滑动窗口算法，可以更平滑地控制请求速率。
 * </p>
 *
 * <h3>功能特性：</h3>
 * <ul>
 *     <li>滑动窗口算法：精确控制请求速率</li>
 *     <li>Lua脚本执行：保证原子性操作</li>
 *     <li>支持自定义窗口大小和限流阈值</li>
 *     <li>支持多种限流场景：API限流、用户限流、IP限流</li>
 *     <li>高性能：单次Redis操作完成限流判断</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>
 * {@code
 * // API限流：每分钟最多100次请求
 * boolean allowed = rateLimiter.tryAcquire("api:chat", 100, 60);
 * 
 * // 用户限流：每用户每小时最多50次请求
 * boolean allowed = rateLimiter.tryAcquire("user:123:chat", 50, 3600);
 * 
 * // IP限流：每IP每分钟最多20次请求
 * boolean allowed = rateLimiter.tryAcquire("ip:192.168.1.1", 20, 60);
 * }
 * </pre>
 *
 * @author Enterprise RAG Team
 * @since 1.0.0
 * @see RedisService
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimiter {

    /**
     * Redis字符串模板
     */
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 限流键前缀
     */
    private static final String RATE_LIMIT_PREFIX = "rate_limit:";

    /**
     * 滑动窗口限流Lua脚本
     * <p>
     * 脚本逻辑：
     * 1. 获取当前时间窗口内的所有请求时间戳
     * 2. 移除超出窗口的旧时间戳
     * 3. 统计当前窗口内的请求数量
     * 4. 如果请求数量小于阈值，则添加当前请求时间戳并返回允许
     * 5. 否则返回拒绝
     * </p>
     */
    private static final String SLIDE_WINDOW_SCRIPT = """
            local key = KEYS[1]
            local limit = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])
            local current = tonumber(ARGV[3])
            
            -- 移除超出窗口的旧数据
            local clearBefore = current - window * 1000
            redis.call('ZREMRANGEBYSCORE', key, 0, clearBefore)
            
            -- 获取当前窗口内的请求数量
            local count = redis.call('ZCARD', key)
            
            -- 判断是否超过限流阈值
            if count < limit then
                -- 添加当前请求时间戳
                redis.call('ZADD', key, current, current)
                -- 设置过期时间（窗口时间 + 1秒缓冲）
                redis.call('EXPIRE', key, window + 1)
                return 1
            else
                return 0
            end
            """;

    /**
     * 固定窗口计数限流Lua脚本
     * <p>
     * 脚本逻辑：
     * 1. 获取当前计数
     * 2. 如果计数小于阈值，则递增并返回允许
     * 3. 否则返回拒绝
     * </p>
     */
    private static final String FIXED_WINDOW_SCRIPT = """
            local key = KEYS[1]
            local limit = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])
            
            -- 获取当前计数
            local count = tonumber(redis.call('GET', key) or '0')
            
            -- 判断是否超过限流阈值
            if count < limit then
                -- 递增计数
                local newCount = redis.call('INCR', key)
                -- 如果是第一次设置，添加过期时间
                if newCount == 1 then
                    redis.call('EXPIRE', key, window)
                end
                return 1
            else
                return 0
            end
            """;

    /**
     * 尝试获取访问许可（滑动窗口算法）
     * <p>
     * 使用滑动窗口算法进行限流，可以更精确地控制请求速率。
     * 滑动窗口会记录每个请求的时间戳，并移除超出窗口的旧请求。
     * </p>
     *
     * @param key      限流键（如api:chat、user:123）
     * @param limit    限流阈值（窗口内最大请求数）
     * @param window   窗口大小（秒）
     * @return 是否允许访问
     */
    public boolean tryAcquire(String key, int limit, int window) {
        return tryAcquire(key, limit, window, true);
    }

    /**
     * 尝试获取访问许可
     *
     * @param key           限流键
     * @param limit         限流阈值
     * @param window        窗口大小（秒）
     * @param useSlideWindow 是否使用滑动窗口算法
     * @return 是否允许访问
     */
    public boolean tryAcquire(String key, int limit, int window, boolean useSlideWindow) {
        String fullKey = RATE_LIMIT_PREFIX + key;
        long currentTime = System.currentTimeMillis();

        log.debug("尝试获取限流许可 - key: {}, limit: {}, window: {}s, slideWindow: {}", 
                fullKey, limit, window, useSlideWindow);

        try {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setScriptText(useSlideWindow ? SLIDE_WINDOW_SCRIPT : FIXED_WINDOW_SCRIPT);
            script.setResultType(Long.class);

            Long result;
            if (useSlideWindow) {
                result = stringRedisTemplate.execute(
                        script,
                        Collections.singletonList(fullKey),
                        String.valueOf(limit),
                        String.valueOf(window),
                        String.valueOf(currentTime)
                );
            } else {
                result = stringRedisTemplate.execute(
                        script,
                        Collections.singletonList(fullKey),
                        String.valueOf(limit),
                        String.valueOf(window)
                );
            }

            if (result != null && result == 1) {
                log.debug("限流许可获取成功 - key: {}", fullKey);
                return true;
            }

            log.warn("限流触发，请求被拒绝 - key: {}, limit: {}, window: {}s", fullKey, limit, window);
            return false;

        } catch (Exception e) {
            log.error("限流检查异常 - key: {}, 错误: {}", fullKey, e.getMessage());
            // 异常情况下默认允许，避免影响正常业务
            return true;
        }
    }

    /**
     * 尝试获取访问许可（带等待）
     * <p>
     * 如果当前请求被限流，则等待一段时间后再次尝试。
     * </p>
     *
     * @param key        限流键
     * @param limit      限流阈值
     * @param window     窗口大小（秒）
     * @param waitTime   最大等待时间（毫秒）
     * @return 是否允许访问
     */
    public boolean tryAcquireWithWait(String key, int limit, int window, long waitTime) {
        String fullKey = RATE_LIMIT_PREFIX + key;
        log.debug("尝试获取限流许可（带等待） - key: {}, limit: {}, window: {}s, wait: {}ms", 
                fullKey, limit, window, waitTime);

        long startTime = System.currentTimeMillis();
        long waitInterval = 100; // 等待间隔100ms

        while (System.currentTimeMillis() - startTime < waitTime) {
            if (tryAcquire(key, limit, window)) {
                return true;
            }

            try {
                Thread.sleep(waitInterval);
            } catch (InterruptedException e) {
                log.warn("等待限流许可时被中断 - key: {}", fullKey);
                Thread.currentThread().interrupt();
                return false;
            }
        }

        log.warn("等待限流许可超时 - key: {}", fullKey);
        return false;
    }

    /**
     * 获取当前限流计数
     *
     * @param key    限流键
     * @param window 窗口大小（秒）
     * @return 当前窗口内的请求数量
     */
    public long getCurrentCount(String key, int window) {
        String fullKey = RATE_LIMIT_PREFIX + key;
        long currentTime = System.currentTimeMillis();
        long clearBefore = currentTime - window * 1000;

        try {
            // 移除超出窗口的旧数据
            stringRedisTemplate.opsForZSet().removeRangeByScore(fullKey, 0, clearBefore);
            
            // 获取当前窗口内的请求数量
            Long count = stringRedisTemplate.opsForZSet().zCard(fullKey);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("获取限流计数异常 - key: {}, 错误: {}", fullKey, e.getMessage());
            return 0;
        }
    }

    /**
     * 重置限流计数
     *
     * @param key 限流键
     * @return 是否重置成功
     */
    public boolean reset(String key) {
        String fullKey = RATE_LIMIT_PREFIX + key;
        log.debug("重置限流计数 - key: {}", fullKey);

        try {
            Boolean result = stringRedisTemplate.delete(fullKey);
            return result != null && result;
        } catch (Exception e) {
            log.error("重置限流计数异常 - key: {}, 错误: {}", fullKey, e.getMessage());
            return false;
        }
    }

    /**
     * 获取剩余可用请求数
     *
     * @param key    限流键
     * @param limit  限流阈值
     * @param window 窗口大小（秒）
     * @return 剩余可用请求数
     */
    public long getRemainingCount(String key, int limit, int window) {
        long currentCount = getCurrentCount(key, window);
        return Math.max(0, limit - currentCount);
    }

    /**
     * 获取限流等待时间（毫秒）
     * <p>
     * 计算需要等待多久才能再次请求。
     * </p>
     *
     * @param key    限流键
     * @param limit  限流阈值
     * @param window 窗口大小（秒）
     * @return 需要等待的时间（毫秒），0表示无需等待
     */
    public long getWaitTime(String key, int limit, int window) {
        String fullKey = RATE_LIMIT_PREFIX + key;

        try {
            // 获取窗口内最早的请求时间戳
            Long earliestTimestamp = stringRedisTemplate.opsForZSet()
                    .range(fullKey, 0, 0)
                    .stream()
                    .findFirst()
                    .map(Long::parseLong)
                    .orElse(null);

            if (earliestTimestamp == null) {
                return 0;
            }

            long currentTime = System.currentTimeMillis();
            long windowEnd = earliestTimestamp + window * 1000;
            long waitTime = windowEnd - currentTime;

            return Math.max(0, waitTime);
        } catch (Exception e) {
            log.error("获取限流等待时间异常 - key: {}, 错误: {}", fullKey, e.getMessage());
            return 0;
        }
    }

    /**
     * 构建API限流键
     *
     * @param apiName API名称
     * @return 限流键
     */
    public String buildApiKey(String apiName) {
        return "api:" + apiName;
    }

    /**
     * 构建用户限流键
     *
     * @param userId  用户ID
     * @param apiName API名称
     * @return 限流键
     */
    public String buildUserKey(String userId, String apiName) {
        return "user:" + userId + ":" + apiName;
    }

    /**
     * 构建IP限流键
     *
     * @param ip      IP地址
     * @param apiName API名称
     * @return 限流键
     */
    public String buildIpKey(String ip, String apiName) {
        return "ip:" + ip + ":" + apiName;
    }
}