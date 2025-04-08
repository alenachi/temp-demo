# a sample


```java
import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.MDC;


public class ServerLoggingFilter implements Filter {

    @SuppressWarnings("unused")
    private FilterConfig config = null;

    /**
     * Initialize the config
     * 
     * @param config
     */
    public void init(FilterConfig config) throws ServletException {
        this.config = config;
    }

    /**
     * Destroy the config
     */
    public void destroy() {
        config = null;
    }

    /**
     * The filter to add the output contents of the log
     * ( Log output settings file : WEB-INF/classes/logback.xml )
     * 
     * @param request
     * @param response
     * @param chain
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
        ServletException {
        
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        HttpSession session = req.getSession();
        String customerId = MyCookieUtil.getCookieValue(req, CookieUtil.COOKIE_NAME);
        String sessionId;

        if (session != null) {
            sessionId = session.getId();
            MDC.put("sessionId", "["+sessionId+"]");
            res.setHeader("session_id", sessionId);
        }

        if (customerId != null) {
            MDC.put("customerId", "["+customerId+"]");
        } else {
            MDC.put("customerId", "");
        }

        try {
            chain.doFilter(request, res);
        } finally {
            MDC.remove("customerId");
            MDC.remove("sessionId");
        }
    }
}

```