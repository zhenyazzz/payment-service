package com.innowise.paymentservice.utils;

import java.util.List;
import java.util.UUID;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.innowise.paymentservice.security.CurrentUser;
import com.innowise.paymentservice.security.Roles;

import lombok.experimental.UtilityClass;

@UtilityClass
public class SecurityTestDataFactory {

    public static final UUID ADMIN_USER_ID = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa1");
    public static final UUID USER_USER_ID = UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbb1");

    public static final String ADMIN_EMAIL = "admin@example.com";
    public static final String USER_EMAIL = "buyer@example.com";

    public CurrentUser buildAdminUser() {
        return new CurrentUser(ADMIN_USER_ID, ADMIN_EMAIL, List.of(Roles.ADMIN, Roles.USER));
    }

    public CurrentUser buildUser() {
        return new CurrentUser(USER_USER_ID, USER_EMAIL, List.of(Roles.USER));
    }

    public UsernamePasswordAuthenticationToken buildAuthentication(CurrentUser currentUser) {
        List<SimpleGrantedAuthority> authorities = currentUser.roles().stream()
            .map(Roles::getAuthority)
            .map(SimpleGrantedAuthority::new)
            .toList();
        return new UsernamePasswordAuthenticationToken(currentUser, null, authorities);
    }

    public AnonymousAuthenticationToken buildAnonymousAuthentication() {
        return new AnonymousAuthenticationToken(
            "key",
            "anonymousUser",
            List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        );
    }
}
