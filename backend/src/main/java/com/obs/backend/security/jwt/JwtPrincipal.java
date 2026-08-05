package com.obs.backend.security.jwt;

import com.obs.backend.security.Role;
import java.util.Set;

public record JwtPrincipal(String subject, Set<Role> roles) {
}
