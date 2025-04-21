# Spring Boot提供的产品就绪功能：跟踪（Tracing）

https://docs.spring.io/spring-boot/docs/3.2.0/reference/htmlsingle/#actuator.micrometer-tracing

Spring Boot Actuator 为 Micrometer Tracing 提供了依赖管理和自动配置，Micrometer Tracing 是一个流行的跟踪器库的外观。

## 支持的跟踪器

Spring Boot 为以下跟踪器提供了自动配置：
OpenTelemetry 与 Zipkin、Wavefront 或 OTLP
OpenZipkin Brave 与 Zipkin 或 Wavefront

## 入门

需要一个示例应用程序来开始跟踪。将使用 OpenTelemetry 跟踪器，并使用 Zipkin 作为跟踪后端。

应用程序的代码如下：

```java
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SpringBootApplication
public class MyApplication {

    private static final Log logger = LogFactory.getLog(MyApplication.class);

    @RequestMapping("/")
    String home() {
        logger.info("home() has been called");
        return "Hello World!";
    }

    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }

}
```

注意：在 `home()` 方法中添加了一个日志记录语句。

现在，需要添加以下依赖项：

- org.springframework.boot:spring-boot-starter-actuator
- io.micrometer:micrometer-tracing-bridge-otel - 将 Micrometer Observation API 桥接到OpenTelemetry。
- io.opentelemetry:opentelemetry-exporter-zipkin - 将跟踪报告给 Zipkin。
- 
添加以下应用程序属性：  
`management.tracing.sampling.probability=1.0`

默认情况下，Spring Boot 仅对 10% 的请求进行采样，以防止跟踪后端过载。此属性将其切换为 100%，以便将每个请求发送到跟踪后端。

要收集和可视化跟踪信息，需要一个正在运行的跟踪后端。在这里使用 Zipkin 作为跟踪后端。

在 Zipkin 运行后，你可以启动你的应用程序。

如果你在 Web 浏览器中打开 localhost:8080，应该看到以下输出：  
`
Hello World!
`
在幕后，为HTTP请求创建了一个观察，这个观察随后被桥接到OpenTelemetry，后者向Zipkin报告了一个新的跟踪。

现在打开位于`localhost:9411`的Zipkin用户界面，并按下“Run Query”按钮以列出所有收集的跟踪。应该看到一个跟踪。按下 “Show” 按钮以查看该跟踪的详细信息。

## 日志记录相关ID（Correlation IDs）

相关ID是一种将日志文件中的行与跨度/跟踪相关联的有用方式。默认情况下，只要`management.tracing.enabled`没有被设置为`false`，当使用Micrometer跟踪时，Spring Boot就会在日志中包含相关ID。

默认的相关ID是由MDC值中的`traceId`和`spanId`构建的。
例如，如果Micrometer跟踪已经添加了一个MDC traceId为`803B448A0489F84084905D3093480352`和一个MDC spanId为`3425F23BB2432450`，则日志输出将包括相关ID `[803B448A0489F84084905D3093480352-3425F23BB2432450]`。

如果希望使用不同格式的相关ID，可以使用`logging.pattern.correlation`属性来定义。例如，以下将为Logback提供相关ID：

```
logging.pattern.correlation=[${spring.application.name:},%X{traceId:-},%X{spanId:-}] 
logging.include-application-name=false
```

注意：在上面的例子中，`logging.include-application-name`被设置为`false`，以避免应用程序名称在日志消息中重复出现（`logging.pattern.correlation`已经包含了它）。此外，还值得一提的是，`logging.pattern.correlation`包含了一个尾随空格，以便默认情况下它与紧跟其后的记录器名称分隔开。

## 传播跟踪

为了自动通过网络传播跟踪信息，请使用自动配置的RestTemplateBuilder或WebClient.Builder来构建客户端。

注意：如果没有使用自动配置的构建器来创建WebClient或RestTemplate，则自动跟踪传播将不起作用！

## 踪器实现

由于Micrometer Tracer 支持多种跟踪器实现，因此Spring Boot可能有多种依赖组合。

所有跟踪器实现都需要`org.springframework.boot:spring-boot-starter-actuator`依赖项。

## OpenTelemetry与Zipkin

使用OpenTelemetry进行跟踪并将报告发送到Zipkin需要以下依赖项：

- io.micrometer:micrometer-tracing-bridge-otel - 将Micrometer
Observation API桥接到OpenTelemetry。
- io.opentelemetry:opentelemetry-exporter-zipkin - 将跟踪报告发送到Zipkin。
使用`management.zipkin.tracing.*`配置属性来配置向Zipkin的报告。

## OpenTelemetry与Wavefront

使用OpenTelemetry进行跟踪并将报告发送到Wavefront需要以下依赖项：

