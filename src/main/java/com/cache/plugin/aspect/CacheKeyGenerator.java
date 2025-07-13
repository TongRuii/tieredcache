package com.cache.plugin.aspect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 缓存键生成器
 */
public class CacheKeyGenerator {
    
    private static final Logger logger = LoggerFactory.getLogger(CacheKeyGenerator.class);
    
    private final ExpressionParser parser = new SpelExpressionParser();
    
    /**
     * 生成缓存键
     * 
     * @param keyExpression 键表达式
     * @param keyGeneratorName 键生成器名称
     * @param method 方法
     * @param args 方法参数
     * @param result 方法返回值
     * @return 缓存键
     */
    public String generate(String keyExpression, String keyGeneratorName, Method method, Object[] args, Object result) {
        try {
            // 如果指定了键表达式，使用SpEL解析
            if (StringUtils.hasText(keyExpression)) {
                return evaluateKeyExpression(keyExpression, method, args, result);
            }
            
            // 如果指定了键生成器，使用自定义生成器
            if (StringUtils.hasText(keyGeneratorName)) {
                return generateByCustomGenerator(keyGeneratorName, method, args, result);
            }
            
            // 默认键生成策略
            return generateDefaultKey(method, args);
            
        } catch (Exception e) {
            logger.error("Failed to generate cache key, using default strategy", e);
            return generateDefaultKey(method, args);
        }
    }
    
    /**
     * 使用SpEL表达式生成键
     */
    private String evaluateKeyExpression(String keyExpression, Method method, Object[] args, Object result) {
        try {
            Expression expression = parser.parseExpression(keyExpression);
            EvaluationContext context = createEvaluationContext(method, args, result);
            Object value = expression.getValue(context);
            return value != null ? value.toString() : generateDefaultKey(method, args);
        } catch (Exception e) {
            logger.error("Failed to evaluate key expression: {}", keyExpression, e);
            return generateDefaultKey(method, args);
        }
    }
    
    /**
     * 创建SpEL评估上下文
     */
    private EvaluationContext createEvaluationContext(Method method, Object[] args, Object result) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        
        // 添加方法参数到上下文
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length && i < args.length; i++) {
            context.setVariable(parameters[i].getName(), args[i]);
            // 同时支持 #p0, #p1 这种形式
            context.setVariable("p" + i, args[i]);
            // 支持 #a0, #a1 这种形式
            context.setVariable("a" + i, args[i]);
        }
        
        // 添加方法返回值到上下文
        if (result != null) {
            context.setVariable("result", result);
        }
        
        // 添加方法信息到上下文
        context.setVariable("method", method);
        context.setVariable("methodName", method.getName());
        context.setVariable("className", method.getDeclaringClass().getSimpleName());
        
        return context;
    }
    
    /**
     * 使用自定义键生成器
     */
    private String generateByCustomGenerator(String keyGeneratorName, Method method, Object[] args, Object result) {
        // 这里可以扩展支持自定义键生成器
        // 目前使用默认策略
        logger.warn("Custom key generator '{}' not implemented, using default strategy", keyGeneratorName);
        return generateDefaultKey(method, args);
    }
    
    /**
     * 生成默认缓存键
     */
    private String generateDefaultKey(Method method, Object[] args) {
        StringBuilder keyBuilder = new StringBuilder();
        
        // 添加类名和方法名
        keyBuilder.append(method.getDeclaringClass().getSimpleName())
                  .append(".")
                  .append(method.getName());
        
        // 添加参数
        if (args != null && args.length > 0) {
            String argsString = Arrays.stream(args)
                    .map(this::convertArgToString)
                    .collect(Collectors.joining(","));
            keyBuilder.append("(").append(argsString).append(")");
        } else {
            keyBuilder.append("()");
        }
        
        return keyBuilder.toString();
    }
    
    /**
     * 将参数转换为字符串
     */
    private String convertArgToString(Object arg) {
        if (arg == null) {
            return "null";
        }
        
        // 对于基本类型和字符串，直接转换
        if (arg instanceof String || arg instanceof Number || arg instanceof Boolean) {
            return arg.toString();
        }
        
        // 对于数组，转换为字符串表示
        if (arg.getClass().isArray()) {
            if (arg instanceof Object[]) {
                return Arrays.toString((Object[]) arg);
            } else if (arg instanceof int[]) {
                return Arrays.toString((int[]) arg);
            } else if (arg instanceof long[]) {
                return Arrays.toString((long[]) arg);
            } else if (arg instanceof double[]) {
                return Arrays.toString((double[]) arg);
            } else if (arg instanceof boolean[]) {
                return Arrays.toString((boolean[]) arg);
            } else {
                return arg.toString();
            }
        }
        
        // 对于其他对象，使用hashCode
        return arg.getClass().getSimpleName() + "@" + arg.hashCode();
    }
}