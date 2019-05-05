package exchange;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.util.logging.Logger;

public class ExchangeServer {
    private static final Logger logger = Logger.getLogger(ExchangeServer.class.getName());

    private Server server;

    /** Start serving requests. */
    public void start() throws IOException {
        int port = 50051;
        server = ServerBuilder.forPort(port)
                .addService(new ExchangeServiceImpl())
                .build()
                .start();
        logger.info("Exchange server started on port " + port);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Use stderr here since the logger may has been reset by its JVM shutdown hook.
            System.err.println("*** shutting down gRPC server since JVM is shutting down");
            ExchangeServer.this.stop();
            System.err.println("*** server shut down");
        }));
    }

    /** Stop serving requests and shutdown resources. */
    public void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    /**
     * Main method to launch server from command line.
     */
    public static void main(String[] args) throws Exception {
        ExchangeServer server = new ExchangeServer();
        server.start();
        server.blockUntilShutdown();
    }
}
