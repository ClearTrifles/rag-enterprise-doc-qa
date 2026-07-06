package com.enterprise.rag.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Redis服务工具类
 * <p>
 * 封装Redis常用操作，提供缓存、限流、防重等功能。
 * 支持字符串、哈希、列表、集合等数据结构的操作。
 * </p>
 *
 * <h3>功能特性：</h3>
 * <ul>
 *     <li>基础缓存操作：get、set、delete</li>
 *     <li>过期时间设置：支持秒、毫秒级别</li>
 *     <li>分布式锁：基于Redis实现</li>
 *     <li>防重机制：防止重复提交</li>
 *     <li>计数器：支持增减操作</li>
 *     <li>限流支持：配合RateLimiter使用</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>
 * {@code
 * // 缓存操作
 * redisService.set("user:123", "张三", 3600);
 * String value = redisService.get("user:123");
 * 
 * // 分布式锁
 * boolean locked = redisService.tryLock("order:123", 30);
 * if (locked) {
 *     // 执行业务逻辑
 *     redisService.unlock("order:123");
 * }
 * 
 * // 防重
 * boolean canSubmit = redisService.tryAcquire("submit:123", 60);
 * }
 * </pre>
 *
 * @author Enterprise RAG Team
 * @since 1.0.0
 * @see StringRedisTemplate
 * @see RateLimiter
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {

    /**
     * Redis字符串模板
     */
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 分布式锁前缀
     */
    private static final String LOCK_PREFIX = "lock:";

    /**
     * 防重前缀
     */
    private static final String PREVENT_PREFIX = "prevent:";

    /**
     * 缓存前缀
     */
    private static final String CACHE_PREFIX = "cache:";

    /**
     * 默认锁值
     */
    private static final String LOCK_VALUE = "1";

    // ==================== 基础缓存操作 ====================

    /**
     * 设置缓存值
     *
     * @param key   缓存键
     * @param value 缓存值
     */
    public void set(String key, String value) {
        log.debug("设置缓存 - key: {}, value: {}", key, value);
        stringRedisTemplate.opsForValue().set(key, value);
    }

    /**
     * 设置缓存值（带过期时间）
     *
     * @param key     缓存键
     * @param value   缓存值
     * @param timeout 过期时间（秒）
     */
    public void set(String key, String value, long timeout) {
        log.debug("设置缓存（带过期） - key: {}, value: {}, timeout: {}s", key, value, timeout);
        stringRedisTemplate.opsForValue().set(key, value, timeout, TimeUnit.SECONDS);
    }

    /**
     * 设置缓存值（带过期时间和单位）
     *
     * @param key     缓存键
     * @param value   缓存值
     * @param timeout 过期时间
     * @param unit    时间单位
     */
    public void set(String key, String value, long timeout, TimeUnit unit) {
        log.debug("设置缓存 - key: {}, value: {}, timeout: {} {}", key, value, timeout, unit);
        stringRedisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * 获取缓存值
     *
     * @param key 缓存键
     * @return 缓存值，不存在返回null
     */
    public String get(String key) {
        log.debug("获取缓存 - key: {}", key);
        return stringRedisTemplate.opsForValue().get(key);
    }

    /**
     * 删除缓存
     *
     * @param key 缓存键
     * @return 是否删除成功
     */
    public boolean delete(String key) {
        log.debug("删除缓存 - key: {}", key);
        Boolean result = stringRedisTemplate.delete(key);
        return result != null && result;
    }

    /**
     * 批量删除缓存
     *
     * @param keys 缓存键集合
     * @return 删除数量
     */
    public long delete(Collection<String> keys) {
        log.debug("批量删除缓存 - 数量: {}", keys.size());
        Long result = stringRedisTemplate.delete(keys);
        return result != null ? result : 0;
    }

    /**
     * 判断缓存是否存在
     *
     * @param key 缓存键
     * @return 是否存在
     */
    public boolean exists(String key) {
        Boolean result = stringRedisTemplate.hasKey(key);
        return result != null && result;
    }

    /**
     * 设置过期时间
     *
     * @param key     缓存键
     * @param timeout 过期时间
     * @param unit    时间单位
     * @return 是否设置成功
     */
    public boolean expire(String key, long timeout, TimeUnit unit) {
        Boolean result = stringRedisTemplate.expire(key, timeout, unit);
        return result != null && result;
    }

    /**
     * 获取过期时间
     *
     * @param key 缓存键
     * @return 过期时间（秒），-1表示永不过期，-2表示不存在
     */
    public long getExpire(String key) {
        Long result = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
        return result != null ? result : -2;
    }

    // ==================== 分布式锁 ====================

    /**
     * 尝试获取分布式锁
     * <p>
     * 使用Redis的setnx命令实现分布式锁。
     * 如果锁不存在，则设置锁并返回true；否则返回false。
     * </p>
     *
     * @param lockKey 锁键
     * @param expire  锁过期时间（秒）
     * @return 是否获取成功
     */
    public boolean tryLock(String lockKey, long expire) {
        String key = LOCK_PREFIX + lockKey;
        log.debug("尝试获取锁 - key: {}, expire: {}s", key, expire);
        
        Boolean result = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, LOCK_VALUE, expire, TimeUnit.SECONDS);
        
        if (result != null && result) {
            log.debug("获取锁成功 - key: {}", key);
            return true;
        }
        
        log.debug("获取锁失败 - key: {}", key);
        return false;
    }

    /**
     * 尝试获取分布式锁（带等待时间）
     * <p>
     * 在指定时间内循环尝试获取锁，直到成功或超时。
     * </p>
     *
     * @param lockKey     锁键
     * @param expire      锁过期时间（秒）
     * @param waitTimeout 等待超时时间（毫秒）
     * @return 是否获取成功
     */
    public boolean tryLock(String lockKey, long expire, long waitTimeout) {
        String key = LOCK_PREFIX + lockKey;
        log.debug("尝试获取锁（带等待） - key: {}, expire: {}s, wait: {}ms", key, expire, waitTimeout);
        
        long startTime = System.currentTimeMillis();
        long waitInterval = 100; // 等待间隔100ms
        
        while (System.currentTimeMillis() - startTime < waitTimeout) {
            Boolean result = stringRedisTemplate.opsForValue()
                    .setIfAbsent(key, LOCK_VALUE, expire, TimeUnit.SECONDS);
            
            if (result != null && result) {
                log.debug("获取锁成功 - key: {}", key);
                return true;
            }
            
            try {
                Thread.sleep(waitInterval);
            } catch (InterruptedException e) {
                log.warn("等待锁时被中断 - key: {}", key);
                Thread.currentThread().interrupt();
                return false;
            }
        }
        
        log.debug("获取锁超时 - key: {}", key);
        return false;
    }

    /**
     * 释放分布式锁
     * <p>
     * 删除锁对应的Redis键。
     * 注意：此方法不保证锁的所有者一致性，建议配合Lua脚本使用。
     * </p>
     *
     * @param lockKey 锁键
     * @return 是否释放成功
     */
    public boolean unlock(String lockKey) {
        String key = LOCK_PREFIX + lockKey;
        log.debug("释放锁 - key: {}", key);
        return delete(key);
    }

    /**
     * 判断锁是否存在
     *
     * @param lockKey 锁键
     * @return 是否存在
     */
    public boolean isLocked(String lockKey) {
        String key = LOCK_PREFIX + lockKey;
        return exists(key);
    }

    // ==================== 防重机制 ====================

    /**
     * 尝试获取防重令牌
     * <p>
     * 用于防止重复提交，如订单重复提交、表单重复提交等。
     * 如果令牌不存在，则设置并返回true；否则返回false。
     * </p>
     *
     * @param preventKey 防重键
     * @param expire     过期时间（秒）
     * @return 是否可以执行
     */
    public boolean tryAcquire(String preventKey, long expire) {
        String key = PREVENT_PREFIX + preventKey;
        log.debug("尝试获取防重令牌 - key: {}, expire: {}s", key, expire);
        
        Boolean result = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, LOCK_VALUE, expire, TimeUnit.SECONDS);
        
        if (result != null && result) {
            log.debug("获取防重令牌成功 - key: {}", key);
            return true;
        }
        
        log.debug("获取防重令牌失败（重复请求） - key: {}", key);
        return false;
    }

    /**
     * 释放防重令牌
     *
     * @param preventKey 防重键
     * @return 是否释放成功
     */
    public boolean release(String preventKey) {
        String key = PREVENT_PREFIX + preventKey;
        log.debug("释放防重令牌 - key: {}", key);
        return delete(key);
    }

    // ==================== 计数器 ====================

    /**
     * 递增计数
     *
     * @param key 计数键
     * @return 递增后的值
     */
    public long increment(String key) {
        log.debug("递增计数 - key: {}", key);
        Long result = stringRedisTemplate.opsForValue().increment(key);
        return result != null ? result : 0;
    }

    /**
     * 递增计数（指定步长）
     *
     * @param key   计数键
     * @param delta 递增步长
     * @return 递增后的值
     */
    public long increment(String key, long delta) {
        log.debug("递增计数 - key: {}, delta: {}", key, delta);
        Long result = stringRedisTemplate.opsForValue().increment(key, delta);
        return result != null ? result : 0;
    }

    /**
     * 递减计数
     *
     * @param key 计数键
     * @return 递减后的值
     */
    public long decrement(String key) {
        log.debug("递减计数 - key: {}", key);
        Long result = stringRedisTemplate.opsForValue().decrement(key);
        return result != null ? result : 0;
    }

    /**
     * 递减计数（指定步长）
     *
     * @param key   计数键
     * @param delta 递减步长
     * @return 递减后的值
     */
    public long decrement(String key, long delta) {
        log.debug("递减计数 - key: {}, delta: {}", key, delta);
        Long result = stringRedisTemplate.opsForValue().decrement(key, delta);
        return result != null ? result : 0;
    }

    // ==================== Hash操作 ====================

    /**
     * 设置Hash字段值
     *
     * @param key     Hash键
     * @param field   字段名
     * @param value   字段值
     */
    public void hSet(String key, String field, String value) {
        log.debug("设置Hash字段 - key: {}, field: {}, value: {}", key, field, value);
        stringRedisTemplate.opsForHash().put(key, field, value);
    }

    /**
     * 获取Hash字段值
     *
     * @param key   Hash键
     * @param field 字段名
     * @return 字段值，不存在返回null
     */
    public Object hGet(String key, String field) {
        log.debug("获取Hash字段 - key: {}, field: {}", key, field);
        return stringRedisTemplate.opsForHash().get(key, field);
    }

    /**
     * 删除Hash字段
     *
     * @param key    Hash键
     * @param fields 字段名数组
     * @return 删除数量
     */
    public long hDelete(String key, String... fields) {
        log.debug("删除Hash字段 - key: {}, fields: {}", key, fields);
        Long result = stringRedisTemplate.opsForHash().delete(key, fields);
        return result != null ? result : 0;
    }

    /**
     * 判断Hash字段是否存在
     *
     * @param key   Hash键
     * @param field 字段名
     * @return 是否存在
     */
    public boolean hExists(String key, String field) {
        Boolean result = stringRedisTemplate.opsForHash().hasKey(key, field);
        return result != null && result;
    }

    // ==================== List操作 ====================

    /**
     * 左推入列表
     *
     * @param key   列表键
     * @param value 值
     * @return 列表长度
     */
    public long lPush(String key, String value) {
        log.debug("左推入列表 - key: {}, value: {}", key, value);
        Long result = stringRedisTemplate.opsForList().leftPush(key, value);
        return result != null ? result : 0;
    }

    /**
     * 右推入列表
     *
     * @param key   列表键
     * @param value 值
     * @return 列表长度
     */
    public long rPush(String key, String value) {
        log.debug("右推入列表 - key: {}, value: {}", key, value);
        Long result = stringRedisTemplate.opsForList().rightPush(key, value);
        return result != null ? result : 0;
    }

    /**
     * 左弹出列表
     *
     * @param key 列表键
     * @return 弹出的值，不存在返回null
     */
    public String lPop(String key) {
        log.debug("左弹出列表 - key: {}", key);
        return stringRedisTemplate.opsForList().leftPop(key);
    }

    /**
     * 右弹出列表
     *
     * @param key 列表键
     * @return 弹出的值，不存在返回null
     */
    public String rPop(String key) {
        log.debug("右弹出列表 - key: {}", key);
        return stringRedisTemplate.opsForList().rightPop(key);
    }

    /**
     * 获取列表长度
     *
     * @param key 列表键
     * @return 列表长度
     */
    public long lSize(String key) {
        Long result = stringRedisTemplate.opsForList().size(key);
        return result != null ? result : 0;
    }

    // ==================== Set操作 ====================

    /**
     * 添加集合元素
     *
     * @param key    集合键
     * @param values 元素值数组
     * @return 添加数量
     */
    public long sAdd(String key, String... values) {
        log.debug("添加集合元素 - key: {}, values: {}", key, values);
        Long result = stringRedisTemplate.opsForSet().add(key, values);
        return result != null ? result : 0;
    }

    /**
     * 移除集合元素
     *
     * @param key    集合键
     * @param values 元素值数组
     * @return 移除数量
     */
    public long sRemove(String key, String... values) {
        log.debug("移除集合元素 - key: {}, values: {}", key, values);
        Long result = stringRedisTemplate.opsForSet().remove(key, values);
        return result != null ? result : 0;
    }

    /**
     * 判断集合元素是否存在
     *
     * @param key   集合键
     * @param value 元素值
     * @return 是否存在
     */
    public boolean sIsMember(String key, String value) {
        Boolean result = stringRedisTemplate.opsForSet().isMember(key, value);
        return result != null && result;
    }

    /**
     * 获取集合大小
     *
     * @param key 集合键
     * @return 集合大小
     */
    public long sSize(String key) {
        Long result = stringRedisTemplate.opsForSet().size(key);
        return result != null ? result : 0;
    }

    // ==================== 工具方法 ====================

    /**
     * 构建缓存键
     *
     * @param prefix 前缀
     * @param key    键
     * @return 完整键
     */
    public String buildKey(String prefix, String key) {
        return prefix + key;
    }

    /**
     * 构建缓存键（带多个参数）
     *
     * @param prefix 前缀
     * @param keys   键数组
     * @return 完整键
     */
    public String buildKey(String prefix, String... keys) {
        StringBuilder sb = new StringBuilder(prefix);
        for (String key : keys) {
            sb.append(key).append(":");
        }
        return sb.substring(0, sb.length() - 1);
    }
}