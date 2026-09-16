package com.ailepro.api.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimiter {
    private static final int LIMIT = 10;
    private static final long WINDOW_MS = 60_000;
    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    public boolean tryConsume(HttpServletRequest request) {
        String key = clientKey(request);
        long now = Instant.now().toEpochMilli();
        Deque<Long> q = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (q) {
            while (!q.isEmpty() && now - q.peekFirst() > WINDOW_MS) {
                q.removeFirst();
            }
            if (q.size() >= LIMIT) {
                return false;
            }
            q.addLast(now);
            return true;
        }
    }

    private String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }
}
