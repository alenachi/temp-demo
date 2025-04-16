
## ParamAwareQuoteConverter

```java
package com.yourcompany.logging.converter;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 增强版引号转换器，处理参数化日志中的JSON内容
 * 
 * 特性：
 * 1. 处理原始消息和参数
 * 2. 保留日志格式结构
 * 3. 只转换参数中的双引号
 * 4. 正确处理转义字符
 */
public class ParamAwareQuoteConverter extends MessageConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParamAwareQuoteConverter.class);
    
    private static final char DOUBLE_QUOTE = '"';
    private static final char SINGLE_QUOTE = '\'';
    private static final char ESCAPE_CHAR = '\\';
    private static final char PLACEHOLDER_START = '{';
    private static final char PLACEHOLDER_END = '}';
    
    private volatile boolean enabled = true;

    @Override
    public String convert(ILoggingEvent event) {
        if (!enabled) {
            return event.getFormattedMessage();
        }
        
        String messagePattern = event.getMessage(); // 获取原始消息模板
        Object[] args = event.getArgumentArray();
        
        if (args == null || args.length == 0) {
            // 无参数时直接转换整个消息
            return convertQuotes(event.getFormattedMessage());
        }
        
        // 处理参数化日志
        return processParameterizedMessage(messagePattern, args);
    }
    
    /**
     * 处理参数化日志消息
     */
    private String processParameterizedMessage(String pattern, Object[] args) {
        StringBuilder result = new StringBuilder();
        int argIndex = 0;
        int i = 0;
        
        while (i < pattern.length()) {
            char c = pattern.charAt(i);
            
            if (c == PLACEHOLDER_START && i + 1 < pattern.length() 
                && pattern.charAt(i + 1) == PLACEHOLDER_END) {
                // 找到占位符 {}
                if (argIndex < args.length) {
                    // 转换参数值
                    String convertedArg = convertQuotes(String.valueOf(args[argIndex]));
                    result.append(convertedArg);
                    argIndex++;
                }
                i += 2; // 跳过{}
            } else {
                // 非占位符部分直接添加
                result.append(c);
                i++;
            }
        }
        
        // 处理多余的参数（虽然不常见，但防御性编程）
        while (argIndex < args.length) {
            result.append(' ').append(convertQuotes(String.valueOf(args[argIndex])));
            argIndex++;
        }
        
        return result.toString();
    }
    
    /**
     * 转换字符串中的双引号为单引号（处理转义字符）
     */
    private String convertQuotes(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        StringBuilder sb = new StringBuilder(input.length());
        boolean escaped = false;
        
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            
            if (!escaped && c == ESCAPE_CHAR) {
                escaped = true;
                sb.append(c);
                continue;
            }
            
            if (!escaped && c == DOUBLE_QUOTE) {
                sb.append(SINGLE_QUOTE);
            } else {
                sb.append(c);
            }
            
            escaped = false;
        }
        
        return sb.toString();
    }
    
    // ========== 配置方法 ==========
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
}
```


## BalancedQuoteConverter

```java

package com.yourcompany.logging.converter;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * 平衡版引号转换器
 * 特性：
 * 1. 处理转义字符（比极简版更安全）
 * 2. 统一转换所有双引号（比完整版更简单）
 * 3. 仍然线程安全
 * 4. 保留基本配置能力
 */
public class BalancedQuoteConverter extends MessageConverter {
    private boolean enabled = true;

    @Override
    public String convert(ILoggingEvent event) {
        if (!enabled) {
            return event.getFormattedMessage();
        }
        
        String message = event.getFormattedMessage();
        if (message == null) {
            return null;
        }
        
        return convertQuotes(message);
    }

    private String convertQuotes(String input) {
        StringBuilder sb = new StringBuilder(input.length());
        boolean escaped = false;
        
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            
            if (!escaped && c == '\\') {
                escaped = true;
                sb.append(c);
                continue;
            }
            
            if (!escaped && c == '"') {
                sb.append('\'');
            } else {
                sb.append(c);
            }
            
            escaped = false;
        }
        
        return sb.toString();
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
```

UT
```java
package com.yourcompany.logging.converter;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BalancedQuoteConverterTest {
    private BalancedQuoteConverter converter;
    private TestLoggingEvent event;

    @BeforeEach
    void setUp() {
        converter = new BalancedQuoteConverter();
        event = new TestLoggingEvent();
    }

    // 基础功能测试
    @Test
    void convert_plainText_unchanged() {
        event.setMessage("This is plain text");
        assertEquals("This is plain text", converter.convert(event));
    }

    @Test
    void convert_doubleQuotes_converted() {
        event.setMessage("Text with \"quotes\"");
        assertEquals("Text with 'quotes'", converter.convert(event));
    }

    // 转义字符处理测试
    @Test
    void convert_escapedQuotes_unchanged() {
        event.setMessage("Escaped \\\"quote\\\"");
        assertEquals("Escaped \\\"quote\\\"", converter.convert(event));
    }

    @Test
    void convert_mixedEscapedAndNormal() {
        event.setMessage("Mix \\\"escaped\" and 'normal'");
        assertEquals("Mix \\\"escaped' and 'normal'", converter.convert(event));
    }

    // 边界条件测试
    @Test
    void convert_emptyString() {
        event.setMessage("");
        assertEquals("", converter.convert(event));
    }

    @Test
    void convert_nullMessage() {
        event.setMessage(null);
        assertNull(converter.convert(event));
    }

    @Test
    void convert_onlyDoubleQuote() {
        event.setMessage("\"");
        assertEquals("'", converter.convert(event));
    }

    // 特殊字符测试
    @Test
    void convert_withNewlines() {
        event.setMessage("Line1\"\nLine2\"");
        assertEquals("Line1'\nLine2'", converter.convert(event));
    }

    @Test
    void convert_withTabs() {
        event.setMessage("\t\"text\"\t");
        assertEquals("\t'text'\t", converter.convert(event));
    }

    // 启用/禁用测试
    @Test
    void whenDisabled_returnsOriginalMessage() {
        converter.setEnabled(false);
        event.setMessage("\"quoted\"");
        assertEquals("\"quoted\"", converter.convert(event));
        
        // 重置状态
        converter.setEnabled(true);
    }

    // 模拟ILoggingEvent的实现
    private static class TestLoggingEvent implements ILoggingEvent {
        private String message;

        public void setMessage(String message) {
            this.message = message;
        }

        @Override
        public String getFormattedMessage() {
            return message;
        }

        // 以下方法需要实现但返回默认值
        @Override public String getThreadName() { return null; }
        @Override public ch.qos.logback.classic.Level getLevel() { return null; }
        @Override public String getLoggerName() { return null; }
        @Override public String getMessage() { return null; }
        @Override public Object[] getArgumentArray() { return new Object[0]; }
        @Override public long getTimeStamp() { return 0; }
        @Override public Throwable getThrowableProxy() { return null; }
        @Override public StackTraceElement[] getCallerData() { return new StackTraceElement[0]; }
        @Override public boolean hasCallerData() { return false; }
        @Override public void prepareForDeferredProcessing() {}
    }
}
```
