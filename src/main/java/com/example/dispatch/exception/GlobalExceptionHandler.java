package com.example.dispatch.exception;

import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
    public record ApiError(Instant timestamp,int status,String error,String message,Map<String,String> details) {}

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex){Map<String,String> details=new TreeMap<>();
        for(FieldError e:ex.getBindingResult().getFieldErrors())details.putIfAbsent(e.getField(),e.getDefaultMessage());
        return response(HttpStatus.BAD_REQUEST,"Validation failed","Please correct the invalid request fields.",details);}

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> badRequest(IllegalArgumentException ex){return response(HttpStatus.BAD_REQUEST,"Invalid request",ex.getMessage(),Map.of());}

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> malformed(HttpMessageNotReadableException ex){return response(HttpStatus.BAD_REQUEST,"Malformed request","Request body is invalid JSON or contains an unsupported enum value.",Map.of());}

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiError> database(DataAccessException ex){return response(HttpStatus.INTERNAL_SERVER_ERROR,"Storage error","The request could not be stored. Please retry.",Map.of());}

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception ex){return response(HttpStatus.INTERNAL_SERVER_ERROR,"Internal server error","An unexpected error occurred.",Map.of());}

    private ResponseEntity<ApiError> response(HttpStatus status,String error,String message,Map<String,String> details){
        return ResponseEntity.status(status).body(new ApiError(Instant.now(),status.value(),error,message,details));}
}
