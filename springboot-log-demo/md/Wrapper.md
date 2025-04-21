
```java
public class MockHttpServletRequestWrapper extends HttpServletRequestWrapper {
    private final byte[] cachedBody;
    
    public BodyReaderHttpServletRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        cachedBody = StreamUtils.copyToByteArray(request.getInputStream());
    }

    @Override
    public ServletInputStream getInputStream() throwsOException {

        final ByteArrayInputStream in = new ByteArrayInputStream(cachedBody);

        return new ServletInputStream() {
            
            @Override
            public boolean isFinished() {
                try {
                    return bais.available() == 0;
                } catch (Exception e) {
                    // log here
                }
                return false;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public int read() throws IOException {
                return in.read();
            }

            @Override
            public void setReadListener(ReadListener readListener) {
              throw new UnsupportedOperationException();ß
            }
        };
    }

  
  @Override
  public BufferedReader getReader() throws IOException {
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(this.cachedBody);
    return new BufferedReader(new InputStreamReader(gbyteArrayInputStream));
  }
}

```