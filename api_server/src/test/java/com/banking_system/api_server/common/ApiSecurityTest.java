package com.banking_system.api_server.common;

import com.banking_system.api_server.common.security.InternalApiKeyFilter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 접근 통제가 실제 HTTP 계층에서 동작하는지 확인한다.
 * 이전 구현은 Spring Data REST 가 인증 없이 전 테이블 CRUD 를 열어두고 있었다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("토큰 없이 계좌 API 를 호출하면 401 을 돌려준다")
    void unauthenticatedIsRejected() throws Exception {
        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("C002"));
    }

    @Test
    @DisplayName("가입 → 로그인 → 토큰으로 내 정보 조회가 이어진다")
    void signUpAndLogin() throws Exception {
        String email = UUID.randomUUID() + "@banking.test";

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "테스터", "email", email, "password", "password1234"))))
                .andExpect(status().isCreated())
                // 응답 어디에도 비밀번호가 실려서는 안 된다.
                .andExpect(jsonPath("$.password").doesNotExist());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", email, "password", "password1234"))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String accessToken = body.get("accessToken").asText();
        assertThat(accessToken).isNotBlank();

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    @DisplayName("비밀번호가 틀리면 401 이고, 이메일 존재 여부는 알려주지 않는다")
    void loginFailure() throws Exception {
        String email = UUID.randomUUID() + "@banking.test";
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "테스터", "email", email, "password", "password1234"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", email, "password", "wrong-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("U003"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "nobody@banking.test", "password", "password1234"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("U003"));
    }

    @Test
    @DisplayName("검증에 실패하면 필드별 사유를 400 으로 돌려준다")
    void validationError() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "", "email", "not-an-email", "password", "short"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"))
                .andExpect(jsonPath("$.errors.length()").value(3));
    }

    @Test
    @DisplayName("내부 API 는 키가 맞을 때만 열린다")
    void internalEndpointRequiresApiKey() throws Exception {
        mockMvc.perform(get("/api/internal/stats/snapshot"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/internal/stats/snapshot")
                        .header(InternalApiKeyFilter.HEADER, "wrong-key"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/internal/stats/snapshot")
                        .header(InternalApiKeyFilter.HEADER, "test-internal-api-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userCount").exists())
                .andExpect(jsonPath("$.totalBalance").exists());
    }

    @Test
    @DisplayName("허용 목록에 있는 오리진의 프리플라이트만 통과한다")
    void corsPreflight() throws Exception {
        mockMvc.perform(options("/api/accounts")
                        .header(HttpHeaders.ORIGIN, "https://app.example.test")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://app.example.test"));

        mockMvc.perform(options("/api/accounts")
                        .header(HttpHeaders.ORIGIN, "https://evil.example.test")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("비밀번호 재설정 엔드포인트는 인증 없이 열려 있고, 잘못된 토큰은 거절한다")
    void passwordResetIsPublicButValidated() throws Exception {
        mockMvc.perform(post("/api/auth/password-reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("token", "not-a-real-token", "newPassword", "password1234"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("U005"));
    }

    @Test
    @DisplayName("재설정 토큰 발급은 내부 API 키가 있어야 한다")
    void issueResetTokenRequiresInternalKey() throws Exception {
        String email = UUID.randomUUID() + "@banking.test";
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "테스터", "email", email, "password", "password1234"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/internal/users/password-reset-tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/internal/users/password-reset-tokens")
                        .header(InternalApiKeyFilter.HEADER, "test-internal-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.expiresAt").isNotEmpty());
    }

    @Test
    @DisplayName("사용자 토큰으로는 내부 API 에 접근할 수 없다")
    void userTokenCannotReachInternalApi() throws Exception {
        String email = UUID.randomUUID() + "@banking.test";
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "테스터", "email", email, "password", "password1234"))))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", email, "password", "password1234"))))
                .andReturn();
        String accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("accessToken").asText();

        mockMvc.perform(get("/api/internal/stats/snapshot")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("C003"));
    }
}
