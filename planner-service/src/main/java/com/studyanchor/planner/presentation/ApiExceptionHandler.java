package com.studyanchor.planner.presentation;

import com.studyanchor.planner.domain.ConflictException;
import com.studyanchor.planner.domain.DomainException;
import com.studyanchor.planner.domain.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps domain errors to HTTP status codes (docs/09-conventions.md): 400, 404, 409. */
@RestControllerAdvice
class ApiExceptionHandler {

    record ErrorResponse(int status, String error, String message) {
    }

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ErrorResponse> notFound(NotFoundException e) {
        return respond(HttpStatus.NOT_FOUND, e);
    }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<ErrorResponse> conflict(ConflictException e) {
        return respond(HttpStatus.CONFLICT, e);
    }

    @ExceptionHandler(DomainException.class)
    ResponseEntity<ErrorResponse> badRequest(DomainException e) {
        return respond(HttpStatus.BAD_REQUEST, e);
    }

    private static ResponseEntity<ErrorResponse> respond(HttpStatus status, RuntimeException e) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(status.value(), status.getReasonPhrase(), e.getMessage()));
    }
}
