package Simulation;

import Order.Order;
import Util.Util;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.commons.lang3.RandomStringUtils;
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

        String[] simulationPropertiesName = {"maxNumberOfOrders","maxOrderBurst","interBurstsInterval"};
        Map<String, String> simulationValues = Util.getSimulationProperties(simulationPropertiesName);
        Integer maxOrders = Integer.parseInt(simulationValues.get("maxNumberOfOrders"));
        Integer orderBurst = Integer.parseInt(simulationValues.get("maxOrderBurst"));
        Integer burstsInterval = Integer.parseInt(simulationValues.get("interBurstsInterval"));

        Integer sentOrders = 0;

        while (sentOrders < maxOrders) {

            for (int i = 0; i < orderBurst; ++i) {
                Integer orderId = sentOrders;
                sentOrders++;
                Random generator = new Random();
                Integer qty = generator.nextInt(5) + 1;
                String product = RandomStringUtils.randomAlphabetic(1).toUpperCase();
                Order order = new Order(orderId.toString(), product, qty);
                System.out.println("Creating order " + order.getOrderId() + " - " + order.getProductId()+ ":" + order.getProductQty());
                channel.basicPublish("", queueName, null, SerializationUtils.serialize(order));
            }

            try {
                Thread.sleep(burstsInterval*1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        channel.close();
        connection.close();

        System.out.println("Finishing order simulations");
        System.exit(0);
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
