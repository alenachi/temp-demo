
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
            .thenReturn(createMockServletInputStream(testData));
        when(mockRequest.getParameterMap()).thenReturn(null);

        // Act
        MockHttpServletRequestWrapper wrapper = new MockHttpServletRequestWrapper(mockRequest);

        // Assert
        verify(mockRequest).getParameterMap();
        verify(mockRequest).getInputStream();
        
        // Verify cached content
        ServletInputStream stream = wrapper.getInputStream();
        byte[] buffer = new byte[testData.length];
        stream.read(buffer);
        assertArrayEquals(testData, buffer);
    }

    @Test
    void getInputStream_shouldReturnWorkingStream() throws IOException {
        // Arrange
        byte[] testData = "input stream".getBytes();
        when(mockRequest.getInputStream())
            .thenReturn(createMockServletInputStream(testData));
        when(mockRequest.getParameterMap()).thenReturn(null);
        MockHttpServletRequestWrapper wrapper = new MockHttpServletRequestWrapper(mockRequest);

        // Act
        ServletInputStream stream = wrapper.getInputStream();

        // Assert
        assertNotNull(stream);
        assertEquals('i', stream.read()); // Test first byte
        assertFalse(stream.isFinished());
    }

    @Test
    void getReader_shouldReturnBufferedReader() throws IOException {
        // Arrange
        String testContent = "reader content";
        when(mockRequest.getInputStream())
            .thenReturn(createMockServletInputStream(testContent.getBytes()));
        when(mockRequest.getParameterMap()).thenReturn(null);
        MockHttpServletRequestWrapper wrapper = new MockHttpServletRequestWrapper(mockRequest);

        // Act
        BufferedReader reader = wrapper.getReader();

        // Assert
        assertNotNull(reader);
        assertEquals(testContent, reader.readLine());
    }

    @Test
    void getInputStream_isFinished_shouldReturnCorrectState() throws IOException {
        // Arrange
        byte[] testData = "tiny".getBytes();
        when(mockRequest.getInputStream())
            .thenReturn(createMockServletInputStream(testData));
        when(mockRequest.getParameterMap()).thenReturn(null);
        MockHttpServletRequestWrapper wrapper = new MockHttpServletRequestWrapper(mockRequest);
        ServletInputStream stream = wrapper.getInputStream();

        // Act & Assert
        assertFalse(stream.isFinished());
        for (int i = 0; i < testData.length; i++) {
            stream.read();
        }
        assertTrue(stream.isFinished());
    }

    @Test
    void getInputStream_setReadListener_shouldThrowException() throws IOException {
        // Arrange
        when(mockRequest.getInputStream())
            .thenReturn(createMockServletInputStream(new byte[0]));
        when(mockRequest.getParameterMap()).thenReturn(null);
        MockHttpServletRequestWrapper wrapper = new MockHttpServletRequestWrapper(mockRequest);

        // Act & Assert
        ServletInputStream stream = wrapper.getInputStream();
        assertThrows(UnsupportedOperationException.class,
            () -> stream.setReadListener(mock(ReadListener.class)));
    }

    // 纯Mockito方式创建模拟ServletInputStream
    private ServletInputStream createMockServletInputStream(byte[] data) throws IOException {
        ServletInputStream mockStream = mock(ServletInputStream.class);
        final ByteArrayInputStream dataStream = new ByteArrayInputStream(data);
        
        // 模拟read()行为
        when(mockStream.read()).thenAnswer(inv -> dataStream.read());
        when(mockStream.read(any(byte[].class))).thenAnswer(inv -> {
            byte[] buffer = inv.getArgument(0);
            return dataStream.read(buffer);
        });
        when(mockStream.read(any(byte[].class), anyInt(), anyInt()).thenAnswer(inv -> {
            byte[] buffer = inv.getArgument(0);
            int off = inv.getArgument(1);
            int len = inv.getArgument(2);
            return dataStream.read(buffer, off, len);
        });
        
        // 模拟isFinished
        when(mockStream.isFinished()).thenAnswer(inv -> dataStream.available() == 0);
        
        // 模拟isReady
        when(mockStream.isReady()).thenReturn(true);
        
        // 模拟setReadListener
        doThrow(new UnsupportedOperationException())
            .when(mockStream).setReadListener(any(ReadListener.class));
        
        return mockStream;
    }
}
```