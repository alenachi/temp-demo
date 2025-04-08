# 基于WebFlux的Spring Boot高级架构项目

作为Spring Boot高级架构师，我将为您构建一个符合企业级标准的基于WebFlux的项目，整合Mybatis并实现SQL日志拦截功能。

## 项目结构

```
springboot-webflux-mybatis/
├── build.gradle
├── settings.gradle
├── src/
│   ├── main/
│   │   ├── java/com/example/demo/
│   │   │   ├── config/
│   │   │   │   ├── MyBatisConfig.java
│   │   │   │   └── WebFluxConfig.java
│   │   │   ├── interceptor/
│   │   │   │   └── SqlLoggingInterceptor.java
│   │   │   ├── controller/
│   │   │   │   └── TestController.java
│   │   │   ├── service/
│   │   │   │   ├── TestService.java
│   │   │   │   └── impl/
│   │   │   │       └── TestServiceImpl.java
│   │   │   ├── mapper/
│   │   │   │   └── TestMapper.java
│   │   │   ├── model/
│   │   │   │   └── TestEntity.java
│   │   │   └── DemoApplication.java
│   │   └── resources/
│   │       ├── application.yml
│   │       └── mybatis-config.xml
│   └── test/
│       └── java/com/example/demo/
│           └── DemoApplicationTests.java
```

## 1. build.gradle 配置

```gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.0'
    id 'io.spring.dependency-management' version '1.1.4'
}

group = 'com.example'
version = '0.0.1-SNAPSHOT'

java {
    sourceCompatibility = '17'
}

configurations {
    compileOnly {
        extendsFrom annotationProcessor
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Starter WebFlux
    implementation 'org.springframework.boot:spring-boot-starter-webflux'
    
    // MyBatis
    implementation 'org.mybatis.spring.boot:mybatis-spring-boot-starter:3.0.3'
    
    // Database
    runtimeOnly 'com.mysql:mysql-connector-j'
    implementation 'com.zaxxer:HikariCP:5.0.1'
    
    // Lombok
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    
    // Testing
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'io.projectreactor:reactor-test'
    
    // Logging
    implementation 'org.apache.logging.log4j:log4j-core:2.20.0'
    implementation 'org.apache.logging.log4j:log4j-api:2.20.0'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

## 2. 应用配置 (application.yml)

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/test_db?useSSL=false&serverTimezone=UTC
    username: root
    password: password
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      pool-name: HikariCP
      maximum-pool-size: 10
      minimum-idle: 5
      idle-timeout: 30000
      max-lifetime: 600000
      connection-timeout: 30000

mybatis:
  config-location: classpath:mybatis-config.xml
  mapper-locations: classpath:mapper/*.xml

logging:
  level:
    root: INFO
    com.example.demo.interceptor: DEBUG
```

## 3. MyBatis 配置 (mybatis-config.xml)

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE configuration
        PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-config.dtd">
<configuration>
    <settings>
        <setting name="mapUnderscoreToCamelCase" value="true"/>
    </settings>
    <plugins>
        <plugin interceptor="com.example.demo.interceptor.SqlLoggingInterceptor"/>
    </plugins>
</configuration>
```

## 4. MyBatis 配置类

```java
package com.example.demo.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

@Configuration
@MapperScan("com.example.demo.mapper")
public class MyBatisConfig {

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);
        
        // 配置MyBatis配置文件
        Resource configLocation = new PathMatchingResourcePatternResolver()
                .getResource("classpath:mybatis-config.xml");
        sessionFactory.setConfigLocation(configLocation);
        
        return sessionFactory.getObject();
    }
}
```

## 5. SQL日志拦截器

```java
package com.example.demo.interceptor;

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
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})
})
@Component
public class SqlLoggingInterceptor implements Interceptor {
    private static final Logger logger = LoggerFactory.getLogger(SqlLoggingInterceptor.class);

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
        Object parameter = invocation.getArgs().length > 1 ? invocation.getArgs()[1] : null;
        
        BoundSql boundSql = mappedStatement.getBoundSql(parameter);
        String sql = boundSql.getSql().replaceAll("[\\s]+", " ");
        
