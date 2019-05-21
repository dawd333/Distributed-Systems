package server;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.DeciderBuilder;
import messages.Request;
import messages.Response;
import messages.ResponseNotFound;
import scala.concurrent.duration.Duration;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static akka.actor.SupervisorStrategy.escalate;
import static akka.actor.SupervisorStrategy.restart;

public class SearchActor extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private final ActorRef searchWorker1 = context().actorOf(Props.create(SearchWorker.class, 1), "searchWorker1");
    private final ActorRef searchWorker2 = context().actorOf(Props.create(SearchWorker.class, 2), "searchWorker2");

    @Override
    public AbstractActor.Receive createReceive() {
        return receiveBuilder()
                .match(Request.class, request -> {
                    searchWorker1.tell(request, getSender());
                    searchWorker2.tell(request, getSender());
                })
                .match(ResponseNotFound.class, response -> {
                    getSender().tell(response, getSelf());
                })
                .match(Response.class, response -> {
                    getSender().tell(response, getSelf());
                })
                .matchAny(s -> log.info("received unknown message"))
                .build();
    }

    private static SupervisorStrategy strategy =
            new OneForOneStrategy(10, Duration.create(1, TimeUnit.MINUTES), DeciderBuilder
                    .match(FileNotFoundException.class, e -> restart())
                    .match(IOException.class, e -> restart())
                    .matchAny(e -> escalate())
                    .build());

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }
}
