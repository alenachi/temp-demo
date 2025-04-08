这是一种基于最新方法的解决方案，截至 _2021 年 5 月_，摘自 官方文档：

```java
 import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;
import reactor.util.context.Context;

@Slf4j
@Configuration
public class RequestIdFilter implements WebFilter {

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    ServerHttpRequest request = exchange.getRequest();
    String requestId = getRequestId(request.getHeaders());
    return chain
        .filter(exchange)
        .doOnEach(logOnEach(r -> log.info("{} {}", request.getMethod(), request.getURI())))
        .contextWrite(Context.of("CONTEXT_KEY", requestId));
  }

  private String getRequestId(HttpHeaders headers) {
    List<String> requestIdHeaders = headers.get("X-Request-ID");
    return requestIdHeaders == null || requestIdHeaders.isEmpty()
        ? UUID.randomUUID().toString()
        : requestIdHeaders.get(0);
  }

  public static <T> Consumer<Signal<T>> logOnEach(Consumer<T> logStatement) {
    return signal -> {
      String contextValue = signal.getContextView().get("CONTEXT_KEY");
      try (MDC.MDCCloseable cMdc = MDC.putCloseable("MDC_KEY", contextValue)) {
        logStatement.accept(signal.get());
      }
    };
  }

  public static <T> Consumer<Signal<T>> logOnNext(Consumer<T> logStatement) {
    return signal -> {
      if (!signal.isOnNext()) return;
      String contextValue = signal.getContextView().get("CONTEXT_KEY");
      try (MDC.MDCCloseable cMdc = MDC.putCloseable("MDC_KEY", contextValue)) {
        logStatement.accept(signal.get());
      }
    };
  }
}
```
鉴于您的 application.properties 中有以下行：

```
 logging.pattern.level=[%X{MDC_KEY}] %5p
```
然后每次调用端点时，您的服务器日志将包含如下日志：
```
 2021-05-06 17:07:41.852 [60b38305-7005-4a05-bac7-ab2636e74d94]  INFO 20158 --- [or-http-epoll-6] my.package.RequestIdFilter    : GET http://localhost:12345/my-endpoint/444444/
```
每次你想在反应上下文中手动记录一些东西时，你都需要将以下内容添加到你的反应链中：
```
 .doOnEach(logOnNext(r -> log.info("Something")))
```
如果您希望将 X-Request-ID 传播到其他服务以进行分布式跟踪，您需要从反应上下文（而不是从 MDC）读取它并使用以下内容包装您的 WebClient 代码：
```java
 Mono.deferContextual(
    ctx -> {
      RequestHeadersSpec<?> request = webClient.get().uri(uri);
      request = request.header("X-Request-ID", ctx.get("CONTEXT_KEY"));
      // The rest of your request logic...
    });
```
原文由 Marco Lackovic 发布，翻译遵循 CC BY-SA 4.0 许可协议