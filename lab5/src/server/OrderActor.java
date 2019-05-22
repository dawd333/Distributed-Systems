package server;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.DeciderBuilder;
import messages.*;
import scala.concurrent.duration.Duration;

import java.io.*;
import java.nio.charset.StandardCharsets;

import static akka.actor.SupervisorStrategy.escalate;

public class OrderActor extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private final ActorRef searchActor = context().actorOf(Props.create(SearchActor.class), "searchActor");

    private String title;

    private String orderPath = "resources/orders.txt";

    private boolean hasResponse;

    @Override
    public AbstractActor.Receive createReceive() {
        return receiveBuilder()
                .match(Request.class, request -> {
                    log.info("received request: " + request.getCommand() + " " + request.getValue());
                    title = request.getValue();
                    hasResponse = false;
                    searchActor.tell(request, getSelf());
                })
                .match(ResponseNotFound.class, response -> {
                    ResponseNotFound res = new ResponseNotFound("", "order unsuccessful - book not found");
                    context().actorSelection("akka.tcp://client_system@127.0.0.1:2552/user/clientActor").tell(res, getSelf());
                })
                .match(Response.class, response -> {
                    if (!hasResponse) {
                        hasResponse = true;
                        addOrder(title);
                        Response res = new Response("order confirmed");
                        context().actorSelection("akka.tcp://client_system@127.0.0.1:2552/user/clientActor").tell(res, getSelf());
                    }
                })
                .match(OrderCheck.class, request -> {
                    OrderCheckResponse response = new OrderCheckResponse(request.getTitle(), checkOrders(request.getTitle()));
                    getSender().tell(response, getSelf());
                })
                .matchAny(s -> log.info("received unknown message"))
                .build();
    }

    private void addOrder(String title){
        try {
            Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(orderPath, true), StandardCharsets.UTF_8));
            writer.append(title).append("\n");
            writer.close();
        } catch (Exception e){
            log.error("exception in orderActor: " + e.getClass().getSimpleName());
        }
    }

    private boolean checkOrders(String title) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(orderPath));
            for (String line; (line = br.readLine()) != null;){
                String[] arguments = line.split(" ");
                if (arguments[0].equals(title)) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("exception in searchWorker: " + e.getClass().getSimpleName());
        }

        return false;
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
