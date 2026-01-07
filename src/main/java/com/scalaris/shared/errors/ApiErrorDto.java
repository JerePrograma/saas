// ============================================================================
// shared/errors/ApiErrorDto.java
// ============================================================================
package com.scalaris.shared.errors;

import java.time.OffsetDateTime;
import java.util.List;

public record ApiErrorDto(
        OffsetDateTime timestamp,
        int status,
        String code,
        String message,
        List<FieldErrorDto> fieldErrors,
        String traceId,
        String path
) {
    public static ApiErrorDto of(
            int status, String code, String message,
            List<FieldErrorDto> fieldErrors,
            String traceId, String path
    ) {
        return new ApiErrorDto(
                OffsetDateTime.now(),
                status,
                code,
                message,
                fieldErrors == null ? List.of() : List.copyOf(fieldErrors),
                traceId,
                path
        );
    }
}
