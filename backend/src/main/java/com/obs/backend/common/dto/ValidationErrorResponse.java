package com.obs.backend.common.dto;

import java.util.Map;

public record ValidationErrorResponse(String error, String message, Map<String, String> fieldErrors) {
}
