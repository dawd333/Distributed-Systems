package messages;

import java.io.Serializable;

public class Request implements Serializable {

    private String command;
    private String value;

    public Request(String command, String value){
        this.command = command;
        this.value = value;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
