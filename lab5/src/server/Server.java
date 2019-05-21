package server;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.File;

public class Server {

    public static void main(String[] args) throws Exception {

        File configFile = new File("server.conf");
        Config config = ConfigFactory.parseFile(configFile);

        final ActorSystem serverSystem = ActorSystem.create("server_system", config);
        final ActorRef serverActor = serverSystem.actorOf(Props.create(ServerActor.class), "serverActor");

        System.out.println("Server system started.");
    }
}
