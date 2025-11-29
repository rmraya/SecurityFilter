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
import java.util.Locale;
import java.util.Set;

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
    private static final Set<String> ROOT_FOLDERS = Set.of("articles", "products", "store", "downloads", "support",
            "tutorials");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String uri = req.getRequestURI();
        
        // forward HTTP to HTTPS
        if (req.getScheme().equals("http")) {
            String httpsUrl = "https://" + req.getServerName() + uri;
            res.sendRedirect(httpsUrl);
            return;
        }

        if (uri.endsWith(".html")) {
            String[] segments = uri.split("/");
            String topFolder = null;
            for (String segment : segments) {
                if (segment.isEmpty()) {
                    continue;
                }
                String normalized = segment.toLowerCase(Locale.ROOT);
                if (ROOT_FOLDERS.contains(normalized)) {
                    if (topFolder == null) {
                        topFolder = normalized;
                    } else if (!normalized.equals(topFolder)) {
                        String remoteAddr = req.getRemoteAddr();
                        System.out.println("Rejected " + uri + " requested from " + remoteAddr);
                        res.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                    }
                }
            }
        }
        if (uri.endsWith(".html") || uri.endsWith(".xml") || uri.endsWith(".com") || uri.endsWith("/")) {
            // html and xml only
            res.addHeader("Content-Security-Policy",
                    "frame-ancestors 'self' *.maxprograms.com; default-src https: data: blob: 'unsafe-inline';");
            res.addHeader("X-XSS-Protection", "1; mode=block");
        }
        res.addHeader("X-Content-Type-Options", "nosniff");
        res.addHeader("Cache-Control", "no-cache");
        res.addHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        res.addHeader("X-Permitted-Cross-Domain-Policies", "master-only");
        res.addHeader("Referrer-Policy", "no-referrer-when-downgrade");
        res.addHeader("Permissions-Policy", "microphone=(), camera=()");
        res.addHeader("Access-Control-Allow-Origin", "https://www.maxprograms.com/");
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

        String remoteAddr = req.getRemoteAddr();
        String referer = req.getHeader("Referer");
        if (referer == null) {
            referer = "direct";
        }
        boolean isBot = false;
        int status = 200;
        String ua = req.getHeader("User-Agent");
        if (ua == null) {
            ua = "";
        }
        String browser = "Unknown";
        if (ua.contains("Chrome") && !ua.contains("Chromium")) {
            browser = "Chrome";
        } else if (ua.contains("Firefox")) {
            browser = "Firefox";
        } else if (ua.contains("Safari") && !ua.contains("Chrome")) {
            browser = "Safari";
        } else if (ua.contains("Edge")) {
            browser = "Edge";
        } else if (ua.contains("MSIE") || ua.contains("Trident")) {
            browser = "Internet Explorer";
        }
        String os = "Unknown";
        if (ua.contains("Windows")) {
            os = "Windows";
        } else if (ua.contains("Macintosh")) {
            os = "Mac";
        } else if (ua.contains("Linux")) {
            os = "Linux";
        } else if (ua.contains("Android")) {
            os = "Android";
        } else if (ua.contains("iPhone") || ua.contains("iPad")) {
            os = "iOS";
        }
        if ("Unknown".equals(os) || "Unknown".equals(browser) || ("Safari".equals(browser) && "Linux".equals(os))) {
            isBot = true;
        }

        // Let the chain run first
        CustomResponseWrapper wrappedResp = new CustomResponseWrapper(res);
        chain.doFilter(request, wrappedResp);

        // Intercept container 404 response to send our own body
        if (wrappedResp.getStatus() == HttpServletResponse.SC_NOT_FOUND) {
            status = 404;
            if (!res.isCommitted()) {
                res.resetBuffer();
            }
            if (isBot) {
                res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                res.setContentType("text/html;charset=UTF-8");

                // Minimal body, no Tomcat signature
                res.getWriter().write("<html><body><h1>404 - Not Found</h1></body></html>");
                res.getWriter().flush();
            } else {
                // redirect user to /missing.html instead
                res.sendRedirect("/missing.html");
            }
        }
        if (status == 404 || uri.endsWith(".html") || uri.endsWith(".tgz") || uri.endsWith(".dmg")
                || uri.endsWith(".msi") || uri.endsWith(".zip") || uri.endsWith(".war")) {
            try {
                Tracker.getInstance().track(remoteAddr, referer, uri, browser, os, status);
            } catch (Exception e) {
                logger.log(Level.ERROR, "Error tracking access log for " + uri + " from " + remoteAddr, e);
            }
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