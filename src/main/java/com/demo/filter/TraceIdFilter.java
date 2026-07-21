package com.demo.filter;


import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
public class TraceIdFilter implements Filter {

    private static final String TRACE_ID = "traceId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        // 尝试从请求头中获取 TraceId（支持分布式链路传递，如从网关传入）
        String traceId = null;
        if (request instanceof HttpServletRequest) {
            traceId = ((HttpServletRequest) request).getHeader("X-Trace-Id");
        }

        // 如果请求头中没有，则生成一个新的
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }

        try {
            // 将 traceId 放入 MDC，后续所有日志都会自动带上
            MDC.put(TRACE_ID, traceId);
            chain.doFilter(request, response);
        } finally {
            // 请求结束后清除 MDC，避免线程复用导致串号
            MDC.remove(TRACE_ID);
        }
    }
}