- io.micrometer:micrometer-tracing-bridge-otel - 将Micrometer
Observation API桥接到OpenTelemetry。
- io.micrometer:micrometer-tracing-reporter-wavefront -
将跟踪报告发送到Wavefront。
使用management.wavefront.*配置属性来配置向Wavefront的报告。

## 使用OTLP的OpenTelemetry

使用OpenTelemetry和OTLP（OpenTelemetry Protocol）进行追踪需要以下依赖项：

- io.micrometer:micrometer-tracing-bridge-otel - 将Micrometer Observation API连接到OpenTelemetry。
- io.opentelemetry:opentelemetry-exporter-otlp -
它负责将追踪数据按照OTLP格式报告给能够接收OTLP数据的收集器（collector）。
要使用OTLP进行报告配置，你可以使用`management.otlp.tracing.*`配置属性。

## 使用OpenZipkin Brave与Zipkin进行追踪
使用OpenZipkin Brave进行追踪并将数据发送到Zipkin需要以下依赖项：

io.micrometer:micrometer-tracing-bridge-brave - 这个桥接器将Micrometer Observation API连接到Brave。
io.zipkin.reporter2:zipkin-reporter-brave - 这个库负责将Brave追踪的数据报告给Zipkin。
注意：如果你的项目不使用Spring MVC或Spring WebFlux，那么还需要io.zipkin.reporter2:zipkin-sender-urlconnection依赖项。

使用management.zipkin.tracing.*配置属性来配置向Zipkin的报告。

## 使用OpenZipkin Brave进行追踪并将数据报告给Wavefront

使用OpenZipkin Brave进行追踪并将追踪数据发送到Wavefront需要以下依赖项：

io.micrometer:micrometer-tracing-bridge-brave - 这个桥接器将Micrometer Observation API连接到Brave。
io.micrometer:micrometer-tracing-reporter-wavefront - 这个库负责将追踪数据报告给Wavefront。
要配置报告给Wavefront，你可以使用management.wavefront.*配置属性。

## 与Micrometer Observation的集成

TracingAwareMeterObservationHandler 会自动在 ObservationRegistry 上注册，它为每个完成的观察（observation）创建跨度（span）。

创建自定义跨度（Creating Custom Spans）
你可以通过开始一个观察来创建自己的跨度。为此，你需要将 ObservationRegistry 注入到你的组件中：

```java
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

import org.springframework.stereotype.Component;

@Component
class CustomObservation {

    private final ObservationRegistry observationRegistry;

    CustomObservation(ObservationRegistry observationRegistry) {
        this.observationRegistry = observationRegistry;
    }

    void someOperation() {
        Observation observation = Observation.createNotStarted("some-operation", this.observationRegistry);
        observation.lowCardinalityKeyValue("some-tag", "some-value");
        observation.observe(() -> {
            // Business logic ...
        });
    }

}
```

这将创建一个名为 “some-operation” 的观察，并带有标签 “some-tag=some-value”。

提示：如果你想要创建一个跨度而不创建一个指标，你需要使用 Micrometer 的低级别 Tracer API。

Baggage（行李）
你可以使用 Tracer API 创建 baggage（行李）：

在分布式追踪中，“baggage”指的是跨越多个服务调用传递的键值对。这些键值对可以用于传递关于用户、会话或其它相关上下文的额外信息。Baggage 可以帮助你在整个分布式系统中跟踪和关联相关信息，从而提供更全面的用户行为视图。

```java
import io.micrometer.tracing.BaggageInScope;
import io.micrometer.tracing.Tracer;

import org.springframework.stereotype.Component;

@Component
class CreatingBaggage {

    private final Tracer tracer;

    CreatingBaggage(Tracer tracer) {
        this.tracer = tracer;
    }

    void doSomething() {
        try (BaggageInScope scope = this.tracer.createBaggageInScope("baggage1", "value1")) {
            // Business logic
        }
    }

}
```

这个示例创建了一个名为 baggage1 的 baggage，其值为 value1。如果你正在使用 W3C 传播机制，baggage 会在网络中自动传播。但如果你使用的是 B3 传播机制，baggage 不会自动传播。要手动在网络中传播 baggage，请使用 management.tracing.baggage.remote-fields 配置属性（这对 W3C 同样有效）。对于上面的示例，将此属性设置为 baggage1 将导致一个 HTTP 头信息 baggage1: value1。

如果你想要将 baggage 传播到 MDC（Mapped Diagnostic Context，映射诊断上下文），请使用 management.tracing.baggage.correlation.fields 配置属性。对于上面的示例，将此属性设置为 baggage1 将导致一个名为 baggage1 的 MDC 条目。

测试
当使用 @SpringBootTest 时，报告数据的追踪组件不会自动配置。
