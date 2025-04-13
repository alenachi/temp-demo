```java
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.util.StreamUtils;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BodyReaderHttpServletRequestWrapperTest {

    @Test
    void testGetInputStreamAndReader() throws IOException {
        String requestBody = "test body";
        byte[] bodyBytes = requestBody.getBytes();

        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getInputStream()).thenReturn(new ServletInputStream() {
            private final ByteArrayInputStream bais = new ByteArrayInputStream(bodyBytes);

            @Override
            public boolean isFinished() {
                return bais.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                // No-op for mock
            }

            @Override
            public int read() {
                return bais.read();
            }
        });
        when(mockRequest.getParameterMap()).thenReturn(java.util.Collections.emptyMap());

        BodyReaderHttpServletRequestWrapper wrapper = new BodyReaderHttpServletRequestWrapper(mockRequest);

        // 测试 getInputStream
        ServletInputStream inputStream = wrapper.getInputStream();
        String result = StreamUtils.copyToString(inputStream, java.nio.charset.StandardCharsets.UTF_8);
        assertEquals(requestBody, result);

        // 测试 getReader
        BufferedReader reader = wrapper.getReader();
        String line = reader.readLine();
        assertEquals(requestBody, line);
    }

    @Test
    void testUnsupportedOperationInSetReadListener() throws IOException {
        String requestBody = "abc";
        byte[] bodyBytes = requestBody.getBytes();

        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getInputStream()).thenReturn(new ServletInputStream() {
            private final ByteArrayInputStream bais = new ByteArrayInputStream(bodyBytes);

            @Override
            public boolean isFinished() {
                return bais.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                throw new UnsupportedOperationException("setReadListener is not supported");
            }

            @Override
            public int read() {
                return bais.read();
            }
        });
        when(mockRequest.getParameterMap()).thenReturn(java.util.Collections.emptyMap());

        BodyReaderHttpServletRequestWrapper wrapper = new BodyReaderHttpServletRequestWrapper(mockRequest);
        ServletInputStream stream = wrapper.getInputStream();

        assertThrows(UnsupportedOperationException.class, () -> {
            stream.setReadListener(null);
        });
    }
}

```