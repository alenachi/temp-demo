
```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MockHttpServletRequestWrapperTest {

    @Mock
    private HttpServletRequest mockRequest;
    @Mock
    private ServletInputStream mockServletInputStream;

    @Test
    void constructor_shouldCacheRequestBody() throws IOException {
        // Arrange
        byte[] testData = "test data".getBytes();
        when(mockRequest.getInputStream())
            .thenReturn(new ByteArrayServletInputStream(testData));
        when(mockRequest.getParameterMap()).thenReturn(null);

        // Act
        MockHttpServletRequestWrapper wrapper = new MockHttpServletRequestWrapper(mockRequest);

        // Assert
        verify(mockRequest).getParameterMap();
        verify(mockRequest).getInputStream();
        
        // Verify cached content
        ServletInputStream stream = wrapper.getInputStream();
        byte[] readBytes = new byte[testData.length];
        stream.read(readBytes);
        assertArrayEquals(testData, readBytes);
    }

    @Test
    void getInputStream_shouldReturnValidStream() throws IOException {
        // Arrange
        byte[] testData = "stream data".getBytes();
        when(mockRequest.getInputStream())
            .thenReturn(new ByteArrayServletInputStream(testData));
        when(mockRequest.getParameterMap()).thenReturn(null);
        MockHttpServletRequestWrapper wrapper = new MockHttpServletRequestWrapper(mockRequest);

        // Act
        ServletInputStream stream = wrapper.getInputStream();

        // Assert
        assertNotNull(stream);
        assertEquals('s', stream.read()); // Test first byte
        assertFalse(stream.isFinished());
    }

    @Test
    void getReader_shouldReturnBufferedReader() throws IOException {
        // Arrange
        String testContent = "reader content";
        when(mockRequest.getInputStream())
            .thenReturn(new ByteArrayServletInputStream(testContent.getBytes()));
        when(mockRequest.getParameterMap()).thenReturn(null);
        MockHttpServletRequestWrapper wrapper = new MockHttpServletRequestWrapper(mockRequest);

        // Act
        BufferedReader reader = wrapper.getReader();

        // Assert
        assertNotNull(reader);
        assertEquals(testContent, reader.readLine());
    }

    @Test
    void getInputStream_isFinished_shouldDetectEOF() throws IOException {
        // Arrange
        byte[] testData = "x".getBytes();
        when(mockRequest.getInputStream())
            .thenReturn(new ByteArrayServletInputStream(testData));
        when(mockRequest.getParameterMap()).thenReturn(null);
        MockHttpServletRequestWrapper wrapper = new MockHttpServletRequestWrapper(mockRequest);
        ServletInputStream stream = wrapper.getInputStream();

        // Act & Assert
        assertFalse(stream.isFinished());
        stream.read();
        assertTrue(stream.isFinished());
    }

    @Test
    void getInputStream_setReadListener_shouldThrowException() throws IOException {
        // Arrange
        when(mockRequest.getInputStream())
            .thenReturn(new ByteArrayServletInputStream(new byte[0]));
        when(mockRequest.getParameterMap()).thenReturn(null);
        MockHttpServletRequestWrapper wrapper = new MockHttpServletRequestWrapper(mockRequest);
        ReadListener listener = mock(ReadListener.class);

        // Act & Assert
        ServletInputStream stream = wrapper.getInputStream();
        assertThrows(UnsupportedOperationException.class, 
            () -> stream.setReadListener(listener));
    }

    // 轻量级替代实现
    private static class ByteArrayServletInputStream extends ServletInputStream {
        private final ByteArrayInputStream delegate;

        public ByteArrayServletInputStream(byte[] data) {
            this.delegate = new ByteArrayInputStream(data);
        }

        @Override
        public int read() throws IOException {
            return delegate.read();
        }

        @Override
        public boolean isFinished() {
            return delegate.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            throw new UnsupportedOperationException();
        }
    }
}
```