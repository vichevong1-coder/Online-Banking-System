package com.obs.backend.security;

import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

// JwtAuthenticationFilter sets the authenticated principal's name to the JWT subject, which is
// the user's id (see AuthenticationServiceImpl). Every account-scoped endpoint needs that id.
@Component
public class CurrentUserProvider {

    public UUID currentUserId() {
        return UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
    }
}
