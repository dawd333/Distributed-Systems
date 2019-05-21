package client;

import akka.actor.AbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import messages.Request;
import messages.Response;
import messages.ResponseNotFound;

public class ClientActor extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    @Override
    public AbstractActor.Receive createReceive() {
        return receiveBuilder()
                .match(Request.class, request -> {
                    getContext().actorSelection("akka.tcp://server_system@127.0.0.1:3552/user/serverActor").tell(request, getSelf());
                })
                .match(ResponseNotFound.class, response -> {
                    System.out.println(response.getMessage());
                })
                .match(Response.class, response -> {
                    System.out.println(response.getResponse());
                })
                .match(String.class, System.out::println)
                .matchAny(s -> log.info("received unknown message"))
                .build();
    }
}
