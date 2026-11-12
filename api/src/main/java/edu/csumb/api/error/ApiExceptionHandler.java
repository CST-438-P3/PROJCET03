package edu.csumb.api.error;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Turns our own exceptions into RFC 9457 problem responses, the Problem schema in openapi.yaml.
 * Only ApiException is mapped, so a bug elsewhere stays a 500 and its message is not shown to clients.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /** Answers with the exception's status, the status's standard title, and the exception's message. */
    @ExceptionHandler(ApiException.class)
    public ProblemDetail handle(ApiException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(e.getStatus(), e.getMessage());
        problem.setTitle(e.getStatus().getReasonPhrase());
        return problem;
    }
}
