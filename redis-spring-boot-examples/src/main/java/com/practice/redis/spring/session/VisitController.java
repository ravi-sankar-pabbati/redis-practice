package com.practice.redis.spring.session;

import jakarta.servlet.http.HttpSession;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Session example: plain {@link HttpSession} code, but the data lives in Redis.
 *
 * <p>Nothing here mentions Redis. Spring Session adds a servlet filter that swaps Tomcat's
 * in-memory session for one stored at {@code demo:sessions:sessions:<id>} (a Redis hash, one
 * field per attribute). The browser gets a {@code SESSION} cookie holding that id.
 *
 * <p>Why bother? Sessions survive an app restart, and several app instances behind a load
 * balancer all see the same session. Try it: call /session/visits a few times, restart the app,
 * and call it again with the same cookie; the count carries on.
 *
 * <pre>
 *   GET    /session/visits         count visits in this session
 *   POST   /session/name?name=Ada  store a value in the session
 *   DELETE /session                log out (deletes the session from Redis)
 * </pre>
 */
@RestController
@RequestMapping("/session")
public class VisitController {

    @GetMapping("/visits")
    public Map<String, Object> visits(HttpSession session) {
        Integer visits = (Integer) session.getAttribute("visits");
        visits = (visits == null) ? 1 : visits + 1;
        session.setAttribute("visits", visits); // written to Redis when the request finishes
        return describe(session);
    }

    @PostMapping("/name")
    public Map<String, Object> setName(@RequestParam String name, HttpSession session) {
        session.setAttribute("name", name);
        return describe(session);
    }

    @DeleteMapping
    public Map<String, String> logout(HttpSession session) {
        String id = session.getId();
        session.invalidate(); // removes the session hash from Redis
        return Map.of("invalidated", id);
    }

    private static Map<String, Object> describe(HttpSession session) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sessionId", session.getId());
        body.put("redisKey", "demo:sessions:sessions:" + session.getId());
        body.put("visits", session.getAttribute("visits"));
        body.put("name", session.getAttribute("name"));
        body.put("expiresAfterIdleSeconds", session.getMaxInactiveInterval());
        return body;
    }
}
