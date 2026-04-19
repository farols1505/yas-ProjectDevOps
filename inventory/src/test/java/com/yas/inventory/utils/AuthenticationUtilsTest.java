package com.yas.inventory.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.AccessDeniedException;
import com.yas.inventory.constants.ApiConstant;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class AuthenticationUtilsTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void extractUserId_returnsSubjectFromJwt() {
        Jwt jwt = Jwt.withTokenValue("token")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(300))
            .headers(h -> h.put("alg", "none"))
            .subject("user-42")
            .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        assertThat(AuthenticationUtils.extractUserId()).isEqualTo("user-42");
    }

    @Test
    void extractUserId_whenAnonymous_thenAccessDenied() {
        AnonymousAuthenticationToken anonymous = new AnonymousAuthenticationToken(
            "key", "anon", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
        SecurityContextHolder.getContext().setAuthentication(anonymous);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, AuthenticationUtils::extractUserId);
        assertThat(ex.getMessage()).isEqualTo(ApiConstant.ACCESS_DENIED);
    }

    @Test
    void extractJwt_returnsTokenValue() {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getTokenValue()).thenReturn("jwt-token-value");
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(jwt);
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);

        assertThat(AuthenticationUtils.extractJwt()).isEqualTo("jwt-token-value");
    }
}
