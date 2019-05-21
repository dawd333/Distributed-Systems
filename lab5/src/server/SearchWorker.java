package server;

import akka.actor.AbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import messages.Request;
import messages.Response;
import messages.ResponseNotFound;

import java.io.BufferedReader;
import java.io.FileReader;

public class SearchWorker extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private String dbPath;

    public SearchWorker(int workerNumber){
        this.dbPath = "resources/db" + workerNumber + ".txt";
    }

    @Override
    public AbstractActor.Receive createReceive() {
        return receiveBuilder()
                .match(Request.class, request -> {
                    Response response = search(request.getValue());
                    context().parent().tell(response, getSender());
                })
                .matchAny(s -> log.info("received unknown message"))
                .build();

    }

    private Response search(String value){
        try {
            BufferedReader br = new BufferedReader(new FileReader(dbPath));
            for (String line; (line = br.readLine()) != null;){
                String[] arguments = line.split(" ");
                if (arguments[0].equals(value)) {
                    return new Response(arguments[1]);
                }
            }
        } catch (Exception e) {
            log.error("exception in searchWorker: " + e.getClass().getSimpleName());
        }

        return new ResponseNotFound("", "search unsuccessful");
    }
}
