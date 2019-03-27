import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Client {
    private DistributedMap map;
    private boolean running;

    public Client(DistributedMap map){
        this.map = map;
        this.running = true;
    }

    public void start(){
        System.out.println("List of proper commands: contains, get, put, remove and quit.");
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        while(running){
            try{
                String line = in.readLine();
                String[] words = line.split(" ");
                if (words.length == 0){
                    System.out.println("Please provide proper command.");
                    System.out.println("List of proper commands: contains, get, put, remove and quit.");
                } else if (words.length == 1){
                    if (Command.getCommand(words[0]) == Command.QUIT) {
                        executeCommand(Command.getCommand(words[0]), words);
                    } else {
                        System.out.println("Command has to have a key attached.");
                    }
                } else {
                    executeCommand(Command.getCommand(words[0]), words);
                }
            } catch(Exception e){
                e.printStackTrace();
            }
        }
        System.exit(0);
    }

    private void executeCommand(Command command, String[] words){
        try {
            boolean valuePresent = (words.length >= 3);

            switch (command) {
                case CONTAINS:
                    if (map.containsKey(words[1])){
                        System.out.println("There is value under key: " + words[1]);
                    } else {
                        System.out.println("There is no value under key: " + words[1]);
                    }
                    break;
                case GET:
                    Integer value = map.get(words[1]);
                    if (value == null){
                        System.out.println("There was no value under key: " + words[1]);
                    } else {
                        System.out.println("There was: " + value.toString() + " under key: " + words[1]);
                    }
                    break;
                case PUT:
                    if (!valuePresent) {
                        System.out.println("This command also needs value to a key.");
                    } else {
                        map.put(words[1], Integer.valueOf(words[2]));
                        System.out.println("Put value: " + words[2] + " under key: " + words[1]);
                    }
                    break;
                case REMOVE:
                    System.out.println("Removed value: " + map.remove(words[1]) + " under key: " + words[1]);
                    break;
                case QUIT:
                    this.running = false;
                    System.out.println("Quit initialized.");
                    break;
                case ERROR:
                    System.out.println("Bad command provided.");
                    System.out.println("List of proper commands: contains, get, put, remove and quit.");
                    break;
            }
        } catch(Exception e){
            e.printStackTrace();
        }
    }
}