        try {
            Object result = invocation.proceed();
            long endTime = System.currentTimeMillis();
            long timeCost = endTime - startTime;
            
            logger.debug("\n============== SQL Log Start ==============\n" +
                    "SQL ID  : {}\n" +
                    "SQL     : {}\n" +
                    "Params  : {}\n" +
                    "TimeCost: {} ms\n" +
                    "Result  : {}\n" +
                    "============== SQL Log End ==============",
                    mappedStatement.getId(), sql, parameter, timeCost, result);
            
            return result;
        } catch (Exception e) {
            logger.error("\n============== SQL Error Log Start ==============\n" +
                    "SQL ID  : {}\n" +
                    "SQL     : {}\n" +
                    "Params  : {}\n" +
                    "Error   : {}\n" +
                    "============== SQL Error Log End ==============",
                    mappedStatement.getId(), sql, parameter, e.getMessage());
            throw e;
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // 可以读取配置文件的属性
    }
}
```

## 6. 测试实体类

```java
package com.example.demo.model;

import lombok.Data;

@Data
public class TestEntity {
    private Long id;
    private String name;
    private String description;
}
```

## 7. Mapper 接口

```java
package com.example.demo.mapper;

import com.example.demo.model.TestEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TestMapper {
    @Select("SELECT * FROM test_table")
    List<TestEntity> findAll();
    
    @Select("SELECT * FROM test_table WHERE id = #{id}")
    TestEntity findById(Long id);
}
```

## 8. 服务层实现

```java
package com.example.demo.service;

import com.example.demo.model.TestEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TestService {
    Flux<TestEntity> getAllTests();
    Mono<TestEntity> getTestById(Long id);
}
```

```java
package com.example.demo.service.impl;

import com.example.demo.mapper.TestMapper;
import com.example.demo.model.TestEntity;
import com.example.demo.service.TestService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Service
public class TestServiceImpl implements TestService {
    private final TestMapper testMapper;

    public TestServiceImpl(TestMapper testMapper) {
        this.testMapper = testMapper;
    }

    @Override
    public Flux<TestEntity> getAllTests() {
        return Mono.fromCallable(testMapper::findAll)
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable);
    }

    @Override
    public Mono<TestEntity> getTestById(Long id) {
        return Mono.fromCallable(() -> testMapper.findById(id))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
```

## 9. 控制器

```java
package com.example.demo.controller;

import com.example.demo.model.TestEntity;
import com.example.demo.service.TestService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/tests")
public class TestController {
    private final TestService testService;

    public TestController(TestService testService) {
        this.testService = testService;
    }

    @GetMapping
    public Flux<TestEntity> getAllTests() {
        return testService.getAllTests();
    }

    @GetMapping("/{id}")
    public Mono<TestEntity> getTestById(@PathVariable Long id) {
        return testService.getTestById(id);
    }
}
```

## 10. 主应用类

```java
package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

## 11. 测试类

```java
package com.example.demo;

import com.example.demo.model.TestEntity;
import com.example.demo.service.TestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.test.StepVerifier;

@SpringBootTest
class DemoApplicationTests {
    @Autowired
    private TestService testService;

    @Test
    void contextLoads() {
    }

    @Test
    void testGetAllTests() {
        StepVerifier.create(testService.getAllTests())
                .expectNextCount(2) // 根据实际数据调整
                .verifyComplete();
    }

    @Test
    void testGetTestById() {
        StepVerifier.create(testService.getTestById(1L))
                .expectNextMatches(test -> test.getId().equals(1L))
                .verifyComplete();
    }
}
```

## 项目说明

1. **技术栈**:
   - Spring WebFlux (响应式编程)
   - MyBatis (ORM框架)
   - HikariCP (高性能连接池)
   - Log4j2 (日志框架)

2. **架构特点**:
   - 清晰的层次结构(Controller-Service-Mapper)
   - 响应式与非阻塞IO处理
   - 完善的SQL日志拦截
   - 符合企业级代码规范

3. **SQL日志拦截**:
   - 拦截所有SQL执行
   - 记录SQL语句、参数、执行时间和结果
   - 错误情况下记录错误信息
   - 使用SLF4J日志框架输出

4. **响应式编程**:
   - 使用Mono和Flux作为返回类型
   - 使用Schedulers.boundedElastic()处理阻塞操作
   - 提供完整的响应式测试

这个项目展示了如何在WebFlux环境中整合MyBatis，并通过拦截器实现SQL日志记录功能，完全符合企业级开发标准。