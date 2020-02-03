package main.controllers;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ExceptionController {

    private static final Logger LOG = LoggerFactory.
            getLogger(ExceptionController.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity handleUnspecifiedRuntimeException(final RuntimeException ex) {
        LOG.error("Unexpected runtime exception occurred: " + ex.getMessage());
        return createError(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    private ResponseEntity createError(final HttpStatus code, final String message) {
        LOG.error(message);
        return new ResponseEntity<>(new ErrorResponse(message), code);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity invalidParamError(final MissingServletRequestParameterException exception) {
        final String message = String.format("Required parameter is missing. Parameter name: '%s'.",
                exception.getParameterName());
        return createError(HttpStatus.BAD_REQUEST, message);
    }

    class ErrorResponse {

        private String message;

        public ErrorResponse(final String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(final String message) {
            this.message = message;
        }
    }

}
