package edu.csumb.api.error;

import org.springframework.http.HttpStatus;

/** Base for our own errors: each kind carries the HTTP status the API answers with. */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    /** Creates the exception with its status and the message the client will see as the problem detail. */
    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    /** The HTTP status the API answers with for this error. */
    public HttpStatus getStatus() {
        return status;
    }
}
