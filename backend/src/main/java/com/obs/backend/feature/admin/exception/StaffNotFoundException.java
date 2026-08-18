package com.obs.backend.feature.admin.exception;

import java.util.UUID;

public class StaffNotFoundException extends RuntimeException {

    public StaffNotFoundException(UUID staffId) {
        super("Staff member not found with ID: " + staffId);
    }
}
