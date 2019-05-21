package messages;

import java.io.Serializable;

public class ResponseNotFound extends Response implements Serializable {
    private String message;

    public ResponseNotFound(String response, String message){
        super(response);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
