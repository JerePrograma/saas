package com.scalaris.api;

public final class ErrorCodes {
    private ErrorCodes() {}

    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String INVALID_REQUEST  = "INVALID_REQUEST";
    public static final String BUSINESS_RULE    = "BUSINESS_RULE";

    public static final String EMAIL_ALREADY_REGISTERED = "EMAIL_ALREADY_REGISTERED";
    public static final String INVALID_CREDENTIALS      = "INVALID_CREDENTIALS";
    public static final String INVALID_TOKEN            = "INVALID_TOKEN";

    public static final String UNAUTHORIZED     = "UNAUTHORIZED";
    public static final String FORBIDDEN        = "FORBIDDEN";
    public static final String METHOD_NOT_ALLOWED = "METHOD_NOT_ALLOWED";
    public static final String MALFORMED_JSON   = "MALFORMED_JSON";
    public static final String INTERNAL_ERROR   = "INTERNAL_ERROR";
}
