package com.obs.backend.feature.admin.exception;

import com.obs.backend.security.Role;

/** Staff accounts are ADMIN or TELLER only; CUSTOMER belongs to the mobile signup flow. */
public class InvalidStaffRoleException extends RuntimeException {

    public InvalidStaffRoleException(Role role) {
        super("Staff role must be ADMIN or TELLER, got: " + role);
    }
}
