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

public class Doctor {
    public static void main(String[] args) throws IOException, TimeoutException {
        String name;

        System.out.println("Doctor");

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Please provide name:");
        name = br.readLine();

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        Connection receptionConnection = factory.newConnection();
        Channel receptionChannel = receptionConnection.createChannel();

        Connection doctorConnection = factory.newConnection();
        Channel doctorChannel = doctorConnection.createChannel();

        Connection infoConnection = factory.newConnection();
        Channel infoChannel = infoConnection.createChannel();

        String EXCHANGE_NAME = "hospital";
        receptionChannel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);
        doctorChannel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);

        String EXCHANGE_NAME2 = "info";
        infoChannel.exchangeDeclare(EXCHANGE_NAME2, BuiltinExchangeType.FANOUT);

        String receptionQueueName = receptionChannel.queueDeclare("reception", false, false, false, null).getQueue();
        receptionChannel.queueBind(receptionQueueName, EXCHANGE_NAME, "doctors");

        String doctorQueueName = doctorChannel.queueDeclare(name, false, false, false, null).getQueue();
        doctorChannel.queueBind(doctorQueueName, EXCHANGE_NAME, name);

        String infoQueueName = infoChannel.queueDeclare().getQueue();
        infoChannel.queueBind(infoQueueName, EXCHANGE_NAME2, "");

        Consumer consumer = new DefaultConsumer(receptionChannel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, StandardCharsets.UTF_8);
                String patientName = message.split(" ")[0];
                String injuryType = message.split(" ")[1];
                System.out.println("Got patient: " + patientName + " with injury: " + injuryType);
                AMQP.BasicProperties props = new AMQP.BasicProperties
                        .Builder()
                        .replyTo(name)
                        .build();
                doctorChannel.basicPublish(EXCHANGE_NAME, "technician."+injuryType, props, message.getBytes(StandardCharsets.UTF_8));
                doctorChannel.basicPublish(EXCHANGE_NAME, "admin", null, message.getBytes(StandardCharsets.UTF_8));
            }
        };
        receptionChannel.basicConsume(receptionQueueName, true, consumer);

        Consumer responseConsumer = new DefaultConsumer(doctorChannel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, StandardCharsets.UTF_8);
                String patientName = message.split(" ")[0];
                String injuryType = message.split(" ")[1];
                System.out.println("Patient: " + patientName + " with injury: " + injuryType + " done");
            }
        };
        doctorChannel.basicConsume(doctorQueueName, true, responseConsumer);

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
