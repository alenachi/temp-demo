package com.akira.springbootlogdemo.logging.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public class ControllerLoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(ControllerLoggingAspect.class);

    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void controllerClass() {}

    @Pointcut("execution(* com.akira.springbootlogdemo.controller..*(..))")
    public void controllerMethod() {}

    @Pointcut("controllerClass() && controllerMethod()")
    public void controllerPointcut() {}

    @Around("controllerPointcut()")
    public Object logControllerRequestResponse(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        // 获取请求参数
        Object[] args = joinPoint.getArgs();
        ServerWebExchange exchange = getExchangeArgument(args);

        if (exchange != null) {
            logger.info("Request: {} {}, headers={}",
                    exchange.getRequest().getMethod(),
                    exchange.getRequest().getURI().getPath(),
                    exchange.getRequest().getHeaders());
        } else {
            logger.info("Entering {}.{}() with args: {}", className, methodName, args);
        }

        long startTime = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long duration = System.currentTimeMillis() - startTime;

        if (result instanceof Mono) {
            return ((Mono<?>) result).doOnSuccess(response -> {
                if (exchange != null) {
                    logger.info("Response: duration={}ms, status={}",
                            duration,
                            exchange.getResponse().getStatusCode());
                }
                logResponseContent(response);
            }).doOnError(error -> {
                logger.error("Controller error: {}, duration={}ms",
                        error.getMessage(), duration);
            });
        } else if (result instanceof ResponseEntity) {
            logger.info("Response: status={}, body={}, duration={}ms",
                    ((ResponseEntity<?>) result).getStatusCode(),
                    ((ResponseEntity<?>) result).getBody(),
                    duration);
        } else {
            logger.info("Exiting {}.{}() with result: {}, duration={}ms",
                    className, methodName, result, duration);
        }

        return result;
    }

    private ServerWebExchange getExchangeArgument(Object[] args) {
        if (args == null) return null;

        for (Object arg : args) {
            if (arg instanceof ServerWebExchange) {
                return (ServerWebExchange) arg;
            }
        }
        return null;
    }

    private void logResponseContent(Object response) {
        if (response instanceof ResponseEntity) {
            ResponseEntity<?> responseEntity = (ResponseEntity<?>) response;
            logger.info("Response: status={}, body={}",
                    responseEntity.getStatusCode(),
                    responseEntity.getBody());
        } else {
            logger.info("Response: {}", response);
        }
    }
}