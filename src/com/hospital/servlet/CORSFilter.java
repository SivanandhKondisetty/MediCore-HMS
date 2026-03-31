package com.hospital.servlet;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;

/**
 * CORSFilter.java
 * Allows the HTML frontend (file:// or any origin) to call our servlet API.
 * Without this, browsers block cross-origin fetch() calls.
 * Project ID: 25FE5A4305
 */
public class CORSFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletResponse response = (HttpServletResponse) res;
        HttpServletRequest  request  = (HttpServletRequest)  req;

        // Allow all origins (for development — restrict in production)
        response.setHeader("Access-Control-Allow-Origin",  "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        response.setHeader("Access-Control-Max-Age",       "3600");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Handle preflight OPTIONS request
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        chain.doFilter(req, res);
    }

    @Override public void init(FilterConfig fc) {}
    @Override public void destroy() {}
}
