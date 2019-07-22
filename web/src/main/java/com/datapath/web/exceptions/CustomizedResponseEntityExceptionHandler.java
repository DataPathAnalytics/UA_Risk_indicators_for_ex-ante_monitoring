package com.datapath.web.exceptions;

import com.datapath.elasticsearchintegration.exception.NoDataFilteredException;
import com.datapath.web.api.rest.exceptions.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.Date;

@RestControllerAdvice
@RestController
@Slf4j
public class CustomizedResponseEntityExceptionHandler {


    @ExceptionHandler(Exception.class)
    public final ResponseEntity<ErrorDetails> handleAllExceptions(Exception ex, WebRequest request) {
        log.error(ex.getMessage(), ex);
        ErrorDetails errorDetails = new ErrorDetails(HttpStatus.INTERNAL_SERVER_ERROR.toString(), new Date(), ex.getMessage(),
                request.getDescription(true), ex.getClass().getSimpleName());
        return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(IncorrectLoginOrPasswordException.class)
    public final ResponseEntity<ErrorDetails> handleIncorrectLoginOrPasswordException(IncorrectLoginOrPasswordException ex, WebRequest request) {
        String message = ex.getMessage();
        log.error(message, ex);
        String statusCode = HttpStatus.UNAUTHORIZED.toString();
        Date timestamp = new Date();
        String description = request.getDescription(false);
        String exceptionClassSimpleName = ex.getClass().getSimpleName();
        ErrorDetails errorDetails = new ErrorDetails(statusCode, timestamp, message,
                description, exceptionClassSimpleName);
        return new ResponseEntity<>(errorDetails, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(NullPointerException.class)
    public final ResponseEntity<ErrorDetails> handleNullPointerException(NullPointerException ex, WebRequest request) {
        log.error(ex.getMessage(), ex);
        ErrorDetails errorDetails = new ErrorDetails(HttpStatus.INTERNAL_SERVER_ERROR.toString(), new Date(), "NullPointerException at " + ex.getLocalizedMessage(),
                request.getDescription(true), ex.getClass().getSimpleName());
        return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(UserAlreadyExistException.class)
    public final ResponseEntity<ErrorDetails> handleUserAlreadyRegisteredException(UserAlreadyExistException ex, WebRequest request) {
        log.error(ex.getMessage(), ex);
        ErrorDetails errorDetails = new ErrorDetails(HttpStatus.CONFLICT.toString(), new Date(), "This email already registered" + ex.getLocalizedMessage(),
                request.getDescription(true), ex.getClass().getSimpleName());
        return new ResponseEntity<>(errorDetails, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(NoDataFilteredException.class)
    public final ResponseEntity<ErrorDetails> handleNoDataFilteredException(NoDataFilteredException ex, WebRequest request) {
        log.error(ex.getMessage(), ex);
        ErrorDetails errorDetails = new ErrorDetails(HttpStatus.UNPROCESSABLE_ENTITY.toString(), new Date(), ex.getMessage(),
                request.getDescription(true), ex.getClass().getSimpleName());
        return new ResponseEntity<>(errorDetails, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(EmptyResultDataAccessException.class)
    public final ResponseEntity<ErrorDetails> handleEmptyResultDataAccessException(EmptyResultDataAccessException ex, WebRequest request) {
        log.error(ex.getMessage(), ex);
        ErrorDetails errorDetails = new ErrorDetails(HttpStatus.INTERNAL_SERVER_ERROR.toString(), new Date(), "Record not found in database: " + ex.getLocalizedMessage(),
                request.getDescription(true), ex.getClass().getSimpleName());
        return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler({ResourceNotFoundException.class, RecordNotFoundException.class})
    public final ResponseEntity<ErrorDetails> handleUserNotFoundException(RecordNotFoundException ex, WebRequest request) {
        log.error(ex.getMessage(), ex);
        ErrorDetails errorDetails = new ErrorDetails(HttpStatus.NOT_FOUND.toString(), new Date(), ex.getLocalizedMessage(),
                request.getDescription(true), ex.getClass().getSimpleName());
        return new ResponseEntity<>(errorDetails, HttpStatus.NOT_FOUND);
    }
}
