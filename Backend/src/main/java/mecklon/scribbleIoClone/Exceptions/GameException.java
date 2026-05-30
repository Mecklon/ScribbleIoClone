package mecklon.scribbleIoClone.Exceptions;

import org.springframework.http.HttpStatus;

public class GameException extends RuntimeException {

    private final HttpStatus status;
    private final GameExceptionType exceptionType;

    public GameException(
            HttpStatus status,
            GameExceptionType error,
            String message
    ) {
        super(message);
        this.status = status;
        this.exceptionType = error;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public GameExceptionType getExceptionType() {
        return exceptionType;
    }
}