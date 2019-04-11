import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

public class Reception {
    public static void main(String[] args) throws IOException, TimeoutException {
        System.out.println("Reception");
        String input = "";

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        Connection receptionConnection = factory.newConnection();
        Channel receptionChannel = receptionConnection.createChannel();

        String EXCHANGE_NAME = "hospital";
        receptionChannel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);

        String receptionQueueName = receptionChannel.queueDeclare("reception", false, false, false, null).getQueue();
        receptionChannel.queueBind(receptionQueueName, EXCHANGE_NAME, "reception");

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String name;
        String injury;
        String message;
        while(!input.equals("exit")){
            System.out.println("Enter patient name: ");
            name = br.readLine();

            System.out.println("Enter injury: ");
            injury = br.readLine();

            if(name.equals("exit") || injury.equals("exit")){
                input = "exit";
            }

            if(injury.equals("hip") || injury.equals("elbow") || injury.equals("knee")){
                message = name + " " + injury;
                receptionChannel.basicPublish(EXCHANGE_NAME, "doctors", null, message.getBytes(StandardCharsets.UTF_8));
            } else {
                System.out.println("Wrong type of injury, only knee, elbow or hip.");
            }
        }

        receptionChannel.close();
        receptionConnection.close();
    }

}
