package Simulation;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.*;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

/**
 * Created by adrian on 16/09/15.
 */
public class OrderSimulator {

    public static void main(String[] args) throws IOException, TimeoutException {
        Properties prop = new Properties();
        InputStream input = null;

        String configFile = "Config/Config.properties";

        input = new FileInputStream(configFile);
        prop.load(input);

        String queueHost = prop.getProperty("queueHost");
        String queueName = prop.getProperty("auditoryQueueName");

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(queueHost);
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

/*queue - the name of the queue
durable - true if we are declaring a durable queue (the queue will survive a server restart)
exclusive - true if we are declaring an exclusive queue (restricted to this connection)
autoDelete - true if we are declaring an autodelete queue (server will delete it when no longer in use)
arguments - other properties (construction arguments) for the queue*/
        channel.queueDeclare(queueName, false, false, false, null);

        String message = "A";
        while (!message.equals("C")) {

            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("Enter String");
            message = br.readLine();

            channel.basicPublish("", queueName, null, message.getBytes());
        }
    }

}