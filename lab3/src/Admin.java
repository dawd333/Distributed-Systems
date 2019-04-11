import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

public class Admin {
    public static void main(String[] args) throws IOException, TimeoutException {
        System.out.println("Admin");

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        Connection infoConnection = factory.newConnection();
        Channel infoChannel = infoConnection.createChannel();

        Connection adminConnection = factory.newConnection();
        Channel adminChannel = adminConnection.createChannel();

        String EXCHANGE_NAME = "hospital";
        infoChannel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);

        String EXCHANGE_NAME2 = "info";
        adminChannel.exchangeDeclare(EXCHANGE_NAME2, BuiltinExchangeType.FANOUT);

        String infoQueueName = infoChannel.queueDeclare("admin", false, false, false, null).getQueue();
        infoChannel.queueBind(infoQueueName, EXCHANGE_NAME, "admin");

        String adminQueueName = adminChannel.queueDeclare().getQueue();
        adminChannel.queueBind(adminQueueName, EXCHANGE_NAME2, "");

        Consumer consumer = new DefaultConsumer(infoChannel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, StandardCharsets.UTF_8);
                System.out.println("Received: " + message);
            }
        };
        infoChannel.basicConsume(infoQueueName, true, consumer);

        System.out.println("Ready for action.");

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        while(true){
            System.out.println("Type: info or exit.");
            String line = br.readLine();
            switch(line){
                case "info":
                    System.out.println("Enter info message: ");
                    String info = br.readLine();
                    adminChannel.basicPublish(EXCHANGE_NAME2, adminQueueName, null, info.getBytes(StandardCharsets.UTF_8));
                    break;
                case "exit":
                    System.exit(1);
                    break;
                default:
                    System.out.println("Wrong type: either info or exit.");
            }
        }
    }
}
