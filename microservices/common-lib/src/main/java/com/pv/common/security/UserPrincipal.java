package com.pv.common.security;

import lombok.Getter;

import java.util.List;

/**
 * JWT claims holder — inject bằng {@code @AuthenticationPrincipal UserPrincipal}.
 */
@Getter
public class UserPrincipal {

    private final String userId;
    private final List<String> roles;

    public UserPrincipal(String userId, List<String> roles) {
        this.userId = userId;
        this.roles = roles != null ? roles : List.of();
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }
}

