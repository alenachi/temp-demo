# Conterver

## 我的需求

前提使用Logback的Converter功能实现，讲日志信息中的双引号转换成单引号的功能。要求如下
0 Springboot项目，logback+slg4j
1 logback的parttern是
   {"timestamp":"%d{yyyy-MM-dd HH:mm:ss}","loglevel":"%level","logmessage":"%msg"}%n
2 logback日志格式用Json，所以为了防止msg中双引号带来的问题，希望把message的双引号改为单引号。
3 message中的内容不可预测，包括但不限于纯字符串，json字符串
4 可能使用
  logger.info("[START] logmessage {} {} {}",stringa ,stringb ,jsonstringc);
  logger.info("[START] logmessage " + stringa +";"+ stringb +";"+ jsonstringc);
  logger.info(jsonstringd);
  logger.info(stringmessage); stringmessage中可能有双引号
5 结合场景，给出需要注意的转义符，并说明该如何处理
6 程序不要过于繁杂，在保证功能的基础上，达到生成环境的水准。
7 给出对应的单体测试代码，要求覆盖率100%，需要考虑有参数，无参数，参数为空白的情况，基础上给出测试代码
8 只需提供Converter和单体测试代码即可，不需要配置信息。
9 这是一个可以在生成环境使用的功能

如果你是很有经验的java程序员，你该如何实现这个功能。

## 参考代码1

```java
package com.example.logging;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class SanitizeMessageConverter extends MessageConverter {

    @Override
    public String convert(ILoggingEvent event) {
        String message = event.getFormattedMessage();
        if (message == null || message.isBlank()) {
            return message;
        }
        // 将所有双引号替换为单引号
        return message.replace("\"", "'");
    }
}

```

UT
```java
package com.example.logging;

import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.classic.Level;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class SanitizeMessageConverterTest {

    private final SanitizeMessageConverter converter = new SanitizeMessageConverter();

    @Test
    void testNormalMessageWithDoubleQuotes() {
        LoggingEvent event = new LoggingEvent();
        event.setMessage("User input is \"dangerous\"");
        assertEquals("User input is 'dangerous'", converter.convert(event));
    }

    @Test
    void testJsonStringMessage() {
        String json = "{\"key\": \"value\", \"number\": 123}";
        LoggingEvent event = new LoggingEvent();
        event.setMessage(json);
        assertEquals("{'key': 'value', 'number': 123}", converter.convert(event));
    }

    @Test
    void testConcatenatedMessage() {
        String msg = "Hello \"World\"!";
        LoggingEvent event = new LoggingEvent();
        event.setMessage("[START] " + msg);
        assertEquals("[START] Hello 'World'!", converter.convert(event));
    }

    @Test
    void testEmptyMessage() {
        LoggingEvent event = new LoggingEvent();
        event.setMessage("");
        assertEquals("", converter.convert(event));
    }

    @Test
    void testBlankMessage() {
        LoggingEvent event = new LoggingEvent();
        event.setMessage("   ");
        assertEquals("   ", converter.convert(event));
    }

    @Test
    void testNullMessage() {
        LoggingEvent event = new LoggingEvent();
        event.setMessage(null);
        assertNull(converter.convert(event));
    }

    @Test
    void testMessageWithoutDoubleQuotes() {
        LoggingEvent event = new LoggingEvent();
        event.setMessage("No quotes here");
        assertEquals("No quotes here", converter.convert(event));
    }

    @Test
    void testComplexJsonWithQuotes() {
        String json = "{\"user\": \"john\", \"details\": {\"email\": \"john@example.com\"}}";
        LoggingEvent event = new LoggingEvent();
        event.setMessage(json);
        assertEquals("{'user': 'john', 'details': {'email': 'john@example.com'}}", converter.convert(event));
    }
}

```


## Sample2

