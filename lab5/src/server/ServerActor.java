package server;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.DeciderBuilder;

import messages.Request;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

import static akka.actor.SupervisorStrategy.restart;

public class ServerActor extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private final ActorRef searchActor = context().actorOf(Props.create(SearchActor.class), "searchActor");
    private final ActorRef orderActor = context().actorOf(Props.create(OrderActor.class), "orderActor");
    private final ActorRef streamActor = context().actorOf(Props.create(StreamActor.class), "streamActor");

    @Override
    public AbstractActor.Receive createReceive() {
        return receiveBuilder()
                .match(Request.class, request -> {
                    String type = request.getCommand();
                    switch (type) {
                        case "search":
                            searchActor.tell(request, getSender());
                            break;
                        case "order":
                            orderActor.tell(request, getSender());
                            break;
                        case "stream":
                            streamActor.tell(request, getSender());
                            break;
                        default:
                            log.info("Wrong type of request");
                            break;
                    }
                })
                .matchAny(s -> log.info("received unknown message"))
                .build();
    }

    private static SupervisorStrategy strategy =
            new OneForOneStrategy(10, Duration.create(1, TimeUnit.MINUTES), DeciderBuilder
                    .matchAny(e -> restart())
                    .build());

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }
}
