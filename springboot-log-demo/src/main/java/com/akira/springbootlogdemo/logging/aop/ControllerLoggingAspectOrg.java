package com.akira.springbootlogdemo.logging.aop;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import com.akira.springbootlogdemo.logging.config.LoggingProperties;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

@Aspect
@Component
@Order(1) // 确保在事务切面之前执行
public class ControllerLoggingAspectOrg {

    private static final Logger logger = LoggerFactory.getLogger("CONTROLLER_LOGGER");
    private static final Logger errorLogger = LoggerFactory.getLogger("CONTROLLER_ERROR_LOGGER");

    private final ObjectMapper objectMapper;

    private final LoggingProperties loggingProperties;

    public ControllerLoggingAspectOrg(ObjectMapper objectMapper, LoggingProperties loggingProperties) {
        this.objectMapper = objectMapper
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        this.loggingProperties = loggingProperties;
    }

    @Around("@within(org.springframework.web.bind.annotation.RestController) || " +
            "@annotation(org.springframework.web.bind.annotation.RestController)")
    public Object logController(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 获取请求元数据
        RequestMetaData metaData = extractRequestMetaData(joinPoint, method);

        if (!loggingProperties.isIncludeErrorStacktrace()) {
            return joinPoint.proceed();
        }

        // 创建请求追踪ID
        String traceId = UUID.randomUUID().toString();
        metaData.setTraceId(traceId);

        // 记录请求日志
        logRequest(metaData);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        try {
            Object result = joinPoint.proceed();
            stopWatch.stop();

            // 处理响应式和非响应式返回值
            if (result instanceof Mono) {
                return processMonoResponse((Mono<?>) result, metaData, stopWatch);
            } else if (result instanceof Flux) {
                return processFluxResponse((Flux<?>) result, metaData, stopWatch);
            } else {
                return processNormalResponse(result, metaData, stopWatch);
            }
        } catch (Throwable ex) {
            stopWatch.stop();
            logError(metaData, ex, stopWatch.getTotalTimeMillis());
            throw ex;
        }
    }

    private RequestMetaData extractRequestMetaData(ProceedingJoinPoint joinPoint, Method method) {
            RequestMetaData metaData = new RequestMetaData();

        // 设置方法信息
        metaData.setClassName(joinPoint.getTarget().getClass().getSimpleName());
        metaData.setMethodName(method.getName());

        // 解析请求映射注解
        resolveRequestMappingAnnotations(method, metaData);

        // 提取请求参数
        extractRequestParameters(joinPoint, metaData);

        return metaData;
    }

    private void resolveRequestMappingAnnotations(Method method, RequestMetaData metaData) {
        // 类级别注解
        RequestMapping classMapping = method.getDeclaringClass().getAnnotation(RequestMapping.class);
        if (classMapping != null) {
            metaData.setBasePath(classMapping.value().length > 0 ? classMapping.value()[0] : "");
        }

        // 方法级别注解
        Arrays.stream(method.getAnnotations())
                .filter(annotation -> annotation.annotationType().isAnnotationPresent(RequestMapping.class))
                .findFirst()
                .ifPresent(annotation -> {
                    if (annotation instanceof GetMapping) {
                        metaData.setHttpMethod("GET");
                        metaData.setPath(((GetMapping) annotation).value());
                    } else if (annotation instanceof PostMapping) {
                        metaData.setHttpMethod("POST");
                        metaData.setPath(((PostMapping) annotation).value());
                    } // 其他HTTP方法类似处理
                });
    }

    private void extractRequestParameters(ProceedingJoinPoint joinPoint, RequestMetaData metaData) {
        Object[] args = joinPoint.getArgs();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Class<?>[] parameterTypes = signature.getParameterTypes();

        List<RequestParamData> params = new ArrayList<>();
        ServerWebExchange exchange = null;

        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof ServerWebExchange) {
                exchange = (ServerWebExchange) args[i];
                continue;
            }

            // 过滤掉不需要记录的参数类型
            if (shouldSkipParameter(parameterTypes[i])) {
                continue;
            }

            RequestParamData paramData = new RequestParamData();
            paramData.setName(parameterNames != null ? parameterNames[i] : "arg" + i);
            paramData.setType(parameterTypes[i].getSimpleName());

            try {
                paramData.setValue(sanitizeParameterValue(args[i]));
            } catch (Exception e) {
                paramData.setValue("[Serialization Error]");
            }

