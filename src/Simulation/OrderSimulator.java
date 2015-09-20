package Simulation;

import Order.Order;
import Util.Util;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.commons.lang3.SerializationUtils;

import java.io.*;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.TimeoutException;
import Error.StockControllerException;

/**
 * Created by adrian on 16/09/15.
 */
public class OrderSimulator {

    public static void main(String[] args) throws IOException, TimeoutException, StockControllerException {

        String[] propertiesName = {"queueHost","newOrderQueueName"};
        Map<String, String> queueNames = Util.getProperties(propertiesName);
        String queueHost = queueNames.get("queueHost");
        String queueName = queueNames.get("newOrderQueueName");

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(queueHost);
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(queueName, false, false, false, null);

        Integer orderId = 0;

        while (! Thread.interrupted()) {

            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("Enter Product: ");
            String product = br.readLine();

            Random generator = new Random();
            Integer qty = generator.nextInt(5) + 1;

            Order order = new Order(orderId.toString(), product, qty);

            channel.basicPublish("", queueName, null, SerializationUtils.serialize(order));

            orderId++;
        }
    }

}


/*queue - the name of the queue
durable - true if we are declaring a durable queue (the queue will survive a server restart)
exclusive - true if we are declaring an exclusive queue (restricted to this connection)
autoDelete - true if we are declaring an autodelete queue (server will delete it when no longer in use)
arguments - other properties (construction arguments) for the queue

channel.queueDeclare(queueName, durable, exclusive, autoDelete, arguments);
channel.queueDeclare(queueName, false, false, false, null);
*/
