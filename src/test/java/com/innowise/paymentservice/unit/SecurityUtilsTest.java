package com.innowise.paymentservice.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.innowise.paymentservice.exception.security.SecurityContextException;
import com.innowise.paymentservice.security.CurrentUser;
import com.innowise.paymentservice.security.SecurityUtils;
import com.innowise.paymentservice.utils.SecurityTestDataFactory;

@DisplayName("SecurityUtils (unit tests)")
class SecurityUtilsTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("returns current user when authentication contains CurrentUser principal")
    void getCurrentUser_whenPrincipalIsCurrentUser_returnsUser() {
        CurrentUser currentUser = SecurityTestDataFactory.buildAdminUser();
        SecurityContextHolder.getContext().setAuthentication(
            SecurityTestDataFactory.buildAuthentication(currentUser)
        );

        var result = SecurityUtils.getCurrentUser();

        assertThat(result).isPresent();
        assertThat(result.orElseThrow()).isEqualTo(currentUser);
    }

    @Test
    @DisplayName("returns empty when authentication is anonymous")
    void getCurrentUser_whenAuthenticationIsAnonymous_returnsEmpty() {
        SecurityContextHolder.getContext().setAuthentication(
            SecurityTestDataFactory.buildAnonymousAuthentication()
        );

        assertThat(SecurityUtils.getCurrentUser()).isEmpty();
    }

    @Test
    @DisplayName("returns empty when principal type is not CurrentUser")
    void getCurrentUser_whenPrincipalIsNotCurrentUser_returnsEmpty() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "plain-principal",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
            )
        );

        assertThat(SecurityUtils.getCurrentUser()).isEmpty();
    }

    @Test
    @DisplayName("returns current user fields and roles correctly")
    void getters_whenUserExists_returnExpectedValues() {
        CurrentUser currentUser = SecurityTestDataFactory.buildUser();
        SecurityContextHolder.getContext().setAuthentication(
            SecurityTestDataFactory.buildAuthentication(currentUser)
        );

        assertThat(SecurityUtils.getCurrentUserId()).isEqualTo(SecurityTestDataFactory.USER_USER_ID.toString());
        assertThat(SecurityUtils.getCurrentUserEmail()).isEqualTo(SecurityTestDataFactory.USER_EMAIL);
        assertThat(SecurityUtils.getCurrentUserRoles()).containsExactly("ROLE_USER");
        assertThat(SecurityUtils.isAdmin()).isFalse();
    }

    @Test
    @DisplayName("isAdmin returns true when current user has admin role")
    void isAdmin_whenUserHasAdminRole_returnsTrue() {
        CurrentUser currentUser = SecurityTestDataFactory.buildAdminUser();
        SecurityContextHolder.getContext().setAuthentication(
            SecurityTestDataFactory.buildAuthentication(currentUser)
        );

        assertThat(SecurityUtils.isAdmin()).isTrue();
    }

    @Test
    @DisplayName("throws SecurityContextException when user missing in context")
    void getters_whenUserMissing_throwSecurityContextException() {
        assertThatThrownBy(SecurityUtils::getCurrentUserId)
            .isInstanceOf(SecurityContextException.class)
            .hasMessage("User not found in security context");
    }
}
