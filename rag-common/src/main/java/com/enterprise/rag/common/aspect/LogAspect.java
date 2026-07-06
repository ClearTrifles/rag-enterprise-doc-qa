package com.enterprise.rag.common.aspect;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 统一日志切面
 * <p>
 * 用于记录Controller层接口的请求日志，包括请求参数、响应结果、执行时间等。
 * 支持对敏感参数进行脱敏处理，保护用户隐私。
 * </p>
 *
 * @author Enterprise RAG Team
 * @version 1.0.0
 */
@Slf4j
@Aspect
@Component
public class LogAspect {

    /**
     * 敏感参数名称（需要脱敏）
     */
    private static final String[] SENSITIVE_PARAMS = {
            "password", "pwd", "token", "secret", "apiKey", "api_key",
            "accessKey", "access_key", "secretKey", "secret_key"
    };

    /**
     * 切入点：所有Controller层的方法
     */
    @Pointcut("execution(* com.enterprise.rag..controller..*.*(..))")
    public void controllerPointcut() {
        // 切入点定义
    }

    /**
     * 环绕通知：记录请求日志
     *
     * @param joinPoint 连接点
     * @return 方法执行结果
     * @throws Throwable 方法执行异常
     */
    @Around("controllerPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }
        
        HttpServletRequest request = attributes.getRequest();
        
        // 记录请求日志
        logRequest(request, joinPoint);
        
        // 执行方法
        Object result = joinPoint.proceed();
        
        // 记录响应日志
        long executionTime = System.currentTimeMillis() - startTime;
        logResponse(result, executionTime);
        
        return result;
    }

    /**
     * 记录请求日志
     *
     * @param request   HTTP请求
     * @param joinPoint 连接点
     */
    private void logRequest(HttpServletRequest request, ProceedingJoinPoint joinPoint) {
        try {
            // 请求基本信息
            String method = request.getMethod();
            String uri = request.getRequestURI();
            String className = joinPoint.getTarget().getClass().getSimpleName();
            String methodName = joinPoint.getSignature().getName();
            String clientIp = getClientIp(request);
            
            // 请求参数
            Map<String, Object> params = getRequestParams(request, joinPoint);
            
            // 记录日志
            log.info("\n==================== 请求开始 ====================" +
                    "\n请求方式: {}" +
                    "\n请求路径: {}" +
                    "\n请求类名: {}" +
                    "\n请求方法: {}" +
                    "\n客户端IP: {}" +
                    "\n请求参数: {}" +
                    "\n================================================",
                    method, uri, className, methodName, clientIp, JSONUtil.toJsonStr(params));
        } catch (Exception e) {
            log.warn("记录请求日志失败: {}", e.getMessage());
        }
    }

    /**
     * 记录响应日志
     *
     * @param result        响应结果
     * @param executionTime 执行时间（毫秒）
     */
    private void logResponse(Object result, long executionTime) {
        try {
            String resultStr = JSONUtil.toJsonStr(result);
            // 限制响应日志长度，避免日志过大
            if (resultStr.length() > 1000) {
                resultStr = resultStr.substring(0, 1000) + "...(已截断)";
            }
            
            log.info("\n==================== 响应结果 ====================" +
                    "\n执行时间: {} ms" +
                    "\n响应结果: {}" +
                    "\n================================================",
                    executionTime, resultStr);
        } catch (Exception e) {
            log.warn("记录响应日志失败: {}", e.getMessage());
        }
    }

    /**
     * 获取请求参数
     *
     * @param request   HTTP请求
     * @param joinPoint 连接点
     * @return 请求参数Map
     */
    private Map<String, Object> getRequestParams(HttpServletRequest request, ProceedingJoinPoint joinPoint) {
        Map<String, Object> params = new HashMap<>(16);
        
        // 获取URL参数
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String name = parameterNames.nextElement();
            String value = request.getParameter(name);
            params.put(name, maskSensitiveParam(name, value));
        }
        
        // 获取方法参数
        Object[] args = joinPoint.getArgs();
        if (args != null && args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                if (arg != null && !(arg instanceof HttpServletRequest) 
                        && !(arg instanceof MultipartFile)) {
                    try {
                        String argStr = JSONUtil.toJsonStr(arg);
                        // 限制参数长度
                        if (argStr.length() > 500) {
                            argStr = argStr.substring(0, 500) + "...(已截断)";
                        }
                        params.put("arg" + i, argStr);
                    } catch (Exception e) {
                        params.put("arg" + i, arg.getClass().getSimpleName());
                    }
                }
            }
        }
        
        return params;
    }

    /**
     * 脱敏敏感参数
     *
     * @param paramName  参数名
     * @param paramValue 参数值
     * @return 脱敏后的参数值
     */
    private String maskSensitiveParam(String paramName, String paramValue) {
        if (StrUtil.isEmpty(paramValue)) {
            return paramValue;
        }
        
        // 检查是否为敏感参数
        for (String sensitive : SENSITIVE_PARAMS) {
            if (paramName.toLowerCase().contains(sensitive.toLowerCase())) {
                // 保留前3位和后3位，中间用*代替
                if (paramValue.length() <= 6) {
                    return "******";
                }
                return paramValue.substring(0, 3) + "******" + paramValue.substring(paramValue.length() - 3);
            }
        }
        
        return paramValue;
    }

    /**
     * 获取客户端IP地址
     *
     * @param request HTTP请求
     * @return 客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (StrUtil.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (StrUtil.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (StrUtil.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (StrUtil.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 对于多个代理的情况，第一个IP才是客户端真实IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}