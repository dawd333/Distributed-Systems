package server;

import akka.actor.AbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.stream.ActorMaterializer;
import akka.stream.IOResult;
import akka.stream.Materializer;
import akka.stream.ThrottleMode;
import akka.stream.javadsl.*;
import akka.util.ByteString;
import messages.Request;
import scala.concurrent.duration.Duration;

import java.nio.file.Paths;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

public class StreamWorker extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private String title;

    private final Materializer materializer = ActorMaterializer.create(context());

    public StreamWorker(String title) {
        this.title = title;
    }

    @Override
    public AbstractActor.Receive createReceive() {
        return receiveBuilder()
                .match(Request.class, request -> {
                    Source<ByteString, CompletionStage<IOResult>> source = FileIO.fromPath(Paths.get("resources/" + title + ".txt"));
                    source.via(Framing.delimiter(ByteString.fromString("\n"), 20000, FramingTruncation.ALLOW))
                            .map(ByteString::utf8String)
                            .throttle(1, Duration.create(1, TimeUnit.SECONDS), 1, ThrottleMode.shaping())
                            .to(Sink.actorRef(getSender(), "stream completed"))
                            .run(materializer);

                })
                .matchAny(s -> log.info("received unknown message"))
                .build();
    }
}
