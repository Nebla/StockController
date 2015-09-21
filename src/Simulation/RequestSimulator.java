package Simulation;

import Util.Util;
import Order.Order;
import Order.OrderStatusMessage;
import Error.StockControllerException;

import com.rabbitmq.client.*;

import java.util.Map;
import java.util.ArrayList;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import org.apache.commons.lang3.SerializationUtils;

/**
 * Created by adrian on 20/09/15.
 */
public class RequestSimulator {

    private static ArrayList<String> missingOrders;

    public static void main(String[] args) throws IOException, TimeoutException, StockControllerException {

        missingOrders = new ArrayList<String>();

        String[] simulationPropertiesName = {"checkStatusInterval","maxNumberOfOrders"};
        Map<String, String> simulationValues = Util.getSimulationProperties(simulationPropertiesName);
        Integer interval = Integer.parseInt(simulationValues.get("checkStatusInterval"));
        Integer maxOrders = Integer.parseInt(simulationValues.get("maxNumberOfOrders"));

        String[] propertiesName = {"queueHost","orderRequestQueueName","orderResponseQueueName"};
        Map<String, String> queueNames = Util.getProperties(propertiesName);
        String queueHost = queueNames.get("queueHost");
        String requestQueueName = queueNames.get("orderRequestQueueName");
        String responseQueueName = queueNames.get("orderResponseQueueName");

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(queueHost);
        Connection connection = factory.newConnection();

        Channel channel = connection.createChannel();
        channel.queueDeclare(requestQueueName, false, false, false, null);
        channel.queueDeclare(responseQueueName, false, false, false, null);

        Consumer consumer = new DefaultConsumer(channel) {
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] bytes)
                    throws IOException {
                OrderStatusMessage  response = SerializationUtils.deserialize(bytes);
                System.out.println("Order " + response.getOrderId() + " Status " + response.getOrderStatus());
                if (response.getOrderStatus() == Order.OrderStatus.ACCEPTED) {
                    addMissingOrder(response.getOrderId());
                }
            }
        };

        Integer lastOrder = 0;
        while ((lastOrder < maxOrders) || (missingOrders.size() > 0)) {
            String orderId = lastOrder.toString();
            if (missingOrders.size() > 0) {
                orderId = missingOrders.get(0);
                missingOrders.remove(0);
                System.out.println("Requesting order " + orderId + " again");
            }
            else {
                System.out.println("Requesting order " + orderId + " for first time");
                lastOrder++;
            }

            channel.basicPublish("", requestQueueName, null, orderId.getBytes());
            channel.basicConsume(responseQueueName, true, consumer);

            try {
                Thread.sleep(interval*1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        channel.close();
        connection.close();
        System.out.println("Finishing request simulations");
        System.exit(0);
    }

    public static void addMissingOrder(String orderId) {
        missingOrders.add(orderId);
    }
}
