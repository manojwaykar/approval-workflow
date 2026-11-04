package com.workflow.approval_workflow.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.workflow.approval_workflow.exception.WorkflowConfigurationException;
import java.time.Clock;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-key-that-is-definitely-longer-than-32-bytes";

    private final JwtService jwtService = new JwtService(SECRET, 60_000, Clock.systemUTC());

    @Test
    void generatedTokenCanBeParsedBackToTheUsername() {
        String token = jwtService.generateToken("alice", "REQUESTER");

        assertThat(jwtService.extractUsername(token)).contains("alice");
        assertThat(jwtService.getExpirationMs()).isEqualTo(60_000);
    }

    @Test
    void garbageTokenIsRejected() {
        assertThat(jwtService.extractUsername("not-a-jwt")).isEmpty();
        assertThat(jwtService.extractUsername("")).isEmpty();
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        JwtService other = new JwtService("a-completely-different-secret-key-of-sufficient-length", 60_000,
                Clock.systemUTC());

        assertThat(jwtService.extractUsername(other.generateToken("alice", "REQUESTER"))).isEmpty();
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService alreadyExpired = new JwtService(SECRET, -1_000, Clock.systemUTC());

        assertThat(jwtService.extractUsername(alreadyExpired.generateToken("alice", "REQUESTER"))).isEmpty();
    }

    @Test
    void tooShortSecretIsRefusedAtStartup() {
        assertThatThrownBy(() -> new JwtService("short", 1000, Clock.systemUTC()))
                .isInstanceOf(WorkflowConfigurationException.class);
    }
}
