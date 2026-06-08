package org.waterwood.waterfunservice.infrastructure.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.waterwood.utils.StringUtil;
import org.waterwood.waterfunservice.service.IpBanService;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class IpBanFilter extends OncePerRequestFilter {
    private final IpBanService ipBanService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String ip = request.getHeader("X-Real-Client-Ip");
        if(StringUtil.isBlank(ip)) {
            ip = getClientIp(request);
        }

        if (ipBanService.isBanned(ip)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            String accept = request.getHeader("Accept");
            boolean wantsJson = accept != null && accept.contains("application/json");

            if (wantsJson) {
                response.setContentType("application/json; charset=UTF-8");
                response.getWriter().write("{\"code\":403,\"message\":\"IP banned\"}");
            } else {
                response.setContentType("text/html; charset=UTF-8");
                response.getWriter().write(html.formatted(ip));
            }
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (hasValidIp(ip)) {
            return ip.split(",")[0].trim();
        }

        ip = request.getHeader("X-Real-IP");
        if (hasValidIp(ip)) {
            return ip.trim();
        }

        return request.getRemoteAddr();
    }

    private boolean hasValidIp(String ip) {
        return ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip);
    }

    private final String html = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <title>Access Denied</title>
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        min-height: 100vh;
                        background: #f5f5f5;
                    }
                    .box {
                        text-align: center;
                        padding: 48px 40px;
                        background: white;
                        border-radius: 16px;
                        box-shadow: 0 4px 24px rgba(0,0,0,0.08);
                        max-width: 400px;
                        margin: 20px;
                    }
                    .icon { font-size: 48px; margin-bottom: 16px; }
                    h1 { color: #e74c3c; font-size: 22px; margin-bottom: 12px; }
                    p { color: #666; line-height: 1.6; margin-bottom: 8px; }
                    .meta {
                        margin-top: 24px;
                        padding-top: 16px;
                        border-top: 1px solid #eee;
                        color: #999;
                        font-size: 12px;
                        font-family: monospace;
                    }
                    .lang-switch {
                        position: absolute;
                        top: 20px;
                        right: 20px;
                        display: flex;
                        gap: 8px;
                    }
                    .lang-switch button {
                        padding: 6px 14px;
                        border: 1px solid #ddd;
                        border-radius: 6px;
                        background: white;
                        color: #666;
                        font-size: 13px;
                        cursor: pointer;
                        transition: all 0.2s;
                    }
                    .lang-switch button:hover {
                        border-color: #e74c3c;
                        color: #e74c3c;
                    }
                    .lang-switch button.active {
                        background: #e74c3c;
                        color: white;
                        border-color: #e74c3c;
                    }
                    .ip-info {
                        margin-top: 12px;
                        padding: 8px 12px;
                        background: #fafafa;
                        border-radius: 8px;
                        color: #888;
                        font-size: 13px;
                        font-family: monospace;
                    }
                    .hidden { display: none; }
                </style>
            <base target="_blank">
            </head>
            <body>
                <div class="lang-switch">
                    <button id="btn-en" class="active" onclick="switchLang('en')">English</button>
                    <button id="btn-zh" onclick="switchLang('zh')">中文</button>
                </div>
            
                <div class="box">
                    <div class="icon">🚫</div>
            
                    <!-- English Version -->
                    <div id="content-en">
                        <h1>Access Denied</h1>
                        <p>Your IP has been banned from accessing this service.</p>
                        <p>Please contact the administrator if you believe this is an error.</p>
                    </div>
            
                    <!-- Chinese Version -->
                    <div id="content-zh" class="hidden">
                        <h1>访问被拒绝</h1>
                        <p>您的 IP 已被禁止访问此服务。</p>
                        <p>如果您认为这是一个错误，请联系管理员。</p>
                    </div>
            
                    <div class="ip-info" id="ip-display">IP: %s</div>
                    <div class="meta">
                        <div>HTTP 403 | <span id="time-display"></span></div>
                    </div>
                </div>
            
                <script>
                    // Display current time
                    function updateTime() {
                        const now = new Date();
                        const timeStr = now.toLocaleString('zh-CN', {
                            year: 'numeric',
                            month: '2-digit',
                            day: '2-digit',
                            hour: '2-digit',
                            minute: '2-digit',
                            second: '2-digit',
                            hour12: false
                        });
                        document.getElementById('time-display').textContent = timeStr;
                    }
                    updateTime();
                    setInterval(updateTime, 1000);
            
                    // Language switch
                    function switchLang(lang) {
                        const enContent = document.getElementById('content-en');
                        const zhContent = document.getElementById('content-zh');
                        const btnEn = document.getElementById('btn-en');
                        const btnZh = document.getElementById('btn-zh');
            
                        if (lang === 'en') {
                            enContent.classList.remove('hidden');
                            zhContent.classList.add('hidden');
                            btnEn.classList.add('active');
                            btnZh.classList.remove('active');
                        } else {
                            enContent.classList.add('hidden');
                            zhContent.classList.remove('hidden');
                            btnEn.classList.remove('active');
                            btnZh.classList.add('active');
                        }
                    }
                </script>
            </body>
            </html>
            """;
}
