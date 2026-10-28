package com.riyarakholiya.authservice.web;

/** A domain failure that maps to one {@link ErrorCode} and its HTTP status. */
public class AuthException extends RuntimeException {

    private final ErrorCode code;

    public AuthException(ErrorCode code) {
        super(code.getDefaultMessage());
        this.code = code;
    }

    public ErrorCode getCode() {
        return code;
    }
}
