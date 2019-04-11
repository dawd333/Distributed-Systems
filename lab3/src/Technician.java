import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeoutException;

public class Technician {
    public static void main(String[] argv) throws IOException, TimeoutException {
        String injury1;
        String injury2;

        System.out.println("Technician");

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Please provide injury type 1:");
        injury1 = br.readLine();
        System.out.println("Please provide injury type 2:");
        injury2 = br.readLine();

        String[] injuries = {"hip", "knee", "elbow"};
        if (!Arrays.asList(injuries).contains(injury1) || !Arrays.asList(injuries).contains(injury2)) {
            System.out.println("You can only be technician in elbow, hip or knee !");
            System.exit(1);
        }

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        Connection technicianConnection = factory.newConnection();
        Channel technicianChannel = technicianConnection.createChannel();

        Connection technicianConnection2 = factory.newConnection();
        Channel technicianChannel2 = technicianConnection2.createChannel();

        Connection infoConnection = factory.newConnection();
        Channel infoChannel = infoConnection.createChannel();

        String EXCHANGE_NAME = "hospital";
        technicianChannel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);
        technicianChannel2.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);

        String EXCHANGE_NAME2 = "info";
        infoChannel.exchangeDeclare(EXCHANGE_NAME2, BuiltinExchangeType.FANOUT);

        String technicianQueueName = technicianChannel.queueDeclare(injury1, false, false, false, null).getQueue();
        String technicianQueueName2 = technicianChannel2.queueDeclare(injury2, false, false, false, null).getQueue();

        technicianChannel.queueBind(technicianQueueName, EXCHANGE_NAME, "technician."+injury1);
        technicianChannel2.queueBind(technicianQueueName2, EXCHANGE_NAME, "technician."+injury2);

        String infoQueueName = infoChannel.queueDeclare().getQueue();
        infoChannel.queueBind(infoQueueName, EXCHANGE_NAME2, "");

        Consumer consumer = new DefaultConsumer(technicianChannel){
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, StandardCharsets.UTF_8);
                String replyTo = properties.getReplyTo();
                String patientName = message.split(" ")[0];
                String injuryType = message.split(" ")[1];
                System.out.println("Got patient: " + patientName + " with injury: " + injuryType + " from doctor: " + replyTo);
                String reply = patientName + " " + injuryType + " done";
                try {
                    Thread.sleep(new Random().nextInt(5000) + 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                technicianChannel.basicPublish(EXCHANGE_NAME, replyTo, null, reply.getBytes(StandardCharsets.UTF_8));
                technicianChannel.basicPublish(EXCHANGE_NAME, "admin", null, reply.getBytes(StandardCharsets.UTF_8));
                System.out.println("Reply: " + reply);
            }
        };
        technicianChannel.basicConsume(technicianQueueName, true, consumer);
        technicianChannel2.basicConsume(technicianQueueName2, true, consumer);

        Consumer infoConsumer = new DefaultConsumer(infoChannel){
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, StandardCharsets.UTF_8);
                System.out.println("Received info: " + message);
            }
        };
        infoChannel.basicConsume(infoQueueName, true, infoConsumer);

        System.out.println("Ready for action.");
    }
}
