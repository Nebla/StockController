package Simulation;

import Error.StockControllerException;
import Order.Order;
import Util.PropertiesManager;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.commons.lang3.SerializationUtils;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Created by adrian on 16/09/15.
 */
public class EmployeeSimulator {

    public static void main(String[] args) throws IOException, TimeoutException, StockControllerException {

        String[] propertiesName = {"queueHost","updateOrderQueueName"};
        Map<String, String> queueNames = PropertiesManager.getProperties(propertiesName);
        String queueHost = queueNames.get("queueHost");
        String queueName = queueNames.get("updateOrderQueueName");

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(queueHost);
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.queueDeclare(queueName, false, false, false, null);

        String[] simulationPropertiesName = {"maxNumberOfOrders","deliverInterval"};
        Map<String, String> simulationValues = PropertiesManager.getSimulationProperties(simulationPropertiesName);
        Integer deliverInterval = Integer.parseInt(simulationValues.get("deliverInterval"));
        Integer maxOrders = Integer.parseInt(simulationValues.get("maxNumberOfOrders"));
        Integer deliveredOrders = 0;

        while (deliveredOrders < maxOrders) {
            Order order = new Order(deliveredOrders.toString(),"",0);
            order.updateStatus(Order.OrderStatus.DELIVERED);

            System.out.println("Delivering: "+deliveredOrders);
            channel.basicPublish("", queueName, null, SerializationUtils.serialize(order));
            deliveredOrders++;
            try {
                Thread.sleep(deliverInterval*1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        channel.close();
        connection.close();

        System.out.println("Finishing employee simulator");
        System.exit(0);
    }
}
