public enum Command {
    CONTAINS,
    GET,
    PUT,
    REMOVE,
    QUIT,
    ERROR;

    public static Command getCommand(String name){
        Command command = null;
        switch(name){
            case "contains":
                command = CONTAINS;
                break;
            case "get":
                command = GET;
                break;
            case "put":
                command = PUT;
                break;
            case "remove":
                command = REMOVE;
                break;
            case "quit":
                command = QUIT;
                break;
        }
        if (command == null){
            return ERROR;
        } else {
            return command;
        }
    }
}