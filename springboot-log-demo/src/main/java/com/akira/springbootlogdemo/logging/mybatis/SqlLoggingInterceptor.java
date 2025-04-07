package com.akira.springbootlogdemo.logging.mybatis;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Intercepts({
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "update",
                args = {MappedStatement.class, Object.class})
})
@Component
public class SqlLoggingInterceptor implements Interceptor {

    private static final Logger logger = LoggerFactory.getLogger("SQL_LOGGER");

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
        Object parameter = invocation.getArgs()[1];

        BoundSql boundSql = mappedStatement.getBoundSql(parameter);
        String sql = boundSql.getSql().replaceAll("\\s+", " ").trim();

        long startTime = System.currentTimeMillis();
        Object result = invocation.proceed();
        long duration = System.currentTimeMillis() - startTime;

        logger.debug("SQL: {}", sql);
        logger.debug("Parameters: {}", boundSql.getParameterObject());
        logger.debug("Execution time: {}ms", duration);

        if (result instanceof java.util.List) {
            logger.debug("Result size: {}", ((java.util.List<?>) result).size());
        } else {
            logger.debug("Result: {}", result);
        }

        return result;
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // No properties to set
    }
}