            params.add(paramData);
        }

        metaData.setParams(params);

        if (exchange != null) {
            extractExchangeData(exchange, metaData);
        }
    }

    private boolean shouldSkipParameter(Class<?> parameterType) {
        return loggingProperties.getExcludedParameterTypes().contains(parameterType.getName());
    }

    private Object sanitizeParameterValue(Object value) throws JsonProcessingException {
        if (value == null) {
            return null;
        }

        // 对敏感数据进行脱敏处理
        if (value.toString().contains("password") || value.toString().contains("secret")) {
            return "******";
        }

        // 简单类型直接返回
        if (value instanceof Number || value instanceof Boolean || value instanceof Character ||
                value instanceof String || value instanceof Enum) {
            return value;
        }

        // 复杂对象序列化为JSON
        return objectMapper.writeValueAsString(value);
    }

    private void extractExchangeData(ServerWebExchange exchange, RequestMetaData metaData) {
        // 请求头
        HttpHeaders headers = exchange.getRequest().getHeaders();
        Map<String, String> headerMap = headers.entrySet().stream()
                .filter(entry -> !loggingProperties.getExcludedHeaders().contains(entry.getKey()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> String.join(",", entry.getValue())
                ));
        metaData.setHeaders(headerMap);

        // 请求路径和查询参数
        metaData.setRequestPath(exchange.getRequest().getPath().value());
        metaData.setQueryParams(exchange.getRequest().getQueryParams().toSingleValueMap());
    }

    private void logRequest(RequestMetaData metaData) {
        if (logger.isInfoEnabled()) {
            try {
                Map<String, Object> logData = new LinkedHashMap<>();
                logData.put("traceId", metaData.getTraceId());
                logData.put("type", "REQUEST");
                logData.put("method", metaData.getHttpMethod());
                logData.put("path", metaData.getFullPath());
                logData.put("class", metaData.getClassName());
                logData.put("method", metaData.getMethodName());
                logData.put("headers", metaData.getHeaders());
                logData.put("params", metaData.getParams());
                logData.put("queryParams", metaData.getQueryParams());

                logger.info(objectMapper.writeValueAsString(logData));
                logger.info("===========================================1");
            } catch (JsonProcessingException e) {
                logger.warn("Failed to serialize request log data", e);
            }
        }
    }

    private Mono<?> processMonoResponse(Mono<?> mono, RequestMetaData metaData, StopWatch stopWatch) {
        return mono
                .doOnSuccess(response -> logResponse(response, metaData, stopWatch.getTotalTimeMillis(), null))
                .doOnError(error -> logError(metaData, error, stopWatch.getTotalTimeMillis()));
    }

    private Flux<?> processFluxResponse(Flux<?> flux, RequestMetaData metaData, StopWatch stopWatch) {
        return flux
                .collectList()
                .doOnSuccess(response -> logResponse(response, metaData, stopWatch.getTotalTimeMillis(), null))
                .doOnError(error -> logError(metaData, error, stopWatch.getTotalTimeMillis()))
                .flatMapMany(Flux::fromIterable);
    }

    private Object processNormalResponse(Object response, RequestMetaData metaData, StopWatch stopWatch) {
        logResponse(response, metaData, stopWatch.getTotalTimeMillis(), null);
        return response;
    }

    private void logResponse(Object response, RequestMetaData metaData, long duration, Throwable error) {
        if (logger.isInfoEnabled()) {
            try {
                Map<String, Object> logData = new LinkedHashMap<>();
                logData.put("traceId", metaData.getTraceId());
                logData.put("type", error != null ? "ERROR" : "RESPONSE");
                logData.put("method", metaData.getHttpMethod());
                logData.put("path", metaData.getFullPath());
                logData.put("duration", duration + "ms");

                if (error != null) {
                    logData.put("error", buildErrorData(error));
                } else {
                    logData.put("response", sanitizeResponse(response));
                }

                String logMessage = objectMapper.writeValueAsString(logData);

                if (error != null) {
                    errorLogger.error(logMessage);
                } else {
                    logger.info(logMessage);
                }
            } catch (JsonProcessingException e) {
                logger.warn("Failed to serialize response log data", e);
            }
        }
    }

    private Object sanitizeResponse(Object response) {
        if (response == null) {
            return null;
        }

        if (response instanceof ResponseEntity) {
            ResponseEntity<?> responseEntity = (ResponseEntity<?>) response;
            Map<String, Object> responseData = new LinkedHashMap<>();
            responseData.put("status", responseEntity.getStatusCodeValue());
            responseData.put("headers", responseEntity.getHeaders());

            try {
                responseData.put("body", objectMapper.writeValueAsString(responseEntity.getBody()));
            } catch (JsonProcessingException e) {
                responseData.put("body", "[Serialization Error]");
            }

            return responseData;
        }

        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            return "[Serialization Error]";
        }
    }

    private Map<String, Object> buildErrorData(Throwable error) {
        Map<String, Object> errorData = new LinkedHashMap<>();
        errorData.put("message", error.getMessage());
        errorData.put("type", error.getClass().getName());

        if (loggingProperties.isIncludeErrorStacktrace()) {
            errorData.put("stackTrace", Arrays.stream(error.getStackTrace())
                    .map(StackTraceElement::toString)
                    .collect(Collectors.toList()));
        }

        return errorData;
    }

    private void logError(RequestMetaData metaData, Throwable error, long duration) {
        logResponse(null, metaData, duration, error);
    }

    // 内部数据结构类
    @Data
    private static class RequestMetaData {
        private String traceId;
        private String className;
        private String methodName;
        private String httpMethod;
        private String basePath = "";
        private String[] path = new String[0];
        private String requestPath;
        private Map<String, String> headers;
        private Map<String, String> queryParams;
        private List<RequestParamData> params;

        public String getFullPath() {
            return basePath + (path.length > 0 ? path[0] : "") + (requestPath != null ? requestPath : "");
        }

        // getters and setters
    }

    @Data
    private static class RequestParamData {
        private String name;
        private String type;
        private Object value;

        // getters and setters
    }
}