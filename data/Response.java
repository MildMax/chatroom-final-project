package data;

import java.io.Serializable;

public class Response implements Serializable {
    private final ResponseStatus status;
    private final String message;

    public Response(ResponseStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public ResponseStatus getStatus() {
        return this.status;
    }

    public String getMessage() {
        return this.message;
    }
}
