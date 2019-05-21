package client;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import messages.Request;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class Client {

    public static void main(String[] args) throws Exception {

        File configFile = new File("client.conf");
        Config config = ConfigFactory.parseFile(configFile);

        final ActorSystem clientSystem = ActorSystem.create("client_system", config);
        final ActorRef clientActor = clientSystem.actorOf(Props.create(ClientActor.class), "clientActor");

        System.out.println("Client system started. Commands: 'search' [title], 'order' [title], 'stream' [title], 'quit'");

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            String line = br.readLine();
            if (line.equals("quit")) {
                break;
            } else if (validRequest(line)){
                String[] arguments = line.split(" ");
                if (arguments.length == 2){
                    Request request = new Request(arguments[0], arguments[1]);
                    clientActor.tell(request, null);
                } else {
                    System.out.println("Wrong number of arguments");
                }
            } else {
                System.out.println("Wrong command");
            }
        }

        clientSystem.terminate();
    }

    private static boolean validRequest(String request){
        return request.startsWith("search") || request.startsWith("order") || request.startsWith("stream");
    }
}
