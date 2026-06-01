package com.exambank.common.exception;

/** Thrown on login when the email is unknown or the password does not match. */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
