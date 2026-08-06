package com.demo.filter;


import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.skywalking.apm.toolkit.trace.TraceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;

@Component
public class TraceIdFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(TraceIdFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            // 优先读取网关透传的traceId，微服务调用场景
            String headerTraceId = ((HttpServletRequest) request).getHeader("traceId");
            String traceId;

            if(StringUtils.hasText(headerTraceId)){
                traceId = headerTraceId;
            }else {
                // 没有上游传递，则使用SkyWalking生成的链路ID
                traceId = TraceContext.traceId();
            }
            String spanId = String.valueOf(TraceContext.spanId());

            MDC.put("traceId", traceId);
            MDC.put("spanId", spanId);
        } catch (Exception e) {
            log.error("设置链路MDC上下文失败", e);
        }

        try {
            chain.doFilter(request, response);
        }finally {
            // JVM级别兜底清空，防止tomcat线程池MDC残留串号
            MDC.clear();
        }
    }
}
