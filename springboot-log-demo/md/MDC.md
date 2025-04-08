
This library is a utility for adding MDC logs to a Spring Webflux application (with Spring Boot version 3).
https://github.com/vincenzo-racca/spring-webflux-mdc


在 Reactor（Spring WebFlux 的底层库）中，`Hooks.enableAutomaticContextPropagation()` 可以让 Reactor **自动传递上下文（Context）**，包括 `MDC`（Mapped Diagnostic Context）和 Reactor 的 `Context`。  

### 🔧 **如何设置 `userId` 并让它在日志中正确显示？**
#### **1. 启用自动上下文传播**
在 Spring Boot 启动时（如 `@PostConstruct` 或 `ApplicationRunner`），启用 Reactor 的自动上下文传播：
```java
import reactor.core.publisher.Hooks;

@PostConstruct
public void enableContextPropagation() {
    Hooks.enableAutomaticContextPropagation();  // 自动传递 MDC 和 Reactor Context
}
```
这样，当你在某个地方设置 `MDC` 或 Reactor `Context`，它会在整个响应式链中传递。

---

#### **2. 设置 `userId` 的方式**
##### **方式 1：使用 `contextWrite` 设置 Reactor Context**
```java
import reactor.util.context.Context;

public Mono<String> getUserData(String userId) {
    return Mono.just("some-data")
        .flatMap(data -> {
            log.info("Processing data for user: {}", userId);  // 日志会自动带上 MDC
            return Mono.just(data);
        })
        .contextWrite(Context.of("userId", userId));  // 设置 userId 到 Reactor Context
}
```
**日志输出示例**：
```
2024-01-01 12:00:00 [INFO] 12345 MyClass - Processing data for user: 12345
```

##### **方式 2：结合 `WebFilter` 自动设置（推荐）**
```java
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import org.slf4j.MDC;

public class UserIdWebFilter implements WebFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-ID");
        
        return chain.filter(exchange)
            .contextWrite(ctx -> ctx.put("userId", userId))  // 存入 Reactor Context
            .doOnEach(signal -> {
                // 在每个信号（Signal）恢复 MDC
                if (signal.isOnNext()) {
                    MDC.put("userId", signal.getContextView().getOrDefault("userId", "UNKNOWN"));
                }
            })
            .doFinally(signal -> MDC.clear());  // 请求结束时清理 MDC
    }
}
```
**日志格式**（`logback.xml`）：
```xml
<pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %X{userId} %logger{36} - %msg%n</pattern>
```

---

#### **3. 在日志中正确显示 `userId`**
确保你的 `logback.xml`（或 `logback-spring.xml`）包含 `%X{userId}`：
```xml
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %X{userId} %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    <root level="INFO">
        <appender-ref ref="STDOUT" />
    </root>
</configuration>
```

---

### ✅ **总结**
| 方法 | 适用场景 | 优点 | 缺点 |
|------|----------|------|------|
| **`contextWrite` 手动设置** | 单个响应式流 | 精确控制 | 需要手动传递 |
| **`WebFilter` 自动设置** | WebFlux 全局处理 | 自动管理 `userId` | 需要处理线程切换 |
| **`Hooks.enableAutomaticContextPropagation`** | 全局自动传递 | 无需额外代码 | 可能轻微影响性能 |

**推荐做法**：
1. **启用 `Hooks.enableAutomaticContextPropagation()`**（全局支持）。
2. **使用 `WebFilter` 自动设置 `userId`**（适用于 WebFlux）。
3. **在 `logback.xml` 中使用 `%X{userId}`** 显示日志。

这样，即使在响应式环境下，`userId` 也能正确传递，日志不会互相干扰！ 🚀