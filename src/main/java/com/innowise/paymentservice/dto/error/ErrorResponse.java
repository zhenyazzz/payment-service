package com.innowise.paymentservice.dto.error;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        String error,
        String message,
        Instant timestamp,
        List<String> details
) {
}