```java
package com.example.logging;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class SafeJsonMessageConverter extends ClassicConverter {

    @Override
    public String convert(ILoggingEvent event) {
        String message = event.getFormattedMessage();
        if (message == null) {
            return "";
        }
        // 替换 msg 中的双引号为单引号
        return message.replace("\"", "'");
    }
}

```
UT
```java
package com.example.logging;

import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import org.junit.jupiter.api.Test;
import org.slf4j.Marker;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SafeJsonMessageConverterTest {

    private final SafeJsonMessageConverter converter = new SafeJsonMessageConverter();

    @Test
    void testMessageWithDoubleQuotes() {
        String msg = "This is a \"test\" message with \"quotes\"";
        LoggingEvent event = createLoggingEvent(msg);
        String result = converter.convert(event);
        assertEquals("This is a 'test' message with 'quotes'", result);
    }

    @Test
    void testMessageWithoutDoubleQuotes() {
        String msg = "This is a test message";
        LoggingEvent event = createLoggingEvent(msg);
        String result = converter.convert(event);
        assertEquals("This is a test message", result);
    }

    @Test
    void testNullMessage() {
        LoggingEvent event = createLoggingEvent(null);
        String result = converter.convert(event);
        assertEquals("", result);
    }

    @Test
    void testEmptyMessage() {
        LoggingEvent event = createLoggingEvent("");
        String result = converter.convert(event);
        assertEquals("", result);
    }

    @Test
    void testJsonStringMessage() {
        String msg = "{\"key\": \"value\", \"array\": [\"one\", \"two\"]}";
        LoggingEvent event = createLoggingEvent(msg);
        String result = converter.convert(event);
        assertEquals("{'key': 'value', 'array': ['one', 'two']}", result);
    }

    private LoggingEvent createLoggingEvent(String message) {
        return new LoggingEvent(
                "com.example.MyClass",
                new ch.qos.logback.classic.Logger(new LoggerContext(), "com.example.MyClass"),
                Level.INFO,
                message,
                null,
                null
        );
    }
}
```

## Sample 3
```java
package com.example.logging;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class QuoteEscapeConverter extends MessageConverter {

    @Override
    public String convert(ILoggingEvent event) {
        String originalMessage = event.getFormattedMessage();
        if (originalMessage == null) {
            return null;
        }

        // 先处理转义的 \"，然后处理普通的 "
        return originalMessage
                .replace("\\\"", "'")  // 替换转义双引号为单引号
                .replace("\"", "'");   // 替换普通双引号为单引号
    }
}
```
UT
```java
package com.example.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.ContextBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QuoteEscapeConverterTest {

    private QuoteEscapeConverter converter;

    @BeforeEach
    void setUp() {
        converter = new QuoteEscapeConverter();
        converter.setContext(new ContextBase());
    }

    @Test
    void testNullMessage() {
        LoggingEvent event = new LoggingEvent();
        event.setMessage(null);
        assertNull(converter.convert(event));
    }

    @Test
    void testEmptyMessage() {
        LoggingEvent event = new LoggingEvent();
        event.setMessage("");
        event.setLevel(Level.INFO);
        assertEquals("", converter.convert(event));
    }

    @Test
    void testMessageWithoutQuotes() {
        LoggingEvent event = new LoggingEvent();
        event.setMessage("No quotes here.");
        event.setLevel(Level.INFO);
        assertEquals("No quotes here.", converter.convert(event));
    }

    @Test
    void testMessageWithDoubleQuotes() {
        LoggingEvent event = new LoggingEvent();
        event.setMessage("He said \"Hello\"");
        event.setLevel(Level.INFO);
        assertEquals("He said 'Hello'", converter.convert(event));
    }

    @Test
    void testMessageWithEscapedQuotes() {
        LoggingEvent event = new LoggingEvent();
        event.setMessage("Path: \\\"C:\\\\Program Files\\\" End");
        event.setLevel(Level.INFO);
        assertEquals("Path: 'C:\\\\Program Files' End", converter.convert(event));
    }

    @Test
    void testComplexJsonMessage() {
        String json = "{\"user\":\"john\",\"roles\":[\"admin\",\"dev\"]}";
        LoggingEvent event = new LoggingEvent();
        event.setMessage(json);
        event.setLevel(Level.INFO);
        String expected = "{'user':'john','roles':['admin','dev']}";
        assertEquals(expected, converter.convert(event));
    }

    @Test
    void testMixedQuotes() {
        String message = "Plain \"text\" and escaped \\\"text\\\".";
        LoggingEvent event = new LoggingEvent();
        event.setMessage(message);
        event.setLevel(Level.INFO);
        assertEquals("Plain 'text' and escaped 'text'.", converter.convert(event));
    }

    @Test
    void testMessageWithOnlyEscapedQuotes() {
        LoggingEvent event = new LoggingEvent();
        event.setMessage("\\\"Hello\\\" \\\"World\\\"");
        event.setLevel(Level.INFO);
        assertEquals("'Hello' 'World'", converter.convert(event));
    }
}
```

