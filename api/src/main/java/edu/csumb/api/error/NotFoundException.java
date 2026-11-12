package edu.csumb.api.error;

import org.springframework.http.HttpStatus;

/** Thrown when the thing a request asks for does not exist; the API answers 404. */
public class NotFoundException extends ApiException {

    /** Creates the exception with the message the client will see as the problem detail. */
    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
