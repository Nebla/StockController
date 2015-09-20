package Order;

import Common.CommonQueueManager;
import Util.Util;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import Error.StockControllerException;
import com.rabbitmq.client.DefaultConsumer;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Created by adrian on 20/09/15.
 */
public class OrderRequestManager {
    public static void main(String[] argv) {
        try {
            receiveAndUpdateOrders();
        } catch (StockControllerException e) {
            e.printStackTrace();
        }
    }

    private static void receiveAndUpdateOrders() throws StockControllerException {
        String[] propertiesName = {"queueHost","updateOrderQueueName"};
        Map<String, String> queueNames = Util.getProperties(propertiesName);
        String queueHost = queueNames.get("queueHost");
        String queueName = queueNames.get("updateOrderQueueName");

        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(queueHost);
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            channel.queueDeclare(queueName, false, false, false, null);

            OrderRequestConsumer consumer = new OrderRequestConsumer(channel);
            channel.basicConsume(queueName, false, consumer);
        } catch (TimeoutException e) {
            throw new StockControllerException("There was a timeout while trying to connect to queue server");
        } catch (IOException e) {
            throw new StockControllerException("There was a problem while trying to connect to the I/O device");
        }
    }
}
