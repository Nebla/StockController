package Simulation;

import Stock.Stock;
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

        Properties prop = new Properties();
        InputStream input;

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

        while (! Thread.interrupted()) {

            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("Enter Product: ");
            String message = br.readLine();

            Stock stockMessage = new Stock(message, 10);

            channel.basicPublish("", queueName, null, SerializationUtils.serialize(stockMessage));
        }
    }

    /*byte[] data = SerializationUtils.serialize(yourObject);
    YourObject yourObject = (YourObject) SerializationUtils.deserialize(byte[] data)*/
}
