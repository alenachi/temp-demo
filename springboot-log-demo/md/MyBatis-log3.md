# 企业级响应式Spring Boot中MyBatis SQL日志记录的优化建议

您提出的需求是在响应式Spring Boot应用中记录MyBatis的SQL、参数和结果。上面的代码已经提供了基础实现，但为了完全符合企业级开发要求，还需要进行以下优化：

## 当前方案的优缺点分析

**优点**：
1. 已经实现了基本的SQL拦截和日志记录功能
2. 区分了正常执行和错误情况
3. 记录了执行时间
4. 使用了响应式编程模型

**不足**：
1. 日志输出格式可以更结构化
2. 敏感数据需要脱敏处理
3. 性能影响需要优化
4. 缺少上下文信息(如traceId)
5. 大结果集处理不够完善

## 企业级优化建议

### 1. 日志结构化输出

```java
// 在SqlLoggingInterceptor中改进日志输出
logger.debug("SQL execution trace", StructuredArguments.entries(Map.of(
    "type", "sql",
    "id", mappedStatement.getId(),
    "sql", sql,
    "params", maskSensitiveData(parameter),
    "duration", timeCost + "ms",
    "resultSize", getResultSize(result)
)));
```

### 2. 敏感数据脱敏

```java
private Object maskSensitiveData(Object parameter) {
    if (parameter instanceof Map) {
        Map<String, Object> masked = new HashMap<>((Map<?, ?>) parameter);
        masked.replaceAll((k, v) -> k.toLowerCase().contains("password") ? "******" : v);
        return masked;
    }
    return parameter;
}
```

### 3. 性能优化

```java
// 只在DEBUG级别记录完整结果
if (logger.isDebugEnabled()) {
    logger.debug("Full result: {}", truncateResult(result));
} else {
    logger.info("Result size: {}", getResultSize(result));
}

private Object truncateResult(Object result) {
    if (result instanceof Collection) {
        Collection<?> col = (Collection<?>) result;
        if (col.size() > 10) {
            return col.stream().limit(10).collect(Collectors.toList()) + "...(truncated)";
        }
    }
    return result;
}
```

### 4. 添加请求上下文信息

```java
// 添加TraceID支持
private String getTraceId() {
    try {
        return Optional.ofNullable(MDC.get("traceId"))
                .orElseGet(() -> Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                        .map(attrs -> attrs.getAttribute("traceId", RequestAttributes.SCOPE_REQUEST))
                        .orElse("N/A"));
    } catch (Exception e) {
        return "N/A";
    }
}
```

### 5. 响应式上下文传递

```java
// 在响应式环境中获取上下文
private Mono<String> getReactiveTraceId() {
    return Mono.deferContextual(ctx -> 
        Mono.just(ctx.getOrDefault("traceId", "N/A"))
    );
}
```

### 6. 完整的优化后拦截器

```java
@Intercepts({
    @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
    @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})
})
@Component
public class SqlLoggingInterceptor implements Interceptor {
    private static final Logger logger = LoggerFactory.getLogger(SqlLoggingInterceptor.class);
    private static final int MAX_RESULT_LOG_LENGTH = 2000;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        if (!logger.isDebugEnabled()) {
            return invocation.proceed();
        }

        long startTime = System.currentTimeMillis();
        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
        Object parameter = invocation.getArgs().length > 1 ? invocation.getArgs()[1] : null;
        BoundSql boundSql = ms.getBoundSql(parameter);
        String sql = boundSql.getSql().replaceAll("\\s+", " ").trim();
        String traceId = getTraceId();

        try {
            Object result = invocation.proceed();
            long timeCost = System.currentTimeMillis() - startTime;

            logExecution(traceId, ms.getId(), sql, parameter, timeCost, result, null);
            return result;
        } catch (Exception e) {
            long timeCost = System.currentTimeMillis() - startTime;
            logExecution(traceId, ms.getId(), sql, parameter, timeCost, null, e);
            throw e;
        }
    }

    private void logExecution(String traceId, String sqlId, String sql, 
                            Object params, long timeCost, 
                            Object result, Exception error) {
        Map<String, Object> logData = new LinkedHashMap<>();
        logData.put("traceId", traceId);
        logData.put("sqlId", sqlId);
        logData.put("sql", sql);
        logData.put("params", maskSensitiveData(params));
        logData.put("timeCost", timeCost + "ms");

        if (error != null) {
            logData.put("error", error.getMessage());
            logger.error("SQL execution failed", StructuredArguments.entries(logData));
        } else {
            logData.put("resultSize", getResultSize(result));
            if (logger.isDebugEnabled()) {
                logData.put("result", truncateResult(result));
            }
            logger.debug("SQL execution trace", StructuredArguments.entries(logData));
        }
    }

    // ... 其他辅助方法保持不变 ...
}
```

### 7. 配置优化

在`application.yml`中添加：

```yaml
logging:
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - traceId=%X{traceId} - %msg%n"
```

## 企业级额外考虑

1. **监控集成**：
   - 将SQL执行时间指标导出到Prometheus
   - 设置慢SQL报警阈值

2. **动态开关**：
   ```java
   @Configuration
   @ConditionalOnProperty(name = "logging.sql.enabled", havingValue = "true")
   public class SqlLoggingAutoConfiguration {
       // 自动配置
   }
   ```

3. **异步日志**：
   - 使用Log4j2的AsyncLogger减少对主线程的影响

4. **采样率控制**：
   ```java
   private boolean shouldLog() {
       return logger.isDebugEnabled() && 
              (ThreadLocalRandom.current().nextDouble() < samplingRate);
   }
   ```

5. **结果序列化优化**：
   - 使用Jackson过滤敏感字段
   - 控制序列化深度

## 最终建议

对于企业级应用，我建议采用以下组合方案：

1. **基础日志**：使用优化后的拦截器记录SQL基本信息和执行时间
2. **详细调试**：在DEBUG级别记录完整参数和结果(脱敏后)
3. **监控**：将SQL指标集成到应用监控系统
4. **追踪**：与分布式追踪系统(如Sleuth/Zipkin)集成
5. **动态控制**：提供运行时开关控制日志详细程度

这样的组合既能满足开发和运维需求，又不会对生产环境造成过大性能影响，同时符合企业安全规范。

您可以根据实际项目需求和团队规范，选择适合的优化点进行实现。对于大多数企业级应用，实现1-4点优化就已经足够完善了。