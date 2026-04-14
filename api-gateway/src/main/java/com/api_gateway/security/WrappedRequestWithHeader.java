package com.api_gateway.security;

import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

public class WrappedRequestWithHeader extends HttpServletRequestWrapper {

    private final Map<String, String> customHeaders = new LinkedHashMap<>();

    public WrappedRequestWithHeader(HttpServletRequest request, String name, String value) {
        super(request);
        customHeaders.put(name, value);
    }

    @Override
    public String getHeader(String name) {
        String headerValue = customHeaders.get(name);
        if (headerValue != null) {
            return headerValue;
        }
        return super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        String headerValue = customHeaders.get(name);
        if (headerValue != null) {
            return Collections.enumeration(Collections.singleton(headerValue));
        }
        return super.getHeaders(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        Map<String, String> combined = new LinkedHashMap<>();
        Collections.list(super.getHeaderNames()).forEach(name -> combined.put(name, super.getHeader(name)));
        customHeaders.forEach(combined::put);
        return Collections.enumeration(combined.keySet());
    }
}
