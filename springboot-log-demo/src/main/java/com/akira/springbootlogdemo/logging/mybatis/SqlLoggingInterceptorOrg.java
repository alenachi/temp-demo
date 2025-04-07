package com.akira.springbootlogdemo.logging.mybatis;

import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Intercepts({
        @Signature(type = Executor.class, method = "update",
                args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class}),
        @Signature(type = Executor.class, method = "queryCursor",
                args = {MappedStatement.class, Object.class, RowBounds.class})
})
@Component
public class SqlLoggingInterceptorOrg implements Interceptor {

    private static final Logger logger = LoggerFactory.getLogger("SQL_LOGGER");
    private static final Logger slowQueryLogger = LoggerFactory.getLogger("SLOW_SQL_LOGGER");

    // SQL操作类型映射
    private static final Map<String, String> SQL_OPERATION_MAP = new HashMap<>();

    static {
        SQL_OPERATION_MAP.put("insert", "CREATE");
        SQL_OPERATION_MAP.put("delete", "DELETE");
        SQL_OPERATION_MAP.put("update", "UPDATE");
        SQL_OPERATION_MAP.put("select", "READ");
        SQL_OPERATION_MAP.put("count", "READ");
    }

    private long slowQueryThreshold = 500; // 慢查询阈值(ms)
    private boolean showResults = true;
    private boolean showParams = true;
    private int maxResultLength = 1000;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
        Object parameter = invocation.getArgs()[1];

        // 获取SQL操作类型
        String operationType = determineOperationType(ms.getId());

        // 获取BoundSql
        BoundSql boundSql = ms.getBoundSql(parameter);
        String sql = formatSql(boundSql.getSql());

        // 获取连接信息
        Connection connection = (Connection) invocation.getTarget()
                .getClass()
                .getMethod("getTransaction")
                .invoke(invocation.getTarget())
                .getConnection();

        // 准备日志数据
        Map<String, Object> logData = new LinkedHashMap<>();
        logData.put("operation", operationType);
        logData.put("sqlId", ms.getId());
        logData.put("database", connection.getCatalog());

        if (showParams) {
            logData.put("parameters", getParameterValue(boundSql));
        }

        // 执行SQL并计时
        long start = System.nanoTime();
        Object result = invocation.proceed();
        long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        // 记录执行时间
        logData.put("executionTime", elapsed + "ms");

        // 处理结果
        if (showResults && result != null) {
            logData.put("result", processResult(result, operationType));
        }

        // 记录SQL日志
        String logMessage = buildLogMessage(logData);
        logger.debug(logMessage);

        // 慢查询日志
        if (elapsed > slowQueryThreshold) {
            slowQueryLogger.warn("Slow query detected: " + logMessage);
        }

        return result;
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        this.slowQueryThreshold = Long.parseLong(properties.getProperty("slowQueryThreshold", "500"));
        this.showResults = Boolean.parseBoolean(properties.getProperty("showResults", "true"));
        this.showParams = Boolean.parseBoolean(properties.getProperty("showParams", "true"));
        this.maxResultLength = Integer.parseInt(properties.getProperty("maxResultLength", "1000"));
    }

    private String determineOperationType(String statementId) {
        String lowerId = statementId.toLowerCase();
        return SQL_OPERATION_MAP.entrySet().stream()
                .filter(entry -> lowerId.contains(entry.getKey()))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse("UNKNOWN");
    }

    private String formatSql(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }

    private Object getParameterValue(BoundSql boundSql) {
        try {
            Object parameterObject = boundSql.getParameterObject();

            if (parameterObject == null) {
                return null;
            }

            // 处理Map参数
            if (parameterObject instanceof Map) {
                return sanitizeParameterMap((Map<?, ?>) parameterObject);
            }

            // 处理简单类型
            if (parameterObject instanceof Number || parameterObject instanceof String ||
                    parameterObject instanceof Boolean || parameterObject instanceof Date) {
                return parameterObject;
            }

            // 处理实体对象
            return extractEntityFields(parameterObject);
        } catch (Exception e) {
            return "[Parameter extract error: " + e.getMessage() + "]";
        }
    }

    private Map<String, Object> sanitizeParameterMap(Map<?, ?> parameterMap) {
        Map<String, Object> result = new LinkedHashMap<>();
        parameterMap.forEach((key, value) -> {
            if (value != null) {
                if (key.toString().toLowerCase().contains("password") ||
                        key.toString().toLowerCase().contains("secret")) {
                    result.put(key.toString(), "******");
                } else {
                    result.put(key.toString(), value.toString());
                }
            }
        });
        return result;
    }

    private Map<String, Object> extractEntityFields(Object entity) {
        Map<String, Object> fields = new LinkedHashMap<>();
        try {
            for (Field field : entity.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Object value = field.get(entity);
                if (value != null) {
                    if (field.getName().toLowerCase().contains("password") ||
                            field.getName().toLowerCase().contains("secret")) {
                        fields.put(field.getName(), "******");
                    } else {
                        fields.put(field.getName(), value);
                    }
                }
            }
        } catch (IllegalAccessException e) {
            fields.put("error", "Failed to extract entity fields: " + e.getMessage());
        }
        return fields;
    }

    private Object processResult(Object result, String operationType) {
        try {
            if (result instanceof List) {
                return processListResult((List<?>) result, operationType);
            } else if (result instanceof Integer || result instanceof Long) {
                return operationType + " affected rows: " + result;
            } else {
                return truncate(result.toString(), maxResultLength);
            }
        } catch (Exception e) {
            return "[Result process error: " + e.getMessage() + "]";
        }
    }

    private Object processListResult(List<?> list, String operationType) {
        if (list.isEmpty()) {
            return "Empty result set";
        }

        if (operationType.equals("READ")) {
            int size = list.size();
            if (size > 3) { // 只显示前3条记录样本
                List<Object> sample = new ArrayList<>(list.subList(0, 3));
                sample.add("... and " + (size - 3) + " more records");
                return sample;
            }
            return list;
        }

        return operationType + " affected " + list.size() + " records";
    }

    private String truncate(String str, int maxLength) {
        if (str == null || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength) + "...[truncated]";
    }

    private String buildLogMessage(Map<String, Object> logData) {
        StringBuilder sb = new StringBuilder();
        logData.forEach((key, value) -> {
            sb.append(key).append(": ");
            if (value instanceof Map || value instanceof List) {
                sb.append(value.toString().replace("\n", "").replace("\r", ""));
            } else {
                sb.append(value);
            }
            sb.append(" | ");
        });
        return sb.toString();
    }
}
