package edu.csumb.api.error;

import org.springframework.http.HttpStatus;

/** Thrown when a signed-in user may not do this, like editing another shelter's post; the API answers 403. */
public class ForbiddenException extends ApiException {

    /** Creates the exception with the message the client will see as the problem detail. */
    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
