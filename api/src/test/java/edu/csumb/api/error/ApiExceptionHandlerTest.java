package edu.csumb.api.error;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void badRequestIs400WithTheMessage() {
        ProblemDetail problem = handler.handle(new BadRequestException("Name is required"));
        assertEquals(400, problem.getStatus());
        assertEquals("Bad Request", problem.getTitle());
        assertEquals("Name is required", problem.getDetail());
    }

    @Test
    void forbiddenIs403WithTheMessage() {
        ProblemDetail problem = handler.handle(new ForbiddenException("You can't edit this post"));
        assertEquals(403, problem.getStatus());
        assertEquals("Forbidden", problem.getTitle());
        assertEquals("You can't edit this post", problem.getDetail());
    }

    @Test
    void notFoundIs404WithTheMessage() {
        ProblemDetail problem = handler.handle(new NotFoundException("Post not found"));
        assertEquals(404, problem.getStatus());
        assertEquals("Not Found", problem.getTitle());
        assertEquals("Post not found", problem.getDetail());
    }
}
