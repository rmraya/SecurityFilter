/*******************************************************************************
 * Copyright (c) 2023 Maxprograms.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-v10.html
 *
 * Contributors:
 *     Maxprograms - initial API and implementation
 *******************************************************************************/
package com.maxprograms.filter;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SecurityFilter implements Filter {

    private static Logger logger = System.getLogger(SecurityFilter.class.getName());

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String uri = req.getRequestURI();


        if (uri.endsWith(".html") || uri.endsWith(".xml") || uri.endsWith(".com") || uri.endsWith("/") ) {
            // html and xml only
            res.addHeader("Content-Security-Policy", "frame-ancestors 'self'; default-src https: data: blob: 'unsafe-inline' 'unsafe-eval';");
            res.addHeader("X-XSS-Protection", "1; mode=block");
        } 
        res.addHeader("X-Content-Type-Options", "nosniff");
        res.addHeader("Cache-Control", "no-cache");
        res.addHeader("Strict-Transport-Security", "max-age=150; includeSubDomains");
        res.addHeader("X-Permitted-Cross-Domain-Policies", "master-only");
        res.addHeader("Referrer-Policy", "no-referrer-when-downgrade");
        res.addHeader("Permissions-Policy", "microphone=(), camera=()");

        if (uri.endsWith(".html")) {
            res.setContentType("text/html");
        }
        if (uri.endsWith(".tgz")) {
            res.setContentType("application/gzip");
        }
        if (uri.endsWith(".zip")) {
            res.setContentType("application/zip");
        }
        if (uri.endsWith(".war")) {
            res.setContentType("application/octet-stream");
        }
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        try {
            chain.doFilter(request, response);
        } catch (IOException e) {
            logger.log(Level.ERROR, e);
        }
    }

    @Override
    public void destroy() {
        // do nothing
    }

    @Override
    public void init(FilterConfig filterConfig) {
        // do nothing
    }
}