package net.spookly.kodama.brain.controller;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
    String message =
        ex.getBindingResult().getFieldErrors().stream()
            .map(this::formatFieldError)
            .distinct()
            .collect(Collectors.joining("; "));

    if (message.isBlank()) {
      message = "Validation failed";
    }

    return build(HttpStatus.BAD_REQUEST, message);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleNotReadable() {
    return build(HttpStatus.BAD_REQUEST, "Malformed JSON request body");
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex) {
    HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
    if (status == null) {
      status = HttpStatus.INTERNAL_SERVER_ERROR;
    }
    String message = ex.getReason();
    if (message == null || message.isBlank()) {
      message = status.getReasonPhrase();
    }
    return build(status, message);
  }

  private ResponseEntity<ErrorResponse> build(HttpStatus status, String message) {
    return ResponseEntity.status(status)
        .body(new ErrorResponse(status.value(), status.getReasonPhrase(), message));
  }

  private String formatFieldError(FieldError fieldError) {
    String field = fieldError.getField();
    String defaultMessage = fieldError.getDefaultMessage();
    if (defaultMessage == null || defaultMessage.isBlank()) {
      return field + " is invalid";
    }
    return field + " " + defaultMessage;
  }

  public record ErrorResponse(int status, String error, String message) {}
}
