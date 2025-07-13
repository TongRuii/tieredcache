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

/**
 * 缓存条件评估器
 */
public class CacheConditionEvaluator {
    
    private static final Logger logger = LoggerFactory.getLogger(CacheConditionEvaluator.class);
    
    private final ExpressionParser parser = new SpelExpressionParser();
    
    /**
     * 评估缓存条件
     * 
     * @param condition 条件表达式
     * @param method 方法
     * @param args 方法参数
     * @param result 方法返回值
     * @return 是否满足条件
     */
    public boolean evaluate(String condition, Method method, Object[] args, Object result) {
        // 如果没有条件表达式，默认返回true
        if (!StringUtils.hasText(condition)) {
            return true;
        }
        
        try {
            Expression expression = parser.parseExpression(condition);
            EvaluationContext context = createEvaluationContext(method, args, result);
            
            Object value = expression.getValue(context, Boolean.class);
            return Boolean.TRUE.equals(value);
            
        } catch (Exception e) {
            logger.error("Failed to evaluate cache condition: {}, defaulting to true", condition, e);
            return true; // 条件评估失败时，默认允许缓存操作
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
        
        // 添加常用的条件判断函数
        context.setVariable("isEmpty", new IsEmptyFunction());
        context.setVariable("isNotEmpty", new IsNotEmptyFunction());
        context.setVariable("isNull", new IsNullFunction());
        context.setVariable("isNotNull", new IsNotNullFunction());
        
        return context;
    }
    
    /**
     * 判断是否为空的函数
     */
    public static class IsEmptyFunction {
        public boolean apply(Object obj) {
            if (obj == null) {
                return true;
            }
            if (obj instanceof String) {
                return ((String) obj).isEmpty();
            }
            if (obj instanceof java.util.Collection) {
                return ((java.util.Collection<?>) obj).isEmpty();
            }
            if (obj instanceof java.util.Map) {
                return ((java.util.Map<?, ?>) obj).isEmpty();
            }
            if (obj.getClass().isArray()) {
                return java.lang.reflect.Array.getLength(obj) == 0;
            }
            return false;
        }
    }
    
    /**
     * 判断是否不为空的函数
     */
    public static class IsNotEmptyFunction {
        private final IsEmptyFunction isEmpty = new IsEmptyFunction();
        
        public boolean apply(Object obj) {
            return !isEmpty.apply(obj);
        }
    }
    
    /**
     * 判断是否为null的函数
     */
    public static class IsNullFunction {
        public boolean apply(Object obj) {
            return obj == null;
        }
    }
    
    /**
     * 判断是否不为null的函数
     */
    public static class IsNotNullFunction {
        public boolean apply(Object obj) {
            return obj != null;
        }
    }
}