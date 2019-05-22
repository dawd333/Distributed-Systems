package client;

import akka.actor.AbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import messages.Request;
import messages.Response;
import messages.ResponseNotFound;

public class ClientActor extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private boolean ignoreUnsuccessful;

    private boolean hasResponse;

    @Override
    public AbstractActor.Receive createReceive() {
        return receiveBuilder()
                .match(Request.class, request -> {
                    log.info("requested: " + request.getCommand() + " " + request.getValue());
                    ignoreUnsuccessful = false;
                    hasResponse = false;
                    getContext().actorSelection("akka.tcp://server_system@127.0.0.1:3552/user/serverActor").tell(request, getSelf());
                })
                .match(ResponseNotFound.class, response -> {
                    if (!ignoreUnsuccessful) {
                        System.out.println(response.getMessage());
                    }
                })
                .match(Response.class, response -> {
                    if (!hasResponse) {
                        ignoreUnsuccessful = true;
                        hasResponse = true;
                        System.out.println(response.getResponse());
                    }
                })
                .match(String.class, System.out::println)
                .matchAny(s -> log.info("received unknown message"))
                .build();
    }
}