## Sample 4

```java
package com.example.logging;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * 自定义日志转换器，将日志 message 中的双引号转换为单引号。
 * 支持处理普通双引号（"）和转义双引号（\"），避免 JSON 格式嵌套引号冲突。
 */
public class QuoteEscapeConverter extends MessageConverter {

    @Override
    public String convert(ILoggingEvent event) {
        if (event == null) {
            return null;
        }

        String originalMessage = event.getFormattedMessage();
        if (originalMessage == null || originalMessage.isEmpty()) {
            return originalMessage;
        }

        // 若消息中没有双引号，直接返回原始值（性能优化）
        if (!originalMessage.contains("\"")) {
            return originalMessage;
        }

        // 替换顺序：先处理 \"（转义双引号）再处理普通双引号
        String replaced = originalMessage
                .replace("\\\"", "'") // 转义双引号
                .replace("\"", "'");  // 普通双引号

        return replaced;
    }
}
```

UT
```java
package com.example.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.ContextBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QuoteEscapeConverterTest {

    private QuoteEscapeConverter converter;

    @BeforeEach
    void setUp() {
        converter = new QuoteEscapeConverter();
        converter.setContext(new ContextBase());
    }

    @Test
    void testNullEventMessage() {
        LoggingEvent event = new LoggingEvent();
        event.setMessage(null);
        assertNull(converter.convert(event));
    }

    @Test
    void testEmptyMessage() {
        assertEquals("", convertMsg(""));
    }

    @Test
    void testNoQuotes() {
        assertEquals("plain text", convertMsg("plain text"));
    }

    @Test
    void testOnlyDoubleQuotes() {
        assertEquals("''", convertMsg("\"\""));
    }

    @Test
    void testEscapedQuotes() {
        assertEquals("path: 'C:\\Program Files'", convertMsg("path: \\\"C:\\Program Files\\\""));
    }

    @Test
    void testMixedQuotes() {
        String input = "Mixed \"json\" and escaped \\\"path\\\"";
        String expected = "Mixed 'json' and escaped 'path'";
        assertEquals(expected, convertMsg(input));
    }

    @Test
    void testJsonLikeMessage() {
        String input = "{\"key\":\"value\",\"arr\":[\"a\",\"b\"]}";
        String expected = "{'key':'value','arr':['a','b']}";
        assertEquals(expected, convertMsg(input));
    }

    @Test
    void testDeeplyNestedJsonString() {
        String input = "{\"outer\":{\"inner\":{\"name\":\"\\\"John\\\"\"}}}";
        String expected = "{'outer':{'inner':{'name':''John''}}}";
        assertEquals(expected, convertMsg(input));
    }

    @Test
    void testBlankSpaceOnly() {
        assertEquals("   ", convertMsg("   "));
    }

    private String convertMsg(String msg) {
        LoggingEvent event = new LoggingEvent();
        event.setLevel(Level.INFO);
        event.setMessage(msg);
        return converter.convert(event);
    }
}
```







