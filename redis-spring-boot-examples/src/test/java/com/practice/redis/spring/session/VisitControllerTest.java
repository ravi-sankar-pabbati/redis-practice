package com.practice.redis.spring.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.jayway.jsonpath.JsonPath;
import com.practice.redis.spring.RedisIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

class VisitControllerTest extends RedisIntegrationTest {

    @Test
    void sessionIsStoredInRedisAndFollowsTheCookie() throws Exception {
        // First request: no cookie, so Spring Session creates a new session
        MvcResult first = mvc.perform(get("/session/visits"))
                .andExpect(jsonPath("$.visits").value(1))
                .andReturn();
        Cookie sessionCookie = first.getResponse().getCookie("SESSION");
        assertThat(sessionCookie).isNotNull();

        // The session is a hash in Redis, one field per attribute
        String sessionId = JsonPath.read(first.getResponse().getContentAsString(), "$.sessionId");
        String key = "demo:sessions:sessions:" + sessionId;
        assertThat(redis.hasKey(key)).isTrue();
        assertThat(redis.opsForHash().keys(key)).contains("sessionAttr:visits");

        // Sending the cookie back continues the same session
        mvc.perform(get("/session/visits").cookie(sessionCookie))
                .andExpect(jsonPath("$.visits").value(2))
                .andExpect(jsonPath("$.sessionId").value(sessionId));

        // Without the cookie you get a brand-new session
        mvc.perform(get("/session/visits")).andExpect(jsonPath("$.visits").value(1));

        // Logging out deletes the session from Redis
        mvc.perform(delete("/session").cookie(sessionCookie));
        assertThat(redis.hasKey(key)).isFalse();
    }
}
