
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
import java.io.InputStreamReader;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MockHttpServletRequestWrapperTest {

    @Mock
    private HttpServletRequest mockRequest;

    @Test
    void constructor_shouldCacheRequestBody() throws IOException {
        // Arrange
        byte[] testData = "test payload".getBytes();
        when(mockRequest.getInputStream()).thenReturn(new MockServletInputStream(new ByteArrayInputStream(testData)));
        when(mockRequest.getParameterMap()).thenReturn(null);

        // Act
        MockHttpServletRequestWrapper wrapper = new MockHttpServletRequestWrapper(mockRequest);

        // Assert
        verify(mockRequest).getParameterMap();
        verify(mockRequest).getInputStream();
        assertArrayEquals(testData, wrapper.getInputStream().readAllBytes());
    }

    @Test
    void getInputStream_shouldReturnWorkingStream() throws IOException {
        // Arrange
        byte[] testData = "input stream data".getBytes();
        when(mockRequest.getInputStream()).thenReturn(new MockServletInputStream(new ByteArrayInputStream(testData)));
        when(mockRequest.getParameterMap()).thenReturn(null);
        MockHttpServletRequestWrapper wrapper = new MockHttpServletRequestWrapper(mockRequest);

        // Act
        ServletInputStream stream = wrapper.getInputStream();

        // Assert
        assertNotNull(stream);
        assertEquals('i', stream.read()); // Test first byte
        assertFalse(stream.isFinished()); // Should have more data
    }

    @Test
    void getInputStream_isFinished_shouldReturnCorrectState() throws IOException {
        // Arrange
        byte[] testData = "tiny".getBytes();
        when(mockRequest.getInputStream()).thenReturn(new MockServletInputStream(new ByteArrayInputStream(testData)));
        when(mockRequest.getParameterMap()).thenReturn(null);
        MockHttpServletRequestWrapper wrapper = new MockHttpServletRequestWrapper(mockRequest);
        ServletInputStream stream = wrapper.getInputStream();

        // Act & Assert
        assertFalse(stream.isFinished());
        stream.read();
        stream.read();
        stream.read();
        stream.read(); // Read all 4 bytes
        assertTrue(stream.isFinished());
    }

    @Test
    void getInputStream_isReady_shouldAlwaysReturnTrue() throws IOException {
        // Arrange
        when(mockRequest.getInputStream()).thenReturn(new MockServletInputStream(new ByteArrayInputStream("".getBytes())));
        when(mockRequest.getParameterMap()).thenReturn(null);
        MockHttpServletRequestWrapper wrapper = new MockHttpServletRequestWrapper(mockRequest);

        // Act & Assert
        assertTrue(wrapper.getInputStream().isReady());
    }

    @Test
    void getInputStream_setReadListener_shouldThrowUnsupportedOperationException() throws IOException {
        // Arrange
        when(mockRequest.getInputStream()).thenReturn(new MockServletInputStream(new ByteArrayInputStream("".getBytes())));
        when(mockRequest.getParameterMap()).thenReturn(null);
        MockHttpServletRequestWrapper wrapper = new MockHttpServletRequestWrapper(mockRequest);
        ReadListener listener = mock(ReadListener.class);

        // Act & Assert
        assertThrows(UnsupportedOperationException.class, 
            () -> wrapper.getInputStream().setReadListener(listener));
    }

    @Test
    void getReader_shouldReturnBufferedReaderWithCachedContent() throws IOException {
        // Arrange
        String testContent = "reader content\nsecond line";
        byte[] testData = testContent.getBytes();
        when(mockRequest.getInputStream()).thenReturn(new MockServletInputStream(new ByteArrayInputStream(testData)));
        when(mockRequest.getParameterMap()).thenReturn(null);
        MockHttpServletRequestWrapper wrapper = new MockHttpServletRequestWrapper(mockRequest);

        // Act
        BufferedReader reader = wrapper.getReader();

        // Assert
        assertNotNull(reader);
        assertEquals("reader content", reader.readLine());
        assertEquals("second line", reader.readLine());
        assertNull(reader.readLine()); // EOF
    }

    @Test
    void getInputStream_read_shouldReturnByteByByte() throws IOException {
        // Arrange
        byte[] testData = "abc".getBytes();
        when(mockRequest.getInputStream()).thenReturn(new MockServletInputStream(new ByteArrayInputStream(testData)));
        when(mockRequest.getParameterMap()).thenReturn(null);
        MockHttpServletRequestWrapper wrapper = new MockHttpServletRequestWrapper(mockRequest);
        ServletInputStream stream = wrapper.getInputStream();

        // Act & Assert
        assertEquals('a', stream.read());
        assertEquals('b', stream.read());
        assertEquals('c', stream.read());
        assertEquals(-1, stream.read()); // EOF
    }

    // Helper class for mocking ServletInputStream
    private static class MockServletInputStream extends ServletInputStream {
        private final ByteArrayInputStream source;

        public MockServletInputStream(ByteArrayInputStream source) {
            this.source = source;
        }

        @Override
        public int read() throws IOException {
            return source.read();
        }

        @Override
        public boolean isFinished() {
            return source.available() == 0;
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