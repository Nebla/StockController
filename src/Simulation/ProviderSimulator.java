package Simulation;

import Stock.NewStock;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.commons.lang3.SerializationUtils;

import java.io.*;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

/**
 * Created by adrian on 16/09/15.
 */
public class ProviderSimulator {

    public static void main(String[] args) throws IOException, TimeoutException {

        System.out.println("Hello World!");

        Properties prop = new Properties();
        InputStream input = null;

        String configFile = "Config/Config.properties";

        input = new FileInputStream(configFile);
        prop.load(input);

        String queueHost = prop.getProperty("queueHost");
        String queueName = prop.getProperty("stockQueueName");

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(queueHost);
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(queueName, false, false, false, null);

        String message = "A";
        while (!message.equals("C")) {

            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("Enter String");
            message = br.readLine();

            NewStock stockMessage = new NewStock(message, 10);

            channel.basicPublish("", queueName, null, SerializationUtils.serialize(stockMessage));
        }
    }

    /*byte[] data = SerializationUtils.serialize(yourObject);
    YourObject yourObject = (YourObject) SerializationUtils.deserialize(byte[] data)*/
}
