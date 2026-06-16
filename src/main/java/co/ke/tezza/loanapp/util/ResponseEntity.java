package co.ke.tezza.loanapp.util;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Custom response wrapper.
 * Be careful: This shadows Spring's own ResponseEntity<T>.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResponseEntity<T> {

    private String message;
    private int statusCode;
    private T entity;
    private HttpStatus status;

    public ResponseEntity(String message, int statusCode, T entity) {
        this.message = message;
        this.statusCode = statusCode;
        this.entity = entity;
    }

    public ResponseEntity(String message, T entity, HttpStatus status) {
        this.message = message;
        this.status = status;
        this.entity = entity;
        this.statusCode = status.value(); 
    }
}
