package server;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.DeciderBuilder;
import messages.OrderCheck;
import messages.OrderCheckResponse;
import messages.Request;
import messages.ResponseNotFound;
import scala.concurrent.duration.Duration;

import static akka.actor.SupervisorStrategy.escalate;

public class StreamActor extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private final ActorRef orderActor = context().actorOf(Props.create(OrderActor.class), "orderActor");

    private ActorRef clientActor;

    @Override
    public AbstractActor.Receive createReceive() {
        return receiveBuilder()
                .match(Request.class, request -> {
                    log.info("received request: " + request.getCommand() + " " + request.getValue());
                    clientActor = getSender();
                    orderActor.tell(new OrderCheck(request.getValue()), getSelf());
                })
                .match(OrderCheckResponse.class, response -> {
                    if (!response.isOrder()) {
                        ResponseNotFound res = new ResponseNotFound("", "you didn't order book you want to stream");
                        getContext().actorSelection("akka.tcp://client_system@127.0.0.1:2552/user/clientActor").tell(res, getSelf());
                    } else {
                        Request request = new Request("stream", response.getTitle());
                        context().actorOf(Props.create(StreamWorker.class, response.getTitle())).tell(request, clientActor);
                    }
                })
                .matchAny(s -> log.info("received unknown message"))
                .build();
    }

    private static SupervisorStrategy strategy =
            new OneForOneStrategy(10, Duration.create("1 minute"), DeciderBuilder
                    .matchAny(e -> escalate())
                    .build());

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }
}
