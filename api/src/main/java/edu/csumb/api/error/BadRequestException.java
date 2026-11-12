package edu.csumb.api.error;

import org.springframework.http.HttpStatus;

/** Thrown when a request value breaks one of our rules, like a post name that is too long; the API answers 400. */
public class BadRequestException extends ApiException {

    /** Creates the exception with the message the client will see as the problem detail. */
    